package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventAuthorizationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorizationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

/**
 * Creates an Event Authorization and atomically revises its owning Operational
 * Event and Operational Close when required.
 */
public final class CreateEventAuthorization {

    private final OperationalCloseLockRepository
            closeLockRepository;

    private final OperationalEventRevisionRepository
            eventRevisionRepository;

    private final EventAuthorizationRepository
            authorizationRepository;

    private final CurrentActorProvider
            currentActorProvider;

    private final ApplicationClock
            applicationClock;

    private final UuidGenerator
            uuidGenerator;

    private final TransactionRunner
            transactionRunner;

    private final OperationalDependencyRevisionCoordinator
            revisionCoordinator;

    public CreateEventAuthorization(
            OperationalCloseLockRepository closeLockRepository,
            OperationalEventRevisionRepository eventRevisionRepository,
            EventAuthorizationRepository authorizationRepository,
            CurrentActorProvider currentActorProvider,
            ApplicationClock applicationClock,
            UuidGenerator uuidGenerator,
            TransactionRunner transactionRunner,
            OperationalDependencyRevisionCoordinator
                    revisionCoordinator) {

        this.closeLockRepository =
                Objects.requireNonNull(
                        closeLockRepository);

        this.eventRevisionRepository =
                Objects.requireNonNull(
                        eventRevisionRepository);

        this.authorizationRepository =
                Objects.requireNonNull(
                        authorizationRepository);

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

        this.revisionCoordinator =
                Objects.requireNonNull(
                        revisionCoordinator);
    }

    public CreateEventAuthorizationResult execute(
            CreateEventAuthorizationCommand command) {

        Objects.requireNonNull(
                command,
                "command must not be null");

        return transactionRunner.execute(
                () -> executeInsideTransaction(
                        command));
    }

    private CreateEventAuthorizationResult
            executeInsideTransaction(
                    CreateEventAuthorizationCommand command) {

        AuthenticatedPrincipal principal =
                Objects.requireNonNull(
                        currentActorProvider.currentActor(),
                        "authenticated principal must not be null");

        if (!AuditActor.RESPONSIBLE_USER_ID.equals(
                principal.userId())) {

            return CreateEventAuthorizationResult
                    .actorRejected();
        }

        AuditActor actor;
        AuthorizationInput input;

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
            return CreateEventAuthorizationResult
                    .invalidInput(
                            exception.getMessage());
        }

        Optional<OperationalClose> lockedClose =
                closeLockRepository.findByIdForUpdate(
                        input.closeId());

        if (lockedClose.isEmpty()) {
            return CreateEventAuthorizationResult
                    .closeNotFound();
        }

        OperationalClose operationalClose =
                lockedClose.orElseThrow();

        if (operationalClose.state()
                == OperationalCloseState.SENT_TO_ACCOUNTING) {

            return CreateEventAuthorizationResult
                    .closeNotEditable();
        }

        Optional<OperationalEvent> lockedEvent =
                eventRevisionRepository.findByIdForUpdate(
                        input.closeId(),
                        input.eventId());

        if (lockedEvent.isEmpty()) {
            return CreateEventAuthorizationResult
                    .eventNotFound();
        }

        Instant createdAt =
                Objects.requireNonNull(
                        applicationClock.now(),
                        "application time must not be null");

        UUID authorizationUuid =
                Objects.requireNonNull(
                        uuidGenerator.next(),
                        "generated authorization UUID must not be null");

        EventAuthorization authorization;

        try {
            authorization =
                    EventAuthorization.create(
                            new EventAuthorizationId(
                                    authorizationUuid),
                            input.eventId(),
                            command.authorizedByName(),
                            command.reason(),
                            command.authorizedAt(),
                            command.formalReference(),
                            createdAt,
                            actor);
        }
        catch (IllegalArgumentException exception) {
            return CreateEventAuthorizationResult
                    .invalidInput(
                            exception.getMessage());
        }

        revisionCoordinator.applyAndPersist(
                operationalClose,
                lockedEvent.orElseThrow(),
                createdAt,
                actor);

        authorizationRepository.saveNew(
                authorization);

        return CreateEventAuthorizationResult.created(
                authorizationUuid);
    }

    private static AuthorizationInput parseInput(
            CreateEventAuthorizationCommand command) {

        if (command.closeId() == null) {
            throw new IllegalArgumentException(
                    "closeId must not be null");
        }

        if (command.eventId() == null) {
            throw new IllegalArgumentException(
                    "eventId must not be null");
        }

        return new AuthorizationInput(
                new OperationalCloseId(
                        command.closeId()),
                new OperationalEventId(
                        command.eventId()));
    }

    private record AuthorizationInput(
            OperationalCloseId closeId,
            OperationalEventId eventId) {
    }

}