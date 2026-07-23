package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventRequirementPolicy;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;

/**
 * Creates an Operational Event and its initial state transition atomically.
 *
 * The owning Operational Close is locked before dependent data is read.
 */
public final class CreateOperationalEvent {

    private final OperationalCloseLockRepository
            closeLockRepository;

    private final OperationalEventRepository
            eventRepository;

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

    public CreateOperationalEvent(
            OperationalCloseLockRepository closeLockRepository,
            OperationalEventRepository eventRepository,
            CurrentActorProvider currentActorProvider,
            ApplicationClock applicationClock,
            UuidGenerator uuidGenerator,
            TransactionRunner transactionRunner,
            OperationalEventRequirementPolicy requirementPolicy) {

        this.closeLockRepository =
                Objects.requireNonNull(
                        closeLockRepository);

        this.eventRepository =
                Objects.requireNonNull(
                        eventRepository);

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
    }

    public CreateOperationalEventResult execute(
            CreateOperationalEventCommand command) {

        Objects.requireNonNull(
                command,
                "command must not be null");

        try {
            return transactionRunner.execute(
                    () -> executeInsideTransaction(
                            command));
        }
        catch (DuplicateOperationalEventCancellationException exception) {
            return CreateOperationalEventResult
                    .cancellationConflict();
        }
    }

    private CreateOperationalEventResult
            executeInsideTransaction(
                    CreateOperationalEventCommand command) {

        AuthenticatedPrincipal principal =
                currentActorProvider.currentActor();

        if (!AuditActor.RESPONSIBLE_USER_ID.equals(
                principal.userId())) {

            return CreateOperationalEventResult
                    .actorRejected();
        }

        AuditActor actor;
        OperationalCloseId closeId;
        OperationalEventType eventType;
        OperationalEventAmount amount;

        try {
            actor =
                    new AuditActor(
                            principal.userId(),
                            principal.username());

            closeId =
                    new OperationalCloseId(
                            command.closeId());

            eventType =
                    parseEventType(
                            command.eventType());

            amount =
                    new OperationalEventAmount(
                            command.amount());
        }
        catch (IllegalArgumentException exception) {
            return CreateOperationalEventResult
                    .invalidInput(
                            exception.getMessage());
        }

        Optional<OperationalClose> closeOptional =
                closeLockRepository.findByIdForUpdate(
                        closeId);

        if (closeOptional.isEmpty()) {
            return CreateOperationalEventResult
                    .closeNotFound();
        }

        OperationalClose operationalClose =
                closeOptional.orElseThrow();

        if (!allowsEventCreation(
                operationalClose.state())) {

            return CreateOperationalEventResult
                    .closeNotEditable();
        }

        OperationalEventRequirements requirements =
                Objects.requireNonNull(
                        requirementPolicy.determine(
                                eventType,
                                amount),
                        "requirement policy result must not be null");

        UUID eventUuid =
                Objects.requireNonNull(
                        uuidGenerator.next(),
                        "generated event UUID must not be null");

        UUID transitionUuid =
                Objects.requireNonNull(
                        uuidGenerator.next(),
                        "generated transition UUID must not be null");

        Instant now =
                Objects.requireNonNull(
                        applicationClock.now(),
                        "application time must not be null");

        OperationalEventId eventId =
                new OperationalEventId(
                        eventUuid);

        OperationalEvent operationalEvent;

        try {
            operationalEvent =
                    createEvent(
                            command,
                            operationalClose,
                            eventType,
                            amount,
                            eventId,
                            requirements,
                            now,
                            actor);
        }
        catch (IllegalArgumentException exception) {
            return CreateOperationalEventResult
                    .invalidInput(
                            exception.getMessage());
        }

        if (operationalEvent == null) {
            return CreateOperationalEventResult
                    .reversedEventNotFound();
        }

        if (eventType
                == OperationalEventType.CANCELLATION
                && eventRepository.existsCancellationFor(
                        operationalEvent.reversedEventId())) {

            return CreateOperationalEventResult
                    .cancellationConflict();
        }

        EventStateTransition initialTransition =
                EventStateTransition.initial(
                        new EventStateTransitionId(
                                transitionUuid),
                        eventId,
                        now,
                        actor);

        eventRepository.saveNew(
                operationalEvent,
                initialTransition);

        return CreateOperationalEventResult.created(
                eventUuid);
    }

    private OperationalEvent createEvent(
            CreateOperationalEventCommand command,
            OperationalClose operationalClose,
            OperationalEventType eventType,
            OperationalEventAmount amount,
            OperationalEventId eventId,
            OperationalEventRequirements requirements,
            Instant now,
            AuditActor actor) {

        if (eventType
                != OperationalEventType.CANCELLATION) {

            if (command.reversedEventId() != null) {
                throw new IllegalArgumentException(
                        "reversed event id is only allowed for cancellation");
            }

            return OperationalEvent.create(
                    eventId,
                    operationalClose.id(),
                    eventType,
                    amount,
                    command.occurredAt(),
                    command.responsibleName(),
                    command.description(),
                    requirements.evidenceRequired(),
                    requirements.authorizationRequired(),
                    now,
                    actor);
        }

        if (command.reversedEventId() == null) {
            throw new IllegalArgumentException(
                    "reversed event id is required for cancellation");
        }

        OperationalEventId reversedEventId =
                new OperationalEventId(
                        command.reversedEventId());

        Optional<OperationalEvent> reversedEventOptional =
                eventRepository.findById(
                        reversedEventId);

        if (reversedEventOptional.isEmpty()) {
            return null;
        }

        OperationalEvent reversedEvent =
                reversedEventOptional.orElseThrow();

        if (amount.value().compareTo(
                reversedEvent.amount().value())
                != 0) {

            throw new IllegalArgumentException(
                    "cancellation amount must equal reversed event amount");
        }

        return OperationalEvent.createCancellation(
                eventId,
                operationalClose.id(),
                reversedEvent,
                command.occurredAt(),
                command.responsibleName(),
                command.description(),
                requirements.evidenceRequired(),
                requirements.authorizationRequired(),
                now,
                actor);
    }

    private static OperationalEventType parseEventType(
            String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "event type must not be blank");
        }

        try {
            return OperationalEventType.valueOf(
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT));
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "event type is not supported",
                    exception);
        }
    }

    private static boolean allowsEventCreation(
            OperationalCloseState state) {

        return state
                        == OperationalCloseState.PREPARATION
                || state
                        == OperationalCloseState.BLOCKED;
    }

}