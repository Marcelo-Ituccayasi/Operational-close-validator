package com.marceloituccayasi.ocv.operationalclose.application.port;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Executes an application operation inside an infrastructure-managed transaction.
 */
public interface TransactionRunner {

    <T> T execute(Supplier<T> operation);

    default void execute(Runnable operation) {
        Objects.requireNonNull(operation, "operation must not be null");

        execute(() -> {
            operation.run();
            return null;
        });
    }

}
