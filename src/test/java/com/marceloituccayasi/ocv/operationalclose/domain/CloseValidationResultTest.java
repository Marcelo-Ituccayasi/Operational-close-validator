package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CloseValidationResultTest {

    private static final ValidationResultId RESULT_ID =
            new ValidationResultId(
                    uuid(
                            "b6000000-0000-0000-0000-000000000001"));

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    uuid(
                            "b6000000-0000-0000-0000-000000000002"));

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    uuid(
                            "b6000000-0000-0000-0000-000000000003"));

    private static final Instant EVALUATED_AT =
            Instant.parse(
                    "2026-07-30T18:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Test
    void createsSatisfiedVr008ResultWithConsolidation() {
        CloseValidationResult result =
                CloseValidationResult.create(
                        RESULT_ID,
                        ValidationRuleCode.VR_008,
                        1,
                        CLOSE_ID,
                        ValidationOutcome.SATISFIED,
                        "  Final control passed.  ",
                        EVALUATED_AT,
                        ACTOR,
                        CONSOLIDATION_ID);

        assertThat(
                result.ruleCode())
                .isEqualTo(
                        ValidationRuleCode.VR_008);

        assertThat(
                result.outcome())
                .isEqualTo(
                        ValidationOutcome.SATISFIED);

        assertThat(
                result.detail())
                .isEqualTo(
                        "Final control passed.");

        assertThat(
                result.consolidationId())
                .isEqualTo(
                        CONSOLIDATION_ID);

        assertThat(
                result.current())
                .isTrue();
    }

    @Test
    void createsFailedVr008ResultWithoutConsolidation() {
        CloseValidationResult result =
                CloseValidationResult.create(
                        RESULT_ID,
                        ValidationRuleCode.VR_008,
                        1,
                        CLOSE_ID,
                        ValidationOutcome.FAILED,
                        "Current Consolidation is missing.",
                        EVALUATED_AT,
                        ACTOR,
                        null);

        assertThat(
                result.outcome())
                .isEqualTo(
                        ValidationOutcome.FAILED);

        assertThat(
                result.consolidationId())
                .isNull();
    }

    @Test
    void rejectsEventScopedRule() {
        assertThatThrownBy(
                () -> CloseValidationResult.create(
                        RESULT_ID,
                        ValidationRuleCode.VR_001,
                        1,
                        CLOSE_ID,
                        ValidationOutcome.FAILED,
                        "Invalid scope.",
                        EVALUATED_AT,
                        ACTOR,
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "close validation result requires "
                                + "a close-scoped rule");
    }

    @Test
    void satisfiedResultRequiresConsolidation() {
        assertThatThrownBy(
                () -> CloseValidationResult.create(
                        RESULT_ID,
                        ValidationRuleCode.VR_008,
                        1,
                        CLOSE_ID,
                        ValidationOutcome.SATISFIED,
                        "Final control passed.",
                        EVALUATED_AT,
                        ACTOR,
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "satisfied close validation result "
                                + "requires a consolidation");
    }

    @Test
    void invalidatesWithoutRewritingEvaluationContent() {
        CloseValidationResult current =
                CloseValidationResult.create(
                        RESULT_ID,
                        ValidationRuleCode.VR_008,
                        1,
                        CLOSE_ID,
                        ValidationOutcome.FAILED,
                        "Final control failed.",
                        EVALUATED_AT,
                        ACTOR,
                        CONSOLIDATION_ID);

        Instant invalidatedAt =
                EVALUATED_AT.plusSeconds(
                        60);

        CloseValidationResult invalidated =
                current.invalidate(
                        invalidatedAt,
                        "  Superseded by another VR-008 evaluation.  ");

        assertThat(
                invalidated.current())
                .isFalse();

        assertThat(
                invalidated.invalidatedAt())
                .isEqualTo(
                        invalidatedAt);

        assertThat(
                invalidated.invalidationReason())
                .isEqualTo(
                        "Superseded by another VR-008 evaluation.");

        assertThat(
                invalidated.id())
                .isEqualTo(
                        current.id());

        assertThat(
                invalidated.outcome())
                .isEqualTo(
                        current.outcome());

        assertThat(
                invalidated.consolidationId())
                .isEqualTo(
                        current.consolidationId());
    }

    @Test
    void rejectsSecondInvalidation() {
        CloseValidationResult invalidated =
                failedResult()
                        .invalidate(
                                EVALUATED_AT.plusSeconds(
                                        60),
                                "Superseded.");

        assertThatThrownBy(
                () -> invalidated.invalidate(
                        EVALUATED_AT.plusSeconds(
                                120),
                        "Superseded again."))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "validation result is already invalidated");
    }

    @Test
    void rejectsInvalidationBeforeEvaluation() {
        assertThatThrownBy(
                () -> failedResult()
                        .invalidate(
                                EVALUATED_AT.minusSeconds(
                                        1),
                                "Invalid timestamp."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "invalidation instant must not be before evaluation");
    }

    @Test
    void rejectsCurrentResultWithInvalidationMetadata() {
        assertThatThrownBy(
                () -> new CloseValidationResult(
                        RESULT_ID,
                        ValidationRuleCode.VR_008,
                        1,
                        CLOSE_ID,
                        ValidationOutcome.FAILED,
                        "Final control failed.",
                        EVALUATED_AT,
                        ACTOR,
                        null,
                        true,
                        EVALUATED_AT.plusSeconds(
                                1),
                        "Invalid metadata."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "current validation result must not contain "
                                + "invalidation metadata");
    }

    private CloseValidationResult failedResult() {
        return CloseValidationResult.create(
                RESULT_ID,
                ValidationRuleCode.VR_008,
                1,
                CLOSE_ID,
                ValidationOutcome.FAILED,
                "Final control failed.",
                EVALUATED_AT,
                ACTOR,
                null);
    }

    private static UUID uuid(
            String value) {

        return UUID.fromString(
                value);
    }

}