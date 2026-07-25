package com.marceloituccayasi.ocv.operationalclose.domain;

/**
 * Fixed internal validation-rule codes approved for the MVP.
 */
public enum ValidationRuleCode {

    VR_001("VR-001"),
    VR_002("VR-002"),
    VR_003("VR-003"),
    VR_006("VR-006"),
    VR_008("VR-008");

    private final String persistentValue;

    ValidationRuleCode(
            String persistentValue) {

        this.persistentValue =
                persistentValue;
    }

    public String persistentValue() {
        return persistentValue;
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