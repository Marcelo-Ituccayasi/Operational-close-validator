package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

/**
 * Structured readiness result for Operational Close consolidation.
 *
 * @param eventsPresent whether the Close contains at least one Event
 * @param notValidatedEventIds Events whose state is not VALIDATED
 * @param invalidResultEventIds Events without complete, current and satisfied
 *        applicable Validation Results
 * @param blockingAlertEventIds Events with an open blocking Alert
 */
public record CloseConsolidationReadiness(
        boolean eventsPresent,
        List<OperationalEventId> notValidatedEventIds,
        List<OperationalEventId> invalidResultEventIds,
        List<OperationalEventId> blockingAlertEventIds) {

    public CloseConsolidationReadiness {
        notValidatedEventIds =
                normalize(
                        notValidatedEventIds,
                        "notValidatedEventIds");

        invalidResultEventIds =
                normalize(
                        invalidResultEventIds,
                        "invalidResultEventIds");

        blockingAlertEventIds =
                normalize(
                        blockingAlertEventIds,
                        "blockingAlertEventIds");

        if (!eventsPresent
                && (
                        !notValidatedEventIds.isEmpty()
                                || !invalidResultEventIds.isEmpty()
                                || !blockingAlertEventIds.isEmpty()
                )) {

            throw new IllegalArgumentException(
                    "readiness without events must not contain event issues");
        }
    }

    public static CloseConsolidationReadiness noEvents() {
        return new CloseConsolidationReadiness(
                false,
                List.of(),
                List.of(),
                List.of());
    }

    public static CloseConsolidationReadiness evaluated(
            List<OperationalEventId> notValidatedEventIds,
            List<OperationalEventId> invalidResultEventIds,
            List<OperationalEventId> blockingAlertEventIds) {

        return new CloseConsolidationReadiness(
                true,
                notValidatedEventIds,
                invalidResultEventIds,
                blockingAlertEventIds);
    }

    public boolean ready() {
        return eventsPresent
                && notValidatedEventIds.isEmpty()
                && invalidResultEventIds.isEmpty()
                && blockingAlertEventIds.isEmpty();
    }

    public List<OperationalEventId> affectedEventIds() {
        return Stream.of(
                        notValidatedEventIds,
                        invalidResultEventIds,
                        blockingAlertEventIds)
                .flatMap(
                        List::stream)
                .distinct()
                .sorted(
                        Comparator.comparing(
                                eventId ->
                                        eventId.value()))
                .toList();
    }

    private static List<OperationalEventId> normalize(
            List<OperationalEventId> eventIds,
            String fieldName) {

        if (eventIds == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null");
        }

        return eventIds.stream()
                .map(
                        eventId -> {
                            if (eventId == null) {
                                throw new IllegalArgumentException(
                                        fieldName
                                                + " must not contain null values");
                            }

                            return eventId;
                        })
                .distinct()
                .sorted(
                        Comparator.comparing(
                                eventId ->
                                        eventId.value()))
                .toList();
    }

}