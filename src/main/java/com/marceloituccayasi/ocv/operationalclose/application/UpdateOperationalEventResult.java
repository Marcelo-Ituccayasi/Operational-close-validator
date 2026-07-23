package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.UUID;

/**
 * Explicit application result for Operational Event revision.
 *
 * @param status operation status
 * @param eventId revised identifier when successful
 * @param message safe result description when applicable
 */
public record UpdateOperationalEventResult(
        Status status,
        UUID eventId,
        String message) {

    public enum Status {
        UPDATED,
        INVALID_INPUT,
        ACTOR_REJECTED,
        CLOSE_NOT_FOUND,
        CLOSE_NOT_EDITABLE,
        EVENT_NOT_FOUND,
        REVERSED_EVENT_NOT_FOUND,
        CANCELLATION_CONFLICT
    }

    public UpdateOperationalEventResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        if (status == Status.UPDATED
                && eventId == null) {

            throw new IllegalArgumentException(
                    "updated result must contain eventId");
        }

        if (status != Status.UPDATED
                && eventId != null) {

            throw new IllegalArgumentException(
                    "non-updated result must not contain eventId");
        }

        if (message != null
                && message.isBlank()) {

            throw new IllegalArgumentException(
                    "message must not be blank");
        }
    }

    public static UpdateOperationalEventResult updated(
            UUID eventId) {

        return new UpdateOperationalEventResult(
                Status.UPDATED,
                Objects.requireNonNull(eventId),
                null);
    }

    public static UpdateOperationalEventResult invalidInput(
            String message) {

        return new UpdateOperationalEventResult(
                Status.INVALID_INPUT,
                null,
                Objects.requireNonNull(message));
    }

    public static UpdateOperationalEventResult actorRejected() {
        return new UpdateOperationalEventResult(
                Status.ACTOR_REJECTED,
                null,
                "The authenticated actor cannot perform this operation.");
    }

    public static UpdateOperationalEventResult closeNotFound() {
        return new UpdateOperationalEventResult(
                Status.CLOSE_NOT_FOUND,
                null,
                "The requested Operational Close does not exist.");
    }

    public static UpdateOperationalEventResult closeNotEditable() {
        return new UpdateOperationalEventResult(
                Status.CLOSE_NOT_EDITABLE,
                null,
                "The Operational Close does not allow event revision.");
    }

    public static UpdateOperationalEventResult eventNotFound() {
        return new UpdateOperationalEventResult(
                Status.EVENT_NOT_FOUND,
                null,
                "The requested Operational Event does not exist.");
    }

    public static UpdateOperationalEventResult
            reversedEventNotFound() {

        return new UpdateOperationalEventResult(
                Status.REVERSED_EVENT_NOT_FOUND,
                null,
                "The event requested for cancellation does not exist.");
    }

    public static UpdateOperationalEventResult
            cancellationConflict() {

        return new UpdateOperationalEventResult(
                Status.CANCELLATION_CONFLICT,
                null,
                "The referenced event already has a cancellation.");
    }

}