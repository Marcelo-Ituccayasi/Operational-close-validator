package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class SubmissionAttemptIssueTest {

    private static final AccountingSubmissionAttemptId ATTEMPT_ID =
            new AccountingSubmissionAttemptId(
                    uuid(
                            "92000000-0000-0000-0000-000000000001"));

    private static final SubmissionAttemptIssueId ISSUE_ID =
            new SubmissionAttemptIssueId(
                    uuid(
                            "92000000-0000-0000-0000-000000000002"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    uuid(
                            "92000000-0000-0000-0000-000000000003"));

    private static final ValidationAlertId ALERT_ID =
            new ValidationAlertId(
                    uuid(
                            "92000000-0000-0000-0000-000000000004"));

    private static final ValidationResultId RESULT_ID =
            new ValidationResultId(
                    uuid(
                            "92000000-0000-0000-0000-000000000005"));

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    uuid(
                            "92000000-0000-0000-0000-000000000006"));

    @Test
    void createsStructuredEventIssueAndNormalizesDetail() {
        SubmissionAttemptIssue issue =
                issue(
                        SubmissionAttemptIssueType.EVENT_NOT_VALIDATED,
                        EVENT_ID,
                        null,
                        null,
                        null,
                        "  Event must be validated.  ");

        assertThat(
                issue.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(
                issue.detail())
                .isEqualTo(
                        "Event must be validated.");
    }

    @Test
    void requiresEventForNotValidatedIssue() {
        assertThatThrownBy(
                () -> issue(
                        SubmissionAttemptIssueType.EVENT_NOT_VALIDATED,
                        null,
                        null,
                        null,
                        null,
                        "Event must be validated."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "eventId must not be null");
    }

    @Test
    void createsBlockingAlertIssueWithAlertReference() {
        SubmissionAttemptIssue issue =
                issue(
                        SubmissionAttemptIssueType.BLOCKING_ALERT,
                        EVENT_ID,
                        ALERT_ID,
                        null,
                        null,
                        "A blocking Alert remains open.");

        assertThat(
                issue.alertId())
                .isEqualTo(
                        ALERT_ID);
    }

    @Test
    void createsFailedValidationIssueWithResultReference() {
        SubmissionAttemptIssue issue =
                issue(
                        SubmissionAttemptIssueType
                                .VALIDATION_RESULT_FAILED,
                        EVENT_ID,
                        null,
                        RESULT_ID,
                        null,
                        "A Validation Result failed.");

        assertThat(
                issue.validationResultId())
                .isEqualTo(
                        RESULT_ID);
    }

    @Test
    void createsStaleValidationIssueWithResultReference() {
        SubmissionAttemptIssue issue =
                issue(
                        SubmissionAttemptIssueType
                                .VALIDATION_RESULT_STALE,
                        EVENT_ID,
                        null,
                        RESULT_ID,
                        null,
                        "A Validation Result is stale.");

        assertThat(
                issue.validationResultId())
                .isEqualTo(
                        RESULT_ID);
    }

    @Test
    void requiresAlertForBlockingAlertIssue() {
        assertThatThrownBy(
                () -> issue(
                        SubmissionAttemptIssueType.BLOCKING_ALERT,
                        null,
                        null,
                        null,
                        null,
                        "A blocking Alert remains open."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "alertId must not be null");
    }

    @Test
    void requiresResultForFailedValidationIssue() {
        assertThatThrownBy(
                () -> issue(
                        SubmissionAttemptIssueType
                                .VALIDATION_RESULT_FAILED,
                        null,
                        null,
                        null,
                        null,
                        "A Validation Result failed."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validationResultId must not be null");
    }

    @Test
    void requiresResultForStaleValidationIssue() {
        assertThatThrownBy(
                () -> issue(
                        SubmissionAttemptIssueType
                                .VALIDATION_RESULT_STALE,
                        null,
                        null,
                        null,
                        null,
                        "A Validation Result is stale."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validationResultId must not be null");
    }

    @Test
    void missingConsolidationIssueMustNotReferenceOne() {
        assertThatThrownBy(
                () -> issue(
                        SubmissionAttemptIssueType
                                .CONSOLIDATION_MISSING,
                        null,
                        null,
                        null,
                        CONSOLIDATION_ID,
                        "No current Consolidation exists."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "missing consolidation issue must not "
                                + "reference a consolidation");
    }

    @Test
    void staleConsolidationIssueRequiresReference() {
        assertThatThrownBy(
                () -> issue(
                        SubmissionAttemptIssueType
                                .CONSOLIDATION_STALE,
                        null,
                        null,
                        null,
                        null,
                        "The Consolidation is stale."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "consolidationId must not be null");
    }

    @Test
    void permitsCriticalInconsistencyWithoutEntityReference() {
        SubmissionAttemptIssue issue =
                issue(
                        SubmissionAttemptIssueType
                                .OTHER_CRITICAL_INCONSISTENCY,
                        null,
                        null,
                        null,
                        null,
                        "A critical consistency condition failed.");

        assertThat(
                issue.issueType())
                .isEqualTo(
                        SubmissionAttemptIssueType
                                .OTHER_CRITICAL_INCONSISTENCY);
    }

    @Test
    void rejectsBlankDetail() {
        assertThatThrownBy(
                () -> issue(
                        SubmissionAttemptIssueType
                                .OTHER_CRITICAL_INCONSISTENCY,
                        null,
                        null,
                        null,
                        null,
                        "   "))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "detail must not be blank");
    }

    private SubmissionAttemptIssue issue(
            SubmissionAttemptIssueType issueType,
            OperationalEventId eventId,
            ValidationAlertId alertId,
            ValidationResultId validationResultId,
            ConsolidationId consolidationId,
            String detail) {

        return new SubmissionAttemptIssue(
                ISSUE_ID,
                ATTEMPT_ID,
                issueType,
                eventId,
                alertId,
                validationResultId,
                consolidationId,
                detail);
    }

    private static UUID uuid(
            String value) {

        return UUID.fromString(
                value);
    }

}