package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;

/**
 * Explicit application result for loading the consolidation form.
 *
 * @param status query status
 * @param preview consolidation preview when found and available
 * @param message safe description when unavailable
 */
public record GetOperationalCloseConsolidationResult(
        Status status,
        CloseConsolidationPreview preview,
        String message) {

    public enum Status {
        FOUND,
        NOT_FOUND,
        NOT_AVAILABLE
    }

    public GetOperationalCloseConsolidationResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        if (status == Status.FOUND) {
            Objects.requireNonNull(
                    preview,
                    "found result must contain preview");

            if (message != null) {
                throw new IllegalArgumentException(
                        "found result must not contain message");
            }
        }
        else {
            if (preview != null) {
                throw new IllegalArgumentException(
                        "non-found result must not contain preview");
            }

            if (message == null
                    || message.isBlank()) {

                throw new IllegalArgumentException(
                        "non-found result must contain message");
            }

            message =
                    message.trim();
        }
    }

    public static GetOperationalCloseConsolidationResult found(
            CloseConsolidationPreview preview) {

        return new GetOperationalCloseConsolidationResult(
                Status.FOUND,
                Objects.requireNonNull(
                        preview),
                null);
    }

    public static GetOperationalCloseConsolidationResult notFound() {
        return new GetOperationalCloseConsolidationResult(
                Status.NOT_FOUND,
                null,
                "The requested Operational Close does not exist.");
    }

    public static GetOperationalCloseConsolidationResult
            notAvailable() {

        return new GetOperationalCloseConsolidationResult(
                Status.NOT_AVAILABLE,
                null,
                "The Operational Close must be in preparation "
                        + "or blocked state.");
    }

}