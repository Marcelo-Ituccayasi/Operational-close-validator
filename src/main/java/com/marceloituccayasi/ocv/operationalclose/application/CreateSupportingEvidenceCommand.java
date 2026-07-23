package com.marceloituccayasi.ocv.operationalclose.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Input required to create Supporting Evidence for an Operational Event.
 *
 * @param closeId owning Operational Close
 * @param eventId owning Operational Event
 * @param evidenceType business evidence type
 * @param contentReference stored or opaque content reference
 * @param supportedAmount optional supported amount
 * @param evidenceDate business evidence date
 * @param legibilityStatus requested legibility status
 */
public record CreateSupportingEvidenceCommand(
        UUID closeId,
        UUID eventId,
        String evidenceType,
        String contentReference,
        BigDecimal supportedAmount,
        LocalDate evidenceDate,
        String legibilityStatus) {
}