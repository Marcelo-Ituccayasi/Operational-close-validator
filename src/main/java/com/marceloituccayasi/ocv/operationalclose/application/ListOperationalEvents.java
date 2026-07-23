package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;

/**
 * Lists Operational Events belonging to an Operational Close.
 */
public final class ListOperationalEvents {

    private final OperationalCloseRepository closeRepository;

    private final OperationalEventRepository eventRepository;

    private final TransactionRunner transactionRunner;

    public ListOperationalEvents(
            OperationalCloseRepository closeRepository,
            OperationalEventRepository eventRepository,
            TransactionRunner transactionRunner) {

        this.closeRepository =
                Objects.requireNonNull(closeRepository);

        this.eventRepository =
                Objects.requireNonNull(eventRepository);

        this.transactionRunner =
                Objects.requireNonNull(transactionRunner);
    }

    public ListOperationalEventsResult execute(
            UUID closeId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        return transactionRunner.execute(
                () -> executeInsideTransaction(closeId));
    }

    private ListOperationalEventsResult
            executeInsideTransaction(
                    UUID closeUuid) {

        OperationalCloseId closeId =
                new OperationalCloseId(
                        closeUuid);

        if (closeRepository
                .findById(closeId)
                .isEmpty()) {

            return ListOperationalEventsResult
                    .closeNotFound();
        }

        return ListOperationalEventsResult.found(
                eventRepository
                        .findAllByCloseIdOrderByOccurredAtDescending(
                                closeId)
                        .stream()
                        .map(OperationalEventView::fromDomain)
                        .toList());
    }

}