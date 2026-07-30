package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class AccountingSubmissionSchemaIntegrationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "7ea88b77-0ab1-49f7-8918-810000000001");

    private static final UUID CONSOLIDATION_ID =
            UUID.fromString(
                    "7ea88b77-0ab1-49f7-8918-810000000002");

    private static final UUID FIRST_RESULT_ID =
            UUID.fromString(
                    "7ea88b77-0ab1-49f7-8918-810000000003");

    private static final UUID SECOND_RESULT_ID =
            UUID.fromString(
                    "7ea88b77-0ab1-49f7-8918-810000000004");

    private static final UUID FIRST_ATTEMPT_ID =
            UUID.fromString(
                    "7ea88b77-0ab1-49f7-8918-810000000005");

    private static final UUID SECOND_ATTEMPT_ID =
            UUID.fromString(
                    "7ea88b77-0ab1-49f7-8918-810000000006");

    private static final UUID ISSUE_ID =
            UUID.fromString(
                    "7ea88b77-0ab1-49f7-8918-810000000007");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsApprovedAccountingSubmissionAttemptSchema() {
        assertThat(
                columnNames(
                        "accounting_submission_attempt"))
                .containsExactly(
                        "id",
                        "close_id",
                        "vr008_result_id",
                        "consolidation_id",
                        "outcome",
                        "attempted_at",
                        "attempted_by_user_id",
                        "attempted_by_username",
                        "summary");

        assertThat(
                constraintNames(
                        "accounting_submission_attempt"))
                .contains(
                        "pk_accounting_submission_attempt",
                        "fk_submission_attempt_close",
                        "fk_submission_attempt_vr008_result",
                        "fk_submission_attempt_consolidation",
                        "ck_submission_attempt_outcome",
                        "ck_submission_attempt_consolidation",
                        "ck_submission_attempt_attempted_by_user",
                        "ck_submission_attempt_attempted_by_username");

        assertThat(
                indexNames(
                        "accounting_submission_attempt"))
                .contains(
                        "uq_submission_success_close",
                        "uq_submission_vr008_result",
                        "idx_submission_close_attempted_at",
                        "idx_submission_outcome_attempted_at");

        assertThat(
                triggerNames(
                        "accounting_submission_attempt"))
                .contains(
                        "trg_accounting_submission_attempt_append_only");
    }

    @Test
    void createsApprovedSubmissionAttemptIssueSchema() {
        assertThat(
                columnNames(
                        "submission_attempt_issue"))
                .containsExactly(
                        "id",
                        "submission_attempt_id",
                        "issue_type",
                        "event_id",
                        "alert_id",
                        "validation_result_id",
                        "consolidation_id",
                        "detail");

        assertThat(
                constraintNames(
                        "submission_attempt_issue"))
                .contains(
                        "pk_submission_attempt_issue",
                        "fk_submission_issue_attempt",
                        "fk_submission_issue_event",
                        "fk_submission_issue_alert",
                        "fk_submission_issue_validation_result",
                        "fk_submission_issue_consolidation",
                        "ck_submission_issue_type",
                        "ck_submission_issue_detail");

        assertThat(
                indexNames(
                        "submission_attempt_issue"))
                .contains(
                        "idx_submission_issue_attempt",
                        "idx_submission_issue_event",
                        "idx_submission_issue_alert",
                        "idx_submission_issue_validation_result",
                        "idx_submission_issue_consolidation");

        assertThat(
                triggerNames(
                        "submission_attempt_issue"))
                .contains(
                        "trg_submission_attempt_issue_append_only");
    }

    @Test
    void completesReservedCloseTransitionForeignKey() {
        assertThat(
                constraintNames(
                        "close_state_transition"))
                .contains(
                        "fk_close_state_transition_submission_attempt");
    }

    @Test
    void requiresConsolidationForSuccessfulAttempt() {
        insertClose();
        insertConsolidation();

        insertVr008Result(
                FIRST_RESULT_ID,
                "SATISFIED",
                CONSOLIDATION_ID);

        assertThatThrownBy(
                () -> insertAttempt(
                        FIRST_ATTEMPT_ID,
                        FIRST_RESULT_ID,
                        "SUCCEEDED",
                        null))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void permitsOnlyOneSuccessfulAttemptPerClose() {
        insertClose();
        insertConsolidation();

        insertVr008Result(
                FIRST_RESULT_ID,
                "SATISFIED",
                CONSOLIDATION_ID);

        insertAttempt(
                FIRST_ATTEMPT_ID,
                FIRST_RESULT_ID,
                "SUCCEEDED",
                CONSOLIDATION_ID);

        invalidateResult(
                FIRST_RESULT_ID);

        insertVr008Result(
                SECOND_RESULT_ID,
                "SATISFIED",
                CONSOLIDATION_ID);

        assertThatThrownBy(
                () -> insertAttempt(
                        SECOND_ATTEMPT_ID,
                        SECOND_RESULT_ID,
                        "SUCCEEDED",
                        CONSOLIDATION_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void permitsOnlyOneAttemptPerVr008Result() {
        insertClose();

        insertVr008Result(
                FIRST_RESULT_ID,
                "FAILED",
                null);

        insertAttempt(
                FIRST_ATTEMPT_ID,
                FIRST_RESULT_ID,
                "REJECTED",
                null);

        assertThatThrownBy(
                () -> insertAttempt(
                        SECOND_ATTEMPT_ID,
                        FIRST_RESULT_ID,
                        "REJECTED",
                        null))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void rejectsUnknownSubmissionIssueType() {
        insertRejectedAttempt();

        assertThatThrownBy(
                () -> insertIssue(
                        ISSUE_ID,
                        FIRST_ATTEMPT_ID,
                        "UNKNOWN_ISSUE",
                        "Unknown rejection condition."))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void rejectsBlankSubmissionIssueDetail() {
        insertRejectedAttempt();

        assertThatThrownBy(
                () -> insertIssue(
                        ISSUE_ID,
                        FIRST_ATTEMPT_ID,
                        "OTHER_CRITICAL_INCONSISTENCY",
                        "   "))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void protectsSubmissionAttemptFromModification() {
        insertRejectedAttempt();

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        UPDATE ocv.accounting_submission_attempt
                        SET summary = 'Modified summary.'
                        WHERE id = ?
                        """,
                        FIRST_ATTEMPT_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void protectsSubmissionIssueFromModification() {
        insertRejectedAttempt();

        insertIssue(
                ISSUE_ID,
                FIRST_ATTEMPT_ID,
                "CONSOLIDATION_MISSING",
                "No current consolidation exists.");

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        UPDATE ocv.submission_attempt_issue
                        SET detail = 'Modified detail.'
                        WHERE id = ?
                        """,
                        ISSUE_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    private void insertRejectedAttempt() {
        insertClose();

        insertVr008Result(
                FIRST_RESULT_ID,
                "FAILED",
                null);

        insertAttempt(
                FIRST_ATTEMPT_ID,
                FIRST_RESULT_ID,
                "REJECTED",
                null);
    }

    private void insertClose() {
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
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    'responsible-user',
                    'responsible',
                    CURRENT_TIMESTAMP,
                    'responsible-user',
                    'responsible'
                )
                """,
                CLOSE_ID);
    }

    private void insertConsolidation() {
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
                    CURRENT_TIMESTAMP,
                    'responsible-user',
                    'responsible',
                    NULL,
                    NULL
                )
                """,
                CONSOLIDATION_ID,
                CLOSE_ID);
    }

    private void insertVr008Result(
            UUID resultId,
            String outcome,
            UUID consolidationId) {

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
                    CURRENT_TIMESTAMP,
                    'responsible-user',
                    'responsible',
                    NULL,
                    ?,
                    TRUE,
                    NULL,
                    NULL
                )
                """,
                resultId,
                CLOSE_ID,
                outcome,
                consolidationId);
    }

    private void invalidateResult(
            UUID resultId) {

        jdbcTemplate.update(
                """
                UPDATE ocv.validation_result
                SET is_current = FALSE,
                    invalidated_at = CURRENT_TIMESTAMP,
                    invalidation_reason =
                        'Superseded by another VR-008 evaluation.'
                WHERE id = ?
                """,
                resultId);
    }

    private void insertAttempt(
            UUID attemptId,
            UUID resultId,
            String outcome,
            UUID consolidationId) {

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
                    ?,
                    CURRENT_TIMESTAMP,
                    'responsible-user',
                    'responsible',
                    'Accounting submission attempt fixture.'
                )
                """,
                attemptId,
                CLOSE_ID,
                resultId,
                consolidationId,
                outcome);
    }

    private void insertIssue(
            UUID issueId,
            UUID attemptId,
            String issueType,
            String detail) {

        jdbcTemplate.update(
                """
                INSERT INTO ocv.submission_attempt_issue (
                    id,
                    submission_attempt_id,
                    issue_type,
                    event_id,
                    alert_id,
                    validation_result_id,
                    consolidation_id,
                    detail
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    NULL,
                    NULL,
                    NULL,
                    NULL,
                    ?
                )
                """,
                issueId,
                attemptId,
                issueType,
                detail);
    }

    private List<String> columnNames(
            String tableName) {

        return jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'ocv'
                  AND table_name = ?
                ORDER BY ordinal_position
                """,
                String.class,
                tableName);
    }

    private List<String> constraintNames(
            String tableName) {

        return jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'ocv'
                  AND table_name = ?
                ORDER BY constraint_name
                """,
                String.class,
                tableName);
    }

    private List<String> indexNames(
            String tableName) {

        return jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'ocv'
                  AND tablename = ?
                ORDER BY indexname
                """,
                String.class,
                tableName);
    }

    private List<String> triggerNames(
            String tableName) {

        return jdbcTemplate.queryForList(
                """
                SELECT DISTINCT trigger_name
                FROM information_schema.triggers
                WHERE event_object_schema = 'ocv'
                  AND event_object_table = ?
                ORDER BY trigger_name
                """,
                String.class,
                tableName);
    }

}