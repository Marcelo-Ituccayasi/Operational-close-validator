package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.Consolidation;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssue;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

/**
 * Executes VR-008 and records one internal accounting submission atomically.
 */
public final class SubmitOperationalCloseToAccounting {

    public static final String ACCOUNTING_SUBMISSION_SUCCEEDED =
            "ACCOUNTING_SUBMISSION_SUCCEEDED";

    public static final String ACCOUNTING_SUBMISSION_REJECTED =
            "ACCOUNTING_SUBMISSION_REJECTED";

    private static final int VR_008_VERSION = 1;

    private static final String PREVIOUS_RESULT_INVALIDATION_REASON =
            "Superseded by another VR-008 evaluation.";

    private static final String REJECTED_CONSOLIDATION_INVALIDATION_REASON =
            "VR-008 rejected the accounting submission.";

    private final OperationalCloseLockRepository
            closeLockRepository;

    private final OperationalCloseRevisionRepository
            closeRevisionRepository;

    private final OperationalEventRepository eventRepository;

    private final EventValidationResultRepository
            eventValidationResultRepository;

    private final EventValidationAlertRepository
            eventValidationAlertRepository;

    private final ConsolidationRepository consolidationRepository;

    private final CloseValidationResultRepository
            closeValidationResultRepository;

    private final AccountingSubmissionAttemptRepository
            submissionAttemptRepository;

    private final CloseConsolidationReadinessEvaluator
            readinessEvaluator;

    private final Vr008Evaluator vr008Evaluator;

    private final CurrentActorProvider currentActorProvider;

    private final ApplicationClock applicationClock;

    private final UuidGenerator uuidGenerator;

    private final TransactionRunner transactionRunner;

    public SubmitOperationalCloseToAccounting(
            OperationalCloseLockRepository closeLockRepository,
            OperationalCloseRevisionRepository closeRevisionRepository,
            OperationalEventRepository eventRepository,
            EventValidationResultRepository
                    eventValidationResultRepository,
            EventValidationAlertRepository
                    eventValidationAlertRepository,
            ConsolidationRepository consolidationRepository,
            CloseValidationResultRepository
                    closeValidationResultRepository,
            AccountingSubmissionAttemptRepository
                    submissionAttemptRepository,
            CloseConsolidationReadinessEvaluator readinessEvaluator,
            Vr008Evaluator vr008Evaluator,
            CurrentActorProvider currentActorProvider,
            ApplicationClock applicationClock,
            UuidGenerator uuidGenerator,
            TransactionRunner transactionRunner) {

        this.closeLockRepository =
                Objects.requireNonNull(
                        closeLockRepository);

        this.closeRevisionRepository =
                Objects.requireNonNull(
                        closeRevisionRepository);

        this.eventRepository =
                Objects.requireNonNull(
                        eventRepository);

        this.eventValidationResultRepository =
                Objects.requireNonNull(
                        eventValidationResultRepository);

        this.eventValidationAlertRepository =
                Objects.requireNonNull(
                        eventValidationAlertRepository);

        this.consolidationRepository =
                Objects.requireNonNull(
                        consolidationRepository);

        this.closeValidationResultRepository =
                Objects.requireNonNull(
                        closeValidationResultRepository);

        this.submissionAttemptRepository =
                Objects.requireNonNull(
                        submissionAttemptRepository);

        this.readinessEvaluator =
                Objects.requireNonNull(
                        readinessEvaluator);

        this.vr008Evaluator =
                Objects.requireNonNull(
                        vr008Evaluator);

        this.currentActorProvider =
                Objects.requireNonNull(
                        currentActorProvider);

        this.applicationClock =
                Objects.requireNonNull(
                        applicationClock);

        this.uuidGenerator =
                Objects.requireNonNull(
                        uuidGenerator);

        this.transactionRunner =
                Objects.requireNonNull(
                        transactionRunner);
    }

    public SubmitOperationalCloseToAccountingResult execute(
            SubmitOperationalCloseToAccountingCommand command) {

        Objects.requireNonNull(
                command,
                "command must not be null");

        return transactionRunner.execute(
                () -> executeInsideTransaction(
                        command));
    }

    private SubmitOperationalCloseToAccountingResult
            executeInsideTransaction(
                    SubmitOperationalCloseToAccountingCommand command) {

        AuthenticatedPrincipal principal =
                Objects.requireNonNull(
                        currentActorProvider.currentActor(),
                        "authenticated principal must not be null");

        if (!AuditActor.RESPONSIBLE_USER_ID.equals(
                principal.userId())) {

            return SubmitOperationalCloseToAccountingResult
                    .actorRejected();
        }

        AuditActor actor;
        OperationalCloseId closeId;

        try {
            actor =
                    new AuditActor(
                            principal.userId(),
                            principal.username());

            closeId =
                    parseCloseId(
                            command);
        }
        catch (IllegalArgumentException exception) {
            return SubmitOperationalCloseToAccountingResult
                    .invalidInput(
                            exception.getMessage());
        }

        Optional<OperationalClose> lockedClose =
                closeLockRepository.findByIdForUpdate(
                        closeId);

        if (lockedClose.isEmpty()) {
            return SubmitOperationalCloseToAccountingResult
                    .closeNotFound();
        }

        OperationalClose operationalClose =
                lockedClose.orElseThrow();

        if (operationalClose.state()
                == OperationalCloseState.SENT_TO_ACCOUNTING) {

            return SubmitOperationalCloseToAccountingResult
                    .closeAlreadySubmitted();
        }

        if (operationalClose.state()
                != OperationalCloseState.VALIDATED) {

            return SubmitOperationalCloseToAccountingResult
                    .closeNotSubmittable();
        }

        List<OperationalEvent> events =
                Objects.requireNonNull(
                        eventRepository
                                .findAllByCloseIdOrderByOccurredAtDescending(
                                        closeId),
                        "operational events must not be null");

        CloseConsolidationReadiness readiness =
                Objects.requireNonNull(
                        readinessEvaluator.evaluate(
                                closeId,
                                events),
                        "close readiness must not be null");

        List<EventValidationResult> currentValidationResults =
                loadCurrentValidationResults(
                        events);

        List<EventValidationAlert> openAlerts =
                loadOpenAlerts(
                        events);

        Optional<Consolidation> currentConsolidation =
                Objects.requireNonNull(
                        consolidationRepository
                                .findCurrentByCloseId(
                                        closeId),
                        "current consolidation result must not be null");

        Consolidation consolidation =
                currentConsolidation.orElse(
                        null);

        Vr008Evaluation evaluation =
                Objects.requireNonNull(
                        vr008Evaluator.evaluate(
                                closeId,
                                events,
                                readiness,
                                currentValidationResults,
                                openAlerts,
                                consolidation),
                        "VR-008 evaluation must not be null");

        Instant attemptedAt =
                Objects.requireNonNull(
                        applicationClock.now(),
                        "application time must not be null");

        invalidatePreviousVr008Result(
                closeId,
                attemptedAt);

        ValidationResultId validationResultId =
                new ValidationResultId(
                        nextUuid(
                                "generated validation result UUID "
                                        + "must not be null"));

        CloseValidationResult validationResult =
                CloseValidationResult.create(
                        validationResultId,
                        ValidationRuleCode.VR_008,
                        VR_008_VERSION,
                        closeId,
                        evaluation.outcome(),
                        evaluation.detail(),
                        attemptedAt,
                        actor,
                        evaluation.consolidationId());

        closeValidationResultRepository.saveNew(
                validationResult);

        AccountingSubmissionAttemptId attemptId =
                new AccountingSubmissionAttemptId(
                        nextUuid(
                                "generated submission attempt UUID "
                                        + "must not be null"));

        AccountingSubmissionAttempt attempt =
                createAttempt(
                        attemptId,
                        closeId,
                        validationResultId,
                        evaluation,
                        attemptedAt,
                        actor);

        submissionAttemptRepository.saveNew(
                attempt);

        if (evaluation.satisfied()) {
            persistSuccessfulStateChange(
                    operationalClose,
                    validationResult,
                    attempt,
                    attemptedAt,
                    actor);

            return SubmitOperationalCloseToAccountingResult
                    .submitted(
                            attemptId.value(),
                            validationResultId.value());
        }

        if (consolidation != null) {
            consolidationRepository.saveInvalidation(
                    consolidation.invalidate(
                            attemptedAt,
                            REJECTED_CONSOLIDATION_INVALIDATION_REASON));
        }

        persistRejectedStateChange(
                operationalClose,
                validationResult,
                attempt,
                attemptedAt,
                actor);

        return SubmitOperationalCloseToAccountingResult
                .rejected(
                        attemptId.value(),
                        validationResultId.value(),
                        evaluation.issues()
                                .stream()
                                .map(
                                        Vr008Issue::issueType)
                                .toList());
    }

    private List<EventValidationResult>
            loadCurrentValidationResults(
                    List<OperationalEvent> events) {

        List<EventValidationResult> results =
                new ArrayList<>();

        for (OperationalEvent event : events) {
            List<EventValidationResult> eventResults =
                    Objects.requireNonNull(
                            eventValidationResultRepository
                                    .findAllCurrentByEventIdOrderByRuleCode(
                                            event.id()),
                            "current Event Validation Results "
                                    + "must not be null");

            results.addAll(
                    eventResults);
        }

        return List.copyOf(
                results);
    }

    private List<EventValidationAlert> loadOpenAlerts(
            List<OperationalEvent> events) {

        List<EventValidationAlert> alerts =
                new ArrayList<>();

        for (OperationalEvent event : events) {
            List<EventValidationAlert> eventAlerts =
                    Objects.requireNonNull(
                            eventValidationAlertRepository
                                    .findAllOpenByEventIdOrderByCreatedAt(
                                            event.id()),
                            "open Event Validation Alerts "
                                    + "must not be null");

            alerts.addAll(
                    eventAlerts);
        }

        return List.copyOf(
                alerts);
    }

    private void invalidatePreviousVr008Result(
            OperationalCloseId closeId,
            Instant invalidatedAt) {

        closeValidationResultRepository
                .findCurrentByCloseIdAndRuleCode(
                        closeId,
                        ValidationRuleCode.VR_008)
                .map(
                        current ->
                                current.invalidate(
                                        invalidatedAt,
                                        PREVIOUS_RESULT_INVALIDATION_REASON))
                .ifPresent(
                        closeValidationResultRepository
                                ::saveInvalidation);
    }

    private AccountingSubmissionAttempt createAttempt(
            AccountingSubmissionAttemptId attemptId,
            OperationalCloseId closeId,
            ValidationResultId validationResultId,
            Vr008Evaluation evaluation,
            Instant attemptedAt,
            AuditActor actor) {

        if (evaluation.satisfied()) {
            return AccountingSubmissionAttempt.succeeded(
                    attemptId,
                    closeId,
                    validationResultId,
                    Objects.requireNonNull(
                            evaluation.consolidationId(),
                            "successful evaluation requires "
                                    + "a consolidation"),
                    attemptedAt,
                    actor,
                    evaluation.detail());
        }

        List<SubmissionAttemptIssue> issues =
                evaluation.issues()
                        .stream()
                        .map(
                                issue ->
                                        new SubmissionAttemptIssue(
                                                new SubmissionAttemptIssueId(
                                                        nextUuid(
                                                                "generated "
                                                                        + "submission issue UUID "
                                                                        + "must not be null")),
                                                attemptId,
                                                issue.issueType(),
                                                issue.eventId(),
                                                issue.alertId(),
                                                issue.validationResultId(),
                                                issue.consolidationId(),
                                                issue.detail()))
                        .toList();

        return AccountingSubmissionAttempt.rejected(
                attemptId,
                closeId,
                validationResultId,
                evaluation.consolidationId(),
                attemptedAt,
                actor,
                evaluation.detail(),
                issues);
    }

    private void persistSuccessfulStateChange(
            OperationalClose operationalClose,
            CloseValidationResult validationResult,
            AccountingSubmissionAttempt attempt,
            Instant occurredAt,
            AuditActor actor) {

        OperationalClose submittedClose =
                reviseState(
                        operationalClose,
                        OperationalCloseState.SENT_TO_ACCOUNTING,
                        occurredAt,
                        actor);

        closeRevisionRepository.saveRevision(
                submittedClose);

        CloseStateTransition transition =
                submissionTransition(
                        operationalClose,
                        OperationalCloseState.SENT_TO_ACCOUNTING,
                        ACCOUNTING_SUBMISSION_SUCCEEDED,
                        validationResult.detail(),
                        occurredAt,
                        actor);

        closeRevisionRepository
                .appendSubmissionStateTransition(
                        transition,
                        validationResult.id(),
                        Objects.requireNonNull(
                                attempt.consolidationId()),
                        attempt.id());
    }

    private void persistRejectedStateChange(
            OperationalClose operationalClose,
            CloseValidationResult validationResult,
            AccountingSubmissionAttempt attempt,
            Instant occurredAt,
            AuditActor actor) {

        OperationalClose blockedClose =
                reviseState(
                        operationalClose,
                        OperationalCloseState.BLOCKED,
                        occurredAt,
                        actor);

        closeRevisionRepository.saveRevision(
                blockedClose);

        CloseStateTransition transition =
                submissionTransition(
                        operationalClose,
                        OperationalCloseState.BLOCKED,
                        ACCOUNTING_SUBMISSION_REJECTED,
                        validationResult.detail(),
                        occurredAt,
                        actor);

        if (attempt.consolidationId() == null) {
            closeRevisionRepository
                    .appendSubmissionStateTransition(
                            transition,
                            validationResult.id(),
                            attempt.id());

            return;
        }

        closeRevisionRepository
                .appendSubmissionStateTransition(
                        transition,
                        validationResult.id(),
                        attempt.consolidationId(),
                        attempt.id());
    }

    private CloseStateTransition submissionTransition(
            OperationalClose operationalClose,
            OperationalCloseState targetState,
            String causeCode,
            String detail,
            Instant occurredAt,
            AuditActor actor) {

        return new CloseStateTransition(
                new CloseStateTransitionId(
                        nextUuid(
                                "generated close transition UUID "
                                        + "must not be null")),
                operationalClose.id(),
                operationalClose.state(),
                targetState,
                causeCode,
                detail,
                occurredAt,
                actor);
    }

    private static OperationalClose reviseState(
            OperationalClose operationalClose,
            OperationalCloseState revisedState,
            Instant revisedAt,
            AuditActor actor) {

        if (revisedAt.isBefore(
                operationalClose.updatedAt())) {

            throw new IllegalArgumentException(
                    "state revision instant must not be before "
                            + "previous Close update");
        }

        if (revisedAt.isBefore(
                operationalClose.stateChangedAt())) {

            throw new IllegalArgumentException(
                    "state revision instant must not be before "
                            + "previous Close state change");
        }

        return new OperationalClose(
                operationalClose.id(),
                operationalClose.period(),
                operationalClose.currencyCode(),
                operationalClose.initialBalance(),
                revisedState,
                revisedAt,
                operationalClose.createdAt(),
                operationalClose.createdBy(),
                revisedAt,
                actor);
    }

    private static OperationalCloseId parseCloseId(
            SubmitOperationalCloseToAccountingCommand command) {

        if (command.closeId() == null) {
            throw new IllegalArgumentException(
                    "closeId must not be null");
        }

        return new OperationalCloseId(
                command.closeId());
    }

    private UUID nextUuid(
            String nullMessage) {

        return Objects.requireNonNull(
                uuidGenerator.next(),
                nullMessage);
    }

}