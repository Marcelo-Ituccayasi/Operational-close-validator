package com.marceloituccayasi.ocv.operationalclose.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable snapshot of the exact Operational Event revision used by a
 * consolidation.
 *
 * @param consolidationId owner consolidation
 * @param eventId captured Operational Event
 * @param eventDataRevision captured relevant-data revision
 * @param eventType captured Event type
 * @param amount captured positive nominal amount
 * @param balanceEffect captured signed balance effect
 * @param reversedEventId captured reversed Event for a cancellation
 * @param eventState captured Event state
 * @param capturedAt snapshot instant
 */
public record ConsolidationEventSnapshot(
        ConsolidationId consolidationId,
        OperationalEventId eventId,
        long eventDataRevision,
        OperationalEventType eventType,
        OperationalEventAmount amount,
        BigDecimal balanceEffect,
        OperationalEventId reversedEventId,
        OperationalEventState eventState,
        Instant capturedAt) {

    private static final int MAXIMUM_SCALE = 4;
    private static final int MAXIMUM_INTEGER_DIGITS = 15;

    public ConsolidationEventSnapshot {
        requireNonNull(
                consolidationId,
                "consolidationId");

        requireNonNull(
                eventId,
                "eventId");

        requireNonNull(
                eventType,
                "eventType");

        requireNonNull(
                amount,
                "amount");

        requireNonNull(
                balanceEffect,
                "balanceEffect");

        requireNonNull(
                eventState,
                "eventState");

        requireNonNull(
                capturedAt,
                "capturedAt");

        if (eventDataRevision < 1) {
            throw new IllegalArgumentException(
                    "event data revision must be at least one");
        }

        validateSignedAmount(
                balanceEffect);

        if (balanceEffect.abs()
                .compareTo(
                        amount.value()) != 0) {

            throw new IllegalArgumentException(
                    "absolute balance effect must equal nominal amount");
        }

        if (eventState
                != OperationalEventState.VALIDATED) {

            throw new IllegalArgumentException(
                    "consolidation snapshot requires a validated event");
        }

        if (eventType
                == OperationalEventType.CANCELLATION
                && reversedEventId == null) {

            throw new IllegalArgumentException(
                    "cancellation snapshot must reference a reversed event");
        }

        if (eventType
                != OperationalEventType.CANCELLATION
                && reversedEventId != null) {

            throw new IllegalArgumentException(
                    "only a cancellation snapshot may reference "
                            + "a reversed event");
        }

        if (eventId.equals(
                reversedEventId)) {

            throw new IllegalArgumentException(
                    "cancellation snapshot must not reference itself");
        }

        validateRegularBalanceEffect(
                eventType,
                amount,
                balanceEffect);
    }

    public static ConsolidationEventSnapshot capture(
            ConsolidationId consolidationId,
            OperationalEvent event,
            Instant capturedAt) {

        requireNonNull(
                event,
                "event");

        return new ConsolidationEventSnapshot(
                consolidationId,
                event.id(),
                event.dataRevision(),
                event.eventType(),
                event.amount(),
                event.balanceEffect(),
                event.reversedEventId(),
                event.state(),
                capturedAt);
    }

    private static void validateRegularBalanceEffect(
            OperationalEventType eventType,
            OperationalEventAmount amount,
            BigDecimal balanceEffect) {

        BigDecimal expectedEffect =
                switch (eventType) {
                    case INCOME ->
                            amount.value();

                    case EXPENSE, DISCOUNT ->
                            amount.value().negate();

                    case CANCELLATION ->
                            null;
                };

        if (expectedEffect != null
                && balanceEffect.compareTo(
                        expectedEffect) != 0) {

            throw new IllegalArgumentException(
                    "balance effect does not match captured event type");
        }
    }

    private static void validateSignedAmount(
            BigDecimal value) {

        if (value.scale() > MAXIMUM_SCALE) {
            throw new IllegalArgumentException(
                    "balance effect must not exceed four decimal places");
        }

        int integerDigits =
                Math.max(
                        0,
                        value.precision()
                                - value.scale());

        if (integerDigits
                > MAXIMUM_INTEGER_DIGITS) {

            throw new IllegalArgumentException(
                    "balance effect exceeds numeric(19,4)");
        }
    }

    private static void requireNonNull(
            Object value,
            String fieldName) {

        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null");
        }
    }

}