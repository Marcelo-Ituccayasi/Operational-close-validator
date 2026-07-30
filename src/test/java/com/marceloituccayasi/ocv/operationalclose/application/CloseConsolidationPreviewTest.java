package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

class CloseConsolidationPreviewTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    uuid(
                            "0ae04ea5-d1be-4ec4-991a-780000000001"));

    private static final OperationalEventId INCOME_ID =
            eventId(
                    "0ae04ea5-d1be-4ec4-991a-780000000002");

    private static final OperationalEventId EXPENSE_ID =
            eventId(
                    "0ae04ea5-d1be-4ec4-991a-780000000003");

    private static final OperationalEventId DISCOUNT_ID =
            eventId(
                    "0ae04ea5-d1be-4ec4-991a-780000000004");

    private static final OperationalEventId CANCELLATION_ID =
            eventId(
                    "0ae04ea5-d1be-4ec4-991a-780000000005");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-30T12:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Test
    void calculatesPreliminaryTotalsAndExpectedBalance() {
        CloseConsolidationPreview preview =
                CloseConsolidationPreview.fromDomain(
                        close(),
                        events(),
                        CloseConsolidationReadiness.evaluated(
                                List.of(),
                                List.of(
                                        EXPENSE_ID),
                                List.of()));

        assertThat(
                preview.eventCount())
                .isEqualTo(
                        4);

        assertThat(
                preview.totalIncome())
                .isEqualByComparingTo(
                        decimal(
                                "200.0000"));

        assertThat(
                preview.totalExpense())
                .isEqualByComparingTo(
                        decimal(
                                "50.0000"));

        assertThat(
                preview.totalDiscount())
                .isEqualByComparingTo(
                        decimal(
                                "25.0000"));

        assertThat(
                preview.totalCancellation())
                .isEqualByComparingTo(
                        decimal(
                                "50.0000"));

        assertThat(
                preview.expectedBalance())
                .isEqualByComparingTo(
                        decimal(
                                "1175.0000"));

        assertThat(
                preview.ready())
                .isFalse();

        assertThat(
                preview.affectedEventIds())
                .containsExactly(
                        EXPENSE_ID.value());

        assertThat(
                preview.events())
                .extracting(
                        OperationalEventView::id)
                .containsExactly(
                        INCOME_ID.value(),
                        EXPENSE_ID.value(),
                        DISCOUNT_ID.value(),
                        CANCELLATION_ID.value());

        assertThatThrownBy(
                () -> preview.events()
                        .clear())
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    @Test
    void calculatesInitialBalanceForAnEmptyClose() {
        CloseConsolidationPreview preview =
                CloseConsolidationPreview.fromDomain(
                        close(),
                        List.of(),
                        CloseConsolidationReadiness.noEvents());

        assertThat(
                preview.eventsPresent())
                .isFalse();

        assertThat(
                preview.eventCount())
                .isZero();

        assertThat(
                preview.totalIncome())
                .isEqualByComparingTo(
                        decimal(
                                "0.0000"));

        assertThat(
                preview.totalExpense())
                .isEqualByComparingTo(
                        decimal(
                                "0.0000"));

        assertThat(
                preview.expectedBalance())
                .isEqualByComparingTo(
                        decimal(
                                "1000.0000"));

        assertThat(
                preview.ready())
                .isFalse();
    }

    private OperationalClose close() {
        return new OperationalClose(
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
                OperationalCloseState.PREPARATION,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

    private List<OperationalEvent> events() {
        return List.of(
                event(
                        INCOME_ID,
                        OperationalEventType.INCOME,
                        "200.0000",
                        "200.0000",
                        null),
                event(
                        EXPENSE_ID,
                        OperationalEventType.EXPENSE,
                        "50.0000",
                        "-50.0000",
                        null),
                event(
                        DISCOUNT_ID,
                        OperationalEventType.DISCOUNT,
                        "25.0000",
                        "-25.0000",
                        null),
                event(
                        CANCELLATION_ID,
                        OperationalEventType.CANCELLATION,
                        "50.0000",
                        "50.0000",
                        EXPENSE_ID));
    }

    private OperationalEvent event(
            OperationalEventId eventId,
            OperationalEventType eventType,
            String amount,
            String balanceEffect,
            OperationalEventId reversedEventId) {

        return new OperationalEvent(
                eventId,
                CLOSE_ID,
                eventType,
                new OperationalEventAmount(
                        decimal(
                                amount)),
                decimal(
                        balanceEffect),
                reversedEventId,
                CREATED_AT,
                CREATED_AT,
                "Caja principal",
                "Evento para vista preliminar",
                OperationalEventState.VALIDATED,
                false,
                false,
                3,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

    private static OperationalEventId eventId(
            String value) {

        return new OperationalEventId(
                uuid(
                        value));
    }

    private static UUID uuid(
            String value) {

        return UUID.fromString(
                value);
    }

    private static BigDecimal decimal(
            String value) {

        return new BigDecimal(
                value);
    }

}