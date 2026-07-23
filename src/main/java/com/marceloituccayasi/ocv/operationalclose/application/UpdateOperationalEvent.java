package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventDependentResultInvalidator;
import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventRequirementPolicy;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;

/**
 * Revises an Operational Event and recalculates a dependent cancellation
 * atomically under the owning Operational Close lock.
 */
public final class UpdateOperationalEvent {

    private static final String REVISION_CAUSE_CODE =
            "EVENT_DATA_REVISED";

    private final OperationalCloseLockRepository
            closeLockRepository;

    private final OperationalEventRevisionRepository
            eventRevisionRepository;

    private final CurrentActorProvider
            currentActorProvider;

    private final ApplicationClock
            applicationClock;

    private final UuidGenerator
            uuidGenerator;

    private final TransactionRunner
            transactionRunner;

    private final OperationalEventRequirementPolicy
            requirementPolicy;

    private final OperationalEventDependentResultInvalidator
            dependentResultInvalidator;

    public UpdateOperationalEvent(
            OperationalCloseLockRepository closeLockRepository,
            OperationalEventRevisionRepository eventRevisionRepository,
            CurrentActorProvider currentActorProvider,
            ApplicationClock applicationClock,
            UuidGenerator uuidGenerator,
            TransactionRunner transactionRunner,
            OperationalEventRequirementPolicy requirementPolicy,
            OperationalEventDependentResultInvalidator
                    dependentResultInvalidator) {

        this.closeLockRepository =
                Objects.requireNonNull(
                        closeLockRepository);

        this.eventRevisionRepository =
                Objects.requireNonNull(
                        eventRevisionRepository);

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

        this.requirementPolicy =
                Objects.requireNonNull(
                        requirementPolicy);

        this.dependentResultInvalidator =
                Objects.requireNonNull(
                        dependentResultInvalidator);
    }

    /*
     * Package-private compatibility constructor used by the existing
     * application-unit tests. Production configuration must use the public
     * constructor and provide the invalidation port explicitly.
     */
    UpdateOperationalEvent(
            OperationalCloseLockRepository closeLockRepository,
            OperationalEventRevisionRepository eventRevisionRepository,
            CurrentActorProvider currentActorProvider,
            ApplicationClock applicationClock,
            UuidGenerator uuidGenerator,
            TransactionRunner transactionRunner,
            OperationalEventRequirementPolicy requirementPolicy) {

        this(
                closeLockRepository,
                eventRevisionRepository,
                currentActorProvider,
                applicationClock,
                uuidGenerator,
                transactionRunner,
                requirementPolicy,
                (closeId, revisedEventIds) -> {
                    Objects.requireNonNull(closeId);
                    Objects.requireNonNull(revisedEventIds);
                });
    }

    public UpdateOperationalEventResult execute(
            UpdateOperationalEventCommand command) {

        Objects.requireNonNull(
                command,
                "command must not be null");

        try {
            return transactionRunner.execute(
                    () -> executeInsideTransaction(
                            command));
        }
        catch (DuplicateOperationalEventCancellationException exception) {
            return UpdateOperationalEventResult
                    .cancellationConflict();
        }
    }

    private UpdateOperationalEventResult
            executeInsideTransaction(
                    UpdateOperationalEventCommand command) {

        AuthenticatedPrincipal principal =
                currentActorProvider.currentActor();

        if (!AuditActor.RESPONSIBLE_USER_ID.equals(
                principal.userId())) {

            return UpdateOperationalEventResult
                    .actorRejected();
        }

        AuditActor actor;
        RevisionInput input;

        try {
            actor =
                    new AuditActor(
                            principal.userId(),
                            principal.username());

            input =
                    parseInput(command);
        }
        catch (IllegalArgumentException exception) {
            return UpdateOperationalEventResult
                    .invalidInput(
                            exception.getMessage());
        }

        Optional<OperationalClose> lockedClose =
                closeLockRepository.findByIdForUpdate(
                        input.closeId());

        if (lockedClose.isEmpty()) {
            return UpdateOperationalEventResult
                    .closeNotFound();
        }

        if (!isEditable(
                lockedClose.orElseThrow())) {

            return UpdateOperationalEventResult
                    .closeNotEditable();
        }

        Optional<OperationalEvent> lockedEvent =
                eventRevisionRepository.findByIdForUpdate(
                        input.closeId(),
                        input.eventId());

        if (lockedEvent.isEmpty()) {
            return UpdateOperationalEventResult
                    .eventNotFound();
        }

        OperationalEvent currentEvent =
                lockedEvent.orElseThrow();

        if (!currentEvent.closeId()
                .equals(input.closeId())) {

            return UpdateOperationalEventResult
                    .eventNotFound();
        }

        Optional<OperationalEvent> dependentCancellation =
        eventRevisionRepository
                .findCancellationByReversedEventIdForUpdate(
                        input.closeId(),
                        currentEvent.id());

        Instant revisedAt =
                Objects.requireNonNull(
                        applicationClock.now(),
                        "application time must not be null");

        OperationalEventRequirements requirements;

        try {
            requirements =
                    Objects.requireNonNull(
                            requirementPolicy.determine(
                                    input.eventType(),
                                    input.amount()),
                            "event requirements must not be null");
        }
        catch (IllegalArgumentException exception) {
            return UpdateOperationalEventResult
                    .invalidInput(
                            exception.getMessage());
        }

        RevisionPlan revisionPlan;

        try {
            if (input.eventType()
                    == OperationalEventType.CANCELLATION) {

                UpdateOperationalEventResult conflict =
                        validateConversionToCancellation(
                                currentEvent,
                                dependentCancellation,
                                input);

                if (conflict != null) {
                    return conflict;
                }

                Optional<OperationalEvent> reversedEvent =
                eventRevisionRepository
                .findByIdForUpdate(
                        input.closeId(),
                        input.reversedEventId());

                if (reversedEvent.isEmpty()) {
                    return UpdateOperationalEventResult
                            .reversedEventNotFound();
                }

                OperationalEvent lockedReversedEvent =
                        reversedEvent.orElseThrow();

                Optional<OperationalEvent>
                    existingCancellation =
                            eventRevisionRepository
                            .findCancellationByReversedEventIdForUpdate(
                                    input.closeId(),
                                    lockedReversedEvent.id());

                if (existingCancellation.isPresent()
                        && !existingCancellation
                                .orElseThrow()
                                .id()
                                .equals(currentEvent.id())) {

                    return UpdateOperationalEventResult
                            .cancellationConflict();
                }

                if (input.amount()
                        .value()
                        .compareTo(
                                lockedReversedEvent
                                        .amount()
                                        .value())
                        != 0) {

                    return UpdateOperationalEventResult
                            .invalidInput(
                                    "cancellation amount must equal "
                                            + "the reversed event amount");
                }

                OperationalEvent revisedEvent =
                        currentEvent.reviseCancellation(
                                lockedReversedEvent,
                                input.occurredAt(),
                                input.responsibleName(),
                                input.description(),
                                requirements.evidenceRequired(),
                                requirements.authorizationRequired(),
                                revisedAt,
                                actor);

                revisionPlan =
                        new RevisionPlan(
                                preparePersistence(
                                        revisedEvent,
                                        revisedAt,
                                        actor),
                                null);
            }
            else {
                if (input.reversedEventId() != null) {
                    return UpdateOperationalEventResult
                            .invalidInput(
                                    "only a cancellation may reference "
                                            + "a reversed event");
                }

                OperationalEvent revisedEvent =
                        currentEvent.reviseRegular(
                                input.eventType(),
                                input.amount(),
                                input.occurredAt(),
                                input.responsibleName(),
                                input.description(),
                                requirements.evidenceRequired(),
                                requirements.authorizationRequired(),
                                revisedAt,
                                actor);

                RevisionPersistence revisedPersistence =
                        preparePersistence(
                                revisedEvent,
                                revisedAt,
                                actor);

                RevisionPersistence
                        recalculatedCancellationPersistence =
                                dependentCancellation
                                        .map(cancellation ->
                                                cancellation
                                                        .recalculateFromRevisedOriginal(
                                                                revisedPersistence
                                                                        .event(),
                                                                revisedAt,
                                                                actor))
                                        .map(recalculated ->
                                                preparePersistence(
                                                        recalculated,
                                                        revisedAt,
                                                        actor))
                                        .orElse(null);

                revisionPlan =
                        new RevisionPlan(
                                revisedPersistence,
                                recalculatedCancellationPersistence);
            }
        }
        catch (IllegalArgumentException exception) {
            return UpdateOperationalEventResult
                    .invalidInput(
                            exception.getMessage());
        }

        invalidateDependentResults(
                input.closeId(),
                revisionPlan);

        persistRevisionPlan(
                revisionPlan);

        return UpdateOperationalEventResult.updated(
                currentEvent.id().value());
    }

    private UpdateOperationalEventResult
            validateConversionToCancellation(
                    OperationalEvent currentEvent,
                    Optional<OperationalEvent> dependentCancellation,
                    RevisionInput input) {

        if (dependentCancellation.isPresent()) {
            return UpdateOperationalEventResult
                    .cancellationConflict();
        }

        if (input.reversedEventId() == null) {
            return UpdateOperationalEventResult
                    .invalidInput(
                            "cancellation requires a reversed event");
        }

        if (currentEvent.id()
                .equals(input.reversedEventId())) {

            return UpdateOperationalEventResult
                    .invalidInput(
                            "cancellation must not reference itself");
        }

        return null;
    }

    private void invalidateDependentResults(
            OperationalCloseId closeId,
            RevisionPlan revisionPlan) {

        List<OperationalEventId> revisedEventIds;

        if (revisionPlan.dependentCancellation() == null) {
            revisedEventIds =
                    List.of(
                            revisionPlan
                                    .primary()
                                    .event()
                                    .id());
        }
        else {
            revisedEventIds =
                    List.of(
                            revisionPlan
                                    .primary()
                                    .event()
                                    .id(),
                            revisionPlan
                                    .dependentCancellation()
                                    .event()
                                    .id());
        }

        dependentResultInvalidator
                .invalidateForRevisions(
                        closeId,
                        revisedEventIds);
    }

    private void persistRevisionPlan(
            RevisionPlan revisionPlan) {

        eventRevisionRepository.saveRevision(
                revisionPlan.primary().event());

        if (revisionPlan.dependentCancellation() != null) {
            eventRevisionRepository.saveRevision(
                    revisionPlan
                            .dependentCancellation()
                            .event());
        }

        if (revisionPlan.primary().transition() != null) {
            eventRevisionRepository.appendStateTransition(
                    revisionPlan
                            .primary()
                            .transition());
        }

        if (revisionPlan.dependentCancellation() != null
                && revisionPlan
                        .dependentCancellation()
                        .transition()
                        != null) {

            eventRevisionRepository.appendStateTransition(
                    revisionPlan
                            .dependentCancellation()
                            .transition());
        }
    }

    private RevisionPersistence preparePersistence(
            OperationalEvent revisedEvent,
            Instant revisedAt,
            AuditActor actor) {

        if (revisedEvent.state()
                != OperationalEventState.VALIDATED) {

            return new RevisionPersistence(
                    revisedEvent,
                    null);
        }

        OperationalEvent registeredEvent =
                new OperationalEvent(
                        revisedEvent.id(),
                        revisedEvent.closeId(),
                        revisedEvent.eventType(),
                        revisedEvent.amount(),
                        revisedEvent.balanceEffect(),
                        revisedEvent.reversedEventId(),
                        revisedEvent.occurredAt(),
                        revisedEvent.registeredAt(),
                        revisedEvent.responsibleName(),
                        revisedEvent.description(),
                        OperationalEventState.REGISTERED,
                        revisedEvent.evidenceRequired(),
                        revisedEvent.authorizationRequired(),
                        revisedEvent.dataRevision(),
                        revisedAt,
                        revisedEvent.createdAt(),
                        revisedEvent.createdBy(),
                        revisedAt,
                        actor);

        UUID transitionUuid =
                Objects.requireNonNull(
                        uuidGenerator.next(),
                        "generated transition UUID must not be null");

        EventStateTransition transition =
                new EventStateTransition(
                        new EventStateTransitionId(
                                transitionUuid),
                        registeredEvent.id(),
                        OperationalEventState.VALIDATED,
                        OperationalEventState.REGISTERED,
                        REVISION_CAUSE_CODE,
                        null,
                        null,
                        revisedAt,
                        actor);

        return new RevisionPersistence(
                registeredEvent,
                transition);
    }

    private static RevisionInput parseInput(
            UpdateOperationalEventCommand command) {

        if (command.closeId() == null) {
            throw new IllegalArgumentException(
                    "closeId must not be null");
        }

        if (command.eventId() == null) {
            throw new IllegalArgumentException(
                    "eventId must not be null");
        }

        if (command.amount() == null) {
            throw new IllegalArgumentException(
                    "amount must not be null");
        }

        if (command.occurredAt() == null) {
            throw new IllegalArgumentException(
                    "occurredAt must not be null");
        }

        String eventType =
                requiredText(
                        command.eventType(),
                        "eventType");

        String responsibleName =
                requiredText(
                        command.responsibleName(),
                        "responsibleName");

        String description =
                requiredText(
                        command.description(),
                        "description");

        OperationalEventType parsedType;

        try {
            parsedType =
                    OperationalEventType.valueOf(
                            eventType.toUpperCase(
                                    Locale.ROOT));
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "eventType is not supported",
                    exception);
        }

        OperationalEventId reversedEventId =
                command.reversedEventId() == null
                        ? null
                        : new OperationalEventId(
                                command.reversedEventId());

        return new RevisionInput(
                new OperationalCloseId(
                        command.closeId()),
                new OperationalEventId(
                        command.eventId()),
                parsedType,
                new OperationalEventAmount(
                        command.amount()),
                reversedEventId,
                command.occurredAt(),
                responsibleName,
                description);
    }

    private static boolean isEditable(
            OperationalClose operationalClose) {

        return operationalClose.state()
                        == OperationalCloseState.PREPARATION
                || operationalClose.state()
                        == OperationalCloseState.BLOCKED;
    }

    private static String requiredText(
            String value,
            String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank");
        }

        return value.trim();
    }

    private record RevisionInput(
            OperationalCloseId closeId,
            OperationalEventId eventId,
            OperationalEventType eventType,
            OperationalEventAmount amount,
            OperationalEventId reversedEventId,
            Instant occurredAt,
            String responsibleName,
            String description) {
    }

    private record RevisionPersistence(
            OperationalEvent event,
            EventStateTransition transition) {
    }

    private record RevisionPlan(
            RevisionPersistence primary,
            RevisionPersistence dependentCancellation) {
    }

}