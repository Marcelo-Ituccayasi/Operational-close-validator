package com.marceloituccayasi.ocv.operationalclose.domain;

import java.time.Instant;

/**
 * Atomic domain revision produced by a relevant change to data that depends
 * on an Operational Event, such as Supporting Evidence or Authorization.
 *
 * @param revisedEvent revised Operational Event
 * @param revisedClose revised or unchanged Operational Close
 * @param eventStateChanged whether the Event state changed
 * @param closeStateChanged whether the Close state changed
 */
public record OperationalDependencyRevision(
        OperationalEvent revisedEvent,
        OperationalClose revisedClose,
        boolean eventStateChanged,
        boolean closeStateChanged) {

    public OperationalDependencyRevision {
        requireNonNull(
                revisedEvent,
                "revisedEvent");

        requireNonNull(
                revisedClose,
                "revisedClose");

        if (!revisedClose.id()
                .equals(
                        revisedEvent.closeId())) {

            throw new IllegalArgumentException(
                    "revised event must belong to revised close");
        }
    }

    public static OperationalDependencyRevision apply(
            OperationalClose operationalClose,
            OperationalEvent operationalEvent,
            Instant revisedAt,
            AuditActor actor) {

        requireNonNull(
                operationalClose,
                "operationalClose");

        requireNonNull(
                operationalEvent,
                "operationalEvent");

        requireNonNull(
                revisedAt,
                "revisedAt");

        requireNonNull(
                actor,
                "actor");

        if (!operationalClose.id()
                .equals(
                        operationalEvent.closeId())) {

            throw new IllegalArgumentException(
                    "operational event must belong to operational close");
        }

        if (operationalClose.state()
                == OperationalCloseState.SENT_TO_ACCOUNTING) {

            throw new IllegalStateException(
                    "sent operational close is immutable");
        }

        requireNotBefore(
                revisedAt,
                operationalEvent.updatedAt(),
                "revision instant must not be before event update");

        requireNotBefore(
                revisedAt,
                operationalEvent.stateChangedAt(),
                "revision instant must not be before event state change");

        requireNotBefore(
                revisedAt,
                operationalClose.updatedAt(),
                "revision instant must not be before close update");

        requireNotBefore(
                revisedAt,
                operationalClose.stateChangedAt(),
                "revision instant must not be before close state change");

        OperationalEventState revisedEventState =
                operationalEvent.state();

        Instant revisedEventStateChangedAt =
                operationalEvent.stateChangedAt();

        boolean eventStateChanged =
                operationalEvent.state()
                        == OperationalEventState.VALIDATED;

        if (eventStateChanged) {
            revisedEventState =
                    OperationalEventState.REGISTERED;

            revisedEventStateChangedAt =
                    revisedAt;
        }

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
                        revisedEventState,
                        operationalEvent.evidenceRequired(),
                        operationalEvent.authorizationRequired(),
                        nextRevision(
                                operationalEvent.dataRevision()),
                        revisedEventStateChangedAt,
                        operationalEvent.createdAt(),
                        operationalEvent.createdBy(),
                        revisedAt,
                        actor);

        OperationalClose revisedClose =
                operationalClose;

        boolean closeStateChanged =
                operationalClose.state()
                        == OperationalCloseState.VALIDATED;

        if (closeStateChanged) {
            revisedClose =
                    new OperationalClose(
                            operationalClose.id(),
                            operationalClose.period(),
                            operationalClose.currencyCode(),
                            operationalClose.initialBalance(),
                            OperationalCloseState.BLOCKED,
                            revisedAt,
                            operationalClose.createdAt(),
                            operationalClose.createdBy(),
                            revisedAt,
                            actor);
        }

        return new OperationalDependencyRevision(
                revisedEvent,
                revisedClose,
                eventStateChanged,
                closeStateChanged);
    }

    private static long nextRevision(
            long currentRevision) {

        try {
            return Math.addExact(
                    currentRevision,
                    1L);
        }
        catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "data revision cannot be incremented",
                    exception);
        }
    }

    private static void requireNotBefore(
            Instant actual,
            Instant minimum,
            String message) {

        if (actual.isBefore(minimum)) {
            throw new IllegalArgumentException(
                    message);
        }
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