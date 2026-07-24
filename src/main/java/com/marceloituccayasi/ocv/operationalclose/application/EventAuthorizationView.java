package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorization;

/**
 * Presentation-safe Event Authorization view.
 *
 * @param id authorization identifier
 * @param authorizedByName business authorizer name
 * @param reason authorization reason
 * @param authorizedAt business authorization instant
 * @param formalReference opaque formal reference
 * @param active whether the authorization participates in the current workflow
 * @param revision authorization revision
 * @param createdAt creation instant
 * @param deactivatedAt deactivation instant when historical
 */
public record EventAuthorizationView(
        UUID id,
        String authorizedByName,
        String reason,
        Instant authorizedAt,
        String formalReference,
        boolean active,
        long revision,
        Instant createdAt,
        Instant deactivatedAt) {

    public EventAuthorizationView {
        Objects.requireNonNull(
                id,
                "id must not be null");

        Objects.requireNonNull(
                authorizedByName,
                "authorizedByName must not be null");

        Objects.requireNonNull(
                reason,
                "reason must not be null");

        Objects.requireNonNull(
                authorizedAt,
                "authorizedAt must not be null");

        Objects.requireNonNull(
                formalReference,
                "formalReference must not be null");

        Objects.requireNonNull(
                createdAt,
                "createdAt must not be null");

        if (revision < 1L) {
            throw new IllegalArgumentException(
                    "revision must be positive");
        }

        if (active
                && deactivatedAt != null) {

            throw new IllegalArgumentException(
                    "active authorization must not contain deactivatedAt");
        }

        if (!active
                && deactivatedAt == null) {

            throw new IllegalArgumentException(
                    "inactive authorization must contain deactivatedAt");
        }
    }

    public static EventAuthorizationView fromDomain(
            EventAuthorization authorization) {

        Objects.requireNonNull(
                authorization,
                "authorization must not be null");

        return new EventAuthorizationView(
                authorization.id()
                        .value(),
                authorization.authorizedByName(),
                authorization.reason(),
                authorization.authorizedAt(),
                authorization.formalReference(),
                authorization.active(),
                authorization.revision(),
                authorization.createdAt(),
                authorization.deactivatedAt());
    }

}