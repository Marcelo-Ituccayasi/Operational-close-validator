package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.List;
import java.util.Objects;

import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;

/**
 * Explicit result for Operational Event list queries.
 *
 * @param status query status
 * @param closeState current close state when found
 * @param closeEditable whether the close still permits mutations
 * @param operationalEvents returned events when the close exists
 */
public record ListOperationalEventsResult(
        Status status,
        String closeState,
        boolean closeEditable,
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

        if (status == Status.FOUND) {
            if (closeState == null
                    || closeState.isBlank()) {

                throw new IllegalArgumentException(
                        "found result must contain the close state");
            }

            boolean expectedEditable =
                    !OperationalCloseState
                            .SENT_TO_ACCOUNTING
                            .name()
                            .equals(closeState);

            if (closeEditable != expectedEditable) {
                throw new IllegalArgumentException(
                        "close editability must match the close state");
            }
        }

        if (status == Status.CLOSE_NOT_FOUND
                && (closeState != null
                        || closeEditable
                        || !operationalEvents.isEmpty())) {

            throw new IllegalArgumentException(
                    "close-not-found result must not contain close data");
        }
    }

    public static ListOperationalEventsResult found(
            OperationalClose operationalClose,
            List<OperationalEventView> operationalEvents) {

        Objects.requireNonNull(
                operationalClose,
                "operationalClose must not be null");

        return new ListOperationalEventsResult(
                Status.FOUND,
                operationalClose.state().name(),
                operationalClose.state()
                        != OperationalCloseState.SENT_TO_ACCOUNTING,
                operationalEvents);
    }

    public static ListOperationalEventsResult closeNotFound() {
        return new ListOperationalEventsResult(
                Status.CLOSE_NOT_FOUND,
                null,
                false,
                List.of());
    }

}