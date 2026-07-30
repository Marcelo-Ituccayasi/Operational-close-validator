package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueType;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;

class Vr008EvaluationTest {

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    uuid(
                            "c9000000-0000-0000-0000-000000000001"));

    @Test
    void createsSatisfiedEvaluationWithoutIssues() {
        Vr008Evaluation evaluation =
                Vr008Evaluation.satisfied(
                        CONSOLIDATION_ID);

        assertThat(
                evaluation.outcome())
                .isEqualTo(
                        ValidationOutcome.SATISFIED);

        assertThat(
                evaluation.satisfied())
                .isTrue();

        assertThat(
                evaluation.issues())
                .isEmpty();
    }

    @Test
    void failedEvaluationRequiresIssues() {
        assertThatThrownBy(
                () -> Vr008Evaluation.failed(
                        CONSOLIDATION_ID,
                        List.of()))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "failed VR-008 evaluation "
                                + "requires at least one issue");
    }

    @Test
    void failureWithoutConsolidationRequiresMissingIssue() {
        Vr008Issue issue =
                new Vr008Issue(
                        SubmissionAttemptIssueType
                                .OTHER_CRITICAL_INCONSISTENCY,
                        null,
                        null,
                        null,
                        null,
                        "A critical inconsistency exists.");

        assertThatThrownBy(
                () -> Vr008Evaluation.failed(
                        null,
                        List.of(
                                issue)))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "failed VR-008 evaluation without consolidation "
                                + "requires a missing consolidation issue");
    }

    @Test
    void normalizesIssueOrderAndMakesCollectionImmutable() {
        Vr008Issue stale =
                new Vr008Issue(
                        SubmissionAttemptIssueType
                                .CONSOLIDATION_STALE,
                        null,
                        null,
                        null,
                        CONSOLIDATION_ID,
                        "Consolidation is stale.");

        Vr008Issue other =
                new Vr008Issue(
                        SubmissionAttemptIssueType
                                .OTHER_CRITICAL_INCONSISTENCY,
                        null,
                        null,
                        null,
                        null,
                        "Another critical inconsistency exists.");

        Vr008Evaluation evaluation =
                Vr008Evaluation.failed(
                        CONSOLIDATION_ID,
                        List.of(
                                other,
                                stale));

        assertThat(
                evaluation.issues())
                .extracting(
                        Vr008Issue::issueType)
                .containsExactly(
                        SubmissionAttemptIssueType
                                .CONSOLIDATION_STALE,
                        SubmissionAttemptIssueType
                                .OTHER_CRITICAL_INCONSISTENCY);

        assertThatThrownBy(
                () -> evaluation.issues()
                        .clear())
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    private static UUID uuid(
            String value) {

        return UUID.fromString(
                value);
    }

}