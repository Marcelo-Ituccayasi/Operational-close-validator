package com.marceloituccayasi.ocv.operationalclose.domain;

import java.time.Instant;

/**
 * Operational Close aggregate root.
 *
 * @param id stable identifier
 * @param period covered business period
 * @param currencyCode single currency used by the close
 * @param initialBalance initial balance
 * @param state current state
 * @param stateChangedAt instant of the current state
 * @param createdAt creation instant
 * @param createdBy creation actor
 * @param updatedAt last update instant
 * @param updatedBy last update actor
 */
public record OperationalClose(
        OperationalCloseId id,
        OperationalPeriod period,
        CurrencyCode currencyCode,
        InitialBalance initialBalance,
        OperationalCloseState state,
        Instant stateChangedAt,
        Instant createdAt,
        AuditActor createdBy,
        Instant updatedAt,
        AuditActor updatedBy) {

    public OperationalClose {
        requireNonNull(id, "id");
        requireNonNull(period, "period");
        requireNonNull(currencyCode, "currencyCode");
        requireNonNull(initialBalance, "initialBalance");
        requireNonNull(state, "state");
        requireNonNull(stateChangedAt, "stateChangedAt");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(createdBy, "createdBy");
        requireNonNull(updatedAt, "updatedAt");
        requireNonNull(updatedBy, "updatedBy");

        if (stateChangedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "state change instant must not be before creation");
        }

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "update instant must not be before creation");
        }
    }

    public static OperationalClose create(
            OperationalCloseId id,
            OperationalPeriod period,
            CurrencyCode currencyCode,
            InitialBalance initialBalance,
            Instant createdAt,
            AuditActor actor) {

        return new OperationalClose(
                id,
                period,
                currencyCode,
                initialBalance,
                OperationalCloseState.PREPARATION,
                createdAt,
                createdAt,
                actor,
                createdAt,
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
