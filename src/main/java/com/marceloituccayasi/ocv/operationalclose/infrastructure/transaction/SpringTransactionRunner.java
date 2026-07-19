package com.marceloituccayasi.ocv.operationalclose.infrastructure.transaction;

import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;

/**
 * Executes application operations through Spring's transaction infrastructure.
 */
@Component
public final class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionRunner(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation must not be null");

        return transactionTemplate.execute(status -> operation.get());
    }

}
