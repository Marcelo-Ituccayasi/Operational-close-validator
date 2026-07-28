package com.marceloituccayasi.ocv.operationalclose.domain;

public record EventValidationRuleEvaluation(
        ValidationRuleCode ruleCode,
        int ruleVersion,
        ValidationOutcome outcome,
        String detail) {

    public EventValidationRuleEvaluation {
        if (ruleCode == null) {
            throw new IllegalArgumentException(
                    "validation rule code must not be null");
        }

        if (ruleCode.scope()
                != ValidationRuleScope.EVENT) {

            throw new IllegalArgumentException(
                    "validation rule must be event-scoped");
        }

        if (ruleVersion < 1) {
            throw new IllegalArgumentException(
                    "validation rule version must be at least one");
        }

        if (outcome == null) {
            throw new IllegalArgumentException(
                    "validation outcome must not be null");
        }

        if (detail == null
                || detail.isBlank()) {

            throw new IllegalArgumentException(
                    "validation detail must not be blank");
        }

        detail =
                detail.trim();
    }

    public boolean failed() {
        return outcome
                == ValidationOutcome.FAILED;
    }

}