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
class AlertSchemaIntegrationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "ac6db6c2-d438-48c9-a878-8cd2ea020001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "ac6db6c2-d438-48c9-a878-8cd2ea020002");

    private static final UUID RESULT_ID =
            UUID.fromString(
                    "ac6db6c2-d438-48c9-a878-8cd2ea020003");

    private static final UUID ALERT_ID =
            UUID.fromString(
                    "ac6db6c2-d438-48c9-a878-8cd2ea020004");

    private static final UUID TRANSITION_ID =
            UUID.fromString(
                    "ac6db6c2-d438-48c9-a878-8cd2ea020005");

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse(
                    "2026-07-26T12:00:00Z");

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
    void createsApprovedAlertSchema() {
        assertThat(
                columnNames(
                        "alert"))
                .containsExactly(
                        "id",
                        "event_id",
                        "close_id",
                        "source_validation_result_id",
                        "cause_code",
                        "severity",
                        "is_blocking",
                        "state",
                        "detail",
                        "resolved_by_validation_result_id",
                        "discard_justification",
                        "created_at",
                        "created_by_user_id",
                        "created_by_username",
                        "updated_at",
                        "closed_at");

        assertThat(
                constraintNames(
                        "alert"))
                .contains(
                        "alert_pkey",
                        "fk_alert_event",
                        "fk_alert_close",
                        "fk_alert_source_validation_result",
                        "fk_alert_resolution_validation_result",
                        "ck_alert_target",
                        "ck_alert_cause_code",
                        "ck_alert_severity",
                        "ck_alert_state",
                        "ck_alert_detail",
                        "ck_alert_created_by_user",
                        "ck_alert_created_by_username",
                        "ck_alert_update_instant",
                        "ck_alert_close_instant",
                        "ck_alert_terminal_metadata");

        assertThat(
                indexNames(
                        "alert"))
                .contains(
                        "idx_alert_event_state",
                        "idx_alert_close_state",
                        "idx_alert_blocking_open_event",
                        "idx_alert_blocking_open_close",
                        "idx_alert_source_validation_result");
    }

    @Test
    void createsApprovedTransitionSchema() {
        assertThat(
                columnNames(
                        "alert_transition"))
                .containsExactly(
                        "id",
                        "alert_id",
                        "from_state",
                        "to_state",
                        "action_code",
                        "detail",
                        "justification",
                        "validation_result_id",
                        "occurred_at",
                        "actor_user_id",
                        "actor_username");

        assertThat(
                constraintNames(
                        "alert_transition"))
                .contains(
                        "alert_transition_pkey",
                        "fk_alert_transition_alert",
                        "fk_alert_transition_validation_result",
                        "ck_alert_transition_from_state",
                        "ck_alert_transition_to_state",
                        "ck_alert_transition_state_change",
                        "ck_alert_transition_action_code",
                        "ck_alert_transition_terminal_metadata",
                        "ck_alert_transition_actor_user",
                        "ck_alert_transition_actor_username");

        assertThat(
                indexNames(
                        "alert_transition"))
                .contains(
                        "idx_alert_transition_alert_occurred_at",
                        "idx_alert_transition_to_state");
    }

    @Test
    void persistsActiveEventAlertAndInitialTransition() {
        persistParentCloseAndEvent();
        persistFailedValidationResult();
        persistActiveAlert();

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
                    'Supporting evidence is missing.',
                    NULL,
                    NULL,
                    ?,
                    'responsible-user',
                    'responsible'
                )
                """,
                TRANSITION_ID,
                ALERT_ID,
                NOW.plusMinutes(
                        1L));

        String persistedAlert =
                jdbcTemplate.queryForObject(
                        """
                        SELECT
                            cause_code
                            || '|'
                            || severity
                            || '|'
                            || is_blocking
                            || '|'
                            || state
                        FROM ocv.alert
                        WHERE id = ?
                        """,
                        String.class,
                        ALERT_ID);

        assertThat(persistedAlert)
                .isEqualTo(
                        "VR-003|HIGH|true|ACTIVE");

        String persistedTransition =
                jdbcTemplate.queryForObject(
                        """
                        SELECT
                            COALESCE(
                                from_state,
                                'NULL')
                            || '|'
                            || to_state
                            || '|'
                            || action_code
                        FROM ocv.alert_transition
                        WHERE id = ?
                        """,
                        String.class,
                        TRANSITION_ID);

        assertThat(persistedTransition)
                .isEqualTo(
                        "NULL|ACTIVE|ALERT_CREATED");
    }

    @Test
    void rejectsInvalidAlertTargetsAndClosureMetadata() {
        persistParentCloseAndEvent();
        persistFailedValidationResult();

        assertThatThrownBy(
                () -> jdbcTemplate.update(
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
                            ?,
                            ?,
                            'VR-003',
                            'HIGH',
                            TRUE,
                            'ACTIVE',
                            'Two targets are invalid.',
                            NULL,
                            NULL,
                            ?,
                            'responsible-user',
                            'responsible',
                            ?,
                            NULL
                        )
                        """,
                        UUID.randomUUID(),
                        EVENT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        NOW,
                        NOW))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        assertThatThrownBy(
                () -> jdbcTemplate.update(
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
                            'RESOLVED',
                            'Resolution metadata is missing.',
                            NULL,
                            NULL,
                            ?,
                            'responsible-user',
                            'responsible',
                            ?,
                            ?
                        )
                        """,
                        UUID.randomUUID(),
                        EVENT_ID,
                        RESULT_ID,
                        NOW,
                        NOW.plusMinutes(
                                1L),
                        NOW.plusMinutes(
                                1L)))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        assertThatThrownBy(
                () -> jdbcTemplate.update(
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
                            'DISCARDED',
                            'Discard justification is missing.',
                            NULL,
                            ' ',
                            ?,
                            'responsible-user',
                            'responsible',
                            ?,
                            ?
                        )
                        """,
                        UUID.randomUUID(),
                        EVENT_ID,
                        RESULT_ID,
                        NOW,
                        NOW.plusMinutes(
                                1L),
                        NOW.plusMinutes(
                                1L)))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void rejectsInvalidTransitionClosureMetadata() {
        persistParentCloseAndEvent();
        persistFailedValidationResult();
        persistActiveAlert();

        assertThatThrownBy(
                () -> jdbcTemplate.update(
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
                            'ACTIVE',
                            'RESOLVED',
                            'ALERT_RESOLVED',
                            'Resolution result is missing.',
                            NULL,
                            NULL,
                            ?,
                            'responsible-user',
                            'responsible'
                        )
                        """,
                        UUID.randomUUID(),
                        ALERT_ID,
                        NOW.plusMinutes(
                                1L)))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        assertThatThrownBy(
                () -> jdbcTemplate.update(
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
                            'ACTIVE',
                            'DISCARDED',
                            'ALERT_DISCARDED',
                            'Discard justification is missing.',
                            ' ',
                            NULL,
                            ?,
                            'responsible-user',
                            'responsible'
                        )
                        """,
                        UUID.randomUUID(),
                        ALERT_ID,
                        NOW.plusMinutes(
                                1L)))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
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
                    DATE '2026-08-01',
                    DATE '2026-08-31',
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
                    'Evento para probar Alertas',
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

    private void persistFailedValidationResult() {
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
                    'FAILED',
                    'Required supporting evidence is missing.',
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
                NOW.plusMinutes(
                        1L),
                NOW.plusMinutes(
                        1L));
    }

}