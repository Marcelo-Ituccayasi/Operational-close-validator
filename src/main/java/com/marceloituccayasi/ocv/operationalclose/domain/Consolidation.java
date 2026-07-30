package com.marceloituccayasi.ocv.operationalclose.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable historical consolidation of an Operational Close.
 *
 * @param id stable consolidation identifier
 * @param closeId owner Operational Close
 * @param currencyCode captured Close currency
 * @param eventCount number of included Events
 * @param totalIncome nominal Income total
 * @param totalExpense nominal Expense total
 * @param totalDiscount nominal Discount total
 * @param totalCancellation nominal Cancellation total
 * @param initialBalance captured initial balance
 * @param expectedBalance calculated expected balance
 * @param actualBalance informed actual balance
 * @param difference actual balance minus expected balance
 * @param current whether this is the current Close consolidation
 * @param completedAt completion instant
 * @param completedBy completion actor
 * @param invalidatedAt later invalidation instant
 * @param invalidationReason later invalidation reason
 * @param eventSnapshots exact Event revisions used by the calculation
 */
public record Consolidation(
        ConsolidationId id,
        OperationalCloseId closeId,
        CurrencyCode currencyCode,
        int eventCount,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal totalDiscount,
        BigDecimal totalCancellation,
        InitialBalance initialBalance,
        BigDecimal expectedBalance,
        BigDecimal actualBalance,
        BigDecimal difference,
        boolean current,
        Instant completedAt,
        AuditActor completedBy,
        Instant invalidatedAt,
        String invalidationReason,
        List<ConsolidationEventSnapshot> eventSnapshots) {

    private static final int MAXIMUM_SCALE = 4;
    private static final int MAXIMUM_INTEGER_DIGITS = 15;

    private static final BigDecimal ZERO_MONEY =
            new BigDecimal(
                    "0.0000");

    public Consolidation {
        requireNonNull(
                id,
                "id");

        requireNonNull(
                closeId,
                "closeId");

        requireNonNull(
                currencyCode,
                "currencyCode");

        requireNonNull(
                initialBalance,
                "initialBalance");

        requireNonNull(
                completedAt,
                "completedAt");

        requireNonNull(
                completedBy,
                "completedBy");

        requireNonNull(
                eventSnapshots,
                "eventSnapshots");

        requireMonetaryValue(
                totalIncome,
                "total income");

        requireMonetaryValue(
                totalExpense,
                "total expense");

        requireMonetaryValue(
                totalDiscount,
                "total discount");

        requireMonetaryValue(
                totalCancellation,
                "total cancellation");

        requireMonetaryValue(
                expectedBalance,
                "expected balance");

        requireMonetaryValue(
                actualBalance,
                "actual balance");

        requireMonetaryValue(
                difference,
                "difference");

        if (eventCount < 1) {
            throw new IllegalArgumentException(
                    "consolidation requires at least one event");
        }

        if (totalIncome.signum() < 0
                || totalExpense.signum() < 0
                || totalDiscount.signum() < 0
                || totalCancellation.signum() < 0) {

            throw new IllegalArgumentException(
                    "consolidation nominal totals must not be negative");
        }

        if (actualBalance.signum() < 0) {
            throw new IllegalArgumentException(
                    "actual balance must not be negative");
        }

        for (ConsolidationEventSnapshot snapshot
                : eventSnapshots) {

            requireNonNull(
                    snapshot,
                    "event snapshot");
        }

        eventSnapshots =
                List.copyOf(
                        eventSnapshots);

        if (eventCount
                != eventSnapshots.size()) {

            throw new IllegalArgumentException(
                    "event count must match snapshot count");
        }

        validateSnapshotOwnershipAndUniqueness(
                id,
                eventSnapshots);

        CalculatedAmounts calculatedAmounts =
                calculateAmounts(
                        eventSnapshots);

        requireEqual(
                totalIncome,
                calculatedAmounts.totalIncome(),
                "total income");

        requireEqual(
                totalExpense,
                calculatedAmounts.totalExpense(),
                "total expense");

        requireEqual(
                totalDiscount,
                calculatedAmounts.totalDiscount(),
                "total discount");

        requireEqual(
                totalCancellation,
                calculatedAmounts.totalCancellation(),
                "total cancellation");

        BigDecimal calculatedExpectedBalance =
                initialBalance.value().add(
                        calculatedAmounts.balanceEffect());

        requireMonetaryValue(
                calculatedExpectedBalance,
                "calculated expected balance");

        requireEqual(
                expectedBalance,
                calculatedExpectedBalance,
                "expected balance");

        BigDecimal calculatedDifference =
                actualBalance.subtract(
                        expectedBalance);

        requireMonetaryValue(
                calculatedDifference,
                "calculated difference");

        requireEqual(
                difference,
                calculatedDifference,
                "difference");

        if (invalidationReason != null) {
            if (invalidationReason.isBlank()) {
                throw new IllegalArgumentException(
                        "invalidation reason must not be blank");
            }

            invalidationReason =
                    invalidationReason.trim();
        }

        validateValidityMetadata(
                current,
                completedAt,
                invalidatedAt,
                invalidationReason);
    }

    public static Consolidation complete(
            ConsolidationId id,
            OperationalClose close,
            List<OperationalEvent> events,
            BigDecimal actualBalance,
            Instant completedAt,
            AuditActor actor) {

        requireNonNull(
                id,
                "id");

        requireNonNull(
                close,
                "close");

        requireNonNull(
                events,
                "events");

        requireNonNull(
                completedAt,
                "completedAt");

        requireNonNull(
                actor,
                "actor");

        requireMonetaryValue(
                actualBalance,
                "actual balance");

        if (actualBalance.signum() < 0) {
            throw new IllegalArgumentException(
                    "actual balance must not be negative");
        }

        if (events.isEmpty()) {
            throw new IllegalArgumentException(
                    "consolidation requires at least one event");
        }

        Set<OperationalEventId> eventIds =
                new HashSet<>();

        for (OperationalEvent event : events) {
            requireNonNull(
                    event,
                    "event");

            if (!close.id().equals(
                    event.closeId())) {

                throw new IllegalArgumentException(
                        "all events must belong to the consolidated close");
            }

            if (event.state()
                    != OperationalEventState.VALIDATED) {

                throw new IllegalArgumentException(
                        "all consolidated events must be validated");
            }

            if (!eventIds.add(
                    event.id())) {

                throw new IllegalArgumentException(
                        "consolidation must not contain duplicate events");
            }
        }

        List<ConsolidationEventSnapshot> snapshots =
                events.stream()
                        .map(
                                event ->
                                        ConsolidationEventSnapshot.capture(
                                                id,
                                                event,
                                                completedAt))
                        .toList();

        CalculatedAmounts calculatedAmounts =
                calculateAmounts(
                        snapshots);

        BigDecimal expectedBalance =
                close.initialBalance()
                        .value()
                        .add(
                                calculatedAmounts.balanceEffect());

        requireMonetaryValue(
                expectedBalance,
                "expected balance");

        BigDecimal difference =
                actualBalance.subtract(
                        expectedBalance);

        requireMonetaryValue(
                difference,
                "difference");

        return new Consolidation(
                id,
                close.id(),
                close.currencyCode(),
                snapshots.size(),
                calculatedAmounts.totalIncome(),
                calculatedAmounts.totalExpense(),
                calculatedAmounts.totalDiscount(),
                calculatedAmounts.totalCancellation(),
                close.initialBalance(),
                expectedBalance,
                actualBalance,
                difference,
                true,
                completedAt,
                actor,
                null,
                null,
                snapshots);
    }

    public Consolidation invalidate(
            Instant invalidatedAt,
            String invalidationReason) {

        if (!current) {
            throw new IllegalStateException(
                    "consolidation is already invalidated");
        }

        return new Consolidation(
                id,
                closeId,
                currencyCode,
                eventCount,
                totalIncome,
                totalExpense,
                totalDiscount,
                totalCancellation,
                initialBalance,
                expectedBalance,
                actualBalance,
                difference,
                false,
                completedAt,
                completedBy,
                invalidatedAt,
                invalidationReason,
                eventSnapshots);
    }

    public boolean balanced() {
        return difference.signum() == 0;
    }

    private static void validateSnapshotOwnershipAndUniqueness(
            ConsolidationId consolidationId,
            List<ConsolidationEventSnapshot> snapshots) {

        Set<OperationalEventId> eventIds =
                new HashSet<>();

        for (ConsolidationEventSnapshot snapshot
                : snapshots) {

            if (!consolidationId.equals(
                    snapshot.consolidationId())) {

                throw new IllegalArgumentException(
                        "snapshot must belong to the consolidation");
            }

            if (!eventIds.add(
                    snapshot.eventId())) {

                throw new IllegalArgumentException(
                        "consolidation must not contain duplicate snapshots");
            }
        }
    }

    private static CalculatedAmounts calculateAmounts(
            List<ConsolidationEventSnapshot> snapshots) {

        BigDecimal totalIncome =
                ZERO_MONEY;

        BigDecimal totalExpense =
                ZERO_MONEY;

        BigDecimal totalDiscount =
                ZERO_MONEY;

        BigDecimal totalCancellation =
                ZERO_MONEY;

        BigDecimal balanceEffect =
                ZERO_MONEY;

        for (ConsolidationEventSnapshot snapshot
                : snapshots) {

            switch (snapshot.eventType()) {
                case INCOME ->
                        totalIncome =
                                totalIncome.add(
                                        snapshot.amount().value());

                case EXPENSE ->
                        totalExpense =
                                totalExpense.add(
                                        snapshot.amount().value());

                case DISCOUNT ->
                        totalDiscount =
                                totalDiscount.add(
                                        snapshot.amount().value());

                case CANCELLATION ->
                        totalCancellation =
                                totalCancellation.add(
                                        snapshot.amount().value());
            }

            balanceEffect =
                    balanceEffect.add(
                            snapshot.balanceEffect());
        }

        requireMonetaryValue(
                totalIncome,
                "calculated total income");

        requireMonetaryValue(
                totalExpense,
                "calculated total expense");

        requireMonetaryValue(
                totalDiscount,
                "calculated total discount");

        requireMonetaryValue(
                totalCancellation,
                "calculated total cancellation");

        requireMonetaryValue(
                balanceEffect,
                "calculated balance effect");

        return new CalculatedAmounts(
                totalIncome,
                totalExpense,
                totalDiscount,
                totalCancellation,
                balanceEffect);
    }

    private static void validateValidityMetadata(
            boolean current,
            Instant completedAt,
            Instant invalidatedAt,
            String invalidationReason) {

        if (current) {
            if (invalidatedAt != null
                    || invalidationReason != null) {

                throw new IllegalArgumentException(
                        "current consolidation must not contain "
                                + "invalidation metadata");
            }

            return;
        }

        if (invalidatedAt == null
                || invalidationReason == null) {

            throw new IllegalArgumentException(
                    "invalidated consolidation requires "
                            + "complete invalidation metadata");
        }

        if (invalidatedAt.isBefore(
                completedAt)) {

            throw new IllegalArgumentException(
                    "invalidation instant must not be before completion");
        }
    }

    private static void requireEqual(
            BigDecimal actual,
            BigDecimal expected,
            String fieldName) {

        if (actual.compareTo(
                expected) != 0) {

            throw new IllegalArgumentException(
                    fieldName
                            + " does not match snapshot calculation");
        }
    }

    private static void requireMonetaryValue(
            BigDecimal value,
            String fieldName) {

        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null");
        }

        if (value.scale() > MAXIMUM_SCALE) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must not exceed four decimal places");
        }

        int integerDigits =
                Math.max(
                        0,
                        value.precision()
                                - value.scale());

        if (integerDigits
                > MAXIMUM_INTEGER_DIGITS) {

            throw new IllegalArgumentException(
                    fieldName
                            + " exceeds numeric(19,4)");
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

    private record CalculatedAmounts(
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal totalDiscount,
            BigDecimal totalCancellation,
            BigDecimal balanceEffect) {
    }

}