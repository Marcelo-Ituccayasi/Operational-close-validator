package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.UUID;

/**
 * Explicit application result for Supporting Evidence creation.
 *
 * @param status operation status
 * @param evidenceId created identifier when successful
 * @param message safe result description when applicable
 */
public record CreateSupportingEvidenceResult(
        Status status,
        UUID evidenceId,
        String message) {

    public enum Status {
        CREATED,
        INVALID_INPUT,
        ACTOR_REJECTED,
        CLOSE_NOT_FOUND,
        CLOSE_NOT_EDITABLE,
        EVENT_NOT_FOUND
    }

    public CreateSupportingEvidenceResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        if (status == Status.CREATED
                && evidenceId == null) {

            throw new IllegalArgumentException(
                    "created result must contain evidenceId");
        }

        if (status != Status.CREATED
                && evidenceId != null) {

            throw new IllegalArgumentException(
                    "non-created result must not contain evidenceId");
        }

        if (message != null
                && message.isBlank()) {

            throw new IllegalArgumentException(
                    "message must not be blank");
        }
    }

    public static CreateSupportingEvidenceResult created(
            UUID evidenceId) {

        return new CreateSupportingEvidenceResult(
                Status.CREATED,
                Objects.requireNonNull(
                        evidenceId),
                null);
    }

    public static CreateSupportingEvidenceResult invalidInput(
            String message) {

        return new CreateSupportingEvidenceResult(
                Status.INVALID_INPUT,
                null,
                Objects.requireNonNull(
                        message));
    }

    public static CreateSupportingEvidenceResult actorRejected() {
        return new CreateSupportingEvidenceResult(
                Status.ACTOR_REJECTED,
                null,
                "The authenticated actor cannot perform this operation.");
    }

    public static CreateSupportingEvidenceResult closeNotFound() {
        return new CreateSupportingEvidenceResult(
                Status.CLOSE_NOT_FOUND,
                null,
                "The requested Operational Close does not exist.");
    }

    public static CreateSupportingEvidenceResult closeNotEditable() {
        return new CreateSupportingEvidenceResult(
                Status.CLOSE_NOT_EDITABLE,
                null,
                "The Operational Close does not allow evidence creation.");
    }

    public static CreateSupportingEvidenceResult eventNotFound() {
        return new CreateSupportingEvidenceResult(
                Status.EVENT_NOT_FOUND,
                null,
                "The requested Operational Event does not exist.");
    }

}