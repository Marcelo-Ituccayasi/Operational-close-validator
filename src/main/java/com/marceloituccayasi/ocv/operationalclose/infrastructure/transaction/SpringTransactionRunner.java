package com.marceloituccayasi.ocv.operationalclose.infrastructure.transaction;

import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;

/**
 * Executes application operations through Spring's transaction infrastructure.
 */
@Component
public final class SpringTransactionRunner
        implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    private final JdbcTemplate jdbcTemplate;

    private final String lockTimeoutCommand;

    public SpringTransactionRunner(
            TransactionTemplate transactionTemplate,
            JdbcTemplate jdbcTemplate,
            @Value(
                    "${ocv.persistence.pessimistic-lock-timeout-ms}")
            long pessimisticLockTimeoutMillis) {

        this.transactionTemplate =
                Objects.requireNonNull(
                        transactionTemplate);

        this.jdbcTemplate =
                Objects.requireNonNull(
                        jdbcTemplate);

        if (pessimisticLockTimeoutMillis < 1
                || pessimisticLockTimeoutMillis
                        > Integer.MAX_VALUE) {

            throw new IllegalArgumentException(
                    "pessimistic lock timeout must be between "
                            + "1 and 2147483647 milliseconds");
        }

        this.lockTimeoutCommand =
                "SET LOCAL lock_timeout = '"
                        + pessimisticLockTimeoutMillis
                        + "ms'";
    }

    @Override
    public <T> T execute(
            Supplier<T> operation) {

        Objects.requireNonNull(
                operation,
                "operation must not be null");

        return transactionTemplate.execute(
                status -> {
                    jdbcTemplate.execute(
                            lockTimeoutCommand);

                    return operation.get();
                });
    }

}