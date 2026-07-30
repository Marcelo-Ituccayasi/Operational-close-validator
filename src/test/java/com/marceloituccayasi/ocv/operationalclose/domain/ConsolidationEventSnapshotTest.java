package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ConsolidationEventSnapshotTest {

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    UUID.fromString(
                            "5f15d31e-8f60-4f70-b7e1-710000000001"));

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "5f15d31e-8f60-4f70-b7e1-710000000002"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "5f15d31e-8f60-4f70-b7e1-710000000003"));

    private static final OperationalEventId REVERSED_EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "5f15d31e-8f60-4f70-b7e1-710000000004"));

    private static final Instant EVENT_AT =
            Instant.parse(
                    "2026-07-29T12:00:00Z");

    private static final Instant CAPTURED_AT =
            Instant.parse(
                    "2026-07-29T13:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Test
    void capturesTheExactValidatedEventRevision() {
        OperationalEvent event =
                event(
                        OperationalEventType.INCOME,
                        "125.5000",
                        "125.5000",
                        null,
                        OperationalEventState.VALIDATED);

        ConsolidationEventSnapshot snapshot =
                ConsolidationEventSnapshot.capture(
                        CONSOLIDATION_ID,
                        event,
                        CAPTURED_AT);

        assertThat(
                snapshot.consolidationId())
                .isEqualTo(
                        CONSOLIDATION_ID);

        assertThat(
                snapshot.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(
                snapshot.eventDataRevision())
                .isEqualTo(
                        4);

        assertThat(
                snapshot.eventType())
                .isEqualTo(
                        OperationalEventType.INCOME);

        assertThat(
                snapshot.amount().value())
                .isEqualByComparingTo(
                        new BigDecimal(
                                "125.5000"));

        assertThat(
                snapshot.balanceEffect())
                .isEqualByComparingTo(
                        new BigDecimal(
                                "125.5000"));

        assertThat(
                snapshot.eventState())
                .isEqualTo(
                        OperationalEventState.VALIDATED);

        assertThat(
                snapshot.capturedAt())
                .isEqualTo(
                        CAPTURED_AT);
    }

    @Test
    void rejectsAnEventThatIsNotValidated() {
        OperationalEvent event =
                event(
                        OperationalEventType.INCOME,
                        "125.5000",
                        "125.5000",
                        null,
                        OperationalEventState.OBSERVED);

        assertThatThrownBy(
                () -> ConsolidationEventSnapshot.capture(
                        CONSOLIDATION_ID,
                        event,
                        CAPTURED_AT))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "consolidation snapshot requires a validated event");
    }

    @Test
    void rejectsCancellationWithoutReversedEvent() {
        assertThatThrownBy(
                () -> new ConsolidationEventSnapshot(
                        CONSOLIDATION_ID,
                        EVENT_ID,
                        4,
                        OperationalEventType.CANCELLATION,
                        amount(
                                "50.0000"),
                        decimal(
                                "50.0000"),
                        null,
                        OperationalEventState.VALIDATED,
                        CAPTURED_AT))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "cancellation snapshot must reference "
                                + "a reversed event");
    }

    @Test
    void rejectsRegularEffectThatDoesNotMatchType() {
        assertThatThrownBy(
                () -> new ConsolidationEventSnapshot(
                        CONSOLIDATION_ID,
                        EVENT_ID,
                        4,
                        OperationalEventType.INCOME,
                        amount(
                                "50.0000"),
                        decimal(
                                "-50.0000"),
                        null,
                        OperationalEventState.VALIDATED,
                        CAPTURED_AT))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "balance effect does not match "
                                + "captured event type");
    }

    @Test
    void rejectsSignedValuesOutsideApprovedScale() {
        assertThatThrownBy(
                () -> new ConsolidationEventSnapshot(
                        CONSOLIDATION_ID,
                        EVENT_ID,
                        4,
                        OperationalEventType.CANCELLATION,
                        amount(
                                "1.0000"),
                        decimal(
                                "1.00000"),
                        REVERSED_EVENT_ID,
                        OperationalEventState.VALIDATED,
                        CAPTURED_AT))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "balance effect must not exceed "
                                + "four decimal places");
    }

    private OperationalEvent event(
            OperationalEventType eventType,
            String amount,
            String balanceEffect,
            OperationalEventId reversedEventId,
            OperationalEventState state) {

        return new OperationalEvent(
                EVENT_ID,
                CLOSE_ID,
                eventType,
                amount(
                        amount),
                decimal(
                        balanceEffect),
                reversedEventId,
                EVENT_AT,
                EVENT_AT,
                "Caja principal",
                "Evento capturado para consolidación",
                state,
                false,
                false,
                4,
                EVENT_AT,
                EVENT_AT,
                ACTOR,
                EVENT_AT,
                ACTOR);
    }

    private OperationalEventAmount amount(
            String value) {

        return new OperationalEventAmount(
                decimal(
                        value));
    }

    private BigDecimal decimal(
            String value) {

        return new BigDecimal(
                value);
    }

}