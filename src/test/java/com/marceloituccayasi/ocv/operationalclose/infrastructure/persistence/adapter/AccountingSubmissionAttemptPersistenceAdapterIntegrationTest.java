package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.AccountingSubmissionAttemptRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttempt;
import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttemptId;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssue;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueId;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueType;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AccountingSubmissionAttemptPersistenceAdapterIntegrationTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    uuid(
                            "a5000000-0000-0000-0000-000000000001"));

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    uuid(
                            "a5000000-0000-0000-0000-000000000002"));

    private static final ValidationResultId FIRST_RESULT_ID =
            new ValidationResultId(
                    uuid(
                            "a5000000-0000-0000-0000-000000000003"));

    private static final ValidationResultId SECOND_RESULT_ID =
            new ValidationResultId(
                    uuid(
                            "a5000000-0000-0000-0000-000000000004"));

    private static final AccountingSubmissionAttemptId FIRST_ATTEMPT_ID =
            new AccountingSubmissionAttemptId(
                    uuid(
                            "a5000000-0000-0000-0000-000000000005"));

    private static final AccountingSubmissionAttemptId SECOND_ATTEMPT_ID =
            new AccountingSubmissionAttemptId(
                    uuid(
                            "a5000000-0000-0000-0000-000000000006"));

    private static final AccountingSubmissionAttemptId ROLLBACK_ATTEMPT_ID =
            new AccountingSubmissionAttemptId(
                    uuid(
                            "a5000000-0000-0000-0000-000000000007"));

    private static final OperationalEventId MISSING_EVENT_ID =
            new OperationalEventId(
                    uuid(
                            "a5000000-0000-0000-0000-000000000008"));

    private static final Instant FIRST_ATTEMPTED_AT =
            Instant.parse(
                    "2026-07-30T16:00:00Z");

    private static final Instant SECOND_ATTEMPTED_AT =
            Instant.parse(
                    "2026-07-30T17:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Autowired
    private AccountingSubmissionAttemptRepository attemptRepository;

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
    void persistsAndReconstructsSuccessfulAttempt() {
        persistClose();
        persistConsolidation();

        persistVr008Result(
                FIRST_RESULT_ID,
                "SATISFIED",
                CONSOLIDATION_ID);

        AccountingSubmissionAttempt expected =
                AccountingSubmissionAttempt.succeeded(
                        FIRST_ATTEMPT_ID,
                        CLOSE_ID,
                        FIRST_RESULT_ID,
                        CONSOLIDATION_ID,
                        FIRST_ATTEMPTED_AT,
                        ACTOR,
                        "Close sent to accounting.");

        transactionRunner.execute(
                (Runnable) () ->
                        attemptRepository.saveNew(
                                expected));

        Optional<AccountingSubmissionAttempt> loadedById =
                attemptRepository.findById(
                        FIRST_ATTEMPT_ID);

        Optional<AccountingSubmissionAttempt> latest =
                attemptRepository.findLatestByCloseId(
                        CLOSE_ID);

        assertThat(
                loadedById)
                .contains(
                        expected);

        assertThat(
                latest)
                .contains(
                        expected);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.accounting_submission_attempt
                        WHERE id = ?
                        """,
                        FIRST_ATTEMPT_ID.value()))
                .isEqualTo(
                        1);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.submission_attempt_issue
                        WHERE submission_attempt_id = ?
                        """,
                        FIRST_ATTEMPT_ID.value()))
                .isZero();
    }

    @Test
    void persistsRejectedAttemptIssuesAndLoadsHistoryInOrder() {
        persistClose();

        persistVr008Result(
                FIRST_RESULT_ID,
                "FAILED",
                null);

        AccountingSubmissionAttempt first =
                rejectedAttempt(
                        FIRST_ATTEMPT_ID,
                        FIRST_RESULT_ID,
                        FIRST_ATTEMPTED_AT,
                        "a5000000-0000-0000-0000-000000000011");

        transactionRunner.execute(
                (Runnable) () ->
                        attemptRepository.saveNew(
                                first));

        invalidateResult(
                FIRST_RESULT_ID);

        persistVr008Result(
                SECOND_RESULT_ID,
                "FAILED",
                null);

        AccountingSubmissionAttempt second =
                rejectedAttempt(
                        SECOND_ATTEMPT_ID,
                        SECOND_RESULT_ID,
                        SECOND_ATTEMPTED_AT,
                        "a5000000-0000-0000-0000-000000000012");

        transactionRunner.execute(
                (Runnable) () ->
                        attemptRepository.saveNew(
                                second));

        List<AccountingSubmissionAttempt> history =
                attemptRepository
                        .findAllByCloseIdOrderByAttemptedAt(
                                CLOSE_ID);

        assertThat(
                history)
                .containsExactly(
                        first,
                        second);

        assertThat(
                attemptRepository.findLatestByCloseId(
                        CLOSE_ID))
                .contains(
                        second);

        assertThat(
                history.getFirst()
                        .issues())
                .hasSize(
                        1);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.submission_attempt_issue
                        """))
                .isEqualTo(
                        2);
    }

    @Test
    void rollsBackParentWhenIssuePersistenceFails() {
        persistClose();

        persistVr008Result(
                FIRST_RESULT_ID,
                "FAILED",
                null);

        SubmissionAttemptIssue missingConsolidation =
                new SubmissionAttemptIssue(
                        new SubmissionAttemptIssueId(
                                uuid(
                                        "a5000000-0000-0000-0000-000000000021")),
                        ROLLBACK_ATTEMPT_ID,
                        SubmissionAttemptIssueType.CONSOLIDATION_MISSING,
                        null,
                        null,
                        null,
                        null,
                        "No current Consolidation exists.");

        SubmissionAttemptIssue missingEvent =
                new SubmissionAttemptIssue(
                        new SubmissionAttemptIssueId(
                                uuid(
                                        "a5000000-0000-0000-0000-000000000022")),
                        ROLLBACK_ATTEMPT_ID,
                        SubmissionAttemptIssueType.EVENT_NOT_VALIDATED,
                        MISSING_EVENT_ID,
                        null,
                        null,
                        null,
                        "The affected Event does not exist.");

        AccountingSubmissionAttempt invalid =
                AccountingSubmissionAttempt.rejected(
                        ROLLBACK_ATTEMPT_ID,
                        CLOSE_ID,
                        FIRST_RESULT_ID,
                        null,
                        FIRST_ATTEMPTED_AT,
                        ACTOR,
                        "Submission rejected.",
                        List.of(
                                missingConsolidation,
                                missingEvent));

        assertThatThrownBy(
                () -> transactionRunner.execute(
                        (Runnable) () ->
                                attemptRepository.saveNew(
                                        invalid)))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.accounting_submission_attempt
                        WHERE id = ?
                        """,
                        ROLLBACK_ATTEMPT_ID.value()))
                .isZero();

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.submission_attempt_issue
                        WHERE submission_attempt_id = ?
                        """,
                        ROLLBACK_ATTEMPT_ID.value()))
                .isZero();
    }

    private AccountingSubmissionAttempt rejectedAttempt(
            AccountingSubmissionAttemptId attemptId,
            ValidationResultId resultId,
            Instant attemptedAt,
            String issueId) {

        SubmissionAttemptIssue issue =
                new SubmissionAttemptIssue(
                        new SubmissionAttemptIssueId(
                                uuid(
                                        issueId)),
                        attemptId,
                        SubmissionAttemptIssueType.CONSOLIDATION_MISSING,
                        null,
                        null,
                        null,
                        null,
                        "No current Consolidation exists.");

        return AccountingSubmissionAttempt.rejected(
                attemptId,
                CLOSE_ID,
                resultId,
                null,
                attemptedAt,
                ACTOR,
                "Submission rejected.",
                List.of(
                        issue));
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
                        FIRST_ATTEMPTED_AT),
                databaseTimestamp(
                        FIRST_ATTEMPTED_AT),
                databaseTimestamp(
                        FIRST_ATTEMPTED_AT));
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
                        FIRST_ATTEMPTED_AT.minusSeconds(
                                60)));
    }

    private void persistVr008Result(
            ValidationResultId resultId,
            String outcome,
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
                    ?,
                    'Final accounting submission control.',
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
                resultId.value(),
                CLOSE_ID.value(),
                outcome,
                databaseTimestamp(
                        FIRST_ATTEMPTED_AT),
                consolidationId == null
                        ? null
                        : consolidationId.value());
    }

    private void invalidateResult(
            ValidationResultId resultId) {

        jdbcTemplate.update(
                """
                UPDATE ocv.validation_result
                SET is_current = FALSE,
                    invalidated_at = ?,
                    invalidation_reason =
                        'Superseded by another VR-008 evaluation.'
                WHERE id = ?
                """,
                databaseTimestamp(
                        SECOND_ATTEMPTED_AT),
                resultId.value());
    }

    private long count(
            String sql,
            Object... arguments) {

        Long value =
                jdbcTemplate.queryForObject(
                        sql,
                        Long.class,
                        arguments);

        return value == null
                ? 0L
                : value;
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