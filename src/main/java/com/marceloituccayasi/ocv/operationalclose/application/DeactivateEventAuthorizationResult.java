package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.UUID;

/**
 * Explicit application result for Event Authorization deactivation.
 *
 * @param status operation status
 * @param authorizationId deactivated identifier when successful
 * @param message safe result description when applicable
 */
public record DeactivateEventAuthorizationResult(
        Status status,
        UUID authorizationId,
        String message) {

    public enum Status {
        DEACTIVATED,
        INVALID_INPUT,
        ACTOR_REJECTED,
        CLOSE_NOT_FOUND,
        CLOSE_NOT_EDITABLE,
        EVENT_NOT_FOUND,
        AUTHORIZATION_NOT_FOUND,
        AUTHORIZATION_ALREADY_INACTIVE
    }

    public DeactivateEventAuthorizationResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        if (status == Status.DEACTIVATED
                && authorizationId == null) {

            throw new IllegalArgumentException(
                    "deactivated result must contain authorizationId");
        }

        if (status != Status.DEACTIVATED
                && authorizationId != null) {

            throw new IllegalArgumentException(
                    "non-deactivated result must not contain authorizationId");
        }

        if (message != null
                && message.isBlank()) {

            throw new IllegalArgumentException(
                    "message must not be blank");
        }
    }

    public static DeactivateEventAuthorizationResult deactivated(
            UUID authorizationId) {

        return new DeactivateEventAuthorizationResult(
                Status.DEACTIVATED,
                Objects.requireNonNull(
                        authorizationId),
                null);
    }

    public static DeactivateEventAuthorizationResult invalidInput(
            String message) {

        return new DeactivateEventAuthorizationResult(
                Status.INVALID_INPUT,
                null,
                Objects.requireNonNull(
                        message));
    }

    public static DeactivateEventAuthorizationResult actorRejected() {
        return new DeactivateEventAuthorizationResult(
                Status.ACTOR_REJECTED,
                null,
                "The authenticated actor cannot perform this operation.");
    }

    public static DeactivateEventAuthorizationResult closeNotFound() {
        return new DeactivateEventAuthorizationResult(
                Status.CLOSE_NOT_FOUND,
                null,
                "The requested Operational Close does not exist.");
    }

    public static DeactivateEventAuthorizationResult closeNotEditable() {
        return new DeactivateEventAuthorizationResult(
                Status.CLOSE_NOT_EDITABLE,
                null,
                "The Operational Close does not allow authorization deactivation.");
    }

    public static DeactivateEventAuthorizationResult eventNotFound() {
        return new DeactivateEventAuthorizationResult(
                Status.EVENT_NOT_FOUND,
                null,
                "The requested Operational Event does not exist.");
    }

    public static DeactivateEventAuthorizationResult
            authorizationNotFound() {

        return new DeactivateEventAuthorizationResult(
                Status.AUTHORIZATION_NOT_FOUND,
                null,
                "The requested Event Authorization does not exist.");
    }

    public static DeactivateEventAuthorizationResult
            authorizationAlreadyInactive() {

        return new DeactivateEventAuthorizationResult(
                Status.AUTHORIZATION_ALREADY_INACTIVE,
                null,
                "The requested Event Authorization is already inactive.");
    }

}