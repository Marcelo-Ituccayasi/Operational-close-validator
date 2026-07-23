package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

/**
 * Retrieves an Operational Event detail view within its owning close.
 */
public final class GetOperationalEventDetail {

    private final OperationalEventRepository repository;

    private final TransactionRunner transactionRunner;

    public GetOperationalEventDetail(
            OperationalEventRepository repository,
            TransactionRunner transactionRunner) {

        this.repository =
                Objects.requireNonNull(repository);

        this.transactionRunner =
                Objects.requireNonNull(transactionRunner);
    }

    public GetOperationalEventResult execute(
            UUID closeId,
            UUID eventId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        return transactionRunner.execute(
                () -> repository
                        .findById(
                                new OperationalEventId(
                                        eventId))
                        .filter(operationalEvent ->
                                operationalEvent
                                        .closeId()
                                        .equals(
                                                new OperationalCloseId(
                                                        closeId)))
                        .map(OperationalEventView::fromDomain)
                        .map(GetOperationalEventResult::found)
                        .orElseGet(
                                GetOperationalEventResult::notFound));
    }

}