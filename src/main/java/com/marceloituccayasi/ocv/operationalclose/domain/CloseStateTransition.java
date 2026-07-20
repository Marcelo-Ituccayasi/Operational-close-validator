package com.marceloituccayasi.ocv.operationalclose.domain;

import java.time.Instant;

/**
 * Immutable history entry for an Operational Close state.
 *
 * @param id transition identifier
 * @param closeId affected close
 * @param fromState previous state, or null for creation
 * @param toState resulting state
 * @param causeCode structured transition cause
 * @param detail optional human-readable detail
 * @param occurredAt transition instant
 * @param actor responsible actor
 */
public record CloseStateTransition(
        CloseStateTransitionId id,
        OperationalCloseId closeId,
        OperationalCloseState fromState,
        OperationalCloseState toState,
        String causeCode,
        String detail,
        Instant occurredAt,
        AuditActor actor) {

    public static final String CLOSE_CREATED =
            "CLOSE_CREATED";

    public CloseStateTransition {
        requireNonNull(id, "id");
        requireNonNull(closeId, "closeId");
        requireNonNull(toState, "toState");
        requireNonNull(occurredAt, "occurredAt");
        requireNonNull(actor, "actor");

        if (fromState == toState) {
            throw new IllegalArgumentException(
                    "state transition must change the state");
        }

        if (causeCode == null || causeCode.isBlank()) {
            throw new IllegalArgumentException(
                    "cause code must not be blank");
        }

        if (causeCode.length() > 40) {
            throw new IllegalArgumentException(
                    "cause code must not exceed 40 characters");
        }
    }

    public static CloseStateTransition initial(
            CloseStateTransitionId id,
            OperationalCloseId closeId,
            Instant occurredAt,
            AuditActor actor) {

        return new CloseStateTransition(
                id,
                closeId,
                null,
                OperationalCloseState.PREPARATION,
                CLOSE_CREATED,
                null,
                occurredAt,
                actor);
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
