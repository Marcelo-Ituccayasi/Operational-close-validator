package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventAuthorizationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.SupportingEvidenceRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

/**
 * Retrieves current and historical Supporting Evidence and Event
 * Authorizations within the persisted ownership scope of an Operational Event.
 */
public final class GetOperationalEventSupportingInformation {

    private final OperationalEventRepository
            eventRepository;

    private final SupportingEvidenceRepository
            evidenceRepository;

    private final EventAuthorizationRepository
            authorizationRepository;

    private final TransactionRunner
            transactionRunner;

    public GetOperationalEventSupportingInformation(
            OperationalEventRepository eventRepository,
            SupportingEvidenceRepository evidenceRepository,
            EventAuthorizationRepository authorizationRepository,
            TransactionRunner transactionRunner) {

        this.eventRepository =
                Objects.requireNonNull(
                        eventRepository);

        this.evidenceRepository =
                Objects.requireNonNull(
                        evidenceRepository);

        this.authorizationRepository =
                Objects.requireNonNull(
                        authorizationRepository);

        this.transactionRunner =
                Objects.requireNonNull(
                        transactionRunner);
    }

    public GetOperationalEventSupportingInformationResult execute(
            UUID closeId,
            UUID eventId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        return transactionRunner.execute(
                () -> executeInsideTransaction(
                        new OperationalCloseId(
                                closeId),
                        new OperationalEventId(
                                eventId)));
    }

    private GetOperationalEventSupportingInformationResult
            executeInsideTransaction(
                    OperationalCloseId closeId,
                    OperationalEventId eventId) {

        Optional<OperationalEvent> persistedEvent =
                eventRepository.findById(
                        eventId);

        if (persistedEvent.isEmpty()
                || !persistedEvent
                        .orElseThrow()
                        .closeId()
                        .equals(
                                closeId)) {

            return GetOperationalEventSupportingInformationResult
                    .notFound();
        }

        return GetOperationalEventSupportingInformationResult
                .found(
                        evidenceRepository
                                .findAllByEventIdOrderByEvidenceDateDescending(
                                        eventId)
                                .stream()
                                .map(
                                        SupportingEvidenceView::fromDomain)
                                .toList(),
                        authorizationRepository
                                .findAllByEventIdOrderByAuthorizedAtDescending(
                                        eventId)
                                .stream()
                                .map(
                                        EventAuthorizationView::fromDomain)
                                .toList());
    }

}