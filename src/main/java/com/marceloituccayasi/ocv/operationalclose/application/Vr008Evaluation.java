package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueType;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;

/**
 * Deterministic in-memory result of evaluating VR-008.
 *
 * @param outcome satisfied or failed final-control outcome
 * @param detail sanitized summary
 * @param consolidationId evaluated Consolidation, when one exists
 * @param issues structured rejection causes
 */
public record Vr008Evaluation(
        ValidationOutcome outcome,
        String detail,
        ConsolidationId consolidationId,
        List<Vr008Issue> issues) {

    private static final Comparator<UUID> NULLABLE_UUID_COMPARATOR =
            Comparator.nullsLast(
                    Comparator.naturalOrder());

    public Vr008Evaluation {
        requireNonNull(
                outcome,
                "outcome");

        detail =
                requireText(
                        detail,
                        "detail");

        requireNonNull(
                issues,
                "issues");

        for (Vr008Issue issue : issues) {
            requireNonNull(
                    issue,
                    "issue");
        }

        issues =
                issues.stream()
                        .sorted(
                                issueComparator())
                        .toList();

        validateOutcome(
                outcome,
                consolidationId,
                issues);
    }

    public static Vr008Evaluation satisfied(
            ConsolidationId consolidationId) {

        return new Vr008Evaluation(
                ValidationOutcome.SATISFIED,
                "VR-008 satisfied. The Operational Close is ready "
                        + "for accounting submission.",
                consolidationId,
                List.of());
    }

    public static Vr008Evaluation failed(
            ConsolidationId consolidationId,
            List<Vr008Issue> issues) {

        return new Vr008Evaluation(
                ValidationOutcome.FAILED,
                "VR-008 failed. The Operational Close must not be "
                        + "sent to accounting.",
                consolidationId,
                issues);
    }

    public boolean satisfied() {
        return outcome
                == ValidationOutcome.SATISFIED;
    }

    private static void validateOutcome(
            ValidationOutcome outcome,
            ConsolidationId consolidationId,
            List<Vr008Issue> issues) {

        boolean missingConsolidation =
                issues.stream()
                        .anyMatch(
                                issue ->
                                        issue.issueType()
                                                == SubmissionAttemptIssueType
                                                        .CONSOLIDATION_MISSING);

        if (outcome
                == ValidationOutcome.SATISFIED) {

            if (consolidationId == null) {
                throw new IllegalArgumentException(
                        "satisfied VR-008 evaluation "
                                + "requires a consolidation");
            }

            if (!issues.isEmpty()) {
                throw new IllegalArgumentException(
                        "satisfied VR-008 evaluation "
                                + "must not contain issues");
            }

            return;
        }

        if (issues.isEmpty()) {
            throw new IllegalArgumentException(
                    "failed VR-008 evaluation "
                            + "requires at least one issue");
        }

        if (consolidationId == null
                && !missingConsolidation) {

            throw new IllegalArgumentException(
                    "failed VR-008 evaluation without consolidation "
                            + "requires a missing consolidation issue");
        }

        if (consolidationId != null
                && missingConsolidation) {

            throw new IllegalArgumentException(
                    "failed VR-008 evaluation with consolidation "
                            + "must not claim it is missing");
        }
    }

    private static Comparator<Vr008Issue> issueComparator() {
        return Comparator
                .comparing(
                        (Vr008Issue issue) ->
                                issue.issueType()
                                        .name())
                .thenComparing(
                        issue ->
                                issue.eventId() == null
                                        ? null
                                        : issue.eventId().value(),
                        NULLABLE_UUID_COMPARATOR)
                .thenComparing(
                        issue ->
                                issue.alertId() == null
                                        ? null
                                        : issue.alertId().value(),
                        NULLABLE_UUID_COMPARATOR)
                .thenComparing(
                        issue ->
                                issue.validationResultId() == null
                                        ? null
                                        : issue.validationResultId().value(),
                        NULLABLE_UUID_COMPARATOR)
                .thenComparing(
                        issue ->
                                issue.consolidationId() == null
                                        ? null
                                        : issue.consolidationId().value(),
                        NULLABLE_UUID_COMPARATOR)
                .thenComparing(
                        Vr008Issue::detail);
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