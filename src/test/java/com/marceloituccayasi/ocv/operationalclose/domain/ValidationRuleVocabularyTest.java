package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class ValidationRuleVocabularyTest {

    @Test
    void exposesTheApprovedFixedRuleCodes() {
        assertThat(
                EnumSet.allOf(
                        ValidationRuleCode.class))
                .containsExactly(
                        ValidationRuleCode.VR_001,
                        ValidationRuleCode.VR_002,
                        ValidationRuleCode.VR_003,
                        ValidationRuleCode.VR_006,
                        ValidationRuleCode.VR_008);
    }

    @Test
    void preservesPersistentRuleValues() {
        assertThat(
                ValidationRuleCode.VR_001.persistentValue())
                .isEqualTo(
                        "VR-001");

        assertThat(
                ValidationRuleCode.VR_006.toString())
                .isEqualTo(
                        "VR-006");
    }

    @Test
    void restoresRuleCodeFromPersistentValue() {
        assertThat(
                ValidationRuleCode.fromPersistentValue(
                        " VR-003 "))
                .isEqualTo(
                        ValidationRuleCode.VR_003);
    }

    @Test
    void rejectsUnsupportedOrMissingRuleCode() {
        assertThatThrownBy(
                () -> ValidationRuleCode.fromPersistentValue(
                        "VR-004"))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "unsupported validation rule code: VR-004");

        assertThatThrownBy(
                () -> ValidationRuleCode.fromPersistentValue(
                        " "))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation rule code must not be blank");

        assertThatThrownBy(
                () -> ValidationRuleCode.fromPersistentValue(
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation rule code must not be blank");
    }

    @Test
    void exposesOnlyApprovedScopesSeveritiesAndOutcomes() {
        assertThat(
                ValidationRuleScope.values())
                .containsExactly(
                        ValidationRuleScope.EVENT,
                        ValidationRuleScope.CLOSE);

        assertThat(
                ValidationSeverity.values())
                .containsExactly(
                        ValidationSeverity.CRITICAL,
                        ValidationSeverity.HIGH);

        assertThat(
                ValidationOutcome.values())
                .containsExactly(
                        ValidationOutcome.SATISFIED,
                        ValidationOutcome.FAILED);
    }

}