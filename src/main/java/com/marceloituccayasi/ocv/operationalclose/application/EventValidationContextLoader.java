package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventAuthorizationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.SupportingEvidenceRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationContext;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

public final class EventValidationContextLoader {

    private final OperationalEventRepository
            eventRepository;

    private final SupportingEvidenceRepository
            evidenceRepository;

    private final EventAuthorizationRepository
            authorizationRepository;

    public EventValidationContextLoader(
            OperationalEventRepository eventRepository,
            SupportingEvidenceRepository evidenceRepository,
            EventAuthorizationRepository authorizationRepository) {

        this.eventRepository =
                Objects.requireNonNull(
                        eventRepository);

        this.evidenceRepository =
                Objects.requireNonNull(
                        evidenceRepository);

        this.authorizationRepository =
                Objects.requireNonNull(
                        authorizationRepository);
    }

    public Optional<EventValidationContext> load(
            OperationalCloseId closeId,
            OperationalEventId eventId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        Optional<OperationalEvent> persistedEvent =
                eventRepository.findById(
                        eventId);

        if (persistedEvent.isEmpty()) {
            return Optional.empty();
        }

        OperationalEvent event =
                persistedEvent.orElseThrow();

        if (!event.closeId().equals(
                closeId)) {

            return Optional.empty();
        }

        return Optional.of(
                new EventValidationContext(
                        event,
                        evidenceRepository
                                .findAllByEventIdOrderByEvidenceDateDescending(
                                        eventId),
                        authorizationRepository
                                .findAllByEventIdOrderByAuthorizedAtDescending(
                                        eventId)));
    }

}