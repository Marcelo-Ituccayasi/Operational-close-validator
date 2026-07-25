package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventDependentResultInvalidator;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalDependencyRevision;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;

/**
 * Applies and persists the common Operational Event and Close revision caused
 * by a relevant Supporting Evidence or Event Authorization mutation.
 */
public final class OperationalDependencyRevisionCoordinator {

    public static final String DEPENDENT_DATA_REVISED =
            "DEPENDENT_DATA_REVISED";

    private final OperationalEventRevisionRepository
            eventRevisionRepository;

    private final OperationalCloseRevisionRepository
            closeRevisionRepository;

    private final OperationalEventDependentResultInvalidator
            dependentResultInvalidator;

    private final UuidGenerator uuidGenerator;

    public OperationalDependencyRevisionCoordinator(
            OperationalEventRevisionRepository eventRevisionRepository,
            OperationalCloseRevisionRepository closeRevisionRepository,
            OperationalEventDependentResultInvalidator
                    dependentResultInvalidator,
            UuidGenerator uuidGenerator) {

        this.eventRevisionRepository =
                Objects.requireNonNull(
                        eventRevisionRepository);

        this.closeRevisionRepository =
                Objects.requireNonNull(
                        closeRevisionRepository);

        this.dependentResultInvalidator =
                Objects.requireNonNull(
                        dependentResultInvalidator);

        this.uuidGenerator =
                Objects.requireNonNull(
                        uuidGenerator);
    }

    public OperationalDependencyRevision applyAndPersist(
            OperationalClose lockedClose,
            OperationalEvent lockedEvent,
            Instant revisedAt,
            AuditActor actor) {

        Objects.requireNonNull(
                lockedClose,
                "lockedClose must not be null");

        Objects.requireNonNull(
                lockedEvent,
                "lockedEvent must not be null");

        Objects.requireNonNull(
                revisedAt,
                "revisedAt must not be null");

        Objects.requireNonNull(
                actor,
                "actor must not be null");

        OperationalDependencyRevision revision =
                OperationalDependencyRevision.apply(
                        lockedClose,
                        lockedEvent,
                        revisedAt,
                        actor);

        dependentResultInvalidator
                .invalidateForRevisions(
                        lockedClose.id(),
                        List.of(
                                lockedEvent.id()));

        eventRevisionRepository.saveRevision(
                revision.revisedEvent());

        if (revision.eventStateChanged()) {
            eventRevisionRepository.appendStateTransition(
                    eventTransition(
                            lockedEvent,
                            revision.revisedEvent(),
                            revisedAt,
                            actor));
        }

        if (revision.closeStateChanged()) {
            closeRevisionRepository.saveRevision(
                    revision.revisedClose());

            closeRevisionRepository.appendStateTransition(
                    closeTransition(
                            lockedClose,
                            revision.revisedClose(),
                            revisedAt,
                            actor));
        }

        return revision;
    }

    private EventStateTransition eventTransition(
            OperationalEvent previousEvent,
            OperationalEvent revisedEvent,
            Instant revisedAt,
            AuditActor actor) {

        UUID transitionUuid =
                Objects.requireNonNull(
                        uuidGenerator.next(),
                        "generated event transition UUID must not be null");

        return new EventStateTransition(
                new EventStateTransitionId(
                        transitionUuid),
                revisedEvent.id(),
                previousEvent.state(),
                revisedEvent.state(),
                DEPENDENT_DATA_REVISED,
                null,
                null,
                revisedAt,
                actor);
    }

    private CloseStateTransition closeTransition(
            OperationalClose previousClose,
            OperationalClose revisedClose,
            Instant revisedAt,
            AuditActor actor) {

        UUID transitionUuid =
                Objects.requireNonNull(
                        uuidGenerator.next(),
                        "generated close transition UUID must not be null");

        return new CloseStateTransition(
                new CloseStateTransitionId(
                        transitionUuid),
                revisedClose.id(),
                previousClose.state(),
                revisedClose.state(),
                DEPENDENT_DATA_REVISED,
                null,
                revisedAt,
                actor);
    }

}