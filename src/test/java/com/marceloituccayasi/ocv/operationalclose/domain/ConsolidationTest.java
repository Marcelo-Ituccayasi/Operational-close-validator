package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ConsolidationTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "1d3a84d8-b4d9-4d17-a228-720000000001"));

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    UUID.fromString(
                            "1d3a84d8-b4d9-4d17-a228-720000000002"));

    private static final OperationalEventId INCOME_ID =
            eventId(
                    "1d3a84d8-b4d9-4d17-a228-720000000003");

    private static final OperationalEventId EXPENSE_ID =
            eventId(
                    "1d3a84d8-b4d9-4d17-a228-720000000004");

    private static final OperationalEventId DISCOUNT_ID =
            eventId(
                    "1d3a84d8-b4d9-4d17-a228-720000000005");

    private static final OperationalEventId CANCELLATION_ID =
            eventId(
                    "1d3a84d8-b4d9-4d17-a228-720000000006");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-29T09:00:00Z");

    private static final Instant EVENT_AT =
            Instant.parse(
                    "2026-07-29T10:00:00Z");

    private static final Instant COMPLETED_AT =
            Instant.parse(
                    "2026-07-29T11:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Test
    void completesExactTotalsBalancesAndSnapshots() {
        Consolidation consolidation =
                completeConsolidation(
                        decimal(
                                "1170.0000"));

        assertThat(
                consolidation.closeId())
                .isEqualTo(
                        CLOSE_ID);

        assertThat(
                consolidation.currencyCode())
                .isEqualTo(
                        new CurrencyCode(
                                "PEN"));

        assertThat(
                consolidation.eventCount())
                .isEqualTo(
                        4);

        assertThat(
                consolidation.totalIncome())
                .isEqualByComparingTo(
                        decimal(
                                "200.0000"));

        assertThat(
                consolidation.totalExpense())
                .isEqualByComparingTo(
                        decimal(
                                "50.0000"));

        assertThat(
                consolidation.totalDiscount())
                .isEqualByComparingTo(
                        decimal(
                                "25.0000"));

        assertThat(
                consolidation.totalCancellation())
                .isEqualByComparingTo(
                        decimal(
                                "50.0000"));

        assertThat(
                consolidation.expectedBalance())
                .isEqualByComparingTo(
                        decimal(
                                "1175.0000"));

        assertThat(
                consolidation.actualBalance())
                .isEqualByComparingTo(
                        decimal(
                                "1170.0000"));

        assertThat(
                consolidation.difference())
                .isEqualByComparingTo(
                        decimal(
                                "-5.0000"));

        assertThat(
                consolidation.current())
                .isTrue();

        assertThat(
                consolidation.balanced())
                .isFalse();

        assertThat(
                consolidation.eventSnapshots())
                .extracting(
                        snapshot ->
                                snapshot.eventId())
                .containsExactly(
                        INCOME_ID,
                        EXPENSE_ID,
                        DISCOUNT_ID,
                        CANCELLATION_ID);

        assertThat(
                consolidation.eventSnapshots())
                .allSatisfy(
                        snapshot -> {
                            assertThat(
                                    snapshot.consolidationId())
                                    .isEqualTo(
                                            CONSOLIDATION_ID);

                            assertThat(
                                    snapshot.eventState())
                                    .isEqualTo(
                                            OperationalEventState.VALIDATED);

                            assertThat(
                                    snapshot.eventDataRevision())
                                    .isEqualTo(
                                            3);
                        });

        assertThatThrownBy(
                () -> consolidation
                        .eventSnapshots()
                        .clear())
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    @Test
    void reportsBalancedWhenDifferenceIsZero() {
        Consolidation consolidation =
                completeConsolidation(
                        decimal(
                                "1175.0000"));

        assertThat(
                consolidation.difference())
                .isEqualByComparingTo(
                        BigDecimal.ZERO);

        assertThat(
                consolidation.balanced())
                .isTrue();
    }

    @Test
    void rejectsAnEmptyEventCollection() {
        assertThatThrownBy(
                () -> Consolidation.complete(
                        CONSOLIDATION_ID,
                        close(),
                        List.of(),
                        decimal(
                                "1000.0000"),
                        COMPLETED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "consolidation requires at least one event");
    }

    @Test
    void rejectsAnEventFromAnotherClose() {
        OperationalEvent event =
                event(
                        INCOME_ID,
                        new OperationalCloseId(
                                UUID.fromString(
                                        "1d3a84d8-b4d9-4d17-a228-729999999999")),
                        OperationalEventType.INCOME,
                        "200.0000",
                        "200.0000",
                        null,
                        OperationalEventState.VALIDATED);

        assertThatThrownBy(
                () -> Consolidation.complete(
                        CONSOLIDATION_ID,
                        close(),
                        List.of(
                                event),
                        decimal(
                                "1200.0000"),
                        COMPLETED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "all events must belong to the consolidated close");
    }

    @Test
    void rejectsAnEventThatIsNotValidated() {
        OperationalEvent event =
                event(
                        INCOME_ID,
                        CLOSE_ID,
                        OperationalEventType.INCOME,
                        "200.0000",
                        "200.0000",
                        null,
                        OperationalEventState.OBSERVED);

        assertThatThrownBy(
                () -> Consolidation.complete(
                        CONSOLIDATION_ID,
                        close(),
                        List.of(
                                event),
                        decimal(
                                "1200.0000"),
                        COMPLETED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "all consolidated events must be validated");
    }

    @Test
    void rejectsDuplicateEvents() {
        OperationalEvent event =
                income();

        assertThatThrownBy(
                () -> Consolidation.complete(
                        CONSOLIDATION_ID,
                        close(),
                        List.of(
                                event,
                                event),
                        decimal(
                                "1400.0000"),
                        COMPLETED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "consolidation must not contain duplicate events");
    }

    @Test
    void rejectsInconsistentPersistedCalculation() {
        Consolidation valid =
                completeConsolidation(
                        decimal(
                                "1175.0000"));

        assertThatThrownBy(
                () -> new Consolidation(
                        valid.id(),
                        valid.closeId(),
                        valid.currencyCode(),
                        valid.eventCount(),
                        valid.totalIncome().add(
                                BigDecimal.ONE),
                        valid.totalExpense(),
                        valid.totalDiscount(),
                        valid.totalCancellation(),
                        valid.initialBalance(),
                        valid.expectedBalance(),
                        valid.actualBalance(),
                        valid.difference(),
                        valid.current(),
                        valid.completedAt(),
                        valid.completedBy(),
                        valid.invalidatedAt(),
                        valid.invalidationReason(),
                        valid.eventSnapshots()))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "total income does not match snapshot calculation");
    }

    @Test
    void invalidatesOnceWithoutRewritingCalculation() {
        Consolidation current =
                completeConsolidation(
                        decimal(
                                "1175.0000"));

        Instant invalidatedAt =
                COMPLETED_AT.plusSeconds(
                        60);

        Consolidation invalidated =
                current.invalidate(
                        invalidatedAt,
                        "Operational Event data revision changed.");

        assertThat(
                current.current())
                .isTrue();

        assertThat(
                invalidated.current())
                .isFalse();

        assertThat(
                invalidated.invalidatedAt())
                .isEqualTo(
                        invalidatedAt);

        assertThat(
                invalidated.invalidationReason())
                .isEqualTo(
                        "Operational Event data revision changed.");

        assertThat(
                invalidated.eventSnapshots())
                .isEqualTo(
                        current.eventSnapshots());

        assertThat(
                invalidated.expectedBalance())
                .isEqualByComparingTo(
                        current.expectedBalance());

        assertThatThrownBy(
                () -> invalidated.invalidate(
                        invalidatedAt.plusSeconds(
                                60),
                        "Second invalidation."))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "consolidation is already invalidated");
    }

    @Test
    void rejectsInvalidActualBalance() {
        assertThatThrownBy(
                () -> Consolidation.complete(
                        CONSOLIDATION_ID,
                        close(),
                        List.of(
                                income()),
                        decimal(
                                "-1.0000"),
                        COMPLETED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "actual balance must not be negative");

        assertThatThrownBy(
                () -> Consolidation.complete(
                        CONSOLIDATION_ID,
                        close(),
                        List.of(
                                income()),
                        decimal(
                                "1200.00001"),
                        COMPLETED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "actual balance must not exceed "
                                + "four decimal places");
    }

    @Test
    void rejectsNullConsolidationIdentifier() {
        assertThatThrownBy(
                () -> new ConsolidationId(
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "consolidation id must not be null");
    }

    private Consolidation completeConsolidation(
            BigDecimal actualBalance) {

        return Consolidation.complete(
                CONSOLIDATION_ID,
                close(),
                List.of(
                        income(),
                        expense(),
                        discount(),
                        cancellation()),
                actualBalance,
                COMPLETED_AT,
                ACTOR);
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
                CLOSE_ID,
                OperationalEventType.INCOME,
                "200.0000",
                "200.0000",
                null,
                OperationalEventState.VALIDATED);
    }

    private OperationalEvent expense() {
        return event(
                EXPENSE_ID,
                CLOSE_ID,
                OperationalEventType.EXPENSE,
                "50.0000",
                "-50.0000",
                null,
                OperationalEventState.VALIDATED);
    }

    private OperationalEvent discount() {
        return event(
                DISCOUNT_ID,
                CLOSE_ID,
                OperationalEventType.DISCOUNT,
                "25.0000",
                "-25.0000",
                null,
                OperationalEventState.VALIDATED);
    }

    private OperationalEvent cancellation() {
        return event(
                CANCELLATION_ID,
                CLOSE_ID,
                OperationalEventType.CANCELLATION,
                "50.0000",
                "50.0000",
                EXPENSE_ID,
                OperationalEventState.VALIDATED);
    }

    private OperationalEvent event(
            OperationalEventId eventId,
            OperationalCloseId closeId,
            OperationalEventType eventType,
            String amount,
            String balanceEffect,
            OperationalEventId reversedEventId,
            OperationalEventState state) {

        return new OperationalEvent(
                eventId,
                closeId,
                eventType,
                new OperationalEventAmount(
                        decimal(
                                amount)),
                decimal(
                        balanceEffect),
                reversedEventId,
                EVENT_AT,
                EVENT_AT,
                "Caja principal",
                "Evento para prueba de consolidación",
                state,
                false,
                false,
                3,
                EVENT_AT,
                EVENT_AT,
                ACTOR,
                EVENT_AT,
                ACTOR);
    }

    private static OperationalEventId eventId(
            String value) {

        return new OperationalEventId(
                UUID.fromString(
                        value));
    }

    private BigDecimal decimal(
            String value) {

        return new BigDecimal(
                value);
    }

}