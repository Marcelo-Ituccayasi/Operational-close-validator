package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationContext;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationEngine;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationRuleEvaluation;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationStateResolver;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventValidationRevision;

/**
 * Validates or revalidates one Operational Event inside a single transaction.
 */
public final class ValidateOperationalEvent {

    private static final String REVALIDATION_REASON =
            "Operational Event was revalidated.";

    private static final String VALIDATION_FAILURE_MESSAGE =
            "The event contains failed validation rules.";

    private final OperationalCloseLockRepository
            closeLockRepository;

    private final OperationalEventRevisionRepository
            eventRevisionRepository;

    private final EventValidationResultRepository
            validationResultRepository;

    private final CurrentActorProvider
            currentActorProvider;

    private final ApplicationClock
            applicationClock;

    private final UuidGenerator
            uuidGenerator;

    private final TransactionRunner
            transactionRunner;

    private final EventValidationContextLoader
            contextLoader;

    private final EventValidationEngine
            validationEngine;

    private final EventValidationResultFactory
            resultFactory;

    private final EventValidationAlertSynchronizer
            alertSynchronizer;

    private final EventValidationStateResolver
            stateResolver;

    public ValidateOperationalEvent(
            OperationalCloseLockRepository closeLockRepository,
            OperationalEventRevisionRepository eventRevisionRepository,
            EventValidationResultRepository validationResultRepository,
            CurrentActorProvider currentActorProvider,
            ApplicationClock applicationClock,
            UuidGenerator uuidGenerator,
            TransactionRunner transactionRunner,
            EventValidationContextLoader contextLoader,
            EventValidationEngine validationEngine,
            EventValidationResultFactory resultFactory,
            EventValidationAlertSynchronizer alertSynchronizer,
            EventValidationStateResolver stateResolver) {

        this.closeLockRepository =
                Objects.requireNonNull(
                        closeLockRepository);

        this.eventRevisionRepository =
                Objects.requireNonNull(
                        eventRevisionRepository);

        this.validationResultRepository =
                Objects.requireNonNull(
                        validationResultRepository);

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

        this.contextLoader =
                Objects.requireNonNull(
                        contextLoader);

        this.validationEngine =
                Objects.requireNonNull(
                        validationEngine);

        this.resultFactory =
                Objects.requireNonNull(
                        resultFactory);

        this.alertSynchronizer =
                Objects.requireNonNull(
                        alertSynchronizer);

        this.stateResolver =
                Objects.requireNonNull(
                        stateResolver);
    }

    public ValidateOperationalEventResult execute(
            ValidateOperationalEventCommand command) {

        Objects.requireNonNull(
                command,
                "command must not be null");

        return transactionRunner.execute(
                () -> executeInsideTransaction(
                        command));
    }

    private ValidateOperationalEventResult
            executeInsideTransaction(
                    ValidateOperationalEventCommand command) {

        AuthenticatedPrincipal principal =
                Objects.requireNonNull(
                        currentActorProvider.currentActor(),
                        "authenticated principal must not be null");

        if (!AuditActor.RESPONSIBLE_USER_ID.equals(
                principal.userId())) {

            return ValidateOperationalEventResult
                    .actorRejected();
        }

        AuditActor actor;
        ValidationInput input;

        try {
            actor =
                    new AuditActor(
                            principal.userId(),
                            principal.username());

            input =
                    parseInput(
                            command);
        }
        catch (IllegalArgumentException exception) {
            return ValidateOperationalEventResult
                    .invalidInput(
                            exception.getMessage());
        }

        Optional<OperationalClose> lockedClose =
                closeLockRepository.findByIdForUpdate(
                        input.closeId());

        if (lockedClose.isEmpty()) {
            return ValidateOperationalEventResult
                    .closeNotFound();
        }

        OperationalClose operationalClose =
                lockedClose.orElseThrow();

        if (operationalClose.state()
                == OperationalCloseState.SENT_TO_ACCOUNTING) {

            return ValidateOperationalEventResult
                    .closeNotEditable();
        }

        Optional<OperationalEvent> lockedEvent =
                eventRevisionRepository.findByIdForUpdate(
                        input.closeId(),
                        input.eventId());

        if (lockedEvent.isEmpty()) {
            return ValidateOperationalEventResult
                    .eventNotFound();
        }

        OperationalEvent operationalEvent =
                lockedEvent.orElseThrow();

        EventValidationContext loadedContext =
                contextLoader.load(
                        input.closeId(),
                        input.eventId())
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "locked operational event could not be loaded for validation"));

        EventValidationContext validationContext =
                new EventValidationContext(
                        operationalEvent,
                        loadedContext.supportingEvidence(),
                        loadedContext.authorizations());

        Instant evaluatedAt =
                Objects.requireNonNull(
                        applicationClock.now(),
                        "application time must not be null");

        List<EventValidationRuleEvaluation> evaluations =
                Objects.requireNonNull(
                        validationEngine.evaluate(
                                validationContext),
                        "validation evaluations must not be null");

        invalidateCurrentResults(
                input,
                evaluatedAt);

        List<EventValidationResult> validationResults =
                Objects.requireNonNull(
                        resultFactory.createAll(
                                validationContext,
                                evaluations,
                                evaluatedAt,
                                actor),
                        "created validation results must not be null");

        validationResults.forEach(
                validationResultRepository::saveNew);

        alertSynchronizer.synchronize(
                validationResults,
                evaluatedAt,
                actor);

        boolean blockingAlertOpen =
                alertSynchronizer
                        .hasOpenBlockingAlert(
                                input.eventId());

        OperationalEventState evaluatedState =
                Objects.requireNonNull(
                        stateResolver.resolve(
                                evaluations),
                        "resulting event state must not be null");

        OperationalEventState resultingState =
                EventValidationStateResolver
                        .enforceOpenBlockingAlertInvariant(
                                evaluatedState,
                                blockingAlertOpen);

        applyEventState(
                operationalEvent,
                resultingState,
                evaluatedAt,
                actor);

        if (resultingState
                == OperationalEventState.VALIDATED) {

            return ValidateOperationalEventResult
                    .validated(
                            command.eventId());
        }

        return ValidateOperationalEventResult
                .validationFailed(
                        command.eventId(),
                        VALIDATION_FAILURE_MESSAGE);
    }

    private void invalidateCurrentResults(
            ValidationInput input,
            Instant invalidatedAt) {

        List<EventValidationResult> currentResults =
                Objects.requireNonNull(
                        validationResultRepository
                                .findAllCurrentByEventIdOrderByRuleCode(
                                        input.eventId()),
                        "current validation results must not be null");

        currentResults.stream()
                .map(
                        result -> result.invalidate(
                                invalidatedAt,
                                REVALIDATION_REASON))
                .forEach(
                        validationResultRepository
                                ::saveInvalidation);
    }

    private void applyEventState(
            OperationalEvent operationalEvent,
            OperationalEventState resultingState,
            Instant occurredAt,
            AuditActor actor) {

        EventStateTransitionId transitionId =
                null;

        if (operationalEvent.state()
                != resultingState) {

            UUID transitionUuid =
                    Objects.requireNonNull(
                            uuidGenerator.next(),
                            "generated event state transition UUID must not be null");

            transitionId =
                    new EventStateTransitionId(
                            transitionUuid);
        }

        OperationalEventValidationRevision revision =
                OperationalEventValidationRevision.apply(
                        operationalEvent,
                        resultingState,
                        transitionId,
                        occurredAt,
                        actor);

        if (!revision.stateChanged()) {
            return;
        }

        eventRevisionRepository.saveRevision(
                revision.revisedEvent());

        eventRevisionRepository.appendStateTransition(
                revision.stateTransition());
    }

    private static ValidationInput parseInput(
            ValidateOperationalEventCommand command) {

        if (command.closeId() == null) {
            throw new IllegalArgumentException(
                    "closeId must not be null");
        }

        if (command.eventId() == null) {
            throw new IllegalArgumentException(
                    "eventId must not be null");
        }

        return new ValidationInput(
                new OperationalCloseId(
                        command.closeId()),
                new OperationalEventId(
                        command.eventId()));
    }

    private record ValidationInput(
            OperationalCloseId closeId,
            OperationalEventId eventId) {
    }

}