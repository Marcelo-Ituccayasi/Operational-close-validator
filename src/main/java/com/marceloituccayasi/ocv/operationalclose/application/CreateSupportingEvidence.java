package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
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
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceLegibilityStatus;

/**
 * Creates Supporting Evidence and atomically revises its owning Operational
 * Event and Operational Close when required.
 */
public final class CreateSupportingEvidence {

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

    private final UuidGenerator
            uuidGenerator;

    private final TransactionRunner
            transactionRunner;

    private final OperationalDependencyRevisionCoordinator
            revisionCoordinator;

    public CreateSupportingEvidence(
            OperationalCloseLockRepository closeLockRepository,
            OperationalEventRevisionRepository eventRevisionRepository,
            SupportingEvidenceRepository evidenceRepository,
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

        this.evidenceRepository =
                Objects.requireNonNull(
                        evidenceRepository);

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

    public CreateSupportingEvidenceResult execute(
            CreateSupportingEvidenceCommand command) {

        Objects.requireNonNull(
                command,
                "command must not be null");

        return transactionRunner.execute(
                () -> executeInsideTransaction(
                        command,
                        null));
    }

    CreateSupportingEvidenceResult executeWithEvidenceId(
            CreateSupportingEvidenceCommand command,
            UUID evidenceUuid) {

        Objects.requireNonNull(
                command,
                "command must not be null");

        Objects.requireNonNull(
                evidenceUuid,
                "evidenceUuid must not be null");

        return transactionRunner.execute(
                () -> executeInsideTransaction(
                        command,
                        evidenceUuid));
    }

    private CreateSupportingEvidenceResult
            executeInsideTransaction(
                    CreateSupportingEvidenceCommand command,
                    UUID preassignedEvidenceUuid) {

        AuthenticatedPrincipal principal =
                Objects.requireNonNull(
                        currentActorProvider.currentActor(),
                        "authenticated principal must not be null");

        if (!AuditActor.RESPONSIBLE_USER_ID.equals(
                principal.userId())) {

            return CreateSupportingEvidenceResult
                    .actorRejected();
        }

        AuditActor actor;
        EvidenceInput input;

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
            return CreateSupportingEvidenceResult
                    .invalidInput(
                            exception.getMessage());
        }

        Optional<OperationalClose> lockedClose =
                closeLockRepository.findByIdForUpdate(
                        input.closeId());

        if (lockedClose.isEmpty()) {
            return CreateSupportingEvidenceResult
                    .closeNotFound();
        }

        OperationalClose operationalClose =
                lockedClose.orElseThrow();

        if (operationalClose.state()
                == OperationalCloseState.SENT_TO_ACCOUNTING) {

            return CreateSupportingEvidenceResult
                    .closeNotEditable();
        }

        Optional<OperationalEvent> lockedEvent =
                eventRevisionRepository.findByIdForUpdate(
                        input.closeId(),
                        input.eventId());

        if (lockedEvent.isEmpty()) {
            return CreateSupportingEvidenceResult
                    .eventNotFound();
        }

        Instant createdAt =
                Objects.requireNonNull(
                        applicationClock.now(),
                        "application time must not be null");

        UUID evidenceUuid =
                preassignedEvidenceUuid;

        if (evidenceUuid == null) {
            evidenceUuid =
                    Objects.requireNonNull(
                            uuidGenerator.next(),
                            "generated evidence UUID must not be null");
        }

        SupportingEvidence evidence;

        try {
            evidence =
                    SupportingEvidence.create(
                            new SupportingEvidenceId(
                                    evidenceUuid),
                            input.eventId(),
                            command.evidenceType(),
                            command.contentReference(),
                            command.supportedAmount(),
                            command.evidenceDate(),
                            input.legibilityStatus(),
                            createdAt,
                            actor);
        }
        catch (IllegalArgumentException exception) {
            return CreateSupportingEvidenceResult
                    .invalidInput(
                            exception.getMessage());
        }

        revisionCoordinator.applyAndPersist(
                operationalClose,
                lockedEvent.orElseThrow(),
                createdAt,
                actor);

        evidenceRepository.saveNew(
                evidence);

        return CreateSupportingEvidenceResult.created(
                evidenceUuid);
    }

    private static EvidenceInput parseInput(
            CreateSupportingEvidenceCommand command) {

        if (command.closeId() == null) {
            throw new IllegalArgumentException(
                    "closeId must not be null");
        }

        if (command.eventId() == null) {
            throw new IllegalArgumentException(
                    "eventId must not be null");
        }

        String legibilityValue =
                requireText(
                        command.legibilityStatus(),
                        "legibilityStatus");

        SupportingEvidenceLegibilityStatus
                legibilityStatus;

        try {
            legibilityStatus =
                    SupportingEvidenceLegibilityStatus
                            .valueOf(
                                    legibilityValue
                                            .toUpperCase(
                                                    Locale.ROOT));
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "legibility status is invalid",
                    exception);
        }

        return new EvidenceInput(
                new OperationalCloseId(
                        command.closeId()),
                new OperationalEventId(
                        command.eventId()),
                legibilityStatus);
    }

    private static String requireText(
            String value,
            String fieldName) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    fieldName + " must not be blank");
        }

        return value.trim();
    }

    private record EvidenceInput(
            OperationalCloseId closeId,
            OperationalEventId eventId,
            SupportingEvidenceLegibilityStatus
                    legibilityStatus) {
    }

}