package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ValidationResultSchemaIntegrationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "df2114f4-51d6-4e7c-afda-01c876d10001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "df2114f4-51d6-4e7c-afda-01c876d10002");

    private static final UUID RESULT_ID =
            UUID.fromString(
                    "df2114f4-51d6-4e7c-afda-01c876d10003");

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse(
                    "2026-07-25T18:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
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

    @Test
    void createsApprovedColumnsConstraintsIndexesAndTrigger() {
        assertThat(
                columnNames())
                .containsExactly(
                        "id",
                        "rule_code",
                        "rule_version",
                        "event_id",
                        "close_id",
                        "outcome",
                        "detail",
                        "evaluated_at",
                        "evaluated_by_user_id",
                        "evaluated_by_username",
                        "event_data_revision",
                        "consolidation_id",
                        "is_current",
                        "invalidated_at",
                        "invalidation_reason");

        assertThat(
                constraintNames())
                .contains(
                        "validation_result_pkey",
                        "fk_validation_result_rule",
                        "fk_validation_result_event",
                        "fk_validation_result_close",
                        "ck_validation_result_rule_version",
                        "ck_validation_result_target",
                        "ck_validation_result_outcome",
                        "ck_validation_result_detail",
                        "ck_validation_result_evaluated_by_user",
                        "ck_validation_result_evaluated_by_username",
                        "ck_validation_result_event_revision",
                        "ck_validation_result_consolidation",
                        "ck_validation_result_validity");

        assertThat(
                indexNames())
                .contains(
                        "uq_validation_result_current_event_rule",
                        "uq_validation_result_current_close_rule",
                        "idx_validation_result_event_current",
                        "idx_validation_result_close_current",
                        "idx_validation_result_rule_outcome",
                        "idx_validation_result_evaluated_at");

        assertThat(
                triggerNames())
                .contains(
                        "trg_validation_result_immutability");
    }

    @Test
    void permitsHistoryButOnlyOneCurrentResultPerEventAndRule() {
        persistParentCloseAndEvent();

        persistCurrentEventResult(
                RESULT_ID,
                "VR-001",
                "Movement is registered and traceable.");

        persistInvalidatedEventResult(
                UUID.randomUUID(),
                "VR-001",
                "Previous historical evaluation.");

        assertThatThrownBy(
                () -> persistCurrentEventResult(
                        UUID.randomUUID(),
                        "VR-001",
                        "Second current result."))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        Integer resultCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.validation_result
                        WHERE event_id = ?
                          AND rule_code = 'VR-001'
                        """,
                        Integer.class,
                        EVENT_ID);

        assertThat(resultCount)
                .isEqualTo(
                        2);
    }

    @Test
    void rejectsPendingInvalidTargetsAndInvalidValidityMetadata() {
        persistParentCloseAndEvent();

        assertThatThrownBy(
                () -> jdbcTemplate.update(
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
                            'VR-001',
                            1,
                            ?,
                            NULL,
                            'PENDING',
                            'Unsupported outcome.',
                            ?,
                            'responsible-user',
                            'responsible',
                            1,
                            NULL,
                            TRUE,
                            NULL,
                            NULL
                        )
                        """,
                        UUID.randomUUID(),
                        EVENT_ID,
                        NOW))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        assertThatThrownBy(
                () -> jdbcTemplate.update(
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
                            'VR-001',
                            1,
                            ?,
                            ?,
                            'FAILED',
                            'Two targets are not permitted.',
                            ?,
                            'responsible-user',
                            'responsible',
                            1,
                            NULL,
                            TRUE,
                            NULL,
                            NULL
                        )
                        """,
                        UUID.randomUUID(),
                        EVENT_ID,
                        CLOSE_ID,
                        NOW))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        assertThatThrownBy(
                () -> jdbcTemplate.update(
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
                            'VR-003',
                            1,
                            ?,
                            NULL,
                            'FAILED',
                            'Missing event revision.',
                            ?,
                            'responsible-user',
                            'responsible',
                            NULL,
                            NULL,
                            TRUE,
                            NULL,
                            NULL
                        )
                        """,
                        UUID.randomUUID(),
                        EVENT_ID,
                        NOW))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        assertThatThrownBy(
                () -> jdbcTemplate.update(
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
                            'VR-006',
                            1,
                            ?,
                            NULL,
                            'FAILED',
                            'Invalidated result without metadata.',
                            ?,
                            'responsible-user',
                            'responsible',
                            1,
                            NULL,
                            FALSE,
                            NULL,
                            NULL
                        )
                        """,
                        UUID.randomUUID(),
                        EVENT_ID,
                        NOW))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void permitsOneWayInvalidationButRejectsEvaluationMutation() {
        persistParentCloseAndEvent();

        persistCurrentEventResult(
                RESULT_ID,
                "VR-002",
                "Registered and supported amounts match.");

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        UPDATE ocv.validation_result
                        SET detail = 'Retrospectively changed detail.'
                        WHERE id = ?
                        """,
                        RESULT_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        int invalidatedRows =
                jdbcTemplate.update(
                        """
                        UPDATE ocv.validation_result
                        SET
                            is_current = FALSE,
                            invalidated_at = ?,
                            invalidation_reason = 'Event data changed.'
                        WHERE id = ?
                        """,
                        NOW.plusMinutes(
                                1L),
                        RESULT_ID);

        assertThat(invalidatedRows)
                .isEqualTo(
                        1);

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        UPDATE ocv.validation_result
                        SET
                            is_current = TRUE,
                            invalidated_at = NULL,
                            invalidation_reason = NULL
                        WHERE id = ?
                        """,
                        RESULT_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        String persistedResult =
                jdbcTemplate.queryForObject(
                        """
                        SELECT
                            outcome
                            || '|'
                            || detail
                            || '|'
                            || is_current
                            || '|'
                            || invalidation_reason
                        FROM ocv.validation_result
                        WHERE id = ?
                        """,
                        String.class,
                        RESULT_ID);

        assertThat(persistedResult)
                .isEqualTo(
                        "SATISFIED|Registered and supported amounts match."
                                + "|false|Event data changed.");
    }

    private List<String> columnNames() {
        return jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'ocv'
                  AND table_name = 'validation_result'
                ORDER BY ordinal_position
                """,
                String.class);
    }

    private List<String> constraintNames() {
        return jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'ocv'
                  AND table_name = 'validation_result'
                ORDER BY constraint_name
                """,
                String.class);
    }

    private List<String> indexNames() {
        return jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'ocv'
                  AND tablename = 'validation_result'
                ORDER BY indexname
                """,
                String.class);
    }

    private List<String> triggerNames() {
        return jdbcTemplate.queryForList(
                """
                SELECT trigger_name
                FROM information_schema.triggers
                WHERE event_object_schema = 'ocv'
                  AND event_object_table = 'validation_result'
                ORDER BY trigger_name
                """,
                String.class);
    }

    private void persistParentCloseAndEvent() {
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
                    DATE '2026-07-01',
                    DATE '2026-07-31',
                    'PEN',
                    1000.0000,
                    'PREPARATION',
                    ?,
                    ?,
                    'responsible-user',
                    'responsible',
                    ?,
                    'responsible-user',
                    'responsible'
                )
                """,
                CLOSE_ID,
                NOW,
                NOW,
                NOW);

        jdbcTemplate.update(
                """
                INSERT INTO ocv.operational_event (
                    id,
                    close_id,
                    event_type,
                    amount,
                    balance_effect,
                    reversed_event_id,
                    occurred_at,
                    registered_at,
                    responsible_name,
                    description,
                    state,
                    evidence_required,
                    authorization_required,
                    data_revision,
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
                    ?,
                    'INCOME',
                    100.0000,
                    100.0000,
                    NULL,
                    ?,
                    ?,
                    'Caja principal',
                    'Evento para probar resultados de validación',
                    'REGISTERED',
                    TRUE,
                    TRUE,
                    1,
                    ?,
                    ?,
                    'responsible-user',
                    'responsible',
                    ?,
                    'responsible-user',
                    'responsible'
                )
                """,
                EVENT_ID,
                CLOSE_ID,
                NOW,
                NOW,
                NOW,
                NOW,
                NOW);
    }

    private void persistCurrentEventResult(
            UUID resultId,
            String ruleCode,
            String detail) {

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
                    ?,
                    1,
                    ?,
                    NULL,
                    'SATISFIED',
                    ?,
                    ?,
                    'responsible-user',
                    'responsible',
                    1,
                    NULL,
                    TRUE,
                    NULL,
                    NULL
                )
                """,
                resultId,
                ruleCode,
                EVENT_ID,
                detail,
                NOW);
    }

    private void persistInvalidatedEventResult(
            UUID resultId,
            String ruleCode,
            String detail) {

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
                    ?,
                    1,
                    ?,
                    NULL,
                    'FAILED',
                    ?,
                    ?,
                    'responsible-user',
                    'responsible',
                    1,
                    NULL,
                    FALSE,
                    ?,
                    'Superseded by revalidation.'
                )
                """,
                resultId,
                ruleCode,
                EVENT_ID,
                detail,
                NOW.minusMinutes(
                        2L),
                NOW.minusMinutes(
                        1L));
    }

}