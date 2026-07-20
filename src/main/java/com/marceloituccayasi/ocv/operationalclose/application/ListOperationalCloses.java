package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.List;
import java.util.Objects;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;

/**
 * Lists Operational Closes in deterministic period-descending order.
 */
public final class ListOperationalCloses {

    private final OperationalCloseRepository repository;
    private final TransactionRunner transactionRunner;

    public ListOperationalCloses(
            OperationalCloseRepository repository,
            TransactionRunner transactionRunner) {

        this.repository =
                Objects.requireNonNull(repository);

        this.transactionRunner =
                Objects.requireNonNull(transactionRunner);
    }

    public List<OperationalCloseView> execute() {
        return transactionRunner.execute(
                () -> repository
                        .findAllByPeriodDescending()
                        .stream()
                        .map(OperationalCloseView::fromDomain)
                        .toList());
    }

}
