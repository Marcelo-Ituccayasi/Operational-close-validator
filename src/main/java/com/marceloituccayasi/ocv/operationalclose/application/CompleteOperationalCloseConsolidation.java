package com.marceloituccayasi.ocv.operationalclose.application;

import java.math.BigDecimal;
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
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.ConsolidationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.Consolidation;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

/**
 * Completes one Operational Close consolidation inside a single
 * transaction.
 */
public final class CompleteOperationalCloseConsolidation {

    public static final String CONSOLIDATION_COMPLETED =
            "CONSOLIDATION_COMPLETED";

    public static final String CONSOLIDATION_REJECTED =
            "CONSOLIDATION_REJECTED";

    private static final String PREVIOUS_INVALIDATION_REASON =
            "A newer consolidation was completed.";

    private static final String NO_EVENTS_MESSAGE =
            "The Operational Close requires at least one event "
                    + "before consolidation.";

    private static final String NOT_READY_MESSAGE =
            "The Operational Close contains events, validation results, "
                    + "or blocking alerts that require correction.";

    private static final int MAXIMUM_SCALE = 4;

    private static final int MAXIMUM_INTEGER_DIGITS = 15;

    private final OperationalCloseLockRepository
            closeLockRepository;

    private final OperationalCloseRevisionRepository
            closeRevisionRepository;

    private final OperationalEventRepository
            eventRepository;

    private final ConsolidationRepository
            consolidationRepository;

    private final CloseConsolidationReadinessEvaluator
            readinessEvaluator;

    private final CurrentActorProvider
            currentActorProvider;

    private final ApplicationClock applicationClock;

    private final UuidGenerator uuidGenerator;

    private final TransactionRunner transactionRunner;

    public CompleteOperationalCloseConsolidation(
            OperationalCloseLockRepository closeLockRepository,
            OperationalCloseRevisionRepository closeRevisionRepository,
            OperationalEventRepository eventRepository,
            ConsolidationRepository consolidationRepository,
            CloseConsolidationReadinessEvaluator readinessEvaluator,
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

        this.consolidationRepository =
                Objects.requireNonNull(
                        consolidationRepository);

        this.readinessEvaluator =
                Objects.requireNonNull(
                        readinessEvaluator);

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

    public CompleteOperationalCloseConsolidationResult execute(
            CompleteOperationalCloseConsolidationCommand command) {

        Objects.requireNonNull(
                command,
                "command must not be null");

        return transactionRunner.execute(
                () -> executeInsideTransaction(
                        command));
    }

    private CompleteOperationalCloseConsolidationResult
            executeInsideTransaction(
                    CompleteOperationalCloseConsolidationCommand command) {

        AuthenticatedPrincipal principal =
                Objects.requireNonNull(
                        currentActorProvider.currentActor(),
                        "authenticated principal must not be null");

        if (!AuditActor.RESPONSIBLE_USER_ID.equals(
                principal.userId())) {

            return CompleteOperationalCloseConsolidationResult
                    .actorRejected();
        }

        AuditActor actor;
        ConsolidationInput input;

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
            return CompleteOperationalCloseConsolidationResult
                    .invalidInput(
                            exception.getMessage());
        }

        Optional<OperationalClose> lockedClose =
                closeLockRepository.findByIdForUpdate(
                        input.closeId());

        if (lockedClose.isEmpty()) {
            return CompleteOperationalCloseConsolidationResult
                    .closeNotFound();
        }

        OperationalClose operationalClose =
                lockedClose.orElseThrow();

        if (operationalClose.state()
                != OperationalCloseState.PREPARATION
                && operationalClose.state()
                        != OperationalCloseState.BLOCKED) {

            return CompleteOperationalCloseConsolidationResult
                    .closeNotConsolidatable();
        }

        List<OperationalEvent> events =
                Objects.requireNonNull(
                        eventRepository
                                .findAllByCloseIdOrderByOccurredAtDescending(
                                        input.closeId()),
                        "operational events must not be null");

        CloseConsolidationReadiness readiness =
                Objects.requireNonNull(
                        readinessEvaluator.evaluate(
                                input.closeId(),
                                events),
                        "consolidation readiness must not be null");

        Instant completedAt =
                Objects.requireNonNull(
                        applicationClock.now(),
                        "application time must not be null");

        if (!readiness.ready()) {
            String rejectionMessage =
                    readiness.eventsPresent()
                            ? NOT_READY_MESSAGE
                            : NO_EVENTS_MESSAGE;

            blockCloseWhenRequired(
                    operationalClose,
                    completedAt,
                    actor,
                    rejectionMessage);

            return CompleteOperationalCloseConsolidationResult
                    .rejected(
                            readiness.affectedEventIds()
                                    .stream()
                                    .map(
                                            OperationalEventId::value)
                                    .toList(),
                            rejectionMessage);
        }

        ConsolidationId consolidationId =
                new ConsolidationId(
                        nextUuid(
                                "generated consolidation UUID "
                                        + "must not be null"));

        Consolidation consolidation =
                Consolidation.complete(
                        consolidationId,
                        operationalClose,
                        events,
                        input.actualBalance(),
                        completedAt,
                        actor);

        consolidationRepository
                .findCurrentByCloseId(
                        input.closeId())
                .map(
                        current ->
                                current.invalidate(
                                        completedAt,
                                        PREVIOUS_INVALIDATION_REASON))
                .ifPresent(
                        consolidationRepository
                                ::saveInvalidation);

        consolidationRepository.saveNew(
                consolidation);

        OperationalClose validatedClose =
                reviseState(
                        operationalClose,
                        OperationalCloseState.VALIDATED,
                        completedAt,
                        actor);

        closeRevisionRepository.saveRevision(
                validatedClose);

        CloseStateTransition transition =
                new CloseStateTransition(
                        new CloseStateTransitionId(
                                nextUuid(
                                        "generated close transition UUID "
                                                + "must not be null")),
                        operationalClose.id(),
                        operationalClose.state(),
                        OperationalCloseState.VALIDATED,
                        CONSOLIDATION_COMPLETED,
                        null,
                        completedAt,
                        actor);

        closeRevisionRepository
                .appendConsolidationStateTransition(
                        transition,
                        consolidationId);

        return CompleteOperationalCloseConsolidationResult
                .consolidated(
                        consolidationId.value());
    }

    private void blockCloseWhenRequired(
            OperationalClose operationalClose,
            Instant occurredAt,
            AuditActor actor,
            String detail) {

        if (operationalClose.state()
                == OperationalCloseState.BLOCKED) {

            return;
        }

        OperationalClose blockedClose =
                reviseState(
                        operationalClose,
                        OperationalCloseState.BLOCKED,
                        occurredAt,
                        actor);

        closeRevisionRepository.saveRevision(
                blockedClose);

        CloseStateTransition transition =
                new CloseStateTransition(
                        new CloseStateTransitionId(
                                nextUuid(
                                        "generated close transition UUID "
                                                + "must not be null")),
                        operationalClose.id(),
                        operationalClose.state(),
                        OperationalCloseState.BLOCKED,
                        CONSOLIDATION_REJECTED,
                        detail,
                        occurredAt,
                        actor);

        closeRevisionRepository.appendStateTransition(
                transition);
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

    private static ConsolidationInput parseInput(
            CompleteOperationalCloseConsolidationCommand command) {

        if (command.closeId() == null) {
            throw new IllegalArgumentException(
                    "closeId must not be null");
        }

        BigDecimal actualBalance =
                command.actualBalance();

        if (actualBalance == null) {
            throw new IllegalArgumentException(
                    "actual balance must not be null");
        }

        if (actualBalance.signum() < 0) {
            throw new IllegalArgumentException(
                    "actual balance must not be negative");
        }

        if (actualBalance.scale()
                > MAXIMUM_SCALE) {

            throw new IllegalArgumentException(
                    "actual balance must not exceed four decimal places");
        }

        int integerDigits =
                Math.max(
                        0,
                        actualBalance.precision()
                                - actualBalance.scale());

        if (integerDigits
                > MAXIMUM_INTEGER_DIGITS) {

            throw new IllegalArgumentException(
                    "actual balance exceeds numeric(19,4)");
        }

        return new ConsolidationInput(
                new OperationalCloseId(
                        command.closeId()),
                actualBalance);
    }

    private UUID nextUuid(
            String nullMessage) {

        return Objects.requireNonNull(
                uuidGenerator.next(),
                nullMessage);
    }

    private record ConsolidationInput(
            OperationalCloseId closeId,
            BigDecimal actualBalance) {
    }

}