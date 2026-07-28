package com.marceloituccayasi.ocv.operationalclose.domain;

import java.time.Instant;

/**
 * Atomic domain revision produced by applying the state derived from an
 * Operational Event validation.
 *
 * @param revisedEvent event containing the resulting validation state
 * @param stateTransition append-only transition when the state changed
 */
public record OperationalEventValidationRevision(
        OperationalEvent revisedEvent,
        EventStateTransition stateTransition) {

    public static final String EVENT_VALIDATION_APPLIED =
            "EVENT_VALIDATION_APPLIED";

    private static final String TRANSITION_DETAIL =
            "Operational Event state derived from current validation results.";

    public OperationalEventValidationRevision {
        requireNonNull(
                revisedEvent,
                "revisedEvent");

        if (stateTransition != null) {
            if (!revisedEvent.id()
                    .equals(
                            stateTransition.eventId())) {

                throw new IllegalArgumentException(
                        "validation transition must belong to revised event");
            }

            if (revisedEvent.state()
                    != stateTransition.toState()) {

                throw new IllegalArgumentException(
                        "validation transition state must match revised event");
            }

            if (!revisedEvent.stateChangedAt()
                    .equals(
                            stateTransition.occurredAt())) {

                throw new IllegalArgumentException(
                        "validation transition instant must match event state change");
            }
        }
    }

    public static OperationalEventValidationRevision apply(
            OperationalEvent operationalEvent,
            OperationalEventState resultingState,
            EventStateTransitionId transitionId,
            Instant occurredAt,
            AuditActor actor) {

        requireNonNull(
                operationalEvent,
                "operationalEvent");

        requireNonNull(
                resultingState,
                "resultingState");

        requireNonNull(
                occurredAt,
                "occurredAt");

        requireNonNull(
                actor,
                "actor");

        if (resultingState
                == OperationalEventState.REGISTERED) {

            throw new IllegalArgumentException(
                    "validation resulting state must not be registered");
        }

        if (occurredAt.isBefore(
                operationalEvent.updatedAt())) {

            throw new IllegalArgumentException(
                    "validation state instant must not be before event update");
        }

        if (occurredAt.isBefore(
                operationalEvent.stateChangedAt())) {

            throw new IllegalArgumentException(
                    "validation state instant must not be before previous event state change");
        }

        if (operationalEvent.state()
                == resultingState) {

            return new OperationalEventValidationRevision(
                    operationalEvent,
                    null);
        }

        requireNonNull(
                transitionId,
                "transitionId");

        OperationalEvent revisedEvent =
                new OperationalEvent(
                        operationalEvent.id(),
                        operationalEvent.closeId(),
                        operationalEvent.eventType(),
                        operationalEvent.amount(),
                        operationalEvent.balanceEffect(),
                        operationalEvent.reversedEventId(),
                        operationalEvent.occurredAt(),
                        operationalEvent.registeredAt(),
                        operationalEvent.responsibleName(),
                        operationalEvent.description(),
                        resultingState,
                        operationalEvent.evidenceRequired(),
                        operationalEvent.authorizationRequired(),
                        operationalEvent.dataRevision(),
                        occurredAt,
                        operationalEvent.createdAt(),
                        operationalEvent.createdBy(),
                        occurredAt,
                        actor);

        EventStateTransition transition =
                new EventStateTransition(
                        transitionId,
                        operationalEvent.id(),
                        operationalEvent.state(),
                        resultingState,
                        EVENT_VALIDATION_APPLIED,
                        TRANSITION_DETAIL,
                        null,
                        occurredAt,
                        actor);

        return new OperationalEventValidationRevision(
                revisedEvent,
                transition);
    }

    public boolean stateChanged() {
        return stateTransition != null;
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