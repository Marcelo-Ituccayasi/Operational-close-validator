package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
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
 * Logically deactivates an Event Authorization and atomically revises its
 * owning Operational Event and Operational Close when required.
 */
public final class DeactivateEventAuthorization {

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

    private final TransactionRunner
            transactionRunner;

    private final OperationalDependencyRevisionCoordinator
            revisionCoordinator;

    public DeactivateEventAuthorization(
            OperationalCloseLockRepository closeLockRepository,
            OperationalEventRevisionRepository eventRevisionRepository,
            EventAuthorizationRepository authorizationRepository,
            CurrentActorProvider currentActorProvider,
            ApplicationClock applicationClock,
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

        this.transactionRunner =
                Objects.requireNonNull(
                        transactionRunner);

        this.revisionCoordinator =
                Objects.requireNonNull(
                        revisionCoordinator);
    }

    public DeactivateEventAuthorizationResult execute(
            DeactivateEventAuthorizationCommand command) {

        Objects.requireNonNull(
                command,
                "command must not be null");

        return transactionRunner.execute(
                () -> executeInsideTransaction(
                        command));
    }

    private DeactivateEventAuthorizationResult
            executeInsideTransaction(
                    DeactivateEventAuthorizationCommand command) {

        AuthenticatedPrincipal principal =
                Objects.requireNonNull(
                        currentActorProvider.currentActor(),
                        "authenticated principal must not be null");

        if (!AuditActor.RESPONSIBLE_USER_ID.equals(
                principal.userId())) {

            return DeactivateEventAuthorizationResult
                    .actorRejected();
        }

        AuditActor actor;
        DeactivationInput input;

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
            return DeactivateEventAuthorizationResult
                    .invalidInput(
                            exception.getMessage());
        }

        Optional<OperationalClose> lockedClose =
                closeLockRepository.findByIdForUpdate(
                        input.closeId());

        if (lockedClose.isEmpty()) {
            return DeactivateEventAuthorizationResult
                    .closeNotFound();
        }

        OperationalClose operationalClose =
                lockedClose.orElseThrow();

        if (operationalClose.state()
                == OperationalCloseState.SENT_TO_ACCOUNTING) {

            return DeactivateEventAuthorizationResult
                    .closeNotEditable();
        }

        Optional<OperationalEvent> lockedEvent =
                eventRevisionRepository.findByIdForUpdate(
                        input.closeId(),
                        input.eventId());

        if (lockedEvent.isEmpty()) {
            return DeactivateEventAuthorizationResult
                    .eventNotFound();
        }

        Optional<EventAuthorization> lockedAuthorization =
                authorizationRepository.findByIdForUpdate(
                        input.closeId(),
                        input.eventId(),
                        input.authorizationId());

        if (lockedAuthorization.isEmpty()) {
            return DeactivateEventAuthorizationResult
                    .authorizationNotFound();
        }

        EventAuthorization authorization =
                lockedAuthorization.orElseThrow();

        if (!authorization.active()) {
            return DeactivateEventAuthorizationResult
                    .authorizationAlreadyInactive();
        }

        Instant deactivatedAt =
                Objects.requireNonNull(
                        applicationClock.now(),
                        "application time must not be null");

        EventAuthorization deactivatedAuthorization;

        try {
            deactivatedAuthorization =
                    authorization.deactivate(
                            deactivatedAt,
                            actor);
        }
        catch (IllegalArgumentException exception) {
            return DeactivateEventAuthorizationResult
                    .invalidInput(
                            exception.getMessage());
        }

        revisionCoordinator.applyAndPersist(
                operationalClose,
                lockedEvent.orElseThrow(),
                deactivatedAt,
                actor);

        authorizationRepository.saveRevision(
                deactivatedAuthorization);

        return DeactivateEventAuthorizationResult.deactivated(
                input.authorizationId()
                        .value());
    }

    private static DeactivationInput parseInput(
            DeactivateEventAuthorizationCommand command) {

        if (command.closeId() == null) {
            throw new IllegalArgumentException(
                    "closeId must not be null");
        }

        if (command.eventId() == null) {
            throw new IllegalArgumentException(
                    "eventId must not be null");
        }

        if (command.authorizationId() == null) {
            throw new IllegalArgumentException(
                    "authorizationId must not be null");
        }

        return new DeactivationInput(
                new OperationalCloseId(
                        command.closeId()),
                new OperationalEventId(
                        command.eventId()),
                new EventAuthorizationId(
                        command.authorizationId()));
    }

    private record DeactivationInput(
            OperationalCloseId closeId,
            OperationalEventId eventId,
            EventAuthorizationId authorizationId) {
    }

}