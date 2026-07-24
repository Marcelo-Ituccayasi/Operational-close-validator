package com.marceloituccayasi.ocv.operationalclose.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;

/**
 * Presentation-safe Supporting Evidence view.
 *
 * @param id evidence identifier
 * @param evidenceType business evidence type
 * @param contentReference stored or opaque business reference
 * @param managedContent whether application-managed binary content exists
 * @param supportedAmount optional supported amount
 * @param evidenceDate business evidence date
 * @param legibilityStatus legibility status
 * @param active whether the evidence participates in the current workflow
 * @param revision evidence revision
 * @param createdAt creation instant
 * @param deactivatedAt deactivation instant when historical
 */
public record SupportingEvidenceView(
        UUID id,
        String evidenceType,
        String contentReference,
        boolean managedContent,
        BigDecimal supportedAmount,
        LocalDate evidenceDate,
        String legibilityStatus,
        boolean active,
        long revision,
        Instant createdAt,
        Instant deactivatedAt) {

    private static final String MANAGED_CONTENT_PREFIX =
            "stored:evidence/";

    public SupportingEvidenceView {
        Objects.requireNonNull(
                id,
                "id must not be null");

        Objects.requireNonNull(
                evidenceType,
                "evidenceType must not be null");

        Objects.requireNonNull(
                contentReference,
                "contentReference must not be null");

        Objects.requireNonNull(
                evidenceDate,
                "evidenceDate must not be null");

        Objects.requireNonNull(
                legibilityStatus,
                "legibilityStatus must not be null");

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
                    "active evidence must not contain deactivatedAt");
        }

        if (!active
                && deactivatedAt == null) {

            throw new IllegalArgumentException(
                    "inactive evidence must contain deactivatedAt");
        }
    }

    public static SupportingEvidenceView fromDomain(
            SupportingEvidence evidence) {

        Objects.requireNonNull(
                evidence,
                "evidence must not be null");

        return new SupportingEvidenceView(
                evidence.id()
                        .value(),
                evidence.evidenceType(),
                evidence.contentReference(),
                evidence.contentReference()
                        .startsWith(
                                MANAGED_CONTENT_PREFIX),
                evidence.supportedAmount(),
                evidence.evidenceDate(),
                evidence.legibilityStatus()
                        .name(),
                evidence.active(),
                evidence.revision(),
                evidence.createdAt(),
                evidence.deactivatedAt());
    }

}