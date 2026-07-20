package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;

/**
 * Retrieves an Operational Close detail view.
 */
public final class GetOperationalCloseDetail {

    private final OperationalCloseRepository repository;
    private final TransactionRunner transactionRunner;

    public GetOperationalCloseDetail(
            OperationalCloseRepository repository,
            TransactionRunner transactionRunner) {

        this.repository =
                Objects.requireNonNull(repository);

        this.transactionRunner =
                Objects.requireNonNull(transactionRunner);
    }

    public GetOperationalCloseResult execute(
            UUID closeId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        return transactionRunner.execute(
                () -> repository
                        .findById(
                                new OperationalCloseId(
                                        closeId))
                        .map(OperationalCloseView::fromDomain)
                        .map(GetOperationalCloseResult::found)
                        .orElseGet(
                                GetOperationalCloseResult::notFound));
    }

}
