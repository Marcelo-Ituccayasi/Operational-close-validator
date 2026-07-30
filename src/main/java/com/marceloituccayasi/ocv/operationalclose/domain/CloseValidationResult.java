package com.marceloituccayasi.ocv.operationalclose.domain;

import java.time.Instant;

/**
 * Immutable evaluation of one Close-scoped Validation Rule.
 *
 * <p>The evaluation content never changes. Only its current-status metadata
 * may change when the result is invalidated.</p>
 *
 * @param id stable result identifier
 * @param ruleCode evaluated fixed rule
 * @param ruleVersion exact rule version
 * @param closeId evaluated Operational Close
 * @param outcome satisfied or failed outcome
 * @param detail evaluation explanation
 * @param evaluatedAt evaluation instant
 * @param evaluatedBy authenticated evaluator
 * @param consolidationId evaluated Consolidation, when one exists
 * @param current whether the result remains applicable
 * @param invalidatedAt later invalidation instant
 * @param invalidationReason later invalidation reason
 */
public record CloseValidationResult(
        ValidationResultId id,
        ValidationRuleCode ruleCode,
        int ruleVersion,
        OperationalCloseId closeId,
        ValidationOutcome outcome,
        String detail,
        Instant evaluatedAt,
        AuditActor evaluatedBy,
        ConsolidationId consolidationId,
        boolean current,
        Instant invalidatedAt,
        String invalidationReason) {

    public CloseValidationResult {
        requireNonNull(
                id,
                "id");

        requireNonNull(
                ruleCode,
                "ruleCode");

        requireNonNull(
                closeId,
                "closeId");

        requireNonNull(
                outcome,
                "outcome");

        requireNonNull(
                evaluatedAt,
                "evaluatedAt");

        requireNonNull(
                evaluatedBy,
                "evaluatedBy");

        if (ruleCode.scope()
                != ValidationRuleScope.CLOSE) {

            throw new IllegalArgumentException(
                    "close validation result requires a close-scoped rule");
        }

        if (ruleVersion < 1) {
            throw new IllegalArgumentException(
                    "validation rule version must be at least one");
        }

        detail =
                requireText(
                        detail,
                        "detail");

        if (outcome
                == ValidationOutcome.SATISFIED
                && consolidationId == null) {

            throw new IllegalArgumentException(
                    "satisfied close validation result "
                            + "requires a consolidation");
        }

        if (current) {
            if (invalidatedAt != null
                    || invalidationReason != null) {

                throw new IllegalArgumentException(
                        "current validation result must not contain "
                                + "invalidation metadata");
            }
        }
        else {
            requireNonNull(
                    invalidatedAt,
                    "invalidatedAt");

            invalidationReason =
                    requireText(
                            invalidationReason,
                            "invalidationReason");

            if (invalidatedAt.isBefore(
                    evaluatedAt)) {

                throw new IllegalArgumentException(
                        "invalidation instant must not be before evaluation");
            }
        }
    }

    public static CloseValidationResult create(
            ValidationResultId id,
            ValidationRuleCode ruleCode,
            int ruleVersion,
            OperationalCloseId closeId,
            ValidationOutcome outcome,
            String detail,
            Instant evaluatedAt,
            AuditActor evaluatedBy,
            ConsolidationId consolidationId) {

        return new CloseValidationResult(
                id,
                ruleCode,
                ruleVersion,
                closeId,
                outcome,
                detail,
                evaluatedAt,
                evaluatedBy,
                consolidationId,
                true,
                null,
                null);
    }

    public CloseValidationResult invalidate(
            Instant invalidationAt,
            String reason) {

        if (!current) {
            throw new IllegalStateException(
                    "validation result is already invalidated");
        }

        requireNonNull(
                invalidationAt,
                "invalidationAt");

        String normalizedReason =
                requireText(
                        reason,
                        "reason");

        if (invalidationAt.isBefore(
                evaluatedAt)) {

            throw new IllegalArgumentException(
                    "invalidation instant must not be before evaluation");
        }

        return new CloseValidationResult(
                id,
                ruleCode,
                ruleVersion,
                closeId,
                outcome,
                detail,
                evaluatedAt,
                evaluatedBy,
                consolidationId,
                false,
                invalidationAt,
                normalizedReason);
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