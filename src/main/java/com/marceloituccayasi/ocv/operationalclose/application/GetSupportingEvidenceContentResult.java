package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;

/**
 * Explicit result for Supporting Evidence content retrieval.
 *
 * @param status retrieval status
 * @param content verified stored content when found
 */
public record GetSupportingEvidenceContentResult(
        Status status,
        StoredSupportingEvidenceContent content) {

    public enum Status {
        FOUND,
        NOT_FOUND
    }

    public GetSupportingEvidenceContentResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        if (status == Status.FOUND
                && content == null) {

            throw new IllegalArgumentException(
                    "found result must contain Supporting Evidence content");
        }

        if (status == Status.NOT_FOUND
                && content != null) {

            throw new IllegalArgumentException(
                    "not-found result must not contain Supporting Evidence content");
        }
    }

    public static GetSupportingEvidenceContentResult found(
            StoredSupportingEvidenceContent content) {

        return new GetSupportingEvidenceContentResult(
                Status.FOUND,
                Objects.requireNonNull(
                        content,
                        "content must not be null"));
    }

    public static GetSupportingEvidenceContentResult notFound() {
        return new GetSupportingEvidenceContentResult(
                Status.NOT_FOUND,
                null);
    }

}