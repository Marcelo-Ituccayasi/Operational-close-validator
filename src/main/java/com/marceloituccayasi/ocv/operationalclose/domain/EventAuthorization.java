package com.marceloituccayasi.ocv.operationalclose.domain;

import java.time.Instant;

/**
 * Formal Authorization associated with an Operational Event.
 *
 * @param id stable authorization identifier
 * @param eventId owning Operational Event
 * @param authorizedByName name of the authorizing person
 * @param reason business reason for the authorization
 * @param authorizedAt business authorization instant
 * @param formalReference formal business reference
 * @param active whether the authorization remains active
 * @param revision authorization revision
 * @param createdAt creation instant
 * @param createdBy creation actor
 * @param updatedAt last update instant
 * @param updatedBy last update actor
 * @param deactivatedAt logical deactivation instant
 */
public record EventAuthorization(
        EventAuthorizationId id,
        OperationalEventId eventId,
        String authorizedByName,
        String reason,
        Instant authorizedAt,
        String formalReference,
        boolean active,
        long revision,
        Instant createdAt,
        AuditActor createdBy,
        Instant updatedAt,
        AuditActor updatedBy,
        Instant deactivatedAt) {

    private static final int MAXIMUM_AUTHORIZED_BY_NAME_LENGTH =
            200;

    private static final int MAXIMUM_FORMAL_REFERENCE_LENGTH =
            500;

    public EventAuthorization {
        requireNonNull(
                id,
                "id");

        requireNonNull(
                eventId,
                "eventId");

        requireNonNull(
                authorizedAt,
                "authorizedAt");

        requireNonNull(
                createdAt,
                "createdAt");

        requireNonNull(
                createdBy,
                "createdBy");

        requireNonNull(
                updatedAt,
                "updatedAt");

        requireNonNull(
                updatedBy,
                "updatedBy");

        authorizedByName =
                requireText(
                        authorizedByName,
                        "authorizedByName");

        reason =
                requireText(
                        reason,
                        "reason");

        formalReference =
                requireText(
                        formalReference,
                        "formalReference");

        if (authorizedByName.length()
                > MAXIMUM_AUTHORIZED_BY_NAME_LENGTH) {

            throw new IllegalArgumentException(
                    "authorized-by name must not exceed 200 characters");
        }

        if (formalReference.length()
                > MAXIMUM_FORMAL_REFERENCE_LENGTH) {

            throw new IllegalArgumentException(
                    "formal reference must not exceed 500 characters");
        }

        if (revision < 1L) {
            throw new IllegalArgumentException(
                    "event authorization revision must be at least one");
        }

        if (updatedAt.isBefore(
                createdAt)) {

            throw new IllegalArgumentException(
                    "update instant must not be before creation");
        }

        if (active
                && deactivatedAt != null) {

            throw new IllegalArgumentException(
                    "active event authorization must not be deactivated");
        }

        if (!active
                && deactivatedAt == null) {

            throw new IllegalArgumentException(
                    "inactive event authorization must contain deactivation instant");
        }

        if (deactivatedAt != null
                && deactivatedAt.isBefore(
                        createdAt)) {

            throw new IllegalArgumentException(
                    "deactivation instant must not be before creation");
        }

        if (deactivatedAt != null
                && deactivatedAt.isAfter(
                        updatedAt)) {

            throw new IllegalArgumentException(
                    "deactivation instant must not be after last update");
        }
    }

    public static EventAuthorization create(
            EventAuthorizationId id,
            OperationalEventId eventId,
            String authorizedByName,
            String reason,
            Instant authorizedAt,
            String formalReference,
            Instant createdAt,
            AuditActor actor) {

        return new EventAuthorization(
                id,
                eventId,
                authorizedByName,
                reason,
                authorizedAt,
                formalReference,
                true,
                1L,
                createdAt,
                actor,
                createdAt,
                actor,
                null);
    }

    public EventAuthorization deactivate(
            Instant deactivationAt,
            AuditActor actor) {

        requireNonNull(
                deactivationAt,
                "deactivationAt");

        requireNonNull(
                actor,
                "actor");

        if (!active) {
            throw new IllegalStateException(
                    "event authorization is already inactive");
        }

        if (deactivationAt.isBefore(
                updatedAt)) {

            throw new IllegalArgumentException(
                    "deactivation instant must not be before previous update");
        }

        return new EventAuthorization(
                id,
                eventId,
                authorizedByName,
                reason,
                authorizedAt,
                formalReference,
                false,
                nextRevision(),
                createdAt,
                createdBy,
                deactivationAt,
                actor,
                deactivationAt);
    }

    private long nextRevision() {
        try {
            return Math.addExact(
                    revision,
                    1L);
        }
        catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "event authorization revision cannot be incremented",
                    exception);
        }
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