package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.UUID;

/**
 * Explicit application result for Operational Event validation.
 *
 * @param status operation status
 * @param eventId validated event identifier when the validation was executed
 * @param message safe result description when applicable
 */
public record ValidateOperationalEventResult(
        Status status,
        UUID eventId,
        String message) {

    public enum Status {
        VALIDATED,
        VALIDATION_FAILED,
        INVALID_INPUT,
        ACTOR_REJECTED,
        CLOSE_NOT_FOUND,
        CLOSE_NOT_EDITABLE,
        EVENT_NOT_FOUND
    }

    public ValidateOperationalEventResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        boolean validationExecuted =
                status == Status.VALIDATED
                        || status
                                == Status.VALIDATION_FAILED;

        if (validationExecuted
                && eventId == null) {

            throw new IllegalArgumentException(
                    "executed validation result must contain eventId");
        }

        if (!validationExecuted
                && eventId != null) {

            throw new IllegalArgumentException(
                    "non-executed validation result must not contain eventId");
        }

        if (status == Status.VALIDATED
                && message != null) {

            throw new IllegalArgumentException(
                    "validated result must not contain message");
        }

        if (status != Status.VALIDATED
                && (
                        message == null
                                || message.isBlank()
                )) {

            throw new IllegalArgumentException(
                    "non-validated result must contain message");
        }
    }

    public static ValidateOperationalEventResult validated(
            UUID eventId) {

        return new ValidateOperationalEventResult(
                Status.VALIDATED,
                Objects.requireNonNull(
                        eventId),
                null);
    }

    public static ValidateOperationalEventResult
            validationFailed(
                    UUID eventId,
                    String message) {

        return new ValidateOperationalEventResult(
                Status.VALIDATION_FAILED,
                Objects.requireNonNull(
                        eventId),
                Objects.requireNonNull(
                        message));
    }

    public static ValidateOperationalEventResult invalidInput(
            String message) {

        return new ValidateOperationalEventResult(
                Status.INVALID_INPUT,
                null,
                Objects.requireNonNull(
                        message));
    }

    public static ValidateOperationalEventResult actorRejected() {
        return new ValidateOperationalEventResult(
                Status.ACTOR_REJECTED,
                null,
                "The authenticated actor cannot perform this operation.");
    }

    public static ValidateOperationalEventResult closeNotFound() {
        return new ValidateOperationalEventResult(
                Status.CLOSE_NOT_FOUND,
                null,
                "The requested Operational Close does not exist.");
    }

    public static ValidateOperationalEventResult closeNotEditable() {
        return new ValidateOperationalEventResult(
                Status.CLOSE_NOT_EDITABLE,
                null,
                "The Operational Close does not allow event validation.");
    }

    public static ValidateOperationalEventResult eventNotFound() {
        return new ValidateOperationalEventResult(
                Status.EVENT_NOT_FOUND,
                null,
                "The requested Operational Event does not exist.");
    }

}