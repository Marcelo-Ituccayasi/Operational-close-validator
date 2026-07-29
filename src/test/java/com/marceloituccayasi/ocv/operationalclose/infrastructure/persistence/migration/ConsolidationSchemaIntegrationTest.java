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
class ConsolidationSchemaIntegrationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "1403796f-ec42-4979-b039-270000000001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "1403796f-ec42-4979-b039-270000000002");

    private static final UUID OTHER_EVENT_ID =
            UUID.fromString(
                    "1403796f-ec42-4979-b039-270000000003");

    private static final UUID CONSOLIDATION_ID =
            UUID.fromString(
                    "1403796f-ec42-4979-b039-270000000004");

    private static final UUID OTHER_CONSOLIDATION_ID =
            UUID.fromString(
                    "1403796f-ec42-4979-b039-270000000005");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsApprovedConsolidationSchema() {
        assertThat(
                columnNames(
                        "consolidation"))
                .containsExactly(
                        "id",
                        "close_id",
                        "currency_code",
                        "event_count",
                        "total_income",
                        "total_expense",
                        "total_discount",
                        "total_cancellation",
                        "initial_balance",
                        "expected_balance",
                        "actual_balance",
                        "difference",
                        "is_current",
                        "completed_at",
                        "completed_by_user_id",
                        "completed_by_username",
                        "invalidated_at",
                        "invalidation_reason");

        assertThat(
                constraintNames(
                        "consolidation"))
                .contains(
                        "pk_consolidation",
                        "fk_consolidation_close",
                        "ck_consolidation_currency_code",
                        "ck_consolidation_event_count",
                        "ck_consolidation_totals",
                        "ck_consolidation_initial_balance",
                        "ck_consolidation_actual_balance",
                        "ck_consolidation_difference",
                        "ck_consolidation_completed_by_user",
                        "ck_consolidation_completed_by_username",
                        "ck_consolidation_validity");

        assertThat(
                indexNames(
                        "consolidation"))
                .contains(
                        "uq_consolidation_current_close",
                        "idx_consolidation_close_completed_at");

        assertThat(
                triggerNames(
                        "consolidation"))
                .contains(
                        "trg_consolidation_immutability");
    }

    @Test
    void createsApprovedSnapshotSchema() {
        assertThat(
                columnNames(
                        "consolidation_event_snapshot"))
                .containsExactly(
                        "consolidation_id",
                        "event_id",
                        "event_data_revision",
                        "event_type",
                        "amount",
                        "balance_effect",
                        "reversed_event_id",
                        "event_state",
                        "captured_at");

        assertThat(
                constraintNames(
                        "consolidation_event_snapshot"))
                .contains(
                        "pk_consolidation_event_snapshot",
                        "fk_consolidation_snapshot_consolidation",
                        "fk_consolidation_snapshot_event",
                        "fk_consolidation_snapshot_reversed_event",
                        "ck_consolidation_snapshot_revision",
                        "ck_consolidation_snapshot_type",
                        "ck_consolidation_snapshot_amount",
                        "ck_consolidation_snapshot_balance_effect",
                        "ck_consolidation_snapshot_reversed_reference",
                        "ck_consolidation_snapshot_state");

        assertThat(
                indexNames(
                        "consolidation_event_snapshot"))
                .contains(
                        "idx_consolidation_snapshot_event",
                        "idx_consolidation_snapshot_reversed_event");

        assertThat(
                triggerNames(
                        "consolidation_event_snapshot"))
                .contains(
                        "trg_consolidation_snapshot_append_only");
    }

    @Test
    void completesReservedForeignKeys() {
        assertThat(
                constraintNames(
                        "validation_result"))
                .contains(
                        "fk_validation_result_consolidation");

        assertThat(
                constraintNames(
                        "close_state_transition"))
                .contains(
                        "fk_close_state_transition_validation_result",
                        "fk_close_state_transition_consolidation");
    }

    @Test
    void permitsOnlyOneCurrentConsolidationPerClose() {
        insertClose();
        insertCurrentConsolidation(
                CONSOLIDATION_ID);

        assertThatThrownBy(
                () -> insertCurrentConsolidation(
                        OTHER_CONSOLIDATION_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void enforcesDifferenceFormula() {
        insertClose();

        assertThatThrownBy(
                () -> jdbcTemplate.update(
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
                            1090.0000,
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
                        CLOSE_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void permitsSnapshotOnlyForValidatedCapturedState() {
        insertClose();
        insertEvent(
                EVENT_ID,
                "VALIDATED");
        insertEvent(
                OTHER_EVENT_ID,
                "REGISTERED");
        insertCurrentConsolidation(
                CONSOLIDATION_ID);

        insertSnapshot(
                EVENT_ID,
                "VALIDATED");

        Integer snapshotCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.consolidation_event_snapshot
                        WHERE consolidation_id = ?
                        """,
                        Integer.class,
                        CONSOLIDATION_ID);

        assertThat(
                snapshotCount)
                .isEqualTo(
                        1);

        assertThatThrownBy(
                () -> insertSnapshot(
                        OTHER_EVENT_ID,
                        "REGISTERED"))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void protectsSnapshotFromModification() {
        insertClose();
        insertEvent(
                EVENT_ID,
                "VALIDATED");
        insertCurrentConsolidation(
                CONSOLIDATION_ID);
        insertSnapshot(
                EVENT_ID,
                "VALIDATED");

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        UPDATE ocv.consolidation_event_snapshot
                        SET amount = 200.0000,
                            balance_effect = 200.0000
                        WHERE consolidation_id = ?
                          AND event_id = ?
                        """,
                        CONSOLIDATION_ID,
                        EVENT_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void permitsOnlyValidityInvalidationOnConsolidation() {
        insertClose();
        insertCurrentConsolidation(
                CONSOLIDATION_ID);

        jdbcTemplate.update(
                """
                UPDATE ocv.consolidation
                SET is_current = FALSE,
                    invalidated_at = CURRENT_TIMESTAMP,
                    invalidation_reason =
                        'Operational Event data revision changed.'
                WHERE id = ?
                """,
                CONSOLIDATION_ID);

        Boolean current =
                jdbcTemplate.queryForObject(
                        """
                        SELECT is_current
                        FROM ocv.consolidation
                        WHERE id = ?
                        """,
                        Boolean.class,
                        CONSOLIDATION_ID);

        assertThat(
                current)
                .isFalse();

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        UPDATE ocv.consolidation
                        SET actual_balance = 1200.0000,
                            difference = 100.0000
                        WHERE id = ?
                        """,
                        CONSOLIDATION_ID))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
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
                    DATE '2026-07-01',
                    DATE '2026-07-31',
                    'PEN',
                    1000.0000,
                    'PREPARATION',
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

    private void insertEvent(
            UUID eventId,
            String state) {

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
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    'Caja principal',
                    'Ingreso para prueba de consolidación',
                    ?,
                    FALSE,
                    FALSE,
                    1,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    'responsible-user',
                    'responsible',
                    CURRENT_TIMESTAMP,
                    'responsible-user',
                    'responsible'
                )
                """,
                eventId,
                CLOSE_ID,
                state);
    }

    private void insertCurrentConsolidation(
            UUID consolidationId) {

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
                consolidationId,
                CLOSE_ID);
    }

    private void insertSnapshot(
            UUID eventId,
            String state) {

        jdbcTemplate.update(
                """
                INSERT INTO ocv.consolidation_event_snapshot (
                    consolidation_id,
                    event_id,
                    event_data_revision,
                    event_type,
                    amount,
                    balance_effect,
                    reversed_event_id,
                    event_state,
                    captured_at
                )
                VALUES (
                    ?,
                    ?,
                    1,
                    'INCOME',
                    100.0000,
                    100.0000,
                    NULL,
                    ?,
                    CURRENT_TIMESTAMP
                )
                """,
                CONSOLIDATION_ID,
                eventId,
                state);
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
                SELECT trigger_name
                FROM information_schema.triggers
                WHERE event_object_schema = 'ocv'
                  AND event_object_table = ?
                ORDER BY trigger_name
                """,
                String.class,
                tableName);
    }

}