package com.marceloituccayasi.ocv.operationalclose.domain;

/**
 * Fixed internal validation-rule codes approved for the MVP.
 */
public enum ValidationRuleCode {

    VR_001(
            "VR-001",
            ValidationRuleScope.EVENT,
            ValidationSeverity.CRITICAL),

    VR_002(
            "VR-002",
            ValidationRuleScope.EVENT,
            ValidationSeverity.CRITICAL),

    VR_003(
            "VR-003",
            ValidationRuleScope.EVENT,
            ValidationSeverity.HIGH),

    VR_006(
            "VR-006",
            ValidationRuleScope.EVENT,
            ValidationSeverity.CRITICAL),

    VR_008(
            "VR-008",
            ValidationRuleScope.CLOSE,
            ValidationSeverity.CRITICAL);

    private final String persistentValue;
    private final ValidationRuleScope scope;
    private final ValidationSeverity severity;

    ValidationRuleCode(
            String persistentValue,
            ValidationRuleScope scope,
            ValidationSeverity severity) {

        this.persistentValue =
                persistentValue;

        this.scope =
                scope;

        this.severity =
                severity;
    }

    public String persistentValue() {
        return persistentValue;
    }

    public ValidationRuleScope scope() {
        return scope;
    }

    public ValidationSeverity severity() {
        return severity;
    }

    public static ValidationRuleCode fromPersistentValue(
            String value) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    "validation rule code must not be blank");
        }

        String normalizedValue =
                value.trim();

        for (ValidationRuleCode code : values()) {
            if (code.persistentValue.equals(
                    normalizedValue)) {

                return code;
            }
        }

        throw new IllegalArgumentException(
                "unsupported validation rule code: "
                        + normalizedValue);
    }

    @Override
    public String toString() {
        return persistentValue;
    }

}