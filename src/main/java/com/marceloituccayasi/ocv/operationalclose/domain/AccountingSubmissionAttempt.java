package com.marceloituccayasi.ocv.operationalclose.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable confirmed result of one accounting submission command.
 *
 * @param id stable attempt identifier
 * @param closeId evaluated Operational Close
 * @param vr008ResultId persisted final Validation Result
 * @param consolidationId evaluated Consolidation, when it exists
 * @param outcome succeeded or rejected business result
 * @param attemptedAt command evaluation instant
 * @param attemptedBy authenticated responsible user
 * @param summary optional sanitized summary
 * @param issues structured rejection causes
 */
public record AccountingSubmissionAttempt(
        AccountingSubmissionAttemptId id,
        OperationalCloseId closeId,
        ValidationResultId vr008ResultId,
        ConsolidationId consolidationId,
        AccountingSubmissionOutcome outcome,
        Instant attemptedAt,
        AuditActor attemptedBy,
        String summary,
        List<SubmissionAttemptIssue> issues) {

    public AccountingSubmissionAttempt {
        requireNonNull(
                id,
                "id");

        requireNonNull(
                closeId,
                "closeId");

        requireNonNull(
                vr008ResultId,
                "vr008ResultId");

        requireNonNull(
                outcome,
                "outcome");

        requireNonNull(
                attemptedAt,
                "attemptedAt");

        requireNonNull(
                attemptedBy,
                "attemptedBy");

        requireNonNull(
                issues,
                "issues");

        summary =
                normalizeOptionalText(
                        summary);

        for (SubmissionAttemptIssue issue
                : issues) {

            requireNonNull(
                    issue,
                    "issue");
        }

        issues =
                issues.stream()
                        .sorted(
                                Comparator.comparing(
                                        issue ->
                                                issue.id()
                                                        .value()))
                        .toList();

        validateIssueOwnershipAndUniqueness(
                id,
                consolidationId,
                issues);

        validateOutcome(
                consolidationId,
                outcome,
                issues);
    }

    public static AccountingSubmissionAttempt succeeded(
            AccountingSubmissionAttemptId id,
            OperationalCloseId closeId,
            ValidationResultId vr008ResultId,
            ConsolidationId consolidationId,
            Instant attemptedAt,
            AuditActor attemptedBy,
            String summary) {

        return new AccountingSubmissionAttempt(
                id,
                closeId,
                vr008ResultId,
                consolidationId,
                AccountingSubmissionOutcome.SUCCEEDED,
                attemptedAt,
                attemptedBy,
                summary,
                List.of());
    }

    public static AccountingSubmissionAttempt rejected(
            AccountingSubmissionAttemptId id,
            OperationalCloseId closeId,
            ValidationResultId vr008ResultId,
            ConsolidationId consolidationId,
            Instant attemptedAt,
            AuditActor attemptedBy,
            String summary,
            List<SubmissionAttemptIssue> issues) {

        return new AccountingSubmissionAttempt(
                id,
                closeId,
                vr008ResultId,
                consolidationId,
                AccountingSubmissionOutcome.REJECTED,
                attemptedAt,
                attemptedBy,
                summary,
                issues);
    }

    public boolean isSuccessful() {
        return outcome
                == AccountingSubmissionOutcome.SUCCEEDED;
    }

    public boolean isRejected() {
        return outcome
                == AccountingSubmissionOutcome.REJECTED;
    }

    private static void validateIssueOwnershipAndUniqueness(
            AccountingSubmissionAttemptId attemptId,
            ConsolidationId consolidationId,
            List<SubmissionAttemptIssue> issues) {

        Set<SubmissionAttemptIssueId> issueIds =
                new HashSet<>();

        for (SubmissionAttemptIssue issue : issues) {
            if (!attemptId.equals(
                    issue.submissionAttemptId())) {

                throw new IllegalArgumentException(
                        "submission issue must belong to the attempt");
            }

            if (!issueIds.add(
                    issue.id())) {

                throw new IllegalArgumentException(
                        "submission attempt must not contain duplicate issues");
            }

            if (issue.consolidationId() != null
                    && !issue.consolidationId().equals(
                            consolidationId)) {

                throw new IllegalArgumentException(
                        "submission issue consolidation must match "
                                + "the evaluated consolidation");
            }
        }
    }

    private static void validateOutcome(
            ConsolidationId consolidationId,
            AccountingSubmissionOutcome outcome,
            List<SubmissionAttemptIssue> issues) {

        boolean consolidationMissingIssue =
                issues.stream()
                        .anyMatch(
                                issue ->
                                        issue.issueType()
                                                == SubmissionAttemptIssueType
                                                        .CONSOLIDATION_MISSING);

        if (outcome
                == AccountingSubmissionOutcome.SUCCEEDED) {

            if (consolidationId == null) {
                throw new IllegalArgumentException(
                        "successful submission requires a consolidation");
            }

            if (!issues.isEmpty()) {
                throw new IllegalArgumentException(
                        "successful submission must not contain issues");
            }

            return;
        }

        if (issues.isEmpty()) {
            throw new IllegalArgumentException(
                    "rejected submission requires at least one issue");
        }

        if (consolidationId == null
                && !consolidationMissingIssue) {

            throw new IllegalArgumentException(
                    "rejection without consolidation requires "
                            + "a missing consolidation issue");
        }

        if (consolidationId != null
                && consolidationMissingIssue) {

            throw new IllegalArgumentException(
                    "rejection with consolidation must not contain "
                            + "a missing consolidation issue");
        }
    }

    private static String normalizeOptionalText(
            String value) {

        if (value == null
                || value.isBlank()) {

            return null;
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