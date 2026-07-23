package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.UUID;

/**
 * Explicit application result for Operational Event creation.
 *
 * @param status operation status
 * @param eventId created identifier when successful
 * @param message safe result description when applicable
 */
public record CreateOperationalEventResult(
        Status status,
        UUID eventId,
        String message) {

    public enum Status {
        CREATED,
        INVALID_INPUT,
        ACTOR_REJECTED,
        CLOSE_NOT_FOUND,
        CLOSE_NOT_EDITABLE,
        REVERSED_EVENT_NOT_FOUND,
        CANCELLATION_CONFLICT
    }

    public CreateOperationalEventResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        if (status == Status.CREATED
                && eventId == null) {

            throw new IllegalArgumentException(
                    "created result must contain eventId");
        }

        if (status != Status.CREATED
                && eventId != null) {

            throw new IllegalArgumentException(
                    "non-created result must not contain eventId");
        }

        if (message != null && message.isBlank()) {
            throw new IllegalArgumentException(
                    "message must not be blank");
        }
    }

    public static CreateOperationalEventResult created(
            UUID eventId) {

        return new CreateOperationalEventResult(
                Status.CREATED,
                Objects.requireNonNull(eventId),
                null);
    }

    public static CreateOperationalEventResult invalidInput(
            String message) {

        return new CreateOperationalEventResult(
                Status.INVALID_INPUT,
                null,
                Objects.requireNonNull(message));
    }

    public static CreateOperationalEventResult actorRejected() {
        return new CreateOperationalEventResult(
                Status.ACTOR_REJECTED,
                null,
                "The authenticated actor cannot perform this operation.");
    }

    public static CreateOperationalEventResult closeNotFound() {
        return new CreateOperationalEventResult(
                Status.CLOSE_NOT_FOUND,
                null,
                "The requested Operational Close does not exist.");
    }

    public static CreateOperationalEventResult closeNotEditable() {
        return new CreateOperationalEventResult(
                Status.CLOSE_NOT_EDITABLE,
                null,
                "The Operational Close does not allow event creation.");
    }

    public static CreateOperationalEventResult
            reversedEventNotFound() {

        return new CreateOperationalEventResult(
                Status.REVERSED_EVENT_NOT_FOUND,
                null,
                "The event requested for cancellation does not exist.");
    }

    public static CreateOperationalEventResult
            cancellationConflict() {

        return new CreateOperationalEventResult(
                Status.CANCELLATION_CONFLICT,
                null,
                "The referenced event already has a cancellation.");
    }

}