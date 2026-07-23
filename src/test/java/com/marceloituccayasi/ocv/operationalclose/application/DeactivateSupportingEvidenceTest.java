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
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.SupportingEvidenceRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
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
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceLegibilityStatus;

class DeactivateSupportingEvidenceTest {

    private static final UUID CLOSE_UUID =
            UUID.fromString(
                    "00d8615c-b833-4f05-8155-816130100001");

    private static final UUID EVENT_UUID =
            UUID.fromString(
                    "00d8615c-b833-4f05-8155-816130100002");

    private static final UUID EVIDENCE_UUID =
            UUID.fromString(
                    "00d8615c-b833-4f05-8155-816130100003");

    private static final UUID EVENT_TRANSITION_UUID =
            UUID.fromString(
                    "00d8615c-b833-4f05-8155-816130100004");

    private static final UUID CLOSE_TRANSITION_UUID =
            UUID.fromString(
                    "00d8615c-b833-4f05-8155-816130100005");

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    CLOSE_UUID);

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    EVENT_UUID);

    private static final SupportingEvidenceId EVIDENCE_ID =
            new SupportingEvidenceId(
                    EVIDENCE_UUID);

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-23T08:00:00Z");

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

    private final SupportingEvidenceRepository
            evidenceRepository =
                    mock(
                            SupportingEvidenceRepository.class);

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

    private final DeactivateSupportingEvidence useCase =
            new DeactivateSupportingEvidence(
                    closeLockRepository,
                    eventRevisionRepository,
                    evidenceRepository,
                    currentActorProvider,
                    applicationClock,
                    transactionRunner,
                    revisionCoordinator);

    @Test
    void deactivatesEvidenceAfterLockingCloseEventAndEvidence() {
        OperationalClose operationalClose =
                closeWithState(
                        OperationalCloseState.PREPARATION);

        OperationalEvent operationalEvent =
                eventWithState(
                        OperationalEventState.REGISTERED);

        SupportingEvidence supportingEvidence =
                activeEvidence();

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
                evidenceRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID,
                        EVIDENCE_ID))
                .thenReturn(
                        Optional.of(
                                supportingEvidence));

        when(
                applicationClock.now())
                .thenReturn(
                        NOW);

        DeactivateSupportingEvidenceResult result =
                useCase.execute(
                        validCommand());

        ArgumentCaptor<SupportingEvidence>
                evidenceCaptor =
                        ArgumentCaptor.forClass(
                                SupportingEvidence.class);

        ArgumentCaptor<OperationalEvent>
                eventCaptor =
                        ArgumentCaptor.forClass(
                                OperationalEvent.class);

        verify(
                evidenceRepository)
                .saveRevision(
                        evidenceCaptor.capture());

        verify(
                eventRevisionRepository)
                .saveRevision(
                        eventCaptor.capture());

        SupportingEvidence deactivatedEvidence =
                evidenceCaptor.getValue();

        OperationalEvent revisedEvent =
                eventCaptor.getValue();

        assertThat(result.status())
                .isEqualTo(
                        DeactivateSupportingEvidenceResult.Status
                                .DEACTIVATED);

        assertThat(result.evidenceId())
                .isEqualTo(
                        EVIDENCE_UUID);

        assertThat(result.message())
                .isNull();

        assertThat(deactivatedEvidence.id())
                .isEqualTo(
                        EVIDENCE_ID);

        assertThat(deactivatedEvidence.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(deactivatedEvidence.active())
                .isFalse();

        assertThat(deactivatedEvidence.revision())
                .isEqualTo(2L);

        assertThat(deactivatedEvidence.deactivatedAt())
                .isEqualTo(
                        NOW);

        assertThat(deactivatedEvidence.updatedAt())
                .isEqualTo(
                        NOW);

        assertThat(deactivatedEvidence.updatedBy())
                .isEqualTo(
                        ACTOR);

        assertThat(deactivatedEvidence.createdAt())
                .isEqualTo(
                        CREATED_AT);

        assertThat(deactivatedEvidence.createdBy())
                .isEqualTo(
                        ACTOR);

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
                        evidenceRepository,
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
                evidenceRepository)
                .findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID,
                        EVIDENCE_ID);

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
                evidenceRepository)
                .saveRevision(
                        deactivatedEvidence);
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
                evidenceRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID,
                        EVIDENCE_ID))
                .thenReturn(
                        Optional.of(
                                activeEvidence()));

        when(
                applicationClock.now())
                .thenReturn(
                        NOW);

        when(
                uuidGenerator.next())
                .thenReturn(
                        EVENT_TRANSITION_UUID,
                        CLOSE_TRANSITION_UUID);

        DeactivateSupportingEvidenceResult result =
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
                        DeactivateSupportingEvidenceResult.Status
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
                evidenceRepository)
                .saveRevision(
                        any());

        verify(
                uuidGenerator,
                times(2))
                .next();
    }

    @Test
    void rejectsSentCloseBeforeLockingEventOrEvidence() {
        stubResponsibleActor();

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                closeWithState(
                                        OperationalCloseState
                                                .SENT_TO_ACCOUNTING)));

        DeactivateSupportingEvidenceResult result =
                useCase.execute(
                        validCommand());

        assertThat(result.status())
                .isEqualTo(
                        DeactivateSupportingEvidenceResult.Status
                                .CLOSE_NOT_EDITABLE);

        verifyNoInteractions(
                eventRevisionRepository,
                evidenceRepository,
                applicationClock,
                dependentResultInvalidator,
                closeRevisionRepository,
                uuidGenerator);
    }

    @Test
    void returnsEventNotFoundWithoutLockingEvidence() {
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

        DeactivateSupportingEvidenceResult result =
                useCase.execute(
                        validCommand());

        assertThat(result.status())
                .isEqualTo(
                        DeactivateSupportingEvidenceResult.Status
                                .EVENT_NOT_FOUND);

        verifyNoInteractions(
                evidenceRepository,
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
    void returnsEvidenceNotFoundWithoutRevisionOrClockAccess() {
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
                evidenceRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID,
                        EVIDENCE_ID))
                .thenReturn(
                        Optional.empty());

        DeactivateSupportingEvidenceResult result =
                useCase.execute(
                        validCommand());

        assertThat(result.status())
                .isEqualTo(
                        DeactivateSupportingEvidenceResult.Status
                                .EVIDENCE_NOT_FOUND);

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
                evidenceRepository,
                never())
                .saveRevision(
                        any());
    }

    @Test
    void returnsAlreadyInactiveWithoutRevisingEventOrEvidence() {
        SupportingEvidence inactiveEvidence =
                activeEvidence()
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
                evidenceRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID,
                        EVIDENCE_ID))
                .thenReturn(
                        Optional.of(
                                inactiveEvidence));

        DeactivateSupportingEvidenceResult result =
                useCase.execute(
                        validCommand());

        assertThat(result.status())
                .isEqualTo(
                        DeactivateSupportingEvidenceResult.Status
                                .EVIDENCE_ALREADY_INACTIVE);

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
                evidenceRepository,
                never())
                .saveRevision(
                        any());
    }

    @Test
    void rejectsInvalidInputBeforeAcquiringDatabaseLocks() {
        stubResponsibleActor();

        DeactivateSupportingEvidenceCommand command =
                new DeactivateSupportingEvidenceCommand(
                        CLOSE_UUID,
                        EVENT_UUID,
                        null);

        DeactivateSupportingEvidenceResult result =
                useCase.execute(
                        command);

        assertThat(result.status())
                .isEqualTo(
                        DeactivateSupportingEvidenceResult.Status
                                .INVALID_INPUT);

        assertThat(result.message())
                .isEqualTo(
                        "evidenceId must not be null");

        verifyNoInteractions(
                closeLockRepository,
                eventRevisionRepository,
                evidenceRepository,
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

        DeactivateSupportingEvidenceResult result =
                useCase.execute(
                        validCommand());

        assertThat(result.status())
                .isEqualTo(
                        DeactivateSupportingEvidenceResult.Status
                                .ACTOR_REJECTED);

        verifyNoInteractions(
                closeLockRepository,
                eventRevisionRepository,
                evidenceRepository,
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

    private static DeactivateSupportingEvidenceCommand
            validCommand() {

        return new DeactivateSupportingEvidenceCommand(
                CLOSE_UUID,
                EVENT_UUID,
                EVIDENCE_UUID);
    }

    private static AuthenticatedPrincipal
            responsiblePrincipal() {

        return new AuthenticatedPrincipal(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
    }

    private static SupportingEvidence activeEvidence() {
        return SupportingEvidence.create(
                EVIDENCE_ID,
                EVENT_ID,
                "Receipt",
                "reference:receipt-001",
                new BigDecimal(
                        "80.0000"),
                LocalDate.of(
                        2026,
                        7,
                        23),
                SupportingEvidenceLegibilityStatus.LEGIBLE,
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
                true,
                false,
                1L,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

}