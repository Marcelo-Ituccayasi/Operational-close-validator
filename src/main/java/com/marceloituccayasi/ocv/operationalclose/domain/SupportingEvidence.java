package com.marceloituccayasi.ocv.operationalclose.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Supporting Evidence associated with an Operational Event.
 *
 * @param id stable evidence identifier
 * @param eventId owning Operational Event
 * @param evidenceType business evidence type
 * @param contentReference stored or opaque reference key
 * @param supportedAmount optional amount supported by the evidence
 * @param evidenceDate business date of the evidence
 * @param legibilityStatus informed legibility status
 * @param active whether the evidence remains active
 * @param revision evidence revision
 * @param createdAt creation instant
 * @param createdBy creation actor
 * @param updatedAt last update instant
 * @param updatedBy last update actor
 * @param deactivatedAt logical deactivation instant
 */
public record SupportingEvidence(
        SupportingEvidenceId id,
        OperationalEventId eventId,
        String evidenceType,
        String contentReference,
        BigDecimal supportedAmount,
        LocalDate evidenceDate,
        SupportingEvidenceLegibilityStatus legibilityStatus,
        boolean active,
        long revision,
        Instant createdAt,
        AuditActor createdBy,
        Instant updatedAt,
        AuditActor updatedBy,
        Instant deactivatedAt) {

    private static final int MAXIMUM_EVIDENCE_TYPE_LENGTH = 40;
    private static final int MAXIMUM_CONTENT_REFERENCE_LENGTH = 500;
    private static final int MAXIMUM_SUPPORTED_AMOUNT_SCALE = 4;
    private static final int MAXIMUM_SUPPORTED_AMOUNT_INTEGER_DIGITS = 15;

    private static final String STORED_REFERENCE_PREFIX =
            "stored:";

    private static final String OPAQUE_REFERENCE_PREFIX =
            "reference:";

    public SupportingEvidence {
        requireNonNull(
                id,
                "id");

        requireNonNull(
                eventId,
                "eventId");

        requireNonNull(
                evidenceDate,
                "evidenceDate");

        requireNonNull(
                legibilityStatus,
                "legibilityStatus");

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

        evidenceType =
                requireText(
                        evidenceType,
                        "evidenceType");

        contentReference =
                requireText(
                        contentReference,
                        "contentReference");

        if (evidenceType.length()
                > MAXIMUM_EVIDENCE_TYPE_LENGTH) {

            throw new IllegalArgumentException(
                    "evidence type must not exceed 40 characters");
        }

        if (contentReference.length()
                > MAXIMUM_CONTENT_REFERENCE_LENGTH) {

            throw new IllegalArgumentException(
                    "content reference must not exceed 500 characters");
        }

        if (!hasSupportedReferencePrefix(
                contentReference)) {

            throw new IllegalArgumentException(
                    "content reference must use stored or reference prefix");
        }

        requireReferenceValue(
                contentReference);

        validateSupportedAmount(
                supportedAmount);

        if (revision < 1) {
            throw new IllegalArgumentException(
                    "supporting evidence revision must be at least one");
        }

        if (updatedAt.isBefore(
                createdAt)) {

            throw new IllegalArgumentException(
                    "update instant must not be before creation");
        }

        if (active
                && deactivatedAt != null) {

            throw new IllegalArgumentException(
                    "active supporting evidence must not be deactivated");
        }

        if (!active
                && deactivatedAt == null) {

            throw new IllegalArgumentException(
                    "inactive supporting evidence must contain deactivation instant");
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

    public static SupportingEvidence create(
            SupportingEvidenceId id,
            OperationalEventId eventId,
            String evidenceType,
            String contentReference,
            BigDecimal supportedAmount,
            LocalDate evidenceDate,
            SupportingEvidenceLegibilityStatus legibilityStatus,
            Instant createdAt,
            AuditActor actor) {

        return new SupportingEvidence(
                id,
                eventId,
                evidenceType,
                contentReference,
                supportedAmount,
                evidenceDate,
                legibilityStatus,
                true,
                1L,
                createdAt,
                actor,
                createdAt,
                actor,
                null);
    }

    public SupportingEvidence deactivate(
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
                    "supporting evidence is already inactive");
        }

        if (deactivationAt.isBefore(
                updatedAt)) {

            throw new IllegalArgumentException(
                    "deactivation instant must not be before previous update");
        }

        return new SupportingEvidence(
                id,
                eventId,
                evidenceType,
                contentReference,
                supportedAmount,
                evidenceDate,
                legibilityStatus,
                false,
                nextRevision(),
                createdAt,
                createdBy,
                deactivationAt,
                actor,
                deactivationAt);
    }

    public boolean storedContent() {
        return contentReference.startsWith(
                STORED_REFERENCE_PREFIX);
    }

    public boolean referencedContent() {
        return contentReference.startsWith(
                OPAQUE_REFERENCE_PREFIX);
    }

    private long nextRevision() {
        try {
            return Math.addExact(
                    revision,
                    1L);
        }
        catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "supporting evidence revision cannot be incremented",
                    exception);
        }
    }

    private static void validateSupportedAmount(
            BigDecimal supportedAmount) {

        if (supportedAmount == null) {
            return;
        }

        if (supportedAmount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "supported amount must be positive");
        }

        if (supportedAmount.scale()
                > MAXIMUM_SUPPORTED_AMOUNT_SCALE) {

            throw new IllegalArgumentException(
                    "supported amount must not exceed four decimal places");
        }

        int integerDigits =
                supportedAmount.precision()
                        - supportedAmount.scale();

        if (integerDigits
                > MAXIMUM_SUPPORTED_AMOUNT_INTEGER_DIGITS) {

            throw new IllegalArgumentException(
                    "supported amount exceeds numeric(19,4)");
        }
    }

    private static boolean hasSupportedReferencePrefix(
            String contentReference) {

        return contentReference.startsWith(
                STORED_REFERENCE_PREFIX)
                || contentReference.startsWith(
                        OPAQUE_REFERENCE_PREFIX);
    }

    private static void requireReferenceValue(
            String contentReference) {

        int separatorIndex =
                contentReference.indexOf(
                        ':');

        if (separatorIndex < 0
                || separatorIndex
                        == contentReference.length() - 1
                || contentReference
                        .substring(
                                separatorIndex + 1)
                        .isBlank()) {

            throw new IllegalArgumentException(
                    "content reference value must not be blank");
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