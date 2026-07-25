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
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceLegibilityStatus;

class CreateSupportingEvidenceTest {

    private static final UUID CLOSE_UUID =
            UUID.fromString(
                    "74dd0a86-bde7-4529-8a92-658725100001");

    private static final UUID EVENT_UUID =
            UUID.fromString(
                    "74dd0a86-bde7-4529-8a92-658725100002");

    private static final UUID EVIDENCE_UUID =
            UUID.fromString(
                    "74dd0a86-bde7-4529-8a92-658725100003");

    private static final UUID EVENT_TRANSITION_UUID =
            UUID.fromString(
                    "74dd0a86-bde7-4529-8a92-658725100004");

    private static final UUID CLOSE_TRANSITION_UUID =
            UUID.fromString(
                    "74dd0a86-bde7-4529-8a92-658725100005");

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    CLOSE_UUID);

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    EVENT_UUID);

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

    private final CreateSupportingEvidence useCase =
            new CreateSupportingEvidence(
                    closeLockRepository,
                    eventRevisionRepository,
                    evidenceRepository,
                    currentActorProvider,
                    applicationClock,
                    uuidGenerator,
                    transactionRunner,
                    revisionCoordinator);

    @Test
    void createsEvidenceAfterLockingCloseAndEventAndRevisesEventOnce() {
        OperationalClose operationalClose =
                closeWithState(
                        OperationalCloseState.PREPARATION);

        OperationalEvent operationalEvent =
                eventWithState(
                        OperationalEventState.REGISTERED);

        when(
                currentActorProvider.currentActor())
                .thenReturn(
                        responsiblePrincipal());

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
                applicationClock.now())
                .thenReturn(
                        NOW);

        when(
                uuidGenerator.next())
                .thenReturn(
                        EVIDENCE_UUID);

        CreateSupportingEvidenceResult result =
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
                .saveNew(
                        evidenceCaptor.capture());

        verify(
                eventRevisionRepository)
                .saveRevision(
                        eventCaptor.capture());

        SupportingEvidence evidence =
                evidenceCaptor.getValue();

        OperationalEvent revisedEvent =
                eventCaptor.getValue();

        assertThat(result.status())
                .isEqualTo(
                        CreateSupportingEvidenceResult.Status.CREATED);

        assertThat(result.evidenceId())
                .isEqualTo(
                        EVIDENCE_UUID);

        assertThat(result.message())
                .isNull();

        assertThat(
                evidence.id()
                        .value())
                .isEqualTo(
                        EVIDENCE_UUID);

        assertThat(evidence.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(evidence.evidenceType())
                .isEqualTo(
                        "Receipt");

        assertThat(evidence.contentReference())
                .isEqualTo(
                        "reference:receipt-001");

        assertThat(evidence.supportedAmount())
                .isEqualByComparingTo(
                        "80.0000");

        assertThat(evidence.evidenceDate())
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                7,
                                23));

        assertThat(evidence.legibilityStatus())
                .isEqualTo(
                        SupportingEvidenceLegibilityStatus.LEGIBLE);

        assertThat(evidence.active())
                .isTrue();

        assertThat(evidence.revision())
                .isEqualTo(1L);

        assertThat(evidence.createdAt())
                .isEqualTo(
                        NOW);

        assertThat(evidence.createdBy())
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
                closeRevisionRepository);

        InOrder order =
                inOrder(
                        closeLockRepository,
                        eventRevisionRepository,
                        dependentResultInvalidator,
                        evidenceRepository);

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
                .saveNew(
                        evidence);
    }

    @Test
    void resetsValidatedEventAndBlocksValidatedClose() {
        OperationalClose operationalClose =
                closeWithState(
                        OperationalCloseState.VALIDATED);

        OperationalEvent operationalEvent =
                eventWithState(
                        OperationalEventState.VALIDATED);

        when(
                currentActorProvider.currentActor())
                .thenReturn(
                        responsiblePrincipal());

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
                applicationClock.now())
                .thenReturn(
                        NOW);

        when(
                uuidGenerator.next())
                .thenReturn(
                        EVIDENCE_UUID,
                        EVENT_TRANSITION_UUID,
                        CLOSE_TRANSITION_UUID);

        CreateSupportingEvidenceResult result =
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
                        CreateSupportingEvidenceResult.Status.CREATED);

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
                .saveNew(
                        any());

        verify(
                uuidGenerator,
                times(3))
                .next();
    }

    @Test
    void rejectsSentCloseWithoutLockingEventOrWritingEvidence() {
        when(
                currentActorProvider.currentActor())
                .thenReturn(
                        responsiblePrincipal());

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                closeWithState(
                                        OperationalCloseState
                                                .SENT_TO_ACCOUNTING)));

        CreateSupportingEvidenceResult result =
                useCase.execute(
                        validCommand());

        assertThat(result.status())
                .isEqualTo(
                        CreateSupportingEvidenceResult.Status
                                .CLOSE_NOT_EDITABLE);

        verifyNoInteractions(
                eventRevisionRepository,
                evidenceRepository,
                applicationClock,
                uuidGenerator,
                dependentResultInvalidator,
                closeRevisionRepository);
    }

    @Test
    void returnsEventNotFoundWithoutGeneratingEvidenceOrRevision() {
        when(
                currentActorProvider.currentActor())
                .thenReturn(
                        responsiblePrincipal());

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

        CreateSupportingEvidenceResult result =
                useCase.execute(
                        validCommand());

        assertThat(result.status())
                .isEqualTo(
                        CreateSupportingEvidenceResult.Status
                                .EVENT_NOT_FOUND);

        verify(
                eventRevisionRepository,
                never())
                .saveRevision(
                        any());

        verify(
                eventRevisionRepository,
                never())
                .appendStateTransition(
                        any());

        verifyNoInteractions(
                evidenceRepository,
                applicationClock,
                uuidGenerator,
                dependentResultInvalidator,
                closeRevisionRepository);
    }

    @Test
    void rejectsInvalidLegibilityBeforeAcquiringDatabaseLocks() {
        when(
                currentActorProvider.currentActor())
                .thenReturn(
                        responsiblePrincipal());

        CreateSupportingEvidenceCommand command =
                new CreateSupportingEvidenceCommand(
                        CLOSE_UUID,
                        EVENT_UUID,
                        "Receipt",
                        "reference:receipt-001",
                        new BigDecimal(
                                "80.0000"),
                        LocalDate.of(
                                2026,
                                7,
                                23),
                        "UNKNOWN");

        CreateSupportingEvidenceResult result =
                useCase.execute(
                        command);

        assertThat(result.status())
                .isEqualTo(
                        CreateSupportingEvidenceResult.Status
                                .INVALID_INPUT);

        assertThat(result.message())
                .isEqualTo(
                        "legibility status is invalid");

        verifyNoInteractions(
                closeLockRepository,
                eventRevisionRepository,
                evidenceRepository,
                applicationClock,
                uuidGenerator,
                dependentResultInvalidator,
                closeRevisionRepository);
    }

    @Test
    void rejectsUnauthorizedActorBeforeAcquiringDatabaseLocks() {
        when(
                currentActorProvider.currentActor())
                .thenReturn(
                        new AuthenticatedPrincipal(
                                "other-role",
                                "other-user"));

        CreateSupportingEvidenceResult result =
                useCase.execute(
                        validCommand());

        assertThat(result.status())
                .isEqualTo(
                        CreateSupportingEvidenceResult.Status
                                .ACTOR_REJECTED);

        verifyNoInteractions(
                closeLockRepository,
                eventRevisionRepository,
                evidenceRepository,
                applicationClock,
                uuidGenerator,
                dependentResultInvalidator,
                closeRevisionRepository);
    }

    private static CreateSupportingEvidenceCommand
            validCommand() {

        return new CreateSupportingEvidenceCommand(
                CLOSE_UUID,
                EVENT_UUID,
                "  Receipt  ",
                "reference:receipt-001",
                new BigDecimal(
                        "80.0000"),
                LocalDate.of(
                        2026,
                        7,
                        23),
                "legible");
    }

    private static AuthenticatedPrincipal
            responsiblePrincipal() {

        return new AuthenticatedPrincipal(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
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