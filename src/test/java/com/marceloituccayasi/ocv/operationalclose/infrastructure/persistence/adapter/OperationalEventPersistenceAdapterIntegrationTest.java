package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import com.marceloituccayasi.ocv.operationalclose.application.DuplicateOperationalEventCancellationException;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransitionId;
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
class OperationalEventPersistenceAdapterIntegrationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "0fd48991-38c5-45da-9e7b-a4ca28100001");

    private static final UUID CLOSE_TRANSITION_ID =
            UUID.fromString(
                    "0fd48991-38c5-45da-9e7b-a4ca28100002");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "0fd48991-38c5-45da-9e7b-a4ca28100003");

    private static final UUID EVENT_TRANSITION_ID =
            UUID.fromString(
                    "0fd48991-38c5-45da-9e7b-a4ca28100004");

    private static final UUID UNKNOWN_EVENT_ID =
            UUID.fromString(
                    "0fd48991-38c5-45da-9e7b-a4ca28100005");

    private static final UUID FIRST_CANCELLATION_ID =
            UUID.fromString(
                    "0fd48991-38c5-45da-9e7b-a4ca28100006");

    private static final UUID FIRST_CANCELLATION_TRANSITION_ID =
            UUID.fromString(
                    "0fd48991-38c5-45da-9e7b-a4ca28100007");

    private static final UUID SECOND_CANCELLATION_ID =
            UUID.fromString(
                    "0fd48991-38c5-45da-9e7b-a4ca28100008");

    private static final UUID SECOND_CANCELLATION_TRANSITION_ID =
            UUID.fromString(
                    "0fd48991-38c5-45da-9e7b-a4ca28100009");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-22T08:00:00Z");

    private static final Instant OCCURRED_AT =
            Instant.parse(
                    "2026-07-22T07:30:00Z");

    @Autowired
    private OperationalCloseRepository closeRepository;

    @Autowired
    private OperationalEventRepository eventRepository;

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
    void persistsEventAndInitialTransitionInOneTransaction() {
        persistClose();

        OperationalEvent operationalEvent =
                regularEvent(
                        EVENT_ID,
                        OperationalEventType.INCOME,
                        "125.5000");

        EventStateTransition initialTransition =
                EventStateTransition.initial(
                        new EventStateTransitionId(
                                EVENT_TRANSITION_ID),
                        operationalEvent.id(),
                        NOW,
                        actor());

        transactionRunner.execute(
                (Runnable) () -> eventRepository.saveNew(
                        operationalEvent,
                        initialTransition));

        Long persistedEvents =
                countRows("ocv.operational_event");

        Long persistedTransitions =
                countRows("ocv.event_state_transition");

        String persistedType =
                jdbcTemplate.queryForObject(
                        """
                        SELECT event_type
                        FROM ocv.operational_event
                        WHERE id = ?
                        """,
                        String.class,
                        EVENT_ID);

        BigDecimal persistedEffect =
                jdbcTemplate.queryForObject(
                        """
                        SELECT balance_effect
                        FROM ocv.operational_event
                        WHERE id = ?
                        """,
                        BigDecimal.class,
                        EVENT_ID);

        String persistedState =
                jdbcTemplate.queryForObject(
                        """
                        SELECT state
                        FROM ocv.operational_event
                        WHERE id = ?
                        """,
                        String.class,
                        EVENT_ID);

        Long initialTransitions =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.event_state_transition
                        WHERE id = ?
                          AND event_id = ?
                          AND from_state IS NULL
                          AND to_state = 'REGISTERED'
                          AND cause_code = 'EVENT_CREATED'
                        """,
                        Long.class,
                        EVENT_TRANSITION_ID,
                        EVENT_ID);

        Optional<OperationalEvent> loadedEvent =
                eventRepository.findById(
                        new OperationalEventId(EVENT_ID));

        assertThat(persistedEvents)
                .isEqualTo(1L);

        assertThat(persistedTransitions)
                .isEqualTo(1L);

        assertThat(persistedType)
                .isEqualTo("INCOME");

        assertThat(persistedEffect)
                .isEqualByComparingTo("125.5000");

        assertThat(persistedState)
                .isEqualTo("REGISTERED");

        assertThat(initialTransitions)
                .isEqualTo(1L);

        assertThat(loadedEvent)
                .isPresent();

        assertThat(loadedEvent.orElseThrow().id().value())
                .isEqualTo(EVENT_ID);

        assertThat(loadedEvent.orElseThrow().closeId().value())
                .isEqualTo(CLOSE_ID);

        assertThat(loadedEvent.orElseThrow().eventType())
                .isEqualTo(OperationalEventType.INCOME);

        assertThat(loadedEvent.orElseThrow().amount().value())
                .isEqualByComparingTo("125.5000");

        assertThat(loadedEvent.orElseThrow().balanceEffect())
                .isEqualByComparingTo("125.5000");

        assertThat(loadedEvent.orElseThrow().state())
                .isEqualTo(OperationalEventState.REGISTERED);

        assertThat(loadedEvent.orElseThrow().dataRevision())
                .isEqualTo(1L);
    }

    @Test
    void rollsBackEventWhenInitialTransitionCannotBePersisted() {
        persistClose();

        OperationalEvent operationalEvent =
                regularEvent(
                        EVENT_ID,
                        OperationalEventType.EXPENSE,
                        "80.0000");

        EventStateTransition invalidTransition =
                EventStateTransition.initial(
                        new EventStateTransitionId(
                                EVENT_TRANSITION_ID),
                        new OperationalEventId(
                                UNKNOWN_EVENT_ID),
                        NOW,
                        actor());

        assertThatThrownBy(
                () -> transactionRunner.execute(
                        (Runnable) () -> eventRepository.saveNew(
                                operationalEvent,
                                invalidTransition)))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        assertThat(
                countRows("ocv.operational_event"))
                .isZero();

        assertThat(
                countRows("ocv.event_state_transition"))
                .isZero();

        assertThat(
                countRows("ocv.operational_close"))
                .isEqualTo(1L);
    }

    @Test
    void translatesDuplicateCancellationAndPreservesFirstOne() {
        persistClose();

        OperationalEvent originalEvent =
                regularEvent(
                        EVENT_ID,
                        OperationalEventType.EXPENSE,
                        "80.0000");

        EventStateTransition originalTransition =
                EventStateTransition.initial(
                        new EventStateTransitionId(
                                EVENT_TRANSITION_ID),
                        originalEvent.id(),
                        NOW,
                        actor());

        transactionRunner.execute(
                (Runnable) () -> eventRepository.saveNew(
                        originalEvent,
                        originalTransition));

        OperationalEvent firstCancellation =
                cancellation(
                        FIRST_CANCELLATION_ID,
                        originalEvent,
                        NOW.plusSeconds(1));

        EventStateTransition firstCancellationTransition =
                EventStateTransition.initial(
                        new EventStateTransitionId(
                                FIRST_CANCELLATION_TRANSITION_ID),
                        firstCancellation.id(),
                        NOW.plusSeconds(1),
                        actor());

        transactionRunner.execute(
                (Runnable) () -> eventRepository.saveNew(
                        firstCancellation,
                        firstCancellationTransition));

        OperationalEvent secondCancellation =
                cancellation(
                        SECOND_CANCELLATION_ID,
                        originalEvent,
                        NOW.plusSeconds(2));

        EventStateTransition secondCancellationTransition =
                EventStateTransition.initial(
                        new EventStateTransitionId(
                                SECOND_CANCELLATION_TRANSITION_ID),
                        secondCancellation.id(),
                        NOW.plusSeconds(2),
                        actor());

        assertThatThrownBy(
                () -> transactionRunner.execute(
                        (Runnable) () -> eventRepository.saveNew(
                                secondCancellation,
                                secondCancellationTransition)))
                .isInstanceOf(
                        DuplicateOperationalEventCancellationException.class);

        assertThat(
                countRows("ocv.operational_event"))
                .isEqualTo(2L);

        assertThat(
                countRows("ocv.event_state_transition"))
                .isEqualTo(2L);

        assertThat(
                eventRepository.existsCancellationFor(
                        originalEvent.id()))
                .isTrue();

        Long firstCancellationRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.operational_event
                        WHERE id = ?
                          AND event_type = 'CANCELLATION'
                          AND reversed_event_id = ?
                        """,
                        Long.class,
                        FIRST_CANCELLATION_ID,
                        EVENT_ID);

        Long secondCancellationRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.operational_event
                        WHERE id = ?
                        """,
                        Long.class,
                        SECOND_CANCELLATION_ID);

        assertThat(firstCancellationRows)
                .isEqualTo(1L);

        assertThat(secondCancellationRows)
                .isZero();
    }

    private void persistClose() {
        OperationalClose operationalClose =
                OperationalClose.create(
                        new OperationalCloseId(CLOSE_ID),
                        new OperationalPeriod(
                                LocalDate.of(2026, 7, 1),
                                LocalDate.of(2026, 7, 31)),
                        new CurrencyCode("PEN"),
                        new InitialBalance(
                                new BigDecimal("1000.0000")),
                        NOW,
                        actor());

        CloseStateTransition initialTransition =
                CloseStateTransition.initial(
                        new CloseStateTransitionId(
                                CLOSE_TRANSITION_ID),
                        operationalClose.id(),
                        NOW,
                        actor());

        transactionRunner.execute(
                (Runnable) () -> closeRepository.saveNew(
                        operationalClose,
                        initialTransition));
    }

    private static OperationalEvent regularEvent(
            UUID eventId,
            OperationalEventType eventType,
            String amount) {

        return OperationalEvent.create(
                new OperationalEventId(eventId),
                new OperationalCloseId(CLOSE_ID),
                eventType,
                new OperationalEventAmount(
                        new BigDecimal(amount)),
                OCCURRED_AT,
                "Caja principal",
                "Evento operativo de prueba",
                false,
                false,
                NOW,
                actor());
    }

    private static OperationalEvent cancellation(
            UUID cancellationId,
            OperationalEvent originalEvent,
            Instant registeredAt) {

        return OperationalEvent.createCancellation(
                new OperationalEventId(
                        cancellationId),
                new OperationalCloseId(
                        CLOSE_ID),
                originalEvent,
                OCCURRED_AT.plusSeconds(1),
                "Caja principal",
                "Anulación de evento operativo",
                false,
                false,
                registeredAt,
                actor());
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

    private Long countRows(
            String qualifiedTableName) {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM "
                        + qualifiedTableName,
                Long.class);
    }

    private static AuditActor actor() {
        return new AuditActor(
                "responsible-user",
                "responsible");
    }

}