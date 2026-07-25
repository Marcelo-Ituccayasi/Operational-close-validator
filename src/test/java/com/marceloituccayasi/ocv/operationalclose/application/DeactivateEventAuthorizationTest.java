package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventDependentResultInvalidator;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventAuthorizationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorizationId;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

class DeactivateEventAuthorizationTest {

    private static final UUID CLOSE_UUID =
            UUID.fromString(
                    "7d66a676-ce3a-4757-a275-467865100001");

    private static final UUID EVENT_UUID =
            UUID.fromString(
                    "7d66a676-ce3a-4757-a275-467865100002");

    private static final UUID AUTHORIZATION_UUID =
            UUID.fromString(
                    "7d66a676-ce3a-4757-a275-467865100003");

    private static final UUID EVENT_TRANSITION_UUID =
            UUID.fromString(
                    "7d66a676-ce3a-4757-a275-467865100004");

    private static final UUID CLOSE_TRANSITION_UUID =
            UUID.fromString(
                    "7d66a676-ce3a-4757-a275-467865100005");

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    CLOSE_UUID);

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    EVENT_UUID);

    private static final EventAuthorizationId AUTHORIZATION_ID =
            new EventAuthorizationId(
                    AUTHORIZATION_UUID);

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-23T08:00:00Z");

    private static final Instant AUTHORIZED_AT =
            Instant.parse(
                    "2026-07-23T08:30:00Z");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-23T10:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final OperationalCloseLockRepository
            closeLockRepository =
                    mock(
                            OperationalCloseLockRepository.class);

    private final OperationalEventRevisionRepository
            eventRevisionRepository =
                    mock(
                            OperationalEventRevisionRepository.class);

    private final EventAuthorizationRepository
            authorizationRepository =
                    mock(
                            EventAuthorizationRepository.class);

    private final CurrentActorProvider
            currentActorProvider =
                    mock(
                            CurrentActorProvider.class);

    private final ApplicationClock
            applicationClock =
                    mock(
                            ApplicationClock.class);

    private final UuidGenerator
            uuidGenerator =
                    mock(
                            UuidGenerator.class);

    private final OperationalCloseRevisionRepository
            closeRevisionRepository =
                    mock(
                            OperationalCloseRevisionRepository.class);

    private final OperationalEventDependentResultInvalidator
            dependentResultInvalidator =
                    mock(
                            OperationalEventDependentResultInvalidator.class);

    private final TransactionRunner transactionRunner =
            new TransactionRunner() {

                @Override
                public <T> T execute(
                        Supplier<T> operation) {

                    return Objects.requireNonNull(
                            operation)
                            .get();
                }
            };

    private final OperationalDependencyRevisionCoordinator
            revisionCoordinator =
                    new OperationalDependencyRevisionCoordinator(
                            eventRevisionRepository,
                            closeRevisionRepository,
                            dependentResultInvalidator,
                            uuidGenerator);

    private final DeactivateEventAuthorization useCase =
            new DeactivateEventAuthorization(
                    closeLockRepository,
                    eventRevisionRepository,
                    authorizationRepository,
                    currentActorProvider,
                    applicationClock,
                    transactionRunner,
                    revisionCoordinator);

    @Test
    void deactivatesAuthorizationAfterLockingCloseEventAndAuthorization() {
        OperationalClose operationalClose =
                closeWithState(
                        OperationalCloseState.PREPARATION);

        OperationalEvent operationalEvent =
                eventWithState(
                        OperationalEventState.REGISTERED);

        EventAuthorization authorization =
                activeAuthorization();

        stubResponsibleActor();

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                operationalClose));

        when(
                eventRevisionRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                operationalEvent));

        when(
                authorizationRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID,
                        AUTHORIZATION_ID))
                .thenReturn(
                        Optional.of(
                                authorization));

        when(
                applicationClock.now())
                .thenReturn(
                        NOW);

        DeactivateEventAuthorizationResult result =
                useCase.execute(
                        validCommand());

        ArgumentCaptor<EventAuthorization>
                authorizationCaptor =
                        ArgumentCaptor.forClass(
                                EventAuthorization.class);

        ArgumentCaptor<OperationalEvent>
                eventCaptor =
                        ArgumentCaptor.forClass(
                                OperationalEvent.class);

        verify(
                authorizationRepository)
                .saveRevision(
                        authorizationCaptor.capture());

        verify(
                eventRevisionRepository)
                .saveRevision(
                        eventCaptor.capture());

        EventAuthorization deactivatedAuthorization =
                authorizationCaptor.getValue();

        OperationalEvent revisedEvent =
                eventCaptor.getValue();

        assertThat(result.status())
                .isEqualTo(
                        DeactivateEventAuthorizationResult.Status
                                .DEACTIVATED);

        assertThat(result.authorizationId())
                .isEqualTo(
                        AUTHORIZATION_UUID);

        assertThat(result.message())
                .isNull();

        assertThat(deactivatedAuthorization.id())
                .isEqualTo(
                        AUTHORIZATION_ID);

        assertThat(deactivatedAuthorization.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(deactivatedAuthorization.active())
                .isFalse();

        assertThat(deactivatedAuthorization.revision())
                .isEqualTo(2L);

        assertThat(deactivatedAuthorization.deactivatedAt())
                .isEqualTo(
                        NOW);

        assertThat(deactivatedAuthorization.updatedAt())
                .isEqualTo(
                        NOW);

        assertThat(deactivatedAuthorization.updatedBy())
                .isEqualTo(
                        ACTOR);

        assertThat(deactivatedAuthorization.createdAt())
                .isEqualTo(
                        CREATED_AT);

        assertThat(deactivatedAuthorization.createdBy())
                .isEqualTo(
                        ACTOR);

        assertThat(deactivatedAuthorization.authorizedByName())
                .isEqualTo(
                        "Operations Manager");

        assertThat(deactivatedAuthorization.reason())
                .isEqualTo(
                        "Exceptional operational expense");

        assertThat(deactivatedAuthorization.authorizedAt())
                .isEqualTo(
                        AUTHORIZED_AT);

        assertThat(deactivatedAuthorization.formalReference())
                .isEqualTo(
                        "AUTH-2026-0001");

        assertThat(
                revisedEvent.dataRevision())
                .isEqualTo(2L);

        assertThat(revisedEvent.state())
                .isEqualTo(
                        OperationalEventState.REGISTERED);

        assertThat(revisedEvent.updatedAt())
                .isEqualTo(
                        NOW);

        assertThat(revisedEvent.updatedBy())
                .isEqualTo(
                        ACTOR);

        verify(
                dependentResultInvalidator)
                .invalidateForRevisions(
                        CLOSE_ID,
                        List.of(
                                EVENT_ID));

        verify(
                eventRevisionRepository,
                never())
                .appendStateTransition(
                        any());

        verifyNoInteractions(
                closeRevisionRepository,
                uuidGenerator);

        InOrder order =
                inOrder(
                        closeLockRepository,
                        eventRevisionRepository,
                        authorizationRepository,
                        dependentResultInvalidator);

        order.verify(
                closeLockRepository)
                .findByIdForUpdate(
                        CLOSE_ID);

        order.verify(
                eventRevisionRepository)
                .findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID);

        order.verify(
                authorizationRepository)
                .findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID,
                        AUTHORIZATION_ID);

        order.verify(
                dependentResultInvalidator)
                .invalidateForRevisions(
                        CLOSE_ID,
                        List.of(
                                EVENT_ID));

        order.verify(
                eventRevisionRepository)
                .saveRevision(
                        revisedEvent);

        order.verify(
                authorizationRepository)
                .saveRevision(
                        deactivatedAuthorization);
    }

    @Test
    void resetsValidatedEventAndBlocksValidatedClose() {
        stubResponsibleActor();

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                closeWithState(
                                        OperationalCloseState.VALIDATED)));

        when(
                eventRevisionRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                eventWithState(
                                        OperationalEventState.VALIDATED)));

        when(
                authorizationRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID,
                        AUTHORIZATION_ID))
                .thenReturn(
                        Optional.of(
                                activeAuthorization()));

        when(
                applicationClock.now())
                .thenReturn(
                        NOW);

        when(
                uuidGenerator.next())
                .thenReturn(
                        EVENT_TRANSITION_UUID,
                        CLOSE_TRANSITION_UUID);

        DeactivateEventAuthorizationResult result =
                useCase.execute(
                        validCommand());

        ArgumentCaptor<OperationalEvent>
                eventCaptor =
                        ArgumentCaptor.forClass(
                                OperationalEvent.class);

        ArgumentCaptor<OperationalClose>
                closeCaptor =
                        ArgumentCaptor.forClass(
                                OperationalClose.class);

        verify(
                eventRevisionRepository)
                .saveRevision(
                        eventCaptor.capture());

        verify(
                closeRevisionRepository)
                .saveRevision(
                        closeCaptor.capture());

        assertThat(result.status())
                .isEqualTo(
                        DeactivateEventAuthorizationResult.Status
                                .DEACTIVATED);

        assertThat(
                eventCaptor.getValue()
                        .state())
                .isEqualTo(
                        OperationalEventState.REGISTERED);

        assertThat(
                eventCaptor.getValue()
                        .dataRevision())
                .isEqualTo(2L);

        assertThat(
                closeCaptor.getValue()
                        .state())
                .isEqualTo(
                        OperationalCloseState.BLOCKED);

        verify(
                eventRevisionRepository)
                .appendStateTransition(
                        any());

        verify(
                closeRevisionRepository)
                .appendStateTransition(
                        any());

        verify(
                authorizationRepository)
                .saveRevision(
                        any());

        verify(
                uuidGenerator,
                times(2))
                .next();
    }

    @Test
    void rejectsSentCloseBeforeLockingEventOrAuthorization() {
        stubResponsibleActor();

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                closeWithState(
                                        OperationalCloseState
                                                .SENT_TO_ACCOUNTING)));

        DeactivateEventAuthorizationResult result =
                useCase.execute(
                        validCommand());

        assertThat(result.status())
                .isEqualTo(
                        DeactivateEventAuthorizationResult.Status
                                .CLOSE_NOT_EDITABLE);

        verifyNoInteractions(
                eventRevisionRepository,
                authorizationRepository,
                applicationClock,
                dependentResultInvalidator,
                closeRevisionRepository,
                uuidGenerator);
    }

    @Test
    void returnsEventNotFoundWithoutLockingAuthorization() {
        stubResponsibleActor();

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                closeWithState(
                                        OperationalCloseState.PREPARATION)));

        when(
                eventRevisionRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        Optional.empty());

        DeactivateEventAuthorizationResult result =
                useCase.execute(
                        validCommand());

        assertThat(result.status())
                .isEqualTo(
                        DeactivateEventAuthorizationResult.Status
                                .EVENT_NOT_FOUND);

        verifyNoInteractions(
                authorizationRepository,
                applicationClock,
                dependentResultInvalidator,
                closeRevisionRepository,
                uuidGenerator);

        verify(
                eventRevisionRepository,
                never())
                .saveRevision(
                        any());
    }

    @Test
    void returnsAuthorizationNotFoundWithoutRevisionOrClockAccess() {
        stubResponsibleActor();

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                closeWithState(
                                        OperationalCloseState.PREPARATION)));

        when(
                eventRevisionRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                eventWithState(
                                        OperationalEventState.REGISTERED)));

        when(
                authorizationRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID,
                        AUTHORIZATION_ID))
                .thenReturn(
                        Optional.empty());

        DeactivateEventAuthorizationResult result =
                useCase.execute(
                        validCommand());

        assertThat(result.status())
                .isEqualTo(
                        DeactivateEventAuthorizationResult.Status
                                .AUTHORIZATION_NOT_FOUND);

        verifyNoInteractions(
                applicationClock,
                dependentResultInvalidator,
                closeRevisionRepository,
                uuidGenerator);

        verify(
                eventRevisionRepository,
                never())
                .saveRevision(
                        any());

        verify(
                authorizationRepository,
                never())
                .saveRevision(
                        any());
    }

    @Test
    void returnsAlreadyInactiveWithoutRevisingEventOrAuthorization() {
        EventAuthorization inactiveAuthorization =
                activeAuthorization()
                        .deactivate(
                                CREATED_AT.plusSeconds(
                                        300),
                                ACTOR);

        stubResponsibleActor();

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                closeWithState(
                                        OperationalCloseState.PREPARATION)));

        when(
                eventRevisionRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                eventWithState(
                                        OperationalEventState.REGISTERED)));

        when(
                authorizationRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID,
                        AUTHORIZATION_ID))
                .thenReturn(
                        Optional.of(
                                inactiveAuthorization));

        DeactivateEventAuthorizationResult result =
                useCase.execute(
                        validCommand());

        assertThat(result.status())
                .isEqualTo(
                        DeactivateEventAuthorizationResult.Status
                                .AUTHORIZATION_ALREADY_INACTIVE);

        verifyNoInteractions(
                applicationClock,
                dependentResultInvalidator,
                closeRevisionRepository,
                uuidGenerator);

        verify(
                eventRevisionRepository,
                never())
                .saveRevision(
                        any());

        verify(
                authorizationRepository,
                never())
                .saveRevision(
                        any());
    }

    @Test
    void rejectsInvalidInputBeforeAcquiringDatabaseLocks() {
        stubResponsibleActor();

        DeactivateEventAuthorizationCommand command =
                new DeactivateEventAuthorizationCommand(
                        CLOSE_UUID,
                        EVENT_UUID,
                        null);

        DeactivateEventAuthorizationResult result =
                useCase.execute(
                        command);

        assertThat(result.status())
                .isEqualTo(
                        DeactivateEventAuthorizationResult.Status
                                .INVALID_INPUT);

        assertThat(result.message())
                .isEqualTo(
                        "authorizationId must not be null");

        verifyNoInteractions(
                closeLockRepository,
                eventRevisionRepository,
                authorizationRepository,
                applicationClock,
                dependentResultInvalidator,
                closeRevisionRepository,
                uuidGenerator);
    }

    @Test
    void rejectsUnauthorizedActorBeforeAcquiringDatabaseLocks() {
        when(
                currentActorProvider.currentActor())
                .thenReturn(
                        new AuthenticatedPrincipal(
                                "other-role",
                                "other-user"));

        DeactivateEventAuthorizationResult result =
                useCase.execute(
                        validCommand());

        assertThat(result.status())
                .isEqualTo(
                        DeactivateEventAuthorizationResult.Status
                                .ACTOR_REJECTED);

        verifyNoInteractions(
                closeLockRepository,
                eventRevisionRepository,
                authorizationRepository,
                applicationClock,
                dependentResultInvalidator,
                closeRevisionRepository,
                uuidGenerator);
    }

    private void stubResponsibleActor() {
        when(
                currentActorProvider.currentActor())
                .thenReturn(
                        responsiblePrincipal());
    }

    private static DeactivateEventAuthorizationCommand validCommand() {
        return new DeactivateEventAuthorizationCommand(
                CLOSE_UUID,
                EVENT_UUID,
                AUTHORIZATION_UUID);
    }

    private static AuthenticatedPrincipal responsiblePrincipal() {
        return new AuthenticatedPrincipal(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
    }

    private static EventAuthorization activeAuthorization() {
        return EventAuthorization.create(
                AUTHORIZATION_ID,
                EVENT_ID,
                "Operations Manager",
                "Exceptional operational expense",
                AUTHORIZED_AT,
                "AUTH-2026-0001",
                CREATED_AT,
                ACTOR);
    }

    private static OperationalClose closeWithState(
            OperationalCloseState state) {

        return new OperationalClose(
                CLOSE_ID,
                new OperationalPeriod(
                        LocalDate.of(
                                2026,
                                7,
                                1),
                        LocalDate.of(
                                2026,
                                7,
                                31)),
                new CurrencyCode(
                        "PEN"),
                new InitialBalance(
                        new BigDecimal(
                                "1000.0000")),
                state,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

    private static OperationalEvent eventWithState(
            OperationalEventState state) {

        return new OperationalEvent(
                EVENT_ID,
                CLOSE_ID,
                OperationalEventType.EXPENSE,
                new OperationalEventAmount(
                        new BigDecimal(
                                "80.0000")),
                new BigDecimal(
                        "-80.0000"),
                null,
                CREATED_AT.minusSeconds(
                        60),
                CREATED_AT,
                "Caja principal",
                "Gasto operativo",
                state,
                false,
                true,
                1L,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

}