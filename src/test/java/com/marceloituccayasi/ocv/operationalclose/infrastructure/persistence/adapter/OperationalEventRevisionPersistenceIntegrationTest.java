package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
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
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OperationalEventRevisionPersistenceIntegrationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "c0e350ce-804d-4d97-bfd6-53e703100001");

    private static final UUID OTHER_CLOSE_ID =
            UUID.fromString(
                    "c0e350ce-804d-4d97-bfd6-53e703100008");

    private static final UUID ORIGINAL_EVENT_ID =
            UUID.fromString(
                    "c0e350ce-804d-4d97-bfd6-53e703100002");

    private static final UUID CANCELLATION_EVENT_ID =
            UUID.fromString(
                    "c0e350ce-804d-4d97-bfd6-53e703100003");

    private static final UUID MISSING_EVENT_ID =
            UUID.fromString(
                    "c0e350ce-804d-4d97-bfd6-53e703100004");

    private static final UUID ORIGINAL_TRANSITION_ID =
            UUID.fromString(
                    "c0e350ce-804d-4d97-bfd6-53e703100005");

    private static final UUID CANCELLATION_TRANSITION_ID =
            UUID.fromString(
                    "c0e350ce-804d-4d97-bfd6-53e703100006");

    private static final UUID INVALID_TRANSITION_ID =
            UUID.fromString(
                    "c0e350ce-804d-4d97-bfd6-53e703100007");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-22T08:00:00Z");

    private static final Instant REVISED_AT =
            Instant.parse(
                    "2026-07-22T10:00:00Z");

    @Autowired
    private OperationalEventRepository eventRepository;

    @Autowired
    private OperationalEventRevisionRepository
            revisionRepository;

    @Autowired
    private TransactionRunner transactionRunner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareDatabase() {
        cleanOperationalCloseTables();
        insertOperationalClose();
    }

    @AfterEach
    void cleanDatabase() {
        cleanOperationalCloseTables();
    }

    @Test
    void closeScopedLocksDoNotReturnEventsFromAnotherClose() {
        OperationalEvent originalEvent =
                originalExpense(
                        "80.0000");

        OperationalEvent cancellation =
                OperationalEvent.createCancellation(
                        new OperationalEventId(
                                CANCELLATION_EVENT_ID),
                        new OperationalCloseId(
                                CLOSE_ID),
                        originalEvent,
                        CREATED_AT.plusSeconds(120),
                        "Caja principal",
                        "Anulación inicial",
                        false,
                        false,
                        CREATED_AT.plusSeconds(180),
                        actor());

        transactionRunner.execute(
                () -> {
                    eventRepository.saveNew(
                            originalEvent,
                            initialTransition(
                                    originalEvent,
                                    ORIGINAL_TRANSITION_ID));

                    eventRepository.saveNew(
                            cancellation,
                            initialTransition(
                                    cancellation,
                                    CANCELLATION_TRANSITION_ID));
                });

        OperationalCloseId otherCloseId =
                new OperationalCloseId(
                        OTHER_CLOSE_ID);

        transactionRunner.execute(
                () -> {
                    assertThat(
                            revisionRepository.findByIdForUpdate(
                                    otherCloseId,
                                    originalEvent.id()))
                            .isEmpty();

                    assertThat(
                            revisionRepository
                                    .findCancellationByReversedEventIdForUpdate(
                                            otherCloseId,
                                            originalEvent.id()))
                            .isEmpty();
                });
    }

    @Test
    void locksAndPersistsDependentCancellationRecalculation() {
        OperationalEvent originalEvent =
                originalExpense(
                        "80.0000");

        OperationalEvent cancellation =
                OperationalEvent.createCancellation(
                        new OperationalEventId(
                                CANCELLATION_EVENT_ID),
                        new OperationalCloseId(
                                CLOSE_ID),
                        originalEvent,
                        CREATED_AT.plusSeconds(120),
                        "Caja principal",
                        "Anulación inicial",
                        false,
                        false,
                        CREATED_AT.plusSeconds(180),
                        actor());

        transactionRunner.execute(
                () -> {
                    eventRepository.saveNew(
                            originalEvent,
                            initialTransition(
                                    originalEvent,
                                    ORIGINAL_TRANSITION_ID));

                    eventRepository.saveNew(
                            cancellation,
                            initialTransition(
                                    cancellation,
                                    CANCELLATION_TRANSITION_ID));
                });

        transactionRunner.execute(
                () -> {
                    OperationalEvent lockedOriginal =
                            revisionRepository
                                    .findByIdForUpdate(
                                            new OperationalCloseId(CLOSE_ID),
                                            originalEvent.id())
                                    .orElseThrow();

                    OperationalEvent lockedCancellation =
                            revisionRepository
                                    .findCancellationByReversedEventIdForUpdate(
                                            new OperationalCloseId(CLOSE_ID),
                                            originalEvent.id())
                                    .orElseThrow();

                    OperationalEvent revisedOriginal =
                            lockedOriginal.reviseRegular(
                                    OperationalEventType.EXPENSE,
                                    new OperationalEventAmount(
                                            new BigDecimal(
                                                    "125.2500")),
                                    REVISED_AT.minusSeconds(300),
                                    "Caja revisada",
                                    "Gasto revisado",
                                    false,
                                    false,
                                    REVISED_AT,
                                    actor());

                    OperationalEvent
                            recalculatedCancellation =
                                    lockedCancellation
                                            .recalculateFromRevisedOriginal(
                                                    revisedOriginal,
                                                    REVISED_AT,
                                                    actor());

                    revisionRepository.saveRevision(
                            revisedOriginal);

                    revisionRepository.saveRevision(
                            recalculatedCancellation);
                });

        OperationalEvent persistedOriginal =
                transactionRunner.execute(
                        () -> eventRepository
                                .findById(
                                        originalEvent.id())
                                .orElseThrow());

        OperationalEvent persistedCancellation =
                transactionRunner.execute(
                        () -> eventRepository
                                .findById(
                                        cancellation.id())
                                .orElseThrow());

        assertThat(persistedOriginal.amount().value())
                .isEqualByComparingTo(
                        "125.2500");

        assertThat(persistedOriginal.balanceEffect())
                .isEqualByComparingTo(
                        "-125.2500");

        assertThat(persistedOriginal.dataRevision())
                .isEqualTo(2L);

        assertThat(persistedCancellation.reversedEventId())
                .isEqualTo(
                        persistedOriginal.id());

        assertThat(persistedCancellation.amount().value())
                .isEqualByComparingTo(
                        "125.2500");

        assertThat(persistedCancellation.balanceEffect())
                .isEqualByComparingTo(
                        "125.2500");

        assertThat(persistedCancellation.dataRevision())
                .isEqualTo(2L);

        assertThat(persistedCancellation.updatedAt())
                .isEqualTo(REVISED_AT);

        assertThat(
                countRows(
                        "ocv.operational_event"))
                .isEqualTo(2L);

        assertThat(
                countRows(
                        "ocv.event_state_transition"))
                .isEqualTo(2L);
    }

    @Test
    void rollsBackRevisionWhenLaterTransitionPersistenceFails() {
        OperationalEvent originalEvent =
                originalExpense(
                        "80.0000");

        persistNewEvent(
                originalEvent,
                ORIGINAL_TRANSITION_ID);

        assertThatThrownBy(
                () -> transactionRunner.execute(
                        () -> {
                            OperationalEvent lockedEvent =
                                    revisionRepository
                                            .findByIdForUpdate(
                                            new OperationalCloseId(CLOSE_ID),
                                            originalEvent.id())
                                            .orElseThrow();

                            OperationalEvent revisedEvent =
                                    lockedEvent.reviseRegular(
                                            OperationalEventType.EXPENSE,
                                            new OperationalEventAmount(
                                                    new BigDecimal(
                                                            "200.0000")),
                                            REVISED_AT.minusSeconds(300),
                                            "Caja revisada",
                                            "Cambio que debe revertirse",
                                            false,
                                            false,
                                            REVISED_AT,
                                            actor());

                            revisionRepository.saveRevision(
                                    revisedEvent);

                            revisionRepository.appendStateTransition(
                                    new EventStateTransition(
                                            new EventStateTransitionId(
                                                    INVALID_TRANSITION_ID),
                                            new OperationalEventId(
                                                    MISSING_EVENT_ID),
                                            OperationalEventState.VALIDATED,
                                            OperationalEventState.REGISTERED,
                                            "EVENT_DATA_REVISED",
                                            null,
                                            null,
                                            REVISED_AT,
                                            actor()));
                        }))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        OperationalEvent persistedEvent =
                transactionRunner.execute(
                        () -> eventRepository
                                .findById(
                                        originalEvent.id())
                                .orElseThrow());

        assertThat(persistedEvent.amount().value())
                .isEqualByComparingTo(
                        "80.0000");

        assertThat(persistedEvent.balanceEffect())
                .isEqualByComparingTo(
                        "-80.0000");

        assertThat(persistedEvent.description())
                .isEqualTo(
                        "Gasto operativo inicial");

        assertThat(persistedEvent.dataRevision())
                .isEqualTo(1L);

        assertThat(
                countRows(
                        "ocv.event_state_transition"))
                .isEqualTo(1L);
    }

    private void persistNewEvent(
            OperationalEvent operationalEvent,
            UUID transitionId) {

        transactionRunner.execute(
                () -> eventRepository.saveNew(
                        operationalEvent,
                        initialTransition(
                                operationalEvent,
                                transitionId)));
    }

    private static OperationalEvent originalExpense(
            String amount) {

        return OperationalEvent.create(
                new OperationalEventId(
                        ORIGINAL_EVENT_ID),
                new OperationalCloseId(
                        CLOSE_ID),
                OperationalEventType.EXPENSE,
                new OperationalEventAmount(
                        new BigDecimal(amount)),
                CREATED_AT.plusSeconds(30),
                "Caja principal",
                "Gasto operativo inicial",
                false,
                false,
                CREATED_AT.plusSeconds(60),
                actor());
    }

    private static EventStateTransition initialTransition(
            OperationalEvent operationalEvent,
            UUID transitionId) {

        return new EventStateTransition(
                new EventStateTransitionId(
                        transitionId),
                operationalEvent.id(),
                null,
                OperationalEventState.REGISTERED,
                "EVENT_CREATED",
                null,
                null,
                operationalEvent.registeredAt(),
                actor());
    }

    private void insertOperationalClose() {
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
                Timestamp.from(
                        CREATED_AT),
                Timestamp.from(
                        CREATED_AT),
                Timestamp.from(
                        CREATED_AT));
    }

    private static AuditActor actor() {
        return new AuditActor(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
    }

    private Long countRows(
            String qualifiedTableName) {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM "
                        + qualifiedTableName,
                Long.class);
    }

    private void cleanOperationalCloseTables() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    ocv.event_state_transition,
                    ocv.operational_event,
                    ocv.close_state_transition,
                    ocv.operational_close
                """);
    }

}