package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class EventValidationStateResolverTest {

    private final EventValidationStateResolver resolver =
            new EventValidationStateResolver();

    @Test
    void validatesEventWithoutApplicableRules() {
        assertThat(
                resolver.resolve(
                        List.of()))
                .isEqualTo(
                        OperationalEventState.VALIDATED);
    }

    @Test
    void validatesEventWhenEveryApplicableRuleIsSatisfied() {
        assertThat(
                resolver.resolve(
                        List.of(
                                satisfied(
                                        ValidationRuleCode.VR_001),
                                satisfied(
                                        ValidationRuleCode.VR_003),
                                satisfied(
                                        ValidationRuleCode.VR_006))))
                .isEqualTo(
                        OperationalEventState.VALIDATED);
    }

    @Test
    void derivesPendingSupportFromFailedEvidenceRule() {
        assertThat(
                resolver.resolve(
                        List.of(
                                failed(
                                        ValidationRuleCode.VR_003))))
                .isEqualTo(
                        OperationalEventState
                                .PENDING_SUPPORT);
    }

    @Test
    void derivesPendingAuthorizationFromFailedAuthorizationRules() {
        assertThat(
                resolver.resolve(
                        List.of(
                                failed(
                                        ValidationRuleCode.VR_001))))
                .isEqualTo(
                        OperationalEventState
                                .PENDING_AUTHORIZATION);

        assertThat(
                resolver.resolve(
                        List.of(
                                failed(
                                        ValidationRuleCode.VR_006))))
                .isEqualTo(
                        OperationalEventState
                                .PENDING_AUTHORIZATION);
    }

    @Test
    void derivesObservedFromFailedAmountRule() {
        assertThat(
                resolver.resolve(
                        List.of(
                                failed(
                                        ValidationRuleCode.VR_002))))
                .isEqualTo(
                        OperationalEventState.OBSERVED);
    }

    @Test
    void prioritizesSupportThenAuthorizationThenObservation() {
        assertThat(
                resolver.resolve(
                        List.of(
                                failed(
                                        ValidationRuleCode.VR_002),
                                failed(
                                        ValidationRuleCode.VR_006),
                                failed(
                                        ValidationRuleCode.VR_003))))
                .isEqualTo(
                        OperationalEventState
                                .PENDING_SUPPORT);

        assertThat(
                resolver.resolve(
                        List.of(
                                failed(
                                        ValidationRuleCode.VR_002),
                                failed(
                                        ValidationRuleCode.VR_001))))
                .isEqualTo(
                        OperationalEventState
                                .PENDING_AUTHORIZATION);
    }

    @Test
    void ignoresSatisfiedRulesWhenAnotherRuleFailed() {
        assertThat(
                resolver.resolve(
                        List.of(
                                satisfied(
                                        ValidationRuleCode.VR_003),
                                failed(
                                        ValidationRuleCode.VR_002),
                                satisfied(
                                        ValidationRuleCode.VR_006))))
                .isEqualTo(
                        OperationalEventState.OBSERVED);
    }

    @Test
    void rejectsNullEvaluationCollection() {
        assertThatThrownBy(
                () -> resolver.resolve(
                        null))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "validation evaluations must not be null");
    }

    @Test
    void rejectsNullEvaluation() {
        List<EventValidationRuleEvaluation> evaluations =
                new ArrayList<>();

        evaluations.add(
                null);

        assertThatThrownBy(
                () -> resolver.resolve(
                        evaluations))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "validation evaluation must not be null");
    }

    private static EventValidationRuleEvaluation satisfied(
            ValidationRuleCode ruleCode) {

        return new EventValidationRuleEvaluation(
                ruleCode,
                EventValidationEngine.MVP_RULE_VERSION,
                ValidationOutcome.SATISFIED,
                ruleCode
                        + " satisfied for state resolution.");
    }

    private static EventValidationRuleEvaluation failed(
            ValidationRuleCode ruleCode) {

        return new EventValidationRuleEvaluation(
                ruleCode,
                EventValidationEngine.MVP_RULE_VERSION,
                ValidationOutcome.FAILED,
                ruleCode
                        + " failed for state resolution.");
    }

}