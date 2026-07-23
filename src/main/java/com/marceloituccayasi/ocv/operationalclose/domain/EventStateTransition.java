package com.marceloituccayasi.ocv.operationalclose.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable history entry for an Operational Event state.
 *
 * @param id transition identifier
 * @param eventId affected event
 * @param fromState previous state, or null for creation
 * @param toState resulting state
 * @param causeCode structured transition cause
 * @param detail optional human-readable detail
 * @param validationResultId optional related validation result
 * @param occurredAt transition instant
 * @param actor responsible actor
 */
public record EventStateTransition(
        EventStateTransitionId id,
        OperationalEventId eventId,
        OperationalEventState fromState,
        OperationalEventState toState,
        String causeCode,
        String detail,
        UUID validationResultId,
        Instant occurredAt,
        AuditActor actor) {

    public static final String EVENT_CREATED =
            "EVENT_CREATED";

    public EventStateTransition {
        requireNonNull(id, "id");
        requireNonNull(eventId, "eventId");
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

    public static EventStateTransition initial(
            EventStateTransitionId id,
            OperationalEventId eventId,
            Instant occurredAt,
            AuditActor actor) {

        return new EventStateTransition(
                id,
                eventId,
                null,
                OperationalEventState.REGISTERED,
                EVENT_CREATED,
                null,
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