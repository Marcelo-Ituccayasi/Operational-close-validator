package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.UUID;

/**
 * Input required to validate or revalidate an Operational Event.
 *
 * @param closeId owning Operational Close
 * @param eventId Operational Event to validate
 */
public record ValidateOperationalEventCommand(
        UUID closeId,
        UUID eventId) {
}