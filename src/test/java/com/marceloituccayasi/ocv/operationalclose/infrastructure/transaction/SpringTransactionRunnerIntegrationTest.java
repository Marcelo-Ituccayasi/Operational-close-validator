package com.marceloituccayasi.ocv.operationalclose.infrastructure.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        properties =
                "ocv.persistence.pessimistic-lock-timeout-ms=275")
class SpringTransactionRunnerIntegrationTest {

    private static final String CREATE_PROBE_TABLE =
            """
            CREATE TABLE IF NOT EXISTS ip01_transaction_probe (
                id INTEGER PRIMARY KEY,
                description VARCHAR(100) NOT NULL
            )
            """;

    @Autowired
    private TransactionRunner transactionRunner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareProbeTable() {
        jdbcTemplate.execute(
                CREATE_PROBE_TABLE);

        jdbcTemplate.execute(
                "TRUNCATE TABLE ip01_transaction_probe");
    }

    @Test
    void opensReadCommittedTransactionAndCommitsChanges() {
        Boolean transactionActive =
                transactionRunner.execute(
                        TransactionSynchronizationManager
                                ::isActualTransactionActive);

        String isolationLevel =
                transactionRunner.execute(
                        () -> jdbcTemplate.queryForObject(
                                "SHOW transaction_isolation",
                                String.class));

        transactionRunner.execute(
                (Runnable) () ->
                        jdbcTemplate.update(
                                """
                                INSERT INTO ip01_transaction_probe (
                                    id,
                                    description
                                )
                                VALUES (?, ?)
                                """,
                                1,
                                "committed"));

        Long persistedRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ip01_transaction_probe
                        """,
                        Long.class);

        assertThat(transactionActive)
                .isTrue();

        assertThat(isolationLevel)
                .isEqualTo(
                        "read committed");

        assertThat(persistedRows)
                .isEqualTo(1L);
    }

    @Test
    void appliesExternalizedFiniteLockTimeoutInsideTransaction() {
        String lockTimeout =
                transactionRunner.execute(
                        () -> jdbcTemplate.queryForObject(
                                "SHOW lock_timeout",
                                String.class));

        assertThat(lockTimeout)
                .isEqualTo(
                        "275ms");
    }

    @Test
    void restoresSessionLockTimeoutAfterTransaction() {
        transactionRunner.execute(
                () -> {
                    String transactionalTimeout =
                            jdbcTemplate.queryForObject(
                                    "SHOW lock_timeout",
                                    String.class);

                    assertThat(transactionalTimeout)
                            .isEqualTo(
                                    "275ms");

                    return null;
                });

        String sessionTimeout =
                jdbcTemplate.queryForObject(
                        "SHOW lock_timeout",
                        String.class);

        assertThat(sessionTimeout)
                .isEqualTo(
                        "0");
    }

    @Test
    void rollsBackChangesWhenOperationThrowsRuntimeException() {
        Runnable failingOperation =
                () -> {
                    jdbcTemplate.update(
                            """
                            INSERT INTO ip01_transaction_probe (
                                id,
                                description
                            )
                            VALUES (?, ?)
                            """,
                            2,
                            "rolled back");

                    throw new IllegalStateException(
                            "rollback probe");
                };

        assertThatThrownBy(
                () -> transactionRunner.execute(
                        failingOperation))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "rollback probe");

        Long persistedRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ip01_transaction_probe
                        """,
                        Long.class);

        assertThat(persistedRows)
                .isZero();
    }

}