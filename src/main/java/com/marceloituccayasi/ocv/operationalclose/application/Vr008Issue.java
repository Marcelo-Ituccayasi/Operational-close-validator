package com.marceloituccayasi.ocv.operationalclose.application;

import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueType;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;

/**
 * Structured cause produced by the VR-008 evaluator before persistence.
 *
 * @param issueType classified rejection cause
 * @param eventId affected Event, when applicable
 * @param alertId affected blocking Alert, when applicable
 * @param validationResultId affected Validation Result, when applicable
 * @param consolidationId affected Consolidation, when applicable
 * @param detail sanitized explanation
 */
public record Vr008Issue(
        SubmissionAttemptIssueType issueType,
        OperationalEventId eventId,
        ValidationAlertId alertId,
        ValidationResultId validationResultId,
        ConsolidationId consolidationId,
        String detail) {

    public Vr008Issue {
        requireNonNull(
                issueType,
                "issueType");

        detail =
                requireText(
                        detail,
                        "detail");

        validateRequiredReference(
                issueType,
                eventId,
                alertId,
                validationResultId,
                consolidationId);
    }

    private static void validateRequiredReference(
            SubmissionAttemptIssueType issueType,
            OperationalEventId eventId,
            ValidationAlertId alertId,
            ValidationResultId validationResultId,
            ConsolidationId consolidationId) {

        switch (issueType) {
            case EVENT_NOT_VALIDATED ->
                    requireNonNull(
                            eventId,
                            "eventId");

            case BLOCKING_ALERT ->
                    requireNonNull(
                            alertId,
                            "alertId");

            case VALIDATION_RESULT_FAILED,
                    VALIDATION_RESULT_STALE ->
                    requireNonNull(
                            validationResultId,
                            "validationResultId");

            case CONSOLIDATION_MISSING -> {
                if (consolidationId != null) {
                    throw new IllegalArgumentException(
                            "missing consolidation issue must not "
                                    + "reference a consolidation");
                }
            }

            case CONSOLIDATION_STALE ->
                    requireNonNull(
                            consolidationId,
                            "consolidationId");

            case OTHER_CRITICAL_INCONSISTENCY -> {
                // A detailed explanation is sufficient for this fallback.
            }
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