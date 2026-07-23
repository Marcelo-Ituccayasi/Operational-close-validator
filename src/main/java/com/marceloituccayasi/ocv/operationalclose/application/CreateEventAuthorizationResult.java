package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.UUID;

/**
 * Explicit application result for Event Authorization creation.
 *
 * @param status operation status
 * @param authorizationId created identifier when successful
 * @param message safe result description when applicable
 */
public record CreateEventAuthorizationResult(
        Status status,
        UUID authorizationId,
        String message) {

    public enum Status {
        CREATED,
        INVALID_INPUT,
        ACTOR_REJECTED,
        CLOSE_NOT_FOUND,
        CLOSE_NOT_EDITABLE,
        EVENT_NOT_FOUND
    }

    public CreateEventAuthorizationResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        if (status == Status.CREATED
                && authorizationId == null) {

            throw new IllegalArgumentException(
                    "created result must contain authorizationId");
        }

        if (status != Status.CREATED
                && authorizationId != null) {

            throw new IllegalArgumentException(
                    "non-created result must not contain authorizationId");
        }

        if (message != null
                && message.isBlank()) {

            throw new IllegalArgumentException(
                    "message must not be blank");
        }
    }

    public static CreateEventAuthorizationResult created(
            UUID authorizationId) {

        return new CreateEventAuthorizationResult(
                Status.CREATED,
                Objects.requireNonNull(
                        authorizationId),
                null);
    }

    public static CreateEventAuthorizationResult invalidInput(
            String message) {

        return new CreateEventAuthorizationResult(
                Status.INVALID_INPUT,
                null,
                Objects.requireNonNull(
                        message));
    }

    public static CreateEventAuthorizationResult actorRejected() {
        return new CreateEventAuthorizationResult(
                Status.ACTOR_REJECTED,
                null,
                "The authenticated actor cannot perform this operation.");
    }

    public static CreateEventAuthorizationResult closeNotFound() {
        return new CreateEventAuthorizationResult(
                Status.CLOSE_NOT_FOUND,
                null,
                "The requested Operational Close does not exist.");
    }

    public static CreateEventAuthorizationResult closeNotEditable() {
        return new CreateEventAuthorizationResult(
                Status.CLOSE_NOT_EDITABLE,
                null,
                "The Operational Close does not allow authorization creation.");
    }

    public static CreateEventAuthorizationResult eventNotFound() {
        return new CreateEventAuthorizationResult(
                Status.EVENT_NOT_FOUND,
                null,
                "The requested Operational Event does not exist.");
    }

}