package com.marceloituccayasi.ocv.operationalclose.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Operational Event belonging to an Operational Close.
 *
 * @param id stable event identifier
 * @param closeId owner close identifier
 * @param eventType event type
 * @param amount positive nominal amount
 * @param balanceEffect signed effect on the expected balance
 * @param reversedEventId referenced event for a cancellation
 * @param occurredAt business occurrence instant
 * @param registeredAt registration instant
 * @param responsibleName informed business responsible
 * @param description event reason or description
 * @param state current event state
 * @param evidenceRequired calculated evidence requirement
 * @param authorizationRequired calculated authorization requirement
 * @param dataRevision relevant-data revision
 * @param stateChangedAt instant of the current state
 * @param createdAt creation instant
 * @param createdBy creation actor
 * @param updatedAt last update instant
 * @param updatedBy last update actor
 */
public record OperationalEvent(
        OperationalEventId id,
        OperationalCloseId closeId,
        OperationalEventType eventType,
        OperationalEventAmount amount,
        BigDecimal balanceEffect,
        OperationalEventId reversedEventId,
        Instant occurredAt,
        Instant registeredAt,
        String responsibleName,
        String description,
        OperationalEventState state,
        boolean evidenceRequired,
        boolean authorizationRequired,
        long dataRevision,
        Instant stateChangedAt,
        Instant createdAt,
        AuditActor createdBy,
        Instant updatedAt,
        AuditActor updatedBy) {

    private static final int MAXIMUM_RESPONSIBLE_NAME_LENGTH = 200;

    public OperationalEvent {
        requireNonNull(id, "id");
        requireNonNull(closeId, "closeId");
        requireNonNull(eventType, "eventType");
        requireNonNull(amount, "amount");
        requireNonNull(balanceEffect, "balanceEffect");
        requireNonNull(occurredAt, "occurredAt");
        requireNonNull(registeredAt, "registeredAt");
        requireNonNull(state, "state");
        requireNonNull(stateChangedAt, "stateChangedAt");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(createdBy, "createdBy");
        requireNonNull(updatedAt, "updatedAt");
        requireNonNull(updatedBy, "updatedBy");

        responsibleName =
                requireText(
                        responsibleName,
                        "responsibleName");

        description =
                requireText(
                        description,
                        "description");

        if (responsibleName.length()
                > MAXIMUM_RESPONSIBLE_NAME_LENGTH) {

            throw new IllegalArgumentException(
                    "responsible name must not exceed 200 characters");
        }

        if (dataRevision < 1) {
            throw new IllegalArgumentException(
                    "data revision must be at least one");
        }

        if (registeredAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "registration instant must not be before creation");
        }

        if (stateChangedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "state change instant must not be before creation");
        }

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "update instant must not be before creation");
        }

        if (balanceEffect.abs()
                .compareTo(amount.value()) != 0) {

            throw new IllegalArgumentException(
                    "absolute balance effect must equal nominal amount");
        }

        if (eventType == OperationalEventType.CANCELLATION
                && reversedEventId == null) {

            throw new IllegalArgumentException(
                    "cancellation must reference a reversed event");
        }

        if (eventType != OperationalEventType.CANCELLATION
                && reversedEventId != null) {

            throw new IllegalArgumentException(
                    "only a cancellation may reference a reversed event");
        }

        validateRegularBalanceEffect(
                eventType,
                amount,
                balanceEffect);
    }

    public static OperationalEvent create(
            OperationalEventId id,
            OperationalCloseId closeId,
            OperationalEventType eventType,
            OperationalEventAmount amount,
            Instant occurredAt,
            String responsibleName,
            String description,
            boolean evidenceRequired,
            boolean authorizationRequired,
            Instant registeredAt,
            AuditActor actor) {

        requireNonNull(eventType, "eventType");

        if (eventType == OperationalEventType.CANCELLATION) {
            throw new IllegalArgumentException(
                    "cancellation must be created from a referenced event");
        }

        return new OperationalEvent(
                id,
                closeId,
                eventType,
                amount,
                balanceEffectFor(
                        eventType,
                        amount),
                null,
                occurredAt,
                registeredAt,
                responsibleName,
                description,
                OperationalEventState.REGISTERED,
                evidenceRequired,
                authorizationRequired,
                1,
                registeredAt,
                registeredAt,
                actor,
                registeredAt,
                actor);
    }

    public static OperationalEvent createCancellation(
            OperationalEventId id,
            OperationalCloseId closeId,
            OperationalEvent reversedEvent,
            Instant occurredAt,
            String responsibleName,
            String description,
            boolean evidenceRequired,
            boolean authorizationRequired,
            Instant registeredAt,
            AuditActor actor) {

        requireNonNull(id, "id");
        requireNonNull(closeId, "closeId");
        requireNonNull(reversedEvent, "reversedEvent");

        validateCancellationReference(
                id,
                closeId,
                reversedEvent);

        return new OperationalEvent(
                id,
                closeId,
                OperationalEventType.CANCELLATION,
                reversedEvent.amount(),
                reversedEvent.balanceEffect().negate(),
                reversedEvent.id(),
                occurredAt,
                registeredAt,
                responsibleName,
                description,
                OperationalEventState.REGISTERED,
                evidenceRequired,
                authorizationRequired,
                1,
                registeredAt,
                registeredAt,
                actor,
                registeredAt,
                actor);
    }

    public OperationalEvent reviseRegular(
            OperationalEventType revisedType,
            OperationalEventAmount revisedAmount,
            Instant revisedOccurredAt,
            String revisedResponsibleName,
            String revisedDescription,
            boolean revisedEvidenceRequired,
            boolean revisedAuthorizationRequired,
            Instant revisedAt,
            AuditActor actor) {

        requireNonNull(revisedType, "revisedType");
        requireNonNull(revisedAmount, "revisedAmount");
        requireUpdateInstant(revisedAt);

        if (revisedType == OperationalEventType.CANCELLATION) {
            throw new IllegalArgumentException(
                    "regular revision must not use cancellation type");
        }

        return new OperationalEvent(
                id,
                closeId,
                revisedType,
                revisedAmount,
                balanceEffectFor(
                        revisedType,
                        revisedAmount),
                null,
                revisedOccurredAt,
                registeredAt,
                revisedResponsibleName,
                revisedDescription,
                state,
                revisedEvidenceRequired,
                revisedAuthorizationRequired,
                nextRevision(),
                stateChangedAt,
                createdAt,
                createdBy,
                revisedAt,
                actor);
    }

    public OperationalEvent reviseCancellation(
            OperationalEvent reversedEvent,
            Instant revisedOccurredAt,
            String revisedResponsibleName,
            String revisedDescription,
            boolean revisedEvidenceRequired,
            boolean revisedAuthorizationRequired,
            Instant revisedAt,
            AuditActor actor) {

        requireNonNull(reversedEvent, "reversedEvent");
        requireUpdateInstant(revisedAt);

        validateCancellationReference(
                id,
                closeId,
                reversedEvent);

        return new OperationalEvent(
                id,
                closeId,
                OperationalEventType.CANCELLATION,
                reversedEvent.amount(),
                reversedEvent.balanceEffect().negate(),
                reversedEvent.id(),
                revisedOccurredAt,
                registeredAt,
                revisedResponsibleName,
                revisedDescription,
                state,
                revisedEvidenceRequired,
                revisedAuthorizationRequired,
                nextRevision(),
                stateChangedAt,
                createdAt,
                createdBy,
                revisedAt,
                actor);
    }

    public OperationalEvent recalculateFromRevisedOriginal(
            OperationalEvent revisedOriginal,
            Instant recalculatedAt,
            AuditActor actor) {

        requireNonNull(revisedOriginal, "revisedOriginal");
        requireUpdateInstant(recalculatedAt);

        if (eventType != OperationalEventType.CANCELLATION) {
            throw new IllegalArgumentException(
                    "only a cancellation can be recalculated");
        }

        if (!closeId.equals(revisedOriginal.closeId())) {
            throw new IllegalArgumentException(
                    "revised original must belong to the same close");
        }

        if (!reversedEventId.equals(revisedOriginal.id())) {
            throw new IllegalArgumentException(
                    "revised original does not match reversed event");
        }

        if (revisedOriginal.eventType()
                == OperationalEventType.CANCELLATION) {

            throw new IllegalArgumentException(
                    "cancellation must not reverse another cancellation");
        }

        return new OperationalEvent(
                id,
                closeId,
                eventType,
                revisedOriginal.amount(),
                revisedOriginal.balanceEffect().negate(),
                reversedEventId,
                occurredAt,
                registeredAt,
                responsibleName,
                description,
                state,
                evidenceRequired,
                authorizationRequired,
                nextRevision(),
                stateChangedAt,
                createdAt,
                createdBy,
                recalculatedAt,
                actor);
    }

    private static BigDecimal balanceEffectFor(
            OperationalEventType eventType,
            OperationalEventAmount amount) {

        return switch (eventType) {
            case INCOME -> amount.value();
            case EXPENSE, DISCOUNT -> amount.value().negate();
            case CANCELLATION ->
                    throw new IllegalArgumentException(
                            "cancellation requires a referenced event");
        };
    }

    private static void validateRegularBalanceEffect(
            OperationalEventType eventType,
            OperationalEventAmount amount,
            BigDecimal balanceEffect) {

        if (eventType == OperationalEventType.CANCELLATION) {
            return;
        }

        BigDecimal expectedEffect =
                balanceEffectFor(
                        eventType,
                        amount);

        if (balanceEffect.compareTo(expectedEffect) != 0) {
            throw new IllegalArgumentException(
                    "balance effect does not match event type");
        }
    }

    private static void validateCancellationReference(
            OperationalEventId cancellationId,
            OperationalCloseId cancellationCloseId,
            OperationalEvent reversedEvent) {

        if (cancellationId.equals(reversedEvent.id())) {
            throw new IllegalArgumentException(
                    "cancellation must not reference itself");
        }

        if (!cancellationCloseId.equals(
                reversedEvent.closeId())) {

            throw new IllegalArgumentException(
                    "reversed event must belong to the same close");
        }

        if (reversedEvent.eventType()
                == OperationalEventType.CANCELLATION) {

            throw new IllegalArgumentException(
                    "cancellation must not reverse another cancellation");
        }
    }

    private long nextRevision() {
        try {
            return Math.addExact(dataRevision, 1);
        }
        catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "data revision cannot be incremented",
                    exception);
        }
    }

    private void requireUpdateInstant(
            Instant revisedAt) {

        requireNonNull(revisedAt, "revisedAt");

        if (revisedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException(
                    "revision instant must not be before previous update");
        }
    }

    private static String requireText(
            String value,
            String fieldName) {

        if (value == null || value.isBlank()) {
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