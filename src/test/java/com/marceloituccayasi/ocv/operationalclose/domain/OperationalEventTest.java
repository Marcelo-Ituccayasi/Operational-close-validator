package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OperationalEventTest {

    private static final Instant NOW =
            Instant.parse("2026-07-21T10:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    "responsible-user",
                    "responsible");

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "8a0d3d9c-a2ad-4bdc-b8e5-4a6433c60001"));

    @Test
    void createsIncomeWithPositiveBalanceEffect() {
        OperationalEvent event =
                createRegularEvent(
                        OperationalEventType.INCOME);

        assertThat(event.balanceEffect())
                .isEqualByComparingTo("25.5000");
    }

    @Test
    void createsExpenseWithNegativeBalanceEffect() {
        OperationalEvent event =
                createRegularEvent(
                        OperationalEventType.EXPENSE);

        assertThat(event.balanceEffect())
                .isEqualByComparingTo("-25.5000");
    }

    @Test
    void createsDiscountWithNegativeBalanceEffect() {
        OperationalEvent event =
                createRegularEvent(
                        OperationalEventType.DISCOUNT);

        assertThat(event.balanceEffect())
                .isEqualByComparingTo("-25.5000");
    }

    @Test
    void createsEventRegisteredAtRevisionOneWithAuditMetadata() {
        OperationalEvent event =
                createRegularEvent(
                        OperationalEventType.INCOME);

        assertThat(event.state())
                .isEqualTo(
                        OperationalEventState.REGISTERED);

        assertThat(event.dataRevision())
                .isEqualTo(1);

        assertThat(event.createdAt())
                .isEqualTo(NOW);

        assertThat(event.updatedAt())
                .isEqualTo(NOW);

        assertThat(event.createdBy())
                .isEqualTo(ACTOR);

        assertThat(event.updatedBy())
                .isEqualTo(ACTOR);
    }

    @Test
    void createsCancellationWithOriginalAmountAndInverseEffect() {
        OperationalEvent original =
                createRegularEvent(
                        OperationalEventType.EXPENSE);

        OperationalEvent cancellation =
                OperationalEvent.createCancellation(
                        eventId(
                                "8a0d3d9c-a2ad-4bdc-b8e5-4a6433c60003"),
                        CLOSE_ID,
                        original,
                        NOW.plusSeconds(60),
                        "Caja",
                        "Anulación del egreso",
                        false,
                        true,
                        NOW.plusSeconds(60),
                        ACTOR);

        assertThat(cancellation.amount())
                .isEqualTo(original.amount());

        assertThat(cancellation.balanceEffect())
                .isEqualByComparingTo("25.5000");

        assertThat(cancellation.reversedEventId())
                .isEqualTo(original.id());

        assertThat(cancellation.eventType())
                .isEqualTo(
                        OperationalEventType.CANCELLATION);
    }

    @Test
    void rejectsCancellationWithoutReferencedEvent() {
        assertThatThrownBy(
                () -> OperationalEvent.create(
                        eventId(
                                "8a0d3d9c-a2ad-4bdc-b8e5-4a6433c60004"),
                        CLOSE_ID,
                        OperationalEventType.CANCELLATION,
                        amount(),
                        NOW,
                        "Caja",
                        "Anulación",
                        false,
                        true,
                        NOW,
                        ACTOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "referenced event");
    }

    @Test
    void rejectsSelfReferencingCancellation() {
        OperationalEvent original =
                createRegularEvent(
                        OperationalEventType.INCOME);

        assertThatThrownBy(
                () -> OperationalEvent.createCancellation(
                        original.id(),
                        CLOSE_ID,
                        original,
                        NOW.plusSeconds(60),
                        "Caja",
                        "Anulación",
                        false,
                        true,
                        NOW.plusSeconds(60),
                        ACTOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itself");
    }

    @Test
    void rejectsCancellationAcrossCloses() {
        OperationalEvent original =
                createRegularEvent(
                        OperationalEventType.INCOME);

        OperationalCloseId otherCloseId =
                new OperationalCloseId(
                        UUID.fromString(
                                "8a0d3d9c-a2ad-4bdc-b8e5-4a6433c60005"));

        assertThatThrownBy(
                () -> OperationalEvent.createCancellation(
                        eventId(
                                "8a0d3d9c-a2ad-4bdc-b8e5-4a6433c60006"),
                        otherCloseId,
                        original,
                        NOW.plusSeconds(60),
                        "Caja",
                        "Anulación",
                        false,
                        true,
                        NOW.plusSeconds(60),
                        ACTOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same close");
    }

    @Test
    void rejectsCancellationOfCancellation() {
        OperationalEvent original =
                createRegularEvent(
                        OperationalEventType.INCOME);

        OperationalEvent firstCancellation =
                OperationalEvent.createCancellation(
                        eventId(
                                "8a0d3d9c-a2ad-4bdc-b8e5-4a6433c60007"),
                        CLOSE_ID,
                        original,
                        NOW.plusSeconds(60),
                        "Caja",
                        "Primera anulación",
                        false,
                        true,
                        NOW.plusSeconds(60),
                        ACTOR);

        assertThatThrownBy(
                () -> OperationalEvent.createCancellation(
                        eventId(
                                "8a0d3d9c-a2ad-4bdc-b8e5-4a6433c60008"),
                        CLOSE_ID,
                        firstCancellation,
                        NOW.plusSeconds(120),
                        "Caja",
                        "Segunda anulación",
                        false,
                        true,
                        NOW.plusSeconds(120),
                        ACTOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "another cancellation");
    }

    @Test
    void rejectsBlankResponsibleName() {
        assertThatThrownBy(
                () -> OperationalEvent.create(
                        eventId(
                                "8a0d3d9c-a2ad-4bdc-b8e5-4a6433c60009"),
                        CLOSE_ID,
                        OperationalEventType.INCOME,
                        amount(),
                        NOW,
                        " ",
                        "Ingreso",
                        true,
                        false,
                        NOW,
                        ACTOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "responsibleName");
    }

    @Test
    void rejectsBlankDescription() {
        assertThatThrownBy(
                () -> OperationalEvent.create(
                        eventId(
                                "8a0d3d9c-a2ad-4bdc-b8e5-4a6433c60010"),
                        CLOSE_ID,
                        OperationalEventType.INCOME,
                        amount(),
                        NOW,
                        "Caja",
                        " ",
                        true,
                        false,
                        NOW,
                        ACTOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description");
    }

    private static OperationalEvent createRegularEvent(
            OperationalEventType eventType) {

        return OperationalEvent.create(
                eventId(
                        "8a0d3d9c-a2ad-4bdc-b8e5-4a6433c60002"),
                CLOSE_ID,
                eventType,
                amount(),
                NOW,
                "Caja",
                "Movimiento operativo",
                eventType == OperationalEventType.INCOME,
                eventType == OperationalEventType.DISCOUNT,
                NOW,
                ACTOR);
    }

    private static OperationalEventAmount amount() {
        return new OperationalEventAmount(
                new BigDecimal("25.5000"));
    }

    private static OperationalEventId eventId(
            String value) {

        return new OperationalEventId(
                UUID.fromString(value));
    }

}