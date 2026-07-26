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
class AlertLifecycleSchemaIntegrationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "899ff432-62c4-49bd-b727-d5bcf0e30001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "899ff432-62c4-49bd-b727-d5bcf0e30002");

    private static final UUID RESULT_ID =
            UUID.fromString(
                    "899ff432-62c4-49bd-b727-d5bcf0e30003");

    private static final UUID ALERT_ID =
            UUID.fromString(
                    "899ff432-62c4-49bd-b727-d5bcf0e30004");

    private static final UUID TRANSITION_ID =
            UUID.fromString(
                    "899ff432-62c4-49bd-b727-d5bcf0e30005");

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse(
                    "2026-07-26T14:00:00Z");

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
    void createsLifecycleProtectionTriggers() {
        assertThat(
                triggerNames(
                        "alert"))
                .contains(
                        "trg_alert_protect_lifecycle");

        assertThat(
                triggerNames(
                        "alert_transition"))
                .contains(
                        "trg_alert_transition_append_only");
    }

    @Test
    void rejectsPhysicalAlertDeletionAndTransitionMutation() {
        persistParentCloseAndEvent();
        persistValidationResult(
                "FAILED");
        persistActiveAlert();
        persistInitialTransition();

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        UPDATE ocv.alert_transition
                        SET detail = 'Modified detail.'
                        WHERE id = ?
                        """,
                        TRANSITION_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        DELETE FROM ocv.alert_transition
                        WHERE id = ?
                        """,
                        TRANSITION_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        DELETE FROM ocv.alert
                        WHERE id = ?
                        """,
                        ALERT_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void rejectsModificationOfResolvedAlert() {
        persistParentCloseAndEvent();
        persistValidationResult(
                "SATISFIED");
        persistResolvedAlert();

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        UPDATE ocv.alert
                        SET
                            detail = 'Retroactively modified detail.',
                            updated_at = ?
                        WHERE id = ?
                        """,
                        NOW.plusMinutes(
                                2L),
                        ALERT_ID))
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
                    DATE '2026-09-01',
                    DATE '2026-09-30',
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
                    'EXPENSE',
                    50.0000,
                    -50.0000,
                    NULL,
                    ?,
                    ?,
                    'Caja principal',
                    'Evento para proteger el ciclo de Alertas',
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
                EVENT_ID,
                CLOSE_ID,
                NOW,
                NOW,
                NOW,
                NOW,
                NOW);
    }

    private void persistValidationResult(
            String outcome) {

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
                    'Validation result for lifecycle protection.',
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
                RESULT_ID,
                EVENT_ID,
                outcome,
                NOW);
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
                RESULT_ID,
                NOW,
                NOW);
    }

    private void persistResolvedAlert() {
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
                    NULL,
                    'VR-003',
                    'HIGH',
                    TRUE,
                    'RESOLVED',
                    'Supporting evidence was revalidated.',
                    ?,
                    NULL,
                    ?,
                    'responsible-user',
                    'responsible',
                    ?,
                    ?
                )
                """,
                ALERT_ID,
                EVENT_ID,
                RESULT_ID,
                NOW,
                NOW.plusMinutes(
                        1L),
                NOW.plusMinutes(
                        1L));
    }

    private void persistInitialTransition() {
        jdbcTemplate.update(
                """
                INSERT INTO ocv.alert_transition (
                    id,
                    alert_id,
                    from_state,
                    to_state,
                    action_code,
                    detail,
                    justification,
                    validation_result_id,
                    occurred_at,
                    actor_user_id,
                    actor_username
                )
                VALUES (
                    ?,
                    ?,
                    NULL,
                    'ACTIVE',
                    'ALERT_CREATED',
                    'Alert creation.',
                    NULL,
                    NULL,
                    ?,
                    'responsible-user',
                    'responsible'
                )
                """,
                TRANSITION_ID,
                ALERT_ID,
                NOW);
    }

}