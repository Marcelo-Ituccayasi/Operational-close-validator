package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Input required to create an Event Authorization.
 *
 * @param closeId owning Operational Close
 * @param eventId owning Operational Event
 * @param authorizedByName name of the authorizing person
 * @param reason business reason for the authorization
 * @param authorizedAt business authorization instant
 * @param formalReference opaque formal business reference
 */
public record CreateEventAuthorizationCommand(
        UUID closeId,
        UUID eventId,
        String authorizedByName,
        String reason,
        Instant authorizedAt,
        String formalReference) {
}