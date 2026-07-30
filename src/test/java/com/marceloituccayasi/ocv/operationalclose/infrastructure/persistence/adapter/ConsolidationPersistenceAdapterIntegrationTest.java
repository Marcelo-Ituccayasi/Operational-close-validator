package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.ConsolidationRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.Consolidation;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationEventSnapshot;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConsolidationPersistenceAdapterIntegrationTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "71be5e3a-c1c0-4cc4-a7f1-740000000001"));

    private static final OperationalEventId INCOME_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "71be5e3a-c1c0-4cc4-a7f1-740000000002"));

    private static final OperationalEventId EXPENSE_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "71be5e3a-c1c0-4cc4-a7f1-740000000003"));

    private static final ConsolidationId FIRST_CONSOLIDATION_ID =
            new ConsolidationId(
                    UUID.fromString(
                            "71be5e3a-c1c0-4cc4-a7f1-740000000004"));

    private static final ConsolidationId SECOND_CONSOLIDATION_ID =
            new ConsolidationId(
                    UUID.fromString(
                            "71be5e3a-c1c0-4cc4-a7f1-740000000005"));

    private static final OperationalEventId MISSING_EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "71be5e3a-c1c0-4cc4-a7f1-740000000006"));

    private static final ConsolidationId ROLLBACK_CONSOLIDATION_ID =
            new ConsolidationId(
                    UUID.fromString(
                            "71be5e3a-c1c0-4cc4-a7f1-740000000007"));

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-30T12:00:00Z");

    private static final Instant FIRST_COMPLETED_AT =
            Instant.parse(
                    "2026-07-30T13:00:00Z");

    private static final Instant SECOND_COMPLETED_AT =
            Instant.parse(
                    "2026-07-30T14:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Autowired
    private ConsolidationRepository consolidationRepository;

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
    void persistsAndReconstructsCurrentConsolidationAndSnapshots() {
        persistCloseAndEvents();

        Consolidation expected =
                complete(
                        FIRST_CONSOLIDATION_ID,
                        FIRST_COMPLETED_AT);

        transactionRunner.execute(
                (Runnable) () ->
                        consolidationRepository.saveNew(
                                expected));

        Optional<Consolidation> loadedById =
                consolidationRepository.findById(
                        FIRST_CONSOLIDATION_ID);

        Optional<Consolidation> loadedCurrent =
                consolidationRepository.findCurrentByCloseId(
                        CLOSE_ID);

        assertThat(
                loadedById)
                .contains(
                        expected);

        assertThat(
                loadedCurrent)
                .contains(
                        expected);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.consolidation
                        WHERE id = ?
                        """,
                        FIRST_CONSOLIDATION_ID.value()))
                .isEqualTo(
                        1);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.consolidation_event_snapshot
                        WHERE consolidation_id = ?
                        """,
                        FIRST_CONSOLIDATION_ID.value()))
                .isEqualTo(
                        2);
    }

    @Test
    void persistsInvalidationAndLoadsHistoryInCompletionOrder() {
        persistCloseAndEvents();

        Consolidation first =
                complete(
                        FIRST_CONSOLIDATION_ID,
                        FIRST_COMPLETED_AT);

        transactionRunner.execute(
                (Runnable) () ->
                        consolidationRepository.saveNew(
                                first));

        Consolidation invalidated =
                first.invalidate(
                        FIRST_COMPLETED_AT.plusSeconds(
                                60),
                        "A newer consolidation was completed.");

        transactionRunner.execute(
                (Runnable) () ->
                        consolidationRepository.saveInvalidation(
                                invalidated));

        Consolidation second =
                complete(
                        SECOND_CONSOLIDATION_ID,
                        SECOND_COMPLETED_AT);

        transactionRunner.execute(
                (Runnable) () ->
                        consolidationRepository.saveNew(
                                second));

        List<Consolidation> history =
                consolidationRepository
                        .findAllByCloseIdOrderByCompletedAt(
                                CLOSE_ID);

        assertThat(
                consolidationRepository.findCurrentByCloseId(
                        CLOSE_ID))
                .contains(
                        second);

        assertThat(
                consolidationRepository.findById(
                        FIRST_CONSOLIDATION_ID))
                .contains(
                        invalidated);

        assertThat(
                history)
                .containsExactly(
                        invalidated,
                        second);

        assertThat(
                history.getFirst()
                        .eventSnapshots())
                .hasSize(
                        2);
    }

    @Test
    void rollsBackParentWhenSnapshotPersistenceFails() {
        persistCloseOnly();

        Consolidation invalid =
                consolidationReferencingMissingEvent();

        assertThatThrownBy(
                () -> transactionRunner.execute(
                        (Runnable) () ->
                                consolidationRepository.saveNew(
                                        invalid)))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.consolidation
                        WHERE id = ?
                        """,
                        ROLLBACK_CONSOLIDATION_ID.value()))
                .isZero();

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.consolidation_event_snapshot
                        WHERE consolidation_id = ?
                        """,
                        ROLLBACK_CONSOLIDATION_ID.value()))
                .isZero();
    }

    private Consolidation complete(
            ConsolidationId consolidationId,
            Instant completedAt) {

        return Consolidation.complete(
                consolidationId,
                close(),
                List.of(
                        income(),
                        expense()),
                decimal(
                        "1150.0000"),
                completedAt,
                ACTOR);
    }

    private Consolidation consolidationReferencingMissingEvent() {
        ConsolidationEventSnapshot snapshot =
                new ConsolidationEventSnapshot(
                        ROLLBACK_CONSOLIDATION_ID,
                        MISSING_EVENT_ID,
                        1,
                        OperationalEventType.INCOME,
                        new OperationalEventAmount(
                                decimal(
                                        "10.0000")),
                        decimal(
                                "10.0000"),
                        null,
                        OperationalEventState.VALIDATED,
                        FIRST_COMPLETED_AT);

        return new Consolidation(
                ROLLBACK_CONSOLIDATION_ID,
                CLOSE_ID,
                new CurrencyCode(
                        "PEN"),
                1,
                decimal(
                        "10.0000"),
                decimal(
                        "0.0000"),
                decimal(
                        "0.0000"),
                decimal(
                        "0.0000"),
                new InitialBalance(
                        decimal(
                                "1000.0000")),
                decimal(
                        "1010.0000"),
                decimal(
                        "1010.0000"),
                decimal(
                        "0.0000"),
                true,
                FIRST_COMPLETED_AT,
                ACTOR,
                null,
                null,
                List.of(
                        snapshot));
    }

    private OperationalClose close() {
        return OperationalClose.create(
                CLOSE_ID,
                new OperationalPeriod(
                        LocalDate.of(
                                2026,
                                7,
                                1),
                        LocalDate.of(
                                2026,
                                7,
                                31)),
                new CurrencyCode(
                        "PEN"),
                new InitialBalance(
                        decimal(
                                "1000.0000")),
                CREATED_AT,
                ACTOR);
    }

    private OperationalEvent income() {
        return event(
                INCOME_ID,
                OperationalEventType.INCOME,
                "200.0000",
                "200.0000");
    }

    private OperationalEvent expense() {
        return event(
                EXPENSE_ID,
                OperationalEventType.EXPENSE,
                "50.0000",
                "-50.0000");
    }

    private OperationalEvent event(
            OperationalEventId eventId,
            OperationalEventType eventType,
            String amount,
            String balanceEffect) {

        return new OperationalEvent(
                eventId,
                CLOSE_ID,
                eventType,
                new OperationalEventAmount(
                        decimal(
                                amount)),
                decimal(
                        balanceEffect),
                null,
                CREATED_AT,
                CREATED_AT,
                "Caja principal",
                "Evento persistido para consolidación",
                OperationalEventState.VALIDATED,
                false,
                false,
                2,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

    private void persistCloseAndEvents() {
        persistCloseOnly();

        persistEvent(
                INCOME_ID,
                "INCOME",
                "200.0000",
                "200.0000");

        persistEvent(
                EXPENSE_ID,
                "EXPENSE",
                "50.0000",
                "-50.0000");
    }

    private void persistCloseOnly() {
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
                CLOSE_ID.value(),
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT));
    }

    private void persistEvent(
            OperationalEventId eventId,
            String eventType,
            String amount,
            String balanceEffect) {

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
                    ?,
                    ?,
                    ?,
                    NULL,
                    ?,
                    ?,
                    'Caja principal',
                    'Evento persistido para consolidación',
                    'VALIDATED',
                    FALSE,
                    FALSE,
                    2,
                    ?,
                    ?,
                    'responsible-user',
                    'responsible',
                    ?,
                    'responsible-user',
                    'responsible'
                )
                """,
                eventId.value(),
                CLOSE_ID.value(),
                eventType,
                decimal(
                        amount),
                decimal(
                        balanceEffect),
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT));
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

    private static BigDecimal decimal(
            String value) {

        return new BigDecimal(
                value);
    }

}