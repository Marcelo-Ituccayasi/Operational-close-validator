package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;

/**
 * Explicit result for Operational Event detail queries.
 *
 * @param status query status
 * @param operationalEvent returned event when found
 */
public record GetOperationalEventResult(
        Status status,
        OperationalEventView operationalEvent) {

    public enum Status {
        FOUND,
        NOT_FOUND
    }

    public GetOperationalEventResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        if (status == Status.FOUND
                && operationalEvent == null) {

            throw new IllegalArgumentException(
                    "found result must contain an Operational Event");
        }

        if (status == Status.NOT_FOUND
                && operationalEvent != null) {

            throw new IllegalArgumentException(
                    "not-found result must not contain an Operational Event");
        }
    }

    public static GetOperationalEventResult found(
            OperationalEventView operationalEvent) {

        return new GetOperationalEventResult(
                Status.FOUND,
                Objects.requireNonNull(operationalEvent));
    }

    public static GetOperationalEventResult notFound() {
        return new GetOperationalEventResult(
                Status.NOT_FOUND,
                null);
    }

}