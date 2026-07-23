package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.SupportingEvidenceRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;

/**
 * Logically deactivates Supporting Evidence and atomically revises its owning
 * Operational Event and Operational Close when required.
 */
public final class DeactivateSupportingEvidence {

    private final OperationalCloseLockRepository
            closeLockRepository;

    private final OperationalEventRevisionRepository
            eventRevisionRepository;

    private final SupportingEvidenceRepository
            evidenceRepository;

    private final CurrentActorProvider
            currentActorProvider;

    private final ApplicationClock
            applicationClock;

    private final TransactionRunner
            transactionRunner;

    private final OperationalDependencyRevisionCoordinator
            revisionCoordinator;

    public DeactivateSupportingEvidence(
            OperationalCloseLockRepository closeLockRepository,
            OperationalEventRevisionRepository eventRevisionRepository,
            SupportingEvidenceRepository evidenceRepository,
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

        this.evidenceRepository =
                Objects.requireNonNull(
                        evidenceRepository);

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

    public DeactivateSupportingEvidenceResult execute(
            DeactivateSupportingEvidenceCommand command) {

        Objects.requireNonNull(
                command,
                "command must not be null");

        return transactionRunner.execute(
                () -> executeInsideTransaction(
                        command));
    }

    private DeactivateSupportingEvidenceResult
            executeInsideTransaction(
                    DeactivateSupportingEvidenceCommand command) {

        AuthenticatedPrincipal principal =
                Objects.requireNonNull(
                        currentActorProvider.currentActor(),
                        "authenticated principal must not be null");

        if (!AuditActor.RESPONSIBLE_USER_ID.equals(
                principal.userId())) {

            return DeactivateSupportingEvidenceResult
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
            return DeactivateSupportingEvidenceResult
                    .invalidInput(
                            exception.getMessage());
        }

        Optional<OperationalClose> lockedClose =
                closeLockRepository.findByIdForUpdate(
                        input.closeId());

        if (lockedClose.isEmpty()) {
            return DeactivateSupportingEvidenceResult
                    .closeNotFound();
        }

        OperationalClose operationalClose =
                lockedClose.orElseThrow();

        if (operationalClose.state()
                == OperationalCloseState.SENT_TO_ACCOUNTING) {

            return DeactivateSupportingEvidenceResult
                    .closeNotEditable();
        }

        Optional<OperationalEvent> lockedEvent =
                eventRevisionRepository.findByIdForUpdate(
                        input.closeId(),
                        input.eventId());

        if (lockedEvent.isEmpty()) {
            return DeactivateSupportingEvidenceResult
                    .eventNotFound();
        }

        Optional<SupportingEvidence> lockedEvidence =
                evidenceRepository.findByIdForUpdate(
                        input.closeId(),
                        input.eventId(),
                        input.evidenceId());

        if (lockedEvidence.isEmpty()) {
            return DeactivateSupportingEvidenceResult
                    .evidenceNotFound();
        }

        SupportingEvidence supportingEvidence =
                lockedEvidence.orElseThrow();

        if (!supportingEvidence.active()) {
            return DeactivateSupportingEvidenceResult
                    .evidenceAlreadyInactive();
        }

        Instant deactivatedAt =
                Objects.requireNonNull(
                        applicationClock.now(),
                        "application time must not be null");

        SupportingEvidence deactivatedEvidence;

        try {
            deactivatedEvidence =
                    supportingEvidence.deactivate(
                            deactivatedAt,
                            actor);
        }
        catch (IllegalArgumentException exception) {
            return DeactivateSupportingEvidenceResult
                    .invalidInput(
                            exception.getMessage());
        }

        revisionCoordinator.applyAndPersist(
                operationalClose,
                lockedEvent.orElseThrow(),
                deactivatedAt,
                actor);

        evidenceRepository.saveRevision(
                deactivatedEvidence);

        return DeactivateSupportingEvidenceResult.deactivated(
                input.evidenceId()
                        .value());
    }

    private static DeactivationInput parseInput(
            DeactivateSupportingEvidenceCommand command) {

        if (command.closeId() == null) {
            throw new IllegalArgumentException(
                    "closeId must not be null");
        }

        if (command.eventId() == null) {
            throw new IllegalArgumentException(
                    "eventId must not be null");
        }

        if (command.evidenceId() == null) {
            throw new IllegalArgumentException(
                    "evidenceId must not be null");
        }

        return new DeactivationInput(
                new OperationalCloseId(
                        command.closeId()),
                new OperationalEventId(
                        command.eventId()),
                new SupportingEvidenceId(
                        command.evidenceId()));
    }

    private record DeactivationInput(
            OperationalCloseId closeId,
            OperationalEventId eventId,
            SupportingEvidenceId evidenceId) {
    }

}