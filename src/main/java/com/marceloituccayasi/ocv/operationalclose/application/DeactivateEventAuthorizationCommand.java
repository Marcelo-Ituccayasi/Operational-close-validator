package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.UUID;

/**
 * Input required to deactivate an Event Authorization.
 *
 * @param closeId owning Operational Close
 * @param eventId owning Operational Event
 * @param authorizationId Event Authorization to deactivate
 */
public record DeactivateEventAuthorizationCommand(
        UUID closeId,
        UUID eventId,
        UUID authorizationId) {
}