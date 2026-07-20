package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;

/**
 * Explicit result for Operational Close detail queries.
 *
 * @param status query status
 * @param operationalClose returned view when found
 */
public record GetOperationalCloseResult(
        Status status,
        OperationalCloseView operationalClose) {

    public enum Status {
        FOUND,
        NOT_FOUND
    }

    public GetOperationalCloseResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        if (status == Status.FOUND
                && operationalClose == null) {

            throw new IllegalArgumentException(
                    "found result must contain an Operational Close");
        }

        if (status == Status.NOT_FOUND
                && operationalClose != null) {

            throw new IllegalArgumentException(
                    "not-found result must not contain an Operational Close");
        }
    }

    public static GetOperationalCloseResult found(
            OperationalCloseView operationalClose) {

        return new GetOperationalCloseResult(
                Status.FOUND,
                Objects.requireNonNull(operationalClose));
    }

    public static GetOperationalCloseResult notFound() {
        return new GetOperationalCloseResult(
                Status.NOT_FOUND,
                null);
    }

}
