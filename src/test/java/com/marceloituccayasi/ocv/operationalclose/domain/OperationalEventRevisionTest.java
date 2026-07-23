package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OperationalEventRevisionTest {

    private static final Instant CREATED_AT =
            Instant.parse("2026-07-21T10:00:00Z");

    private static final Instant REVISED_AT =
            Instant.parse("2026-07-21T11:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    "responsible-user",
                    "responsible");

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "7214d248-0ff5-4c8e-9250-4f8932870001"));

    @Test
    void revisesRegularEventAndRecalculatesBalanceEffect() {
        OperationalEvent income =
                regularEvent(
                        eventId(
                                "7214d248-0ff5-4c8e-9250-4f8932870002"),
                        OperationalEventType.INCOME,
                        "25.5000");

        OperationalEvent revised =
                income.reviseRegular(
                        OperationalEventType.EXPENSE,
                        amount("40.0000"),
                        REVISED_AT,
                        "Administración",
                        "Egreso corregido",
                        true,
                        false,
                        REVISED_AT,
                        ACTOR);

        assertThat(revised.eventType())
                .isEqualTo(
                        OperationalEventType.EXPENSE);

        assertThat(revised.amount().value())
                .isEqualByComparingTo("40.0000");

        assertThat(revised.balanceEffect())
                .isEqualByComparingTo("-40.0000");

        assertThat(revised.reversedEventId())
                .isNull();

        assertThat(revised.dataRevision())
                .isEqualTo(2);

        assertThat(revised.updatedAt())
                .isEqualTo(REVISED_AT);
    }

    @Test
    void permitsRevisionFromRegularEventToCoherentCancellation() {
        OperationalEvent editable =
                regularEvent(
                        eventId(
                                "7214d248-0ff5-4c8e-9250-4f8932870003"),
                        OperationalEventType.INCOME,
                        "10.0000");

        OperationalEvent original =
                regularEvent(
                        eventId(
                                "7214d248-0ff5-4c8e-9250-4f8932870004"),
                        OperationalEventType.DISCOUNT,
                        "12.0000");

        OperationalEvent revised =
                editable.reviseCancellation(
                        original,
                        REVISED_AT,
                        "Administración",
                        "Conversión coherente a anulación",
                        false,
                        true,
                        REVISED_AT,
                        ACTOR);

        assertThat(revised.eventType())
                .isEqualTo(
                        OperationalEventType.CANCELLATION);

        assertThat(revised.amount())
                .isEqualTo(original.amount());

        assertThat(revised.balanceEffect())
                .isEqualByComparingTo("12.0000");

        assertThat(revised.reversedEventId())
                .isEqualTo(original.id());

        assertThat(revised.dataRevision())
                .isEqualTo(2);
    }

    @Test
    void permitsRevisionFromCancellationToRegularEvent() {
        OperationalEvent original =
                regularEvent(
                        eventId(
                                "7214d248-0ff5-4c8e-9250-4f8932870005"),
                        OperationalEventType.EXPENSE,
                        "15.0000");

        OperationalEvent cancellation =
                cancellation(
                        eventId(
                                "7214d248-0ff5-4c8e-9250-4f8932870006"),
                        original);

        OperationalEvent revised =
                cancellation.reviseRegular(
                        OperationalEventType.INCOME,
                        amount("18.0000"),
                        REVISED_AT,
                        "Caja",
                        "Ingreso corregido",
                        true,
                        false,
                        REVISED_AT,
                        ACTOR);

        assertThat(revised.eventType())
                .isEqualTo(
                        OperationalEventType.INCOME);

        assertThat(revised.reversedEventId())
                .isNull();

        assertThat(revised.balanceEffect())
                .isEqualByComparingTo("18.0000");

        assertThat(revised.dataRevision())
                .isEqualTo(2);
    }

    @Test
    void recalculatesCancellationAfterOriginalRevision() {
        OperationalEvent original =
                regularEvent(
                        eventId(
                                "7214d248-0ff5-4c8e-9250-4f8932870007"),
                        OperationalEventType.EXPENSE,
                        "25.5000");

        OperationalEvent cancellation =
                cancellation(
                        eventId(
                                "7214d248-0ff5-4c8e-9250-4f8932870008"),
                        original);

        OperationalEvent revisedOriginal =
                original.reviseRegular(
                        OperationalEventType.INCOME,
                        amount("40.0000"),
                        REVISED_AT,
                        "Caja",
                        "Ingreso corregido",
                        true,
                        false,
                        REVISED_AT,
                        ACTOR);

        OperationalEvent recalculated =
                cancellation.recalculateFromRevisedOriginal(
                        revisedOriginal,
                        REVISED_AT,
                        ACTOR);

        assertThat(recalculated.amount())
                .isEqualTo(revisedOriginal.amount());

        assertThat(recalculated.balanceEffect())
                .isEqualByComparingTo("-40.0000");

        assertThat(recalculated.dataRevision())
                .isEqualTo(2);

        assertThat(recalculated.updatedAt())
                .isEqualTo(REVISED_AT);
    }

    @Test
    void rejectsRecalculationOfRegularEvent() {
        OperationalEvent regular =
                regularEvent(
                        eventId(
                                "7214d248-0ff5-4c8e-9250-4f8932870009"),
                        OperationalEventType.INCOME,
                        "10.0000");

        assertThatThrownBy(
                () -> regular.recalculateFromRevisedOriginal(
                        regular,
                        REVISED_AT,
                        ACTOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "only a cancellation");
    }

    @Test
    void rejectsRecalculationFromDifferentOriginal() {
        OperationalEvent original =
                regularEvent(
                        eventId(
                                "7214d248-0ff5-4c8e-9250-4f8932870010"),
                        OperationalEventType.INCOME,
                        "10.0000");

        OperationalEvent anotherOriginal =
                regularEvent(
                        eventId(
                                "7214d248-0ff5-4c8e-9250-4f8932870011"),
                        OperationalEventType.INCOME,
                        "10.0000");

        OperationalEvent cancellation =
                cancellation(
                        eventId(
                                "7214d248-0ff5-4c8e-9250-4f8932870012"),
                        original);

        assertThatThrownBy(
                () -> cancellation.recalculateFromRevisedOriginal(
                        anotherOriginal,
                        REVISED_AT,
                        ACTOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "does not match");
    }

    @Test
    void rejectsRevisionBeforePreviousUpdate() {
        OperationalEvent event =
                regularEvent(
                        eventId(
                                "7214d248-0ff5-4c8e-9250-4f8932870013"),
                        OperationalEventType.INCOME,
                        "10.0000");

        assertThatThrownBy(
                () -> event.reviseRegular(
                        OperationalEventType.INCOME,
                        amount("12.0000"),
                        CREATED_AT.minusSeconds(1),
                        "Caja",
                        "Ingreso corregido",
                        true,
                        false,
                        CREATED_AT.minusSeconds(1),
                        ACTOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "previous update");
    }

    private static OperationalEvent regularEvent(
            OperationalEventId id,
            OperationalEventType type,
            String amount) {

        return OperationalEvent.create(
                id,
                CLOSE_ID,
                type,
                amount(amount),
                CREATED_AT,
                "Caja",
                "Movimiento operativo",
                type == OperationalEventType.INCOME,
                type == OperationalEventType.DISCOUNT,
                CREATED_AT,
                ACTOR);
    }

    private static OperationalEvent cancellation(
            OperationalEventId id,
            OperationalEvent original) {

        return OperationalEvent.createCancellation(
                id,
                CLOSE_ID,
                original,
                CREATED_AT,
                "Caja",
                "Anulación",
                false,
                true,
                CREATED_AT,
                ACTOR);
    }

    private static OperationalEventAmount amount(
            String value) {

        return new OperationalEventAmount(
                new BigDecimal(value));
    }

    private static OperationalEventId eventId(
            String value) {

        return new OperationalEventId(
                UUID.fromString(value));
    }

}
