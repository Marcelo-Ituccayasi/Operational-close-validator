package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.UUID;

/**
 * Input required to deactivate Supporting Evidence.
 *
 * @param closeId owning Operational Close
 * @param eventId owning Operational Event
 * @param evidenceId Supporting Evidence to deactivate
 */
public record DeactivateSupportingEvidenceCommand(
        UUID closeId,
        UUID eventId,
        UUID evidenceId) {
}