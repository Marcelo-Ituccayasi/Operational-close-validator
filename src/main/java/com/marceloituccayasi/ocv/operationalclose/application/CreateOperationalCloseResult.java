package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.UUID;

/**
 * Explicit application result for Operational Close creation.
 *
 * @param status operation status
 * @param closeId created identifier when successful
 * @param message safe result description when applicable
 */
public record CreateOperationalCloseResult(
        Status status,
        UUID closeId,
        String message) {

    public enum Status {
        CREATED,
        INVALID_INPUT,
        PERIOD_CONFLICT,
        ACTOR_REJECTED
    }

    public CreateOperationalCloseResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        if (status == Status.CREATED
                && closeId == null) {

            throw new IllegalArgumentException(
                    "created result must contain closeId");
        }

        if (status != Status.CREATED
                && closeId != null) {

            throw new IllegalArgumentException(
                    "non-created result must not contain closeId");
        }

        if (message != null && message.isBlank()) {
            throw new IllegalArgumentException(
                    "message must not be blank");
        }
    }

    public static CreateOperationalCloseResult created(
            UUID closeId) {

        return new CreateOperationalCloseResult(
                Status.CREATED,
                Objects.requireNonNull(closeId),
                null);
    }

    public static CreateOperationalCloseResult invalidInput(
            String message) {

        return new CreateOperationalCloseResult(
                Status.INVALID_INPUT,
                null,
                Objects.requireNonNull(message));
    }

    public static CreateOperationalCloseResult periodConflict() {
        return new CreateOperationalCloseResult(
                Status.PERIOD_CONFLICT,
                null,
                "An Operational Close already exists for the requested period.");
    }

    public static CreateOperationalCloseResult actorRejected() {
        return new CreateOperationalCloseResult(
                Status.ACTOR_REJECTED,
                null,
                "The authenticated actor cannot perform this operation.");
    }

}
