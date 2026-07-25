package com.marceloituccayasi.ocv.operationalclose.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Input required to create Supporting Evidence whose binary content is managed
 * by the application.
 *
 * @param closeId owning Operational Close
 * @param eventId owning Operational Event
 * @param evidenceType business evidence type
 * @param content binary content to store before the database transaction
 * @param extension identified canonical file extension
 * @param supportedAmount optional supported amount
 * @param evidenceDate business evidence date
 * @param legibilityStatus requested legibility status
 */
public record CreateSupportingEvidenceWithStoredContentCommand(
        UUID closeId,
        UUID eventId,
        String evidenceType,
        byte[] content,
        String extension,
        BigDecimal supportedAmount,
        LocalDate evidenceDate,
        String legibilityStatus) {

    public CreateSupportingEvidenceWithStoredContentCommand {
        if (content != null) {
            content =
                    content.clone();
        }
    }

    @Override
    public byte[] content() {
        if (content == null) {
            return null;
        }

        return content.clone();
    }

}