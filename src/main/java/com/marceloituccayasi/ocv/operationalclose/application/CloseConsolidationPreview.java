package com.marceloituccayasi.ocv.operationalclose.application;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

/**
 * Framework-independent read model for the consolidation form.
 *
 * <p>The preview is informative. The completion use case reloads all data and
 * recalculates the authoritative Consolidation inside its transaction.</p>
 *
 * @param close Operational Close information
 * @param events Events currently included in the preliminary calculation
 * @param eventCount number of Events
 * @param totalIncome nominal Income total
 * @param totalExpense nominal Expense total
 * @param totalDiscount nominal Discount total
 * @param totalCancellation nominal Cancellation total
 * @param expectedBalance preliminary expected balance
 * @param ready whether all consolidation preconditions currently hold
 * @param affectedEventIds Events currently requiring correction
 */
public record CloseConsolidationPreview(
        OperationalCloseView close,
        List<OperationalEventView> events,
        int eventCount,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal totalDiscount,
        BigDecimal totalCancellation,
        BigDecimal expectedBalance,
        boolean ready,
        List<UUID> affectedEventIds) {

    private static final int MAXIMUM_SCALE = 4;

    private static final int MAXIMUM_INTEGER_DIGITS = 15;

    private static final BigDecimal ZERO_MONEY =
            new BigDecimal(
                    "0.0000");

    public CloseConsolidationPreview {
        Objects.requireNonNull(
                close,
                "close must not be null");

        Objects.requireNonNull(
                events,
                "events must not be null");

        Objects.requireNonNull(
                affectedEventIds,
                "affectedEventIds must not be null");

        events =
                events.stream()
                        .map(
                                event ->
                                        Objects.requireNonNull(
                                                event,
                                                "events must not contain "
                                                        + "null values"))
                        .toList();

        affectedEventIds =
                affectedEventIds.stream()
                        .map(
                                eventId ->
                                        Objects.requireNonNull(
                                                eventId,
                                                "affectedEventIds must not "
                                                        + "contain null values"))
                        .distinct()
                        .sorted(
                                Comparator.naturalOrder())
                        .toList();

        if (eventCount
                != events.size()) {

            throw new IllegalArgumentException(
                    "event count must match event view count");
        }

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

        if (totalIncome.signum() < 0
                || totalExpense.signum() < 0
                || totalDiscount.signum() < 0
                || totalCancellation.signum() < 0) {

            throw new IllegalArgumentException(
                    "preview nominal totals must not be negative");
        }

        if (ready
                && (
                        eventCount < 1
                                || !affectedEventIds.isEmpty()
                )) {

            throw new IllegalArgumentException(
                    "ready preview requires Events without issues");
        }
    }

    public static CloseConsolidationPreview fromDomain(
            OperationalClose operationalClose,
            List<OperationalEvent> operationalEvents,
            CloseConsolidationReadiness readiness) {

        Objects.requireNonNull(
                operationalClose,
                "operationalClose must not be null");

        Objects.requireNonNull(
                operationalEvents,
                "operationalEvents must not be null");

        Objects.requireNonNull(
                readiness,
                "readiness must not be null");

        validateEvents(
                operationalClose,
                operationalEvents);

        boolean eventsPresent =
                !operationalEvents.isEmpty();

        if (readiness.eventsPresent()
                != eventsPresent) {

            throw new IllegalArgumentException(
                    "readiness event presence must match loaded Events");
        }

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

        for (OperationalEvent event : operationalEvents) {
            switch (event.eventType()) {
                case INCOME ->
                        totalIncome =
                                totalIncome.add(
                                        event.amount().value());

                case EXPENSE ->
                        totalExpense =
                                totalExpense.add(
                                        event.amount().value());

                case DISCOUNT ->
                        totalDiscount =
                                totalDiscount.add(
                                        event.amount().value());

                case CANCELLATION ->
                        totalCancellation =
                                totalCancellation.add(
                                        event.amount().value());
            }

            balanceEffect =
                    balanceEffect.add(
                            event.balanceEffect());
        }

        BigDecimal expectedBalance =
                operationalClose.initialBalance()
                        .value()
                        .add(
                                balanceEffect);

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
                expectedBalance,
                "calculated expected balance");

        List<OperationalEventView> eventViews =
                operationalEvents.stream()
                        .map(
                                OperationalEventView::fromDomain)
                        .toList();

        List<UUID> affectedEventIds =
                readiness.affectedEventIds()
                        .stream()
                        .map(
                                OperationalEventId::value)
                        .toList();

        return new CloseConsolidationPreview(
                OperationalCloseView.fromDomain(
                        operationalClose),
                eventViews,
                eventViews.size(),
                totalIncome,
                totalExpense,
                totalDiscount,
                totalCancellation,
                expectedBalance,
                readiness.ready(),
                affectedEventIds);
    }

    public boolean eventsPresent() {
        return eventCount > 0;
    }

    private static void validateEvents(
            OperationalClose operationalClose,
            List<OperationalEvent> operationalEvents) {

        Set<OperationalEventId> eventIds =
                new HashSet<>();

        for (OperationalEvent event : operationalEvents) {
            Objects.requireNonNull(
                    event,
                    "operationalEvents must not contain null values");

            if (!operationalClose.id().equals(
                    event.closeId())) {

                throw new IllegalArgumentException(
                        "all Events must belong to the preview Close");
            }

            if (!eventIds.add(
                    event.id())) {

                throw new IllegalArgumentException(
                        "preview must not contain duplicate Events");
            }
        }
    }

    private static void requireMonetaryValue(
            BigDecimal value,
            String fieldName) {

        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null");
        }

        if (value.scale()
                > MAXIMUM_SCALE) {

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

}