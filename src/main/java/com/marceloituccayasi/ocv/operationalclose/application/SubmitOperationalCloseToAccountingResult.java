package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueType;

/**
 * Explicit application result for the internal accounting submission.
 *
 * @param status operation status
 * @param submissionAttemptId persisted attempt for a confirmed operation
 * @param validationResultId persisted VR-008 Result for a confirmed operation
 * @param issueTypes structured causes for a business rejection
 * @param message safe result description when applicable
 */
public record SubmitOperationalCloseToAccountingResult(
        Status status,
        UUID submissionAttemptId,
        UUID validationResultId,
        List<SubmissionAttemptIssueType> issueTypes,
        String message) {

    public enum Status {
        SUBMITTED,
        SUBMISSION_REJECTED,
        INVALID_INPUT,
        ACTOR_REJECTED,
        CLOSE_NOT_FOUND,
        CLOSE_NOT_SUBMITTABLE,
        CLOSE_ALREADY_SUBMITTED
    }

    public SubmitOperationalCloseToAccountingResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        Objects.requireNonNull(
                issueTypes,
                "issueTypes must not be null");

        issueTypes =
                issueTypes.stream()
                        .map(
                                issueType ->
                                        Objects.requireNonNull(
                                                issueType,
                                                "issueTypes must not "
                                                        + "contain null values"))
                        .distinct()
                        .sorted(
                                Comparator.comparing(
                                        Enum::name))
                        .toList();

        if (status == Status.SUBMITTED) {
            requireConfirmedIdentifiers(
                    submissionAttemptId,
                    validationResultId);

            if (!issueTypes.isEmpty()
                    || message != null) {

                throw new IllegalArgumentException(
                        "submitted result must not contain "
                                + "rejection information");
            }
        }
        else if (status == Status.SUBMISSION_REJECTED) {
            requireConfirmedIdentifiers(
                    submissionAttemptId,
                    validationResultId);

            if (issueTypes.isEmpty()) {
                throw new IllegalArgumentException(
                        "rejected result requires issue types");
            }

            message =
                    requireText(
                            message,
                            "message");
        }
        else {
            if (submissionAttemptId != null
                    || validationResultId != null
                    || !issueTypes.isEmpty()) {

                throw new IllegalArgumentException(
                        "unconfirmed operation must not contain "
                                + "persisted result information");
            }

            message =
                    requireText(
                            message,
                            "message");
        }
    }

    public static SubmitOperationalCloseToAccountingResult submitted(
            UUID submissionAttemptId,
            UUID validationResultId) {

        return new SubmitOperationalCloseToAccountingResult(
                Status.SUBMITTED,
                Objects.requireNonNull(
                        submissionAttemptId),
                Objects.requireNonNull(
                        validationResultId),
                List.of(),
                null);
    }

    public static SubmitOperationalCloseToAccountingResult rejected(
            UUID submissionAttemptId,
            UUID validationResultId,
            List<SubmissionAttemptIssueType> issueTypes) {

        return new SubmitOperationalCloseToAccountingResult(
                Status.SUBMISSION_REJECTED,
                Objects.requireNonNull(
                        submissionAttemptId),
                Objects.requireNonNull(
                        validationResultId),
                issueTypes,
                "VR-008 rejected the accounting submission.");
    }

    public static SubmitOperationalCloseToAccountingResult invalidInput(
            String message) {

        return unsuccessful(
                Status.INVALID_INPUT,
                message);
    }

    public static SubmitOperationalCloseToAccountingResult actorRejected() {
        return unsuccessful(
                Status.ACTOR_REJECTED,
                "The authenticated actor cannot perform this operation.");
    }

    public static SubmitOperationalCloseToAccountingResult closeNotFound() {
        return unsuccessful(
                Status.CLOSE_NOT_FOUND,
                "The requested Operational Close does not exist.");
    }

    public static SubmitOperationalCloseToAccountingResult
            closeNotSubmittable() {

        return unsuccessful(
                Status.CLOSE_NOT_SUBMITTABLE,
                "The Operational Close must be in VALIDATED state.");
    }

    public static SubmitOperationalCloseToAccountingResult
            closeAlreadySubmitted() {

        return unsuccessful(
                Status.CLOSE_ALREADY_SUBMITTED,
                "The Operational Close was already sent to accounting.");
    }

    private static SubmitOperationalCloseToAccountingResult unsuccessful(
            Status status,
            String message) {

        return new SubmitOperationalCloseToAccountingResult(
                status,
                null,
                null,
                List.of(),
                message);
    }

    private static void requireConfirmedIdentifiers(
            UUID submissionAttemptId,
            UUID validationResultId) {

        Objects.requireNonNull(
                submissionAttemptId,
                "confirmed result requires submissionAttemptId");

        Objects.requireNonNull(
                validationResultId,
                "confirmed result requires validationResultId");
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

}