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
class AlertResolutionSchemaIntegrationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "162722a9-5d34-4696-ae37-05cc8d0a0001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "162722a9-5d34-4696-ae37-05cc8d0a0002");

    private static final UUID OTHER_EVENT_ID =
            UUID.fromString(
                    "162722a9-5d34-4696-ae37-05cc8d0a0003");

    private static final UUID FAILED_RESULT_ID =
            UUID.fromString(
                    "162722a9-5d34-4696-ae37-05cc8d0a0004");

    private static final UUID SATISFIED_RESULT_ID =
            UUID.fromString(
                    "162722a9-5d34-4696-ae37-05cc8d0a0005");

    private static final UUID INACTIVE_RESULT_ID =
            UUID.fromString(
                    "162722a9-5d34-4696-ae37-05cc8d0a0006");

    private static final UUID MISMATCHED_RESULT_ID =
            UUID.fromString(
                    "162722a9-5d34-4696-ae37-05cc8d0a0007");

    private static final UUID ALERT_ID =
            UUID.fromString(
                    "162722a9-5d34-4696-ae37-05cc8d0a0008");

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse(
                    "2026-07-26T16:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {
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

    @Test
    void createsAlertResolutionValidationTrigger() {
        assertThat(
                triggerNames(
                        "alert"))
                .contains(
                        "trg_alert_validate_resolution_result");
    }

    @Test
    void permitsResolutionWithCurrentSatisfiedCompatibleResult() {
        persistParentCloseAndEvents();

        persistValidationResult(
                FAILED_RESULT_ID,
                EVENT_ID,
                "FAILED",
                true);

        persistActiveAlert();

        jdbcTemplate.update(
                """
                UPDATE ocv.validation_result
                SET
                    is_current = FALSE,
                    invalidated_at = ?,
                    invalidation_reason =
                        'Superseded by revalidation.'
                WHERE id = ?
                """,
                NOW.plusMinutes(
                        1L),
                FAILED_RESULT_ID);

        persistValidationResult(
                SATISFIED_RESULT_ID,
                EVENT_ID,
                "SATISFIED",
                true);

        jdbcTemplate.update(
                """
                UPDATE ocv.alert
                SET
                    state = 'RESOLVED',
                    resolved_by_validation_result_id = ?,
                    updated_at = ?,
                    closed_at = ?
                WHERE id = ?
                """,
                SATISFIED_RESULT_ID,
                NOW.plusMinutes(
                        3L),
                NOW.plusMinutes(
                        3L),
                ALERT_ID);

        String persistedState =
                jdbcTemplate.queryForObject(
                        """
                        SELECT state
                        FROM ocv.alert
                        WHERE id = ?
                        """,
                        String.class,
                        ALERT_ID);

        assertThat(persistedState)
                .isEqualTo(
                        "RESOLVED");
    }

    @Test
    void rejectsFailedInactiveAndMismatchedResolutionResults() {
        persistParentCloseAndEvents();

        persistValidationResult(
                FAILED_RESULT_ID,
                EVENT_ID,
                "FAILED",
                true);

        persistActiveAlert();

        assertThatThrownBy(
                () -> resolveAlertWith(
                        FAILED_RESULT_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        persistValidationResult(
                INACTIVE_RESULT_ID,
                EVENT_ID,
                "SATISFIED",
                false);

        assertThatThrownBy(
                () -> resolveAlertWith(
                        INACTIVE_RESULT_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        persistValidationResult(
                MISMATCHED_RESULT_ID,
                OTHER_EVENT_ID,
                "SATISFIED",
                true);

        assertThatThrownBy(
                () -> resolveAlertWith(
                        MISMATCHED_RESULT_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    private List<String> triggerNames(
            String tableName) {

        return jdbcTemplate.queryForList(
                """
                SELECT trigger.tgname
                FROM pg_trigger trigger
                JOIN pg_class table_definition
                  ON table_definition.oid =
                     trigger.tgrelid
                JOIN pg_namespace table_schema
                  ON table_schema.oid =
                     table_definition.relnamespace
                WHERE table_schema.nspname = 'ocv'
                  AND table_definition.relname = ?
                  AND trigger.tgisinternal = FALSE
                ORDER BY trigger.tgname
                """,
                String.class,
                tableName);
    }

    private void persistParentCloseAndEvents() {
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
                    DATE '2026-10-01',
                    DATE '2026-10-31',
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

        persistEvent(
                EVENT_ID,
                "Evento principal para probar resolución de Alertas");

        persistEvent(
                OTHER_EVENT_ID,
                "Evento distinto para probar incompatibilidad");
    }

    private void persistEvent(
            UUID eventId,
            String description) {

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
                    'EXPENSE',
                    50.0000,
                    -50.0000,
                    NULL,
                    ?,
                    ?,
                    'Caja principal',
                    ?,
                    'REGISTERED',
                    TRUE,
                    FALSE,
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
                eventId,
                CLOSE_ID,
                NOW,
                NOW,
                description,
                NOW,
                NOW,
                NOW);
    }

    private void persistValidationResult(
            UUID resultId,
            UUID eventId,
            String outcome,
            boolean current) {

        OffsetDateTime invalidatedAt =
                current
                        ? null
                        : NOW.plusMinutes(
                                1L);

        String invalidationReason =
                current
                        ? null
                        : "Historical result for schema validation.";

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
                    'VR-003',
                    1,
                    ?,
                    NULL,
                    ?,
                    'Validation result for alert resolution.',
                    ?,
                    'responsible-user',
                    'responsible',
                    1,
                    NULL,
                    ?,
                    ?,
                    ?
                )
                """,
                resultId,
                eventId,
                outcome,
                NOW,
                current,
                invalidatedAt,
                invalidationReason);
    }

    private void persistActiveAlert() {
        jdbcTemplate.update(
                """
                INSERT INTO ocv.alert (
                    id,
                    event_id,
                    close_id,
                    source_validation_result_id,
                    cause_code,
                    severity,
                    is_blocking,
                    state,
                    detail,
                    resolved_by_validation_result_id,
                    discard_justification,
                    created_at,
                    created_by_user_id,
                    created_by_username,
                    updated_at,
                    closed_at
                )
                VALUES (
                    ?,
                    ?,
                    NULL,
                    ?,
                    'VR-003',
                    'HIGH',
                    TRUE,
                    'ACTIVE',
                    'Required supporting evidence is missing.',
                    NULL,
                    NULL,
                    ?,
                    'responsible-user',
                    'responsible',
                    ?,
                    NULL
                )
                """,
                ALERT_ID,
                EVENT_ID,
                FAILED_RESULT_ID,
                NOW,
                NOW);
    }

    private void resolveAlertWith(
            UUID validationResultId) {

        jdbcTemplate.update(
                """
                UPDATE ocv.alert
                SET
                    state = 'RESOLVED',
                    resolved_by_validation_result_id = ?,
                    updated_at = ?,
                    closed_at = ?
                WHERE id = ?
                """,
                validationResultId,
                NOW.plusMinutes(
                        3L),
                NOW.plusMinutes(
                        3L),
                ALERT_ID);
    }

}