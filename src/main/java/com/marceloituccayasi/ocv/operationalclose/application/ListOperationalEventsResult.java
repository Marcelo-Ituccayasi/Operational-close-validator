package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.List;
import java.util.Objects;

/**
 * Explicit result for Operational Event list queries.
 *
 * @param status query status
 * @param operationalEvents returned events when the close exists
 */
public record ListOperationalEventsResult(
        Status status,
        List<OperationalEventView> operationalEvents) {

    public enum Status {
        FOUND,
        CLOSE_NOT_FOUND
    }

    public ListOperationalEventsResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        operationalEvents =
                List.copyOf(
                        Objects.requireNonNull(
                                operationalEvents,
                                "operationalEvents must not be null"));

        if (status == Status.CLOSE_NOT_FOUND
                && !operationalEvents.isEmpty()) {

            throw new IllegalArgumentException(
                    "close-not-found result must not contain events");
        }
    }

    public static ListOperationalEventsResult found(
            List<OperationalEventView> operationalEvents) {

        return new ListOperationalEventsResult(
                Status.FOUND,
                operationalEvents);
    }

    public static ListOperationalEventsResult closeNotFound() {
        return new ListOperationalEventsResult(
                Status.CLOSE_NOT_FOUND,
                List.of());
    }

}