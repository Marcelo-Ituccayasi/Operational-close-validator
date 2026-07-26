package com.marceloituccayasi.ocv.operationalclose.domain;

import java.time.Instant;

/**
 * Immutable append-only history entry for a Validation Alert.
 *
 * @param id stable transition identifier
 * @param alertId affected alert
 * @param fromState previous state, or null for alert creation
 * @param toState resulting state
 * @param actionCode structured lifecycle action
 * @param detail optional human-readable detail
 * @param justification mandatory justification for discard
 * @param validationResultId mandatory successful result for resolution
 * @param occurredAt transition instant
 * @param actor responsible authenticated actor
 */
public record ValidationAlertTransition(
        ValidationAlertTransitionId id,
        ValidationAlertId alertId,
        ValidationAlertState fromState,
        ValidationAlertState toState,
        String actionCode,
        String detail,
        String justification,
        ValidationResultId validationResultId,
        Instant occurredAt,
        AuditActor actor) {

    public static final String ALERT_CREATED =
            "ALERT_CREATED";

    public static final String ALERT_ACKNOWLEDGED =
            "ALERT_ACKNOWLEDGED";

    public static final String ALERT_UNDER_REVIEW =
            "ALERT_UNDER_REVIEW";

    public static final String ALERT_RESOLVED =
            "ALERT_RESOLVED";

    public static final String ALERT_DISCARDED =
            "ALERT_DISCARDED";

    private static final int MAXIMUM_ACTION_CODE_LENGTH =
            40;

    public ValidationAlertTransition {
        requireNonNull(
                id,
                "id");

        requireNonNull(
                alertId,
                "alertId");

        requireNonNull(
                toState,
                "toState");

        requireNonNull(
                occurredAt,
                "occurredAt");

        requireNonNull(
                actor,
                "actor");

        actionCode =
                requireText(
                        actionCode,
                        "actionCode");

        if (actionCode.length()
                > MAXIMUM_ACTION_CODE_LENGTH) {

            throw new IllegalArgumentException(
                    "alert transition action code must not exceed 40 characters");
        }

        if (fromState == toState) {
            throw new IllegalArgumentException(
                    "alert transition must change the state");
        }

        detail =
                normalizeOptionalText(
                        detail);

        if (toState == ValidationAlertState.RESOLVED) {
            requireNonNull(
                    validationResultId,
                    "validationResultId");

            if (justification != null) {
                throw new IllegalArgumentException(
                        "resolved alert transition must not contain discard justification");
            }
        }
        else if (toState == ValidationAlertState.DISCARDED) {
            if (validationResultId != null) {
                throw new IllegalArgumentException(
                        "discarded alert transition must not contain validation result");
            }

            justification =
                    requireText(
                            justification,
                            "justification");
        }
        else {
            if (validationResultId != null
                    || justification != null) {

                throw new IllegalArgumentException(
                        "non-terminal alert transition must not contain closure metadata");
            }
        }
    }

    public static ValidationAlertTransition initial(
            ValidationAlertTransitionId id,
            ValidationAlertId alertId,
            String detail,
            Instant occurredAt,
            AuditActor actor) {

        return new ValidationAlertTransition(
                id,
                alertId,
                null,
                ValidationAlertState.ACTIVE,
                ALERT_CREATED,
                detail,
                null,
                null,
                occurredAt,
                actor);
    }

    public static ValidationAlertTransition acknowledged(
            ValidationAlertTransitionId id,
            ValidationAlertId alertId,
            String detail,
            Instant occurredAt,
            AuditActor actor) {

        return new ValidationAlertTransition(
                id,
                alertId,
                ValidationAlertState.ACTIVE,
                ValidationAlertState.ACKNOWLEDGED,
                ALERT_ACKNOWLEDGED,
                detail,
                null,
                null,
                occurredAt,
                actor);
    }

    public static ValidationAlertTransition underReview(
            ValidationAlertTransitionId id,
            ValidationAlertId alertId,
            ValidationAlertState fromState,
            String detail,
            Instant occurredAt,
            AuditActor actor) {

        if (fromState != ValidationAlertState.ACTIVE
                && fromState
                        != ValidationAlertState.ACKNOWLEDGED) {

            throw new IllegalArgumentException(
                    "review transition requires active or acknowledged state");
        }

        return new ValidationAlertTransition(
                id,
                alertId,
                fromState,
                ValidationAlertState.UNDER_REVIEW,
                ALERT_UNDER_REVIEW,
                detail,
                null,
                null,
                occurredAt,
                actor);
    }

    public static ValidationAlertTransition resolved(
            ValidationAlertTransitionId id,
            ValidationAlertId alertId,
            ValidationAlertState fromState,
            String detail,
            ValidationResultId validationResultId,
            Instant occurredAt,
            AuditActor actor) {

        requireOpenState(
                fromState,
                "resolution");

        return new ValidationAlertTransition(
                id,
                alertId,
                fromState,
                ValidationAlertState.RESOLVED,
                ALERT_RESOLVED,
                detail,
                null,
                validationResultId,
                occurredAt,
                actor);
    }

    public static ValidationAlertTransition discarded(
            ValidationAlertTransitionId id,
            ValidationAlertId alertId,
            ValidationAlertState fromState,
            String detail,
            String justification,
            Instant occurredAt,
            AuditActor actor) {

        requireOpenState(
                fromState,
                "discard");

        return new ValidationAlertTransition(
                id,
                alertId,
                fromState,
                ValidationAlertState.DISCARDED,
                ALERT_DISCARDED,
                detail,
                justification,
                null,
                occurredAt,
                actor);
    }

    private static void requireOpenState(
            ValidationAlertState state,
            String actionName) {

        requireNonNull(
                state,
                "fromState");

        if (state.terminal()) {
            throw new IllegalArgumentException(
                    actionName
                            + " transition requires a non-terminal state");
        }
    }

    private static String normalizeOptionalText(
            String value) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private static String requireText(
            String value,
            String fieldName) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    fieldName + " must not be blank");
        }

        return value.trim();
    }

    private static void requireNonNull(
            Object value,
            String fieldName) {

        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null");
        }
    }

}