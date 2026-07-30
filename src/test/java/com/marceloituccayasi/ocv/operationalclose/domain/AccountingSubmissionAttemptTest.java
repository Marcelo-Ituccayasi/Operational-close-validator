package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AccountingSubmissionAttemptTest {

    private static final AccountingSubmissionAttemptId ATTEMPT_ID =
            attemptId(
                    "93000000-0000-0000-0000-000000000001");

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    uuid(
                            "93000000-0000-0000-0000-000000000002"));

    private static final ValidationResultId RESULT_ID =
            new ValidationResultId(
                    uuid(
                            "93000000-0000-0000-0000-000000000003"));

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    uuid(
                            "93000000-0000-0000-0000-000000000004"));

    private static final Instant ATTEMPTED_AT =
            Instant.parse(
                    "2026-07-30T14:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Test
    void createsSuccessfulAttemptWithConsolidationAndNoIssues() {
        AccountingSubmissionAttempt attempt =
                AccountingSubmissionAttempt.succeeded(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        CONSOLIDATION_ID,
                        ATTEMPTED_AT,
                        ACTOR,
                        "  Close sent to accounting.  ");

        assertThat(
                attempt.outcome())
                .isEqualTo(
                        AccountingSubmissionOutcome.SUCCEEDED);

        assertThat(
                attempt.consolidationId())
                .isEqualTo(
                        CONSOLIDATION_ID);

        assertThat(
                attempt.summary())
                .isEqualTo(
                        "Close sent to accounting.");

        assertThat(
                attempt.issues())
                .isEmpty();

        assertThat(
                attempt.isSuccessful())
                .isTrue();

        assertThat(
                attempt.isRejected())
                .isFalse();
    }

    @Test
    void createsRejectedAttemptWithImmutableSortedIssues() {
        SubmissionAttemptIssue secondIssue =
                otherIssue(
                        ATTEMPT_ID,
                        issueId(
                                "93000000-0000-0000-0000-000000000012"));

        SubmissionAttemptIssue firstIssue =
                otherIssue(
                        ATTEMPT_ID,
                        issueId(
                                "93000000-0000-0000-0000-000000000011"));

        AccountingSubmissionAttempt attempt =
                AccountingSubmissionAttempt.rejected(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        CONSOLIDATION_ID,
                        ATTEMPTED_AT,
                        ACTOR,
                        "Submission rejected.",
                        List.of(
                                secondIssue,
                                firstIssue));

        assertThat(
                attempt.issues())
                .extracting(
                        SubmissionAttemptIssue::id)
                .containsExactly(
                        firstIssue.id(),
                        secondIssue.id());

        assertThatThrownBy(
                () -> attempt.issues()
                        .clear())
                .isInstanceOf(
                        UnsupportedOperationException.class);

        assertThat(
                attempt.isRejected())
                .isTrue();
    }

    @Test
    void successfulAttemptRequiresConsolidation() {
        assertThatThrownBy(
                () -> AccountingSubmissionAttempt.succeeded(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        null,
                        ATTEMPTED_AT,
                        ACTOR,
                        "Close sent."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "successful submission requires a consolidation");
    }

    @Test
    void successfulAttemptMustNotContainIssues() {
        assertThatThrownBy(
                () -> new AccountingSubmissionAttempt(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        CONSOLIDATION_ID,
                        AccountingSubmissionOutcome.SUCCEEDED,
                        ATTEMPTED_AT,
                        ACTOR,
                        "Close sent.",
                        List.of(
                                otherIssue(
                                        ATTEMPT_ID,
                                        issueId(
                                                "93000000-0000-0000-0000-000000000021")))))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "successful submission must not contain issues");
    }

    @Test
    void rejectedAttemptRequiresAtLeastOneIssue() {
        assertThatThrownBy(
                () -> AccountingSubmissionAttempt.rejected(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        CONSOLIDATION_ID,
                        ATTEMPTED_AT,
                        ACTOR,
                        "Submission rejected.",
                        List.of()))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "rejected submission requires at least one issue");
    }

    @Test
    void rejectionWithoutConsolidationRequiresMissingCause() {
        assertThatThrownBy(
                () -> AccountingSubmissionAttempt.rejected(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        null,
                        ATTEMPTED_AT,
                        ACTOR,
                        "Submission rejected.",
                        List.of(
                                otherIssue(
                                        ATTEMPT_ID,
                                        issueId(
                                                "93000000-0000-0000-0000-000000000031")))))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "rejection without consolidation requires "
                                + "a missing consolidation issue");
    }

    @Test
    void permitsRejectionWithoutConsolidationWithMissingCause() {
        SubmissionAttemptIssue missingIssue =
                new SubmissionAttemptIssue(
                        issueId(
                                "93000000-0000-0000-0000-000000000041"),
                        ATTEMPT_ID,
                        SubmissionAttemptIssueType.CONSOLIDATION_MISSING,
                        null,
                        null,
                        null,
                        null,
                        "No current Consolidation exists.");

        AccountingSubmissionAttempt attempt =
                AccountingSubmissionAttempt.rejected(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        null,
                        ATTEMPTED_AT,
                        ACTOR,
                        null,
                        List.of(
                                missingIssue));

        assertThat(
                attempt.consolidationId())
                .isNull();

        assertThat(
                attempt.issues())
                .containsExactly(
                        missingIssue);
    }

    @Test
    void rejectionWithConsolidationMustNotClaimItIsMissing() {
        SubmissionAttemptIssue missingIssue =
                new SubmissionAttemptIssue(
                        issueId(
                                "93000000-0000-0000-0000-000000000051"),
                        ATTEMPT_ID,
                        SubmissionAttemptIssueType.CONSOLIDATION_MISSING,
                        null,
                        null,
                        null,
                        null,
                        "No current Consolidation exists.");

        assertThatThrownBy(
                () -> AccountingSubmissionAttempt.rejected(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        CONSOLIDATION_ID,
                        ATTEMPTED_AT,
                        ACTOR,
                        "Submission rejected.",
                        List.of(
                                missingIssue)))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "rejection with consolidation must not contain "
                                + "a missing consolidation issue");
    }

    @Test
    void issueMustBelongToAttempt() {
        AccountingSubmissionAttemptId otherAttemptId =
                attemptId(
                        "93000000-0000-0000-0000-000000000061");

        assertThatThrownBy(
                () -> AccountingSubmissionAttempt.rejected(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        CONSOLIDATION_ID,
                        ATTEMPTED_AT,
                        ACTOR,
                        "Submission rejected.",
                        List.of(
                                otherIssue(
                                        otherAttemptId,
                                        issueId(
                                                "93000000-0000-0000-0000-000000000062")))))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "submission issue must belong to the attempt");
    }

    @Test
    void attemptMustNotContainDuplicateIssueIdentifiers() {
        SubmissionAttemptIssueId issueId =
                issueId(
                        "93000000-0000-0000-0000-000000000071");

        assertThatThrownBy(
                () -> AccountingSubmissionAttempt.rejected(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        CONSOLIDATION_ID,
                        ATTEMPTED_AT,
                        ACTOR,
                        "Submission rejected.",
                        List.of(
                                otherIssue(
                                        ATTEMPT_ID,
                                        issueId),
                                otherIssue(
                                        ATTEMPT_ID,
                                        issueId))))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "submission attempt must not contain duplicate issues");
    }

    @Test
    void issueConsolidationMustMatchEvaluatedConsolidation() {
        ConsolidationId otherConsolidationId =
                new ConsolidationId(
                        uuid(
                                "93000000-0000-0000-0000-000000000081"));

        SubmissionAttemptIssue staleIssue =
                new SubmissionAttemptIssue(
                        issueId(
                                "93000000-0000-0000-0000-000000000082"),
                        ATTEMPT_ID,
                        SubmissionAttemptIssueType.CONSOLIDATION_STALE,
                        null,
                        null,
                        null,
                        otherConsolidationId,
                        "The evaluated Consolidation is stale.");

        assertThatThrownBy(
                () -> AccountingSubmissionAttempt.rejected(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        CONSOLIDATION_ID,
                        ATTEMPTED_AT,
                        ACTOR,
                        "Submission rejected.",
                        List.of(
                                staleIssue)))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "submission issue consolidation must match "
                                + "the evaluated consolidation");
    }

    @Test
    void blankOptionalSummaryIsNormalizedToNull() {
        AccountingSubmissionAttempt attempt =
                AccountingSubmissionAttempt.succeeded(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        CONSOLIDATION_ID,
                        ATTEMPTED_AT,
                        ACTOR,
                        "   ");

        assertThat(
                attempt.summary())
                .isNull();
    }

    @Test
    void identifiersRejectNullValues() {
        assertThatThrownBy(
                () -> new AccountingSubmissionAttemptId(
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "accounting submission attempt id must not be null");

        assertThatThrownBy(
                () -> new SubmissionAttemptIssueId(
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "submission attempt issue id must not be null");
    }

    private static SubmissionAttemptIssue otherIssue(
            AccountingSubmissionAttemptId attemptId,
            SubmissionAttemptIssueId issueId) {

        return new SubmissionAttemptIssue(
                issueId,
                attemptId,
                SubmissionAttemptIssueType
                        .OTHER_CRITICAL_INCONSISTENCY,
                null,
                null,
                null,
                null,
                "A critical consistency condition failed.");
    }

    private static AccountingSubmissionAttemptId attemptId(
            String value) {

        return new AccountingSubmissionAttemptId(
                uuid(
                        value));
    }

    private static SubmissionAttemptIssueId issueId(
            String value) {

        return new SubmissionAttemptIssueId(
                uuid(
                        value));
    }

    private static UUID uuid(
            String value) {

        return UUID.fromString(
                value);
    }

}