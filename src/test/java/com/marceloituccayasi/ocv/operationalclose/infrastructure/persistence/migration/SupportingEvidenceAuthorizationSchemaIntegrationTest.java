package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SupportingEvidenceAuthorizationSchemaIntegrationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "8e09bc80-96d2-4f74-b420-635a72150001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "8e09bc80-96d2-4f74-b420-635a72150002");

    private static final UUID FIRST_EVIDENCE_ID =
            UUID.fromString(
                    "8e09bc80-96d2-4f74-b420-635a72150003");

    private static final UUID SECOND_EVIDENCE_ID =
            UUID.fromString(
                    "8e09bc80-96d2-4f74-b420-635a72150004");

    private static final UUID FIRST_AUTHORIZATION_ID =
            UUID.fromString(
                    "8e09bc80-96d2-4f74-b420-635a72150005");

    private static final UUID SECOND_AUTHORIZATION_ID =
            UUID.fromString(
                    "8e09bc80-96d2-4f74-b420-635a72150006");

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse(
                    "2026-07-23T16:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {
        truncateOperationalTables();
    }

    @Test
    void createsApprovedTablesConstraintsAndIndexes() {
        assertThat(
                columnNames(
                        "supporting_evidence"))
                .containsExactly(
                        "id",
                        "event_id",
                        "evidence_type",
                        "content_reference",
                        "supported_amount",
                        "evidence_date",
                        "legibility_status",
                        "is_active",
                        "revision",
                        "created_at",
                        "created_by_user_id",
                        "created_by_username",
                        "updated_at",
                        "updated_by_user_id",
                        "updated_by_username",
                        "deactivated_at");

        assertThat(
                constraintNames(
                        "supporting_evidence"))
                .contains(
                        "fk_supporting_evidence_event",
                        "ck_supporting_evidence_evidence_type",
                        "ck_supporting_evidence_content_reference",
                        "ck_supporting_evidence_supported_amount",
                        "ck_supporting_evidence_legibility_status",
                        "ck_supporting_evidence_revision",
                        "ck_supporting_evidence_activity",
                        "ck_supporting_evidence_created_by_user",
                        "ck_supporting_evidence_created_by_username",
                        "ck_supporting_evidence_updated_by_user",
                        "ck_supporting_evidence_updated_by_username");

        assertThat(
                indexNames(
                        "supporting_evidence"))
                .contains(
                        "idx_supporting_evidence_event_active",
                        "idx_supporting_evidence_event_date");

        assertThat(
                columnNames(
                        "event_authorization"))
                .containsExactly(
                        "id",
                        "event_id",
                        "authorized_by_name",
                        "reason",
                        "authorized_at",
                        "formal_reference",
                        "is_active",
                        "revision",
                        "created_at",
                        "created_by_user_id",
                        "created_by_username",
                        "updated_at",
                        "updated_by_user_id",
                        "updated_by_username",
                        "deactivated_at");

        assertThat(
                constraintNames(
                        "event_authorization"))
                .contains(
                        "fk_event_authorization_event",
                        "ck_event_authorization_authorized_by_name",
                        "ck_event_authorization_reason",
                        "ck_event_authorization_formal_reference",
                        "ck_event_authorization_revision",
                        "ck_event_authorization_activity",
                        "ck_event_authorization_created_by_user",
                        "ck_event_authorization_created_by_username",
                        "ck_event_authorization_updated_by_user",
                        "ck_event_authorization_updated_by_username");

        assertThat(
                indexNames(
                        "event_authorization"))
                .contains(
                        "idx_event_authorization_event_active",
                        "idx_event_authorization_authorized_at");
    }

    @Test
    void permitsMultipleActiveEvidenceAndAuthorizationsPerEvent() {
        persistParentCloseAndEvent();

        persistActiveEvidence(
                FIRST_EVIDENCE_ID,
                "reference:first-evidence");

        persistActiveEvidence(
                SECOND_EVIDENCE_ID,
                "reference:second-evidence");

        persistActiveAuthorization(
                FIRST_AUTHORIZATION_ID,
                "AUTHORIZATION-1");

        persistActiveAuthorization(
                SECOND_AUTHORIZATION_ID,
                "AUTHORIZATION-2");

        Long activeEvidence =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.supporting_evidence
                        WHERE event_id = ?
                          AND is_active = TRUE
                        """,
                        Long.class,
                        EVENT_ID);

        Long activeAuthorizations =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.event_authorization
                        WHERE event_id = ?
                          AND is_active = TRUE
                        """,
                        Long.class,
                        EVENT_ID);

        assertThat(activeEvidence)
                .isEqualTo(2L);

        assertThat(activeAuthorizations)
                .isEqualTo(2L);
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
                    'Evento para validar relaciones múltiples',
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

    private void persistActiveEvidence(
            UUID evidenceId,
            String contentReference) {

        jdbcTemplate.update(
                """
                INSERT INTO ocv.supporting_evidence (
                    id,
                    event_id,
                    evidence_type,
                    content_reference,
                    supported_amount,
                    evidence_date,
                    legibility_status,
                    is_active,
                    revision,
                    created_at,
                    created_by_user_id,
                    created_by_username,
                    updated_at,
                    updated_by_user_id,
                    updated_by_username,
                    deactivated_at
                )
                VALUES (
                    ?,
                    ?,
                    'RECEIPT',
                    ?,
                    100.0000,
                    DATE '2026-07-22',
                    'UNVERIFIED',
                    TRUE,
                    1,
                    ?,
                    'responsible-user',
                    'responsible',
                    ?,
                    'responsible-user',
                    'responsible',
                    NULL
                )
                """,
                evidenceId,
                EVENT_ID,
                contentReference,
                NOW,
                NOW);
    }

    private void persistActiveAuthorization(
            UUID authorizationId,
            String formalReference) {

        jdbcTemplate.update(
                """
                INSERT INTO ocv.event_authorization (
                    id,
                    event_id,
                    authorized_by_name,
                    reason,
                    authorized_at,
                    formal_reference,
                    is_active,
                    revision,
                    created_at,
                    created_by_user_id,
                    created_by_username,
                    updated_at,
                    updated_by_user_id,
                    updated_by_username,
                    deactivated_at
                )
                VALUES (
                    ?,
                    ?,
                    'Jefatura de Operaciones',
                    'Excepción aprobada por contingencia',
                    ?,
                    ?,
                    TRUE,
                    1,
                    ?,
                    'responsible-user',
                    'responsible',
                    ?,
                    'responsible-user',
                    'responsible',
                    NULL
                )
                """,
                authorizationId,
                EVENT_ID,
                NOW,
                formalReference,
                NOW,
                NOW);
    }

    private void truncateOperationalTables() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
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

}