package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttemptId;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AccountingSubmissionTransitionPersistenceIntegrationTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    uuid(
                            "cc000000-0000-0000-0000-000000000001"));

    private static final ValidationResultId RESULT_ID =
            new ValidationResultId(
                    uuid(
                            "cc000000-0000-0000-0000-000000000002"));

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    uuid(
                            "cc000000-0000-0000-0000-000000000003"));

    private static final AccountingSubmissionAttemptId ATTEMPT_ID =
            new AccountingSubmissionAttemptId(
                    uuid(
                            "cc000000-0000-0000-0000-000000000004"));

    private static final CloseStateTransitionId TRANSITION_ID =
            new CloseStateTransitionId(
                    uuid(
                            "cc000000-0000-0000-0000-000000000005"));

    private static final Instant OCCURRED_AT =
            Instant.parse(
                    "2026-07-30T16:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Autowired
    private OperationalCloseRevisionRepository
            closeRevisionRepository;

    @Autowired
    private TransactionRunner transactionRunner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanBeforeTest() {
        cleanOperationalCloseTables();
    }

    @AfterEach
    void cleanAfterTest() {
        cleanOperationalCloseTables();
    }

    @Test
    void persistsSubmissionTransitionWithConsolidationTrace() {
        persistClose();
        persistConsolidation();
        persistVr008Result(
                CONSOLIDATION_ID);
        persistRejectedAttempt(
                CONSOLIDATION_ID);

        transactionRunner.execute(
                (Runnable) () ->
                        closeRevisionRepository
                                .appendSubmissionStateTransition(
                                        rejectedTransition(),
                                        RESULT_ID,
                                        CONSOLIDATION_ID,
                                        ATTEMPT_ID));

        Map<String, Object> row =
                transitionTrace();

        assertThat(
                row.get(
                        "validation_result_id"))
                .isEqualTo(
                        RESULT_ID.value());

        assertThat(
                row.get(
                        "consolidation_id"))
                .isEqualTo(
                        CONSOLIDATION_ID.value());

        assertThat(
                row.get(
                        "submission_attempt_id"))
                .isEqualTo(
                        ATTEMPT_ID.value());
    }

    @Test
    void persistsSubmissionTransitionWithoutConsolidationTrace() {
        persistClose();
        persistVr008Result(
                null);
        persistRejectedAttempt(
                null);

        transactionRunner.execute(
                (Runnable) () ->
                        closeRevisionRepository
                                .appendSubmissionStateTransition(
                                        rejectedTransition(),
                                        RESULT_ID,
                                        ATTEMPT_ID));

        Map<String, Object> row =
                transitionTrace();

        assertThat(
                row.get(
                        "validation_result_id"))
                .isEqualTo(
                        RESULT_ID.value());

        assertThat(
                row.get(
                        "consolidation_id"))
                .isNull();

        assertThat(
                row.get(
                        "submission_attempt_id"))
                .isEqualTo(
                        ATTEMPT_ID.value());
    }

    private CloseStateTransition rejectedTransition() {
        return new CloseStateTransition(
                TRANSITION_ID,
                CLOSE_ID,
                OperationalCloseState.VALIDATED,
                OperationalCloseState.BLOCKED,
                "ACCOUNTING_SUBMISSION_REJECTED",
                "VR-008 rejected the accounting submission.",
                OCCURRED_AT,
                ACTOR);
    }

    private Map<String, Object> transitionTrace() {
        return jdbcTemplate.queryForMap(
                """
                SELECT
                    validation_result_id,
                    consolidation_id,
                    submission_attempt_id
                FROM ocv.close_state_transition
                WHERE id = ?
                """,
                TRANSITION_ID.value());
    }

    private void persistClose() {
        jdbcTemplate.update(
                """
                INSERT INTO ocv.operational_close (
                    id,
                    period_start,
                    period_end,
                    currency_code,
                    initial_balance,
                    state,
                    state_changed_at,
                    created_at,
                    created_by_user_id,
                    created_by_username,
                    updated_at,
                    updated_by_user_id,
                    updated_by_username
                )
                VALUES (
                    ?,
                    DATE '2026-08-01',
                    DATE '2026-08-31',
                    'PEN',
                    1000.0000,
                    'VALIDATED',
                    ?,
                    ?,
                    'responsible-user',
                    'responsible',
                    ?,
                    'responsible-user',
                    'responsible'
                )
                """,
                CLOSE_ID.value(),
                databaseTimestamp(
                        OCCURRED_AT),
                databaseTimestamp(
                        OCCURRED_AT),
                databaseTimestamp(
                        OCCURRED_AT));
    }

    private void persistConsolidation() {
        jdbcTemplate.update(
                """
                INSERT INTO ocv.consolidation (
                    id,
                    close_id,
                    currency_code,
                    event_count,
                    total_income,
                    total_expense,
                    total_discount,
                    total_cancellation,
                    initial_balance,
                    expected_balance,
                    actual_balance,
                    difference,
                    is_current,
                    completed_at,
                    completed_by_user_id,
                    completed_by_username,
                    invalidated_at,
                    invalidation_reason
                )
                VALUES (
                    ?,
                    ?,
                    'PEN',
                    1,
                    100.0000,
                    0.0000,
                    0.0000,
                    0.0000,
                    1000.0000,
                    1100.0000,
                    1100.0000,
                    0.0000,
                    TRUE,
                    ?,
                    'responsible-user',
                    'responsible',
                    NULL,
                    NULL
                )
                """,
                CONSOLIDATION_ID.value(),
                CLOSE_ID.value(),
                databaseTimestamp(
                        OCCURRED_AT.minusSeconds(
                                60)));
    }

    private void persistVr008Result(
            ConsolidationId consolidationId) {

        jdbcTemplate.update(
                """
                INSERT INTO ocv.validation_result (
                    id,
                    rule_code,
                    rule_version,
                    event_id,
                    close_id,
                    outcome,
                    detail,
                    evaluated_at,
                    evaluated_by_user_id,
                    evaluated_by_username,
                    event_data_revision,
                    consolidation_id,
                    is_current,
                    invalidated_at,
                    invalidation_reason
                )
                VALUES (
                    ?,
                    'VR-008',
                    1,
                    NULL,
                    ?,
                    'FAILED',
                    'VR-008 rejected the accounting submission.',
                    ?,
                    'responsible-user',
                    'responsible',
                    NULL,
                    ?,
                    TRUE,
                    NULL,
                    NULL
                )
                """,
                RESULT_ID.value(),
                CLOSE_ID.value(),
                databaseTimestamp(
                        OCCURRED_AT),
                consolidationId == null
                        ? null
                        : consolidationId.value());
    }

    private void persistRejectedAttempt(
            ConsolidationId consolidationId) {

        jdbcTemplate.update(
                """
                INSERT INTO ocv.accounting_submission_attempt (
                    id,
                    close_id,
                    vr008_result_id,
                    consolidation_id,
                    outcome,
                    attempted_at,
                    attempted_by_user_id,
                    attempted_by_username,
                    summary
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    'REJECTED',
                    ?,
                    'responsible-user',
                    'responsible',
                    'VR-008 rejected the accounting submission.'
                )
                """,
                ATTEMPT_ID.value(),
                CLOSE_ID.value(),
                RESULT_ID.value(),
                consolidationId == null
                        ? null
                        : consolidationId.value(),
                databaseTimestamp(
                        OCCURRED_AT));
    }

    private void cleanOperationalCloseTables() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    ocv.submission_attempt_issue,
                    ocv.accounting_submission_attempt,
                    ocv.consolidation_event_snapshot,
                    ocv.consolidation,
                    ocv.alert_transition,
                    ocv.alert,
                    ocv.validation_result,
                    ocv.supporting_evidence,
                    ocv.event_authorization,
                    ocv.event_state_transition,
                    ocv.operational_event,
                    ocv.close_state_transition,
                    ocv.operational_close
                """);
    }

    private static java.time.OffsetDateTime databaseTimestamp(
            Instant instant) {

        return java.time.OffsetDateTime.ofInstant(
                instant,
                java.time.ZoneOffset.UTC);
    }

    private static UUID uuid(
            String value) {

        return UUID.fromString(
                value);
    }

}