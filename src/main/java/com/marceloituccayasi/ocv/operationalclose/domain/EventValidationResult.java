package com.marceloituccayasi.ocv.operationalclose.domain;

import java.time.Instant;

/**
 * Immutable evaluation of one Validation Rule over one Operational Event.
 *
 * <p>The evaluation content never changes. Only its current-status metadata
 * may change when the result is invalidated.</p>
 *
 * @param id stable result identifier
 * @param ruleCode evaluated fixed rule
 * @param ruleVersion exact rule version
 * @param eventId evaluated Operational Event
 * @param outcome satisfied or failed outcome
 * @param detail evaluation explanation
 * @param evaluatedAt evaluation instant
 * @param evaluatedBy authenticated evaluator
 * @param eventDataRevision exact event revision evaluated
 * @param current whether the result remains applicable
 * @param invalidatedAt later invalidation instant
 * @param invalidationReason later invalidation reason
 */
public record EventValidationResult(
        ValidationResultId id,
        ValidationRuleCode ruleCode,
        int ruleVersion,
        OperationalEventId eventId,
        ValidationOutcome outcome,
        String detail,
        Instant evaluatedAt,
        AuditActor evaluatedBy,
        long eventDataRevision,
        boolean current,
        Instant invalidatedAt,
        String invalidationReason) {

    public EventValidationResult {
        requireNonNull(
                id,
                "id");

        requireNonNull(
                ruleCode,
                "ruleCode");

        requireNonNull(
                eventId,
                "eventId");

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
                != ValidationRuleScope.EVENT) {

            throw new IllegalArgumentException(
                    "event validation result requires an event-scoped rule");
        }

        if (ruleVersion < 1) {
            throw new IllegalArgumentException(
                    "validation rule version must be at least one");
        }

        if (eventDataRevision < 1L) {
            throw new IllegalArgumentException(
                    "event data revision must be at least one");
        }

        detail =
                requireText(
                        detail,
                        "detail");

        if (current) {
            if (invalidatedAt != null
                    || invalidationReason != null) {

                throw new IllegalArgumentException(
                        "current validation result must not contain invalidation metadata");
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

    public static EventValidationResult create(
            ValidationResultId id,
            ValidationRuleCode ruleCode,
            int ruleVersion,
            OperationalEventId eventId,
            ValidationOutcome outcome,
            String detail,
            Instant evaluatedAt,
            AuditActor evaluatedBy,
            long eventDataRevision) {

        return new EventValidationResult(
                id,
                ruleCode,
                ruleVersion,
                eventId,
                outcome,
                detail,
                evaluatedAt,
                evaluatedBy,
                eventDataRevision,
                true,
                null,
                null);
    }

    public EventValidationResult invalidate(
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

        return new EventValidationResult(
                id,
                ruleCode,
                ruleVersion,
                eventId,
                outcome,
                detail,
                evaluatedAt,
                evaluatedBy,
                eventDataRevision,
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