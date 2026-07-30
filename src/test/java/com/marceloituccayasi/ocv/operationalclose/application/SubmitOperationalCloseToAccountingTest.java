package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.AccountingSubmissionAttemptRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.CloseValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.ConsolidationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationAlertRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttempt;
import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttemptId;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.Consolidation;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
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
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueType;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

class SubmitOperationalCloseToAccountingTest {

    private static final UUID CLOSE_UUID =
            uuid(
                    "cd000000-0000-0000-0000-000000000001");

    private static final UUID EVENT_UUID =
            uuid(
                    "cd000000-0000-0000-0000-000000000002");

    private static final UUID CONSOLIDATION_UUID =
            uuid(
                    "cd000000-0000-0000-0000-000000000003");

    private static final UUID EVENT_RESULT_UUID =
            uuid(
                    "cd000000-0000-0000-0000-000000000004");

    private static final UUID PREVIOUS_VR008_UUID =
            uuid(
                    "cd000000-0000-0000-0000-000000000005");

    private static final UUID NEW_VR008_UUID =
            uuid(
                    "cd000000-0000-0000-0000-000000000006");

    private static final UUID ATTEMPT_UUID =
            uuid(
                    "cd000000-0000-0000-0000-000000000007");

    private static final UUID ISSUE_UUID =
            uuid(
                    "cd000000-0000-0000-0000-000000000008");

    private static final UUID TRANSITION_UUID =
            uuid(
                    "cd000000-0000-0000-0000-000000000009");

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    CLOSE_UUID);

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    EVENT_UUID);

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    CONSOLIDATION_UUID);

    private static final ValidationResultId NEW_VR008_ID =
            new ValidationResultId(
                    NEW_VR008_UUID);

    private static final AccountingSubmissionAttemptId ATTEMPT_ID =
            new AccountingSubmissionAttemptId(
                    ATTEMPT_UUID);

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-30T12:00:00Z");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-30T15:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final OperationalCloseLockRepository
            closeLockRepository =
                    mock(
                            OperationalCloseLockRepository.class);

    private final OperationalCloseRevisionRepository
            closeRevisionRepository =
                    mock(
                            OperationalCloseRevisionRepository.class);

    private final OperationalEventRepository eventRepository =
            mock(
                    OperationalEventRepository.class);

    private final EventValidationResultRepository
            eventValidationResultRepository =
                    mock(
                            EventValidationResultRepository.class);

    private final EventValidationAlertRepository
            eventValidationAlertRepository =
                    mock(
                            EventValidationAlertRepository.class);

    private final ConsolidationRepository consolidationRepository =
            mock(
                    ConsolidationRepository.class);

    private final CloseValidationResultRepository
            closeValidationResultRepository =
                    mock(
                            CloseValidationResultRepository.class);

    private final AccountingSubmissionAttemptRepository
            submissionAttemptRepository =
                    mock(
                            AccountingSubmissionAttemptRepository.class);

    private final CloseConsolidationReadinessEvaluator
            readinessEvaluator =
                    mock(
                            CloseConsolidationReadinessEvaluator.class);

    private final Vr008Evaluator vr008Evaluator =
            mock(
                    Vr008Evaluator.class);

    private final CurrentActorProvider currentActorProvider =
            mock(
                    CurrentActorProvider.class);

    private final ApplicationClock applicationClock =
            mock(
                    ApplicationClock.class);

    private final UuidGenerator uuidGenerator =
            mock(
                    UuidGenerator.class);

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

    private final SubmitOperationalCloseToAccounting useCase =
            new SubmitOperationalCloseToAccounting(
                    closeLockRepository,
                    closeRevisionRepository,
                    eventRepository,
                    eventValidationResultRepository,
                    eventValidationAlertRepository,
                    consolidationRepository,
                    closeValidationResultRepository,
                    submissionAttemptRepository,
                    readinessEvaluator,
                    vr008Evaluator,
                    currentActorProvider,
                    applicationClock,
                    uuidGenerator,
                    transactionRunner);

    @Test
    void submitsValidatedCloseAndReplacesPreviousVr008Result() {
        OperationalClose operationalClose =
                close(
                        OperationalCloseState.VALIDATED);

        OperationalEvent event =
                event();

        Consolidation consolidation =
                consolidation(
                        operationalClose,
                        event);

        Vr008Evaluation evaluation =
                Vr008Evaluation.satisfied(
                        CONSOLIDATION_ID);

        configureResponsibleActor();

        configureReloadedData(
                operationalClose,
                event,
                Optional.of(
                        consolidation),
                evaluation);

        CloseValidationResult previousResult =
                previousVr008Result();

        when(closeValidationResultRepository
                .findCurrentByCloseIdAndRuleCode(
                        CLOSE_ID,
                        ValidationRuleCode.VR_008))
                .thenReturn(
                        Optional.of(
                                previousResult));

        when(uuidGenerator.next())
                .thenReturn(
                        NEW_VR008_UUID,
                        ATTEMPT_UUID,
                        TRANSITION_UUID);

        SubmitOperationalCloseToAccountingResult result =
                useCase.execute(
                        command());

        assertThat(
                result.status())
                .isEqualTo(
                        SubmitOperationalCloseToAccountingResult
                                .Status.SUBMITTED);

        assertThat(
                result.submissionAttemptId())
                .isEqualTo(
                        ATTEMPT_UUID);

        assertThat(
                result.validationResultId())
                .isEqualTo(
                        NEW_VR008_UUID);

        ArgumentCaptor<CloseValidationResult> invalidatedCaptor =
                ArgumentCaptor.forClass(
                        CloseValidationResult.class);

        ArgumentCaptor<CloseValidationResult> resultCaptor =
                ArgumentCaptor.forClass(
                        CloseValidationResult.class);

        InOrder resultPersistenceOrder =
                inOrder(
                        closeValidationResultRepository);

        resultPersistenceOrder.verify(
                closeValidationResultRepository)
                .findCurrentByCloseIdAndRuleCode(
                        CLOSE_ID,
                        ValidationRuleCode.VR_008);

        resultPersistenceOrder.verify(
                closeValidationResultRepository)
                .saveInvalidation(
                        invalidatedCaptor.capture());

        resultPersistenceOrder.verify(
                closeValidationResultRepository)
                .saveNew(
                        resultCaptor.capture());

        CloseValidationResult invalidated =
                invalidatedCaptor.getValue();

        assertThat(
                invalidated.id())
                .isEqualTo(
                        previousResult.id());

        assertThat(
                invalidated.current())
                .isFalse();

        assertThat(
                invalidated.invalidatedAt())
                .isEqualTo(
                        NOW);

        CloseValidationResult savedResult =
                resultCaptor.getValue();

        assertThat(
                savedResult.id())
                .isEqualTo(
                        NEW_VR008_ID);

        assertThat(
                savedResult.outcome())
                .isEqualTo(
                        ValidationOutcome.SATISFIED);

        assertThat(
                savedResult.consolidationId())
                .isEqualTo(
                        CONSOLIDATION_ID);

        ArgumentCaptor<AccountingSubmissionAttempt> attemptCaptor =
                ArgumentCaptor.forClass(
                        AccountingSubmissionAttempt.class);

        verify(submissionAttemptRepository)
                .saveNew(
                        attemptCaptor.capture());

        AccountingSubmissionAttempt attempt =
                attemptCaptor.getValue();

        assertThat(
                attempt.id())
                .isEqualTo(
                        ATTEMPT_ID);

        assertThat(
                attempt.isSuccessful())
                .isTrue();

        assertThat(
                attempt.issues())
                .isEmpty();

        ArgumentCaptor<OperationalClose> closeCaptor =
                ArgumentCaptor.forClass(
                        OperationalClose.class);

        verify(closeRevisionRepository)
                .saveRevision(
                        closeCaptor.capture());

        assertThat(
                closeCaptor.getValue()
                        .state())
                .isEqualTo(
                        OperationalCloseState.SENT_TO_ACCOUNTING);

        ArgumentCaptor<CloseStateTransition> transitionCaptor =
                ArgumentCaptor.forClass(
                        CloseStateTransition.class);

        verify(closeRevisionRepository)
                .appendSubmissionStateTransition(
                        transitionCaptor.capture(),
                        eq(
                                NEW_VR008_ID),
                        eq(
                                CONSOLIDATION_ID),
                        eq(
                                ATTEMPT_ID));

        assertThat(
                transitionCaptor.getValue()
                        .causeCode())
                .isEqualTo(
                        SubmitOperationalCloseToAccounting
                                .ACCOUNTING_SUBMISSION_SUCCEEDED);

        verify(consolidationRepository, never())
                .saveInvalidation(
                        any(
                                Consolidation.class));
    }

    @Test
    void rejectsSubmissionWithoutConsolidationAndBlocksClose() {
        OperationalClose operationalClose =
                close(
                        OperationalCloseState.VALIDATED);

        OperationalEvent event =
                event();

        Vr008Issue missingConsolidation =
                new Vr008Issue(
                        SubmissionAttemptIssueType
                                .CONSOLIDATION_MISSING,
                        null,
                        null,
                        null,
                        null,
                        "The current Consolidation is missing.");

        Vr008Evaluation evaluation =
                Vr008Evaluation.failed(
                        null,
                        List.of(
                                missingConsolidation));

        configureResponsibleActor();

        configureReloadedData(
                operationalClose,
                event,
                Optional.empty(),
                evaluation);

        when(closeValidationResultRepository
                .findCurrentByCloseIdAndRuleCode(
                        CLOSE_ID,
                        ValidationRuleCode.VR_008))
                .thenReturn(
                        Optional.empty());

        when(uuidGenerator.next())
                .thenReturn(
                        NEW_VR008_UUID,
                        ATTEMPT_UUID,
                        ISSUE_UUID,
                        TRANSITION_UUID);

        SubmitOperationalCloseToAccountingResult result =
                useCase.execute(
                        command());

        assertThat(
                result.status())
                .isEqualTo(
                        SubmitOperationalCloseToAccountingResult
                                .Status.SUBMISSION_REJECTED);

        assertThat(
                result.issueTypes())
                .containsExactly(
                        SubmissionAttemptIssueType
                                .CONSOLIDATION_MISSING);

        ArgumentCaptor<AccountingSubmissionAttempt> attemptCaptor =
                ArgumentCaptor.forClass(
                        AccountingSubmissionAttempt.class);

        verify(submissionAttemptRepository)
                .saveNew(
                        attemptCaptor.capture());

        AccountingSubmissionAttempt attempt =
                attemptCaptor.getValue();

        assertThat(
                attempt.isRejected())
                .isTrue();

        assertThat(
                attempt.consolidationId())
                .isNull();

        assertThat(
                attempt.issues())
                .singleElement()
                .satisfies(
                        issue -> {
                            assertThat(
                                    issue.id().value())
                                    .isEqualTo(
                                            ISSUE_UUID);

                            assertThat(
                                    issue.issueType())
                                    .isEqualTo(
                                            SubmissionAttemptIssueType
                                                    .CONSOLIDATION_MISSING);
                        });

        ArgumentCaptor<OperationalClose> closeCaptor =
                ArgumentCaptor.forClass(
                        OperationalClose.class);

        verify(closeRevisionRepository)
                .saveRevision(
                        closeCaptor.capture());

        assertThat(
                closeCaptor.getValue()
                        .state())
                .isEqualTo(
                        OperationalCloseState.BLOCKED);

        verify(closeRevisionRepository)
                .appendSubmissionStateTransition(
                        any(
                                CloseStateTransition.class),
                        eq(
                                NEW_VR008_ID),
                        eq(
                                ATTEMPT_ID));

        verify(consolidationRepository, never())
                .saveInvalidation(
                        any(
                                Consolidation.class));
    }

    @Test
    void rejectedSubmissionInvalidatesEvaluatedConsolidation() {
        OperationalClose operationalClose =
                close(
                        OperationalCloseState.VALIDATED);

        OperationalEvent event =
                event();

        Consolidation consolidation =
                consolidation(
                        operationalClose,
                        event);

        Vr008Issue staleConsolidation =
                new Vr008Issue(
                        SubmissionAttemptIssueType
                                .CONSOLIDATION_STALE,
                        null,
                        null,
                        null,
                        CONSOLIDATION_ID,
                        "The Consolidation is stale.");

        Vr008Evaluation evaluation =
                Vr008Evaluation.failed(
                        CONSOLIDATION_ID,
                        List.of(
                                staleConsolidation));

        configureResponsibleActor();

        configureReloadedData(
                operationalClose,
                event,
                Optional.of(
                        consolidation),
                evaluation);

        when(closeValidationResultRepository
                .findCurrentByCloseIdAndRuleCode(
                        CLOSE_ID,
                        ValidationRuleCode.VR_008))
                .thenReturn(
                        Optional.empty());

        when(uuidGenerator.next())
                .thenReturn(
                        NEW_VR008_UUID,
                        ATTEMPT_UUID,
                        ISSUE_UUID,
                        TRANSITION_UUID);

        useCase.execute(
                command());

        ArgumentCaptor<Consolidation> invalidatedCaptor =
                ArgumentCaptor.forClass(
                        Consolidation.class);

        InOrder persistenceOrder =
                inOrder(
                        closeValidationResultRepository,
                        submissionAttemptRepository,
                        consolidationRepository,
                        closeRevisionRepository);

        persistenceOrder.verify(
                closeValidationResultRepository)
                .saveNew(
                        any(
                                CloseValidationResult.class));

        persistenceOrder.verify(
                submissionAttemptRepository)
                .saveNew(
                        any(
                                AccountingSubmissionAttempt.class));

        persistenceOrder.verify(
                consolidationRepository)
                .saveInvalidation(
                        invalidatedCaptor.capture());

        persistenceOrder.verify(
                closeRevisionRepository)
                .saveRevision(
                        any(
                                OperationalClose.class));

        persistenceOrder.verify(
                closeRevisionRepository)
                .appendSubmissionStateTransition(
                        any(
                                CloseStateTransition.class),
                        eq(
                                NEW_VR008_ID),
                        eq(
                                CONSOLIDATION_ID),
                        eq(
                                ATTEMPT_ID));

        Consolidation invalidated =
                invalidatedCaptor.getValue();

        assertThat(
                invalidated.id())
                .isEqualTo(
                        CONSOLIDATION_ID);

        assertThat(
                invalidated.current())
                .isFalse();

        assertThat(
                invalidated.invalidatedAt())
                .isEqualTo(
                        NOW);

        assertThat(
                invalidated.invalidationReason())
                .isEqualTo(
                        "VR-008 rejected the accounting submission.");
    }

    @Test
    void doesNotReprocessCloseAlreadySentToAccounting() {
        configureResponsibleActor();

        when(closeLockRepository.findByIdForUpdate(
                CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                close(
                                        OperationalCloseState
                                                .SENT_TO_ACCOUNTING)));

        SubmitOperationalCloseToAccountingResult result =
                useCase.execute(
                        command());

        assertThat(
                result.status())
                .isEqualTo(
                        SubmitOperationalCloseToAccountingResult
                                .Status.CLOSE_ALREADY_SUBMITTED);

        verifyNoInteractions(
                eventRepository,
                readinessEvaluator,
                eventValidationResultRepository,
                eventValidationAlertRepository,
                consolidationRepository,
                closeValidationResultRepository,
                submissionAttemptRepository,
                applicationClock,
                uuidGenerator);

        verifyNoInteractions(
                closeRevisionRepository);
    }

    @Test
    void requiresValidatedCloseState() {
        configureResponsibleActor();

        when(closeLockRepository.findByIdForUpdate(
                CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                close(
                                        OperationalCloseState.BLOCKED)));

        SubmitOperationalCloseToAccountingResult result =
                useCase.execute(
                        command());

        assertThat(
                result.status())
                .isEqualTo(
                        SubmitOperationalCloseToAccountingResult
                                .Status.CLOSE_NOT_SUBMITTABLE);

        verifyNoInteractions(
                submissionAttemptRepository,
                closeValidationResultRepository,
                closeRevisionRepository);
    }

    @Test
    void rejectsUnexpectedAuthenticatedActorBeforeLocking() {
        when(currentActorProvider.currentActor())
                .thenReturn(
                        new AuthenticatedPrincipal(
                                "another-user",
                                "another"));

        SubmitOperationalCloseToAccountingResult result =
                useCase.execute(
                        command());

        assertThat(
                result.status())
                .isEqualTo(
                        SubmitOperationalCloseToAccountingResult
                                .Status.ACTOR_REJECTED);

        verifyNoInteractions(
                closeLockRepository,
                submissionAttemptRepository,
                closeValidationResultRepository);
    }

    private void configureResponsibleActor() {
        when(currentActorProvider.currentActor())
                .thenReturn(
                        new AuthenticatedPrincipal(
                                ACTOR.userId(),
                                ACTOR.username()));
    }

    private void configureReloadedData(
            OperationalClose operationalClose,
            OperationalEvent event,
            Optional<Consolidation> consolidation,
            Vr008Evaluation evaluation) {

        List<OperationalEvent> events =
                List.of(
                        event);

        List<EventValidationResult> validationResults =
                List.of(
                        eventValidationResult(
                                event));

        CloseConsolidationReadiness readiness =
                CloseConsolidationReadiness.evaluated(
                        List.of(),
                        List.of(),
                        List.of());

        when(closeLockRepository.findByIdForUpdate(
                CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                operationalClose));

        when(eventRepository
                .findAllByCloseIdOrderByOccurredAtDescending(
                        CLOSE_ID))
                .thenReturn(
                        events);

        when(readinessEvaluator.evaluate(
                CLOSE_ID,
                events))
                .thenReturn(
                        readiness);

        when(eventValidationResultRepository
                .findAllCurrentByEventIdOrderByRuleCode(
                        EVENT_ID))
                .thenReturn(
                        validationResults);

        when(eventValidationAlertRepository
                .findAllOpenByEventIdOrderByCreatedAt(
                        EVENT_ID))
                .thenReturn(
                        List.of());

        when(consolidationRepository.findCurrentByCloseId(
                CLOSE_ID))
                .thenReturn(
                        consolidation);

        when(vr008Evaluator.evaluate(
                CLOSE_ID,
                events,
                readiness,
                validationResults,
                List.of(),
                consolidation.orElse(
                        null)))
                .thenReturn(
                        evaluation);

        when(applicationClock.now())
                .thenReturn(
                        NOW);
    }

    private CloseValidationResult previousVr008Result() {
        return CloseValidationResult.create(
                new ValidationResultId(
                        PREVIOUS_VR008_UUID),
                ValidationRuleCode.VR_008,
                1,
                CLOSE_ID,
                ValidationOutcome.FAILED,
                "Previous final control failed.",
                NOW.minusSeconds(
                        120),
                ACTOR,
                null);
    }

    private EventValidationResult eventValidationResult(
            OperationalEvent event) {

        return EventValidationResult.create(
                new ValidationResultId(
                        EVENT_RESULT_UUID),
                ValidationRuleCode.VR_001,
                1,
                event.id(),
                ValidationOutcome.SATISFIED,
                "Event control passed.",
                NOW.minusSeconds(
                        120),
                ACTOR,
                event.dataRevision());
    }

    private OperationalClose close(
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
                        decimal(
                                "1000.0000")),
                state,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

    private OperationalEvent event() {
        return new OperationalEvent(
                EVENT_ID,
                CLOSE_ID,
                OperationalEventType.INCOME,
                new OperationalEventAmount(
                        decimal(
                                "100.0000")),
                decimal(
                        "100.0000"),
                null,
                CREATED_AT,
                CREATED_AT,
                "Caja principal",
                "Evento para envío contable",
                OperationalEventState.VALIDATED,
                false,
                false,
                1L,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

    private Consolidation consolidation(
            OperationalClose operationalClose,
            OperationalEvent event) {

        return Consolidation.complete(
                CONSOLIDATION_ID,
                operationalClose,
                List.of(
                        event),
                decimal(
                        "1100.0000"),
                NOW.minusSeconds(
                        60),
                ACTOR);
    }

    private SubmitOperationalCloseToAccountingCommand command() {
        return new SubmitOperationalCloseToAccountingCommand(
                CLOSE_UUID);
    }

    private static BigDecimal decimal(
            String value) {

        return new BigDecimal(
                value);
    }

    private static UUID uuid(
            String value) {

        return UUID.fromString(
                value);
    }

}