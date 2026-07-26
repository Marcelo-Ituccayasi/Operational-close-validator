package com.marceloituccayasi.ocv.operationalclose.domain;

import java.time.Instant;

/**
 * Visible inconsistency produced by a failed Event Validation Result.
 *
 * @param id stable alert identifier
 * @param eventId affected Operational Event
 * @param sourceValidationResultId failed result that originated the alert
 * @param causeRuleCode rule that detected the inconsistency
 * @param severity severity inherited from the rule
 * @param blocking whether the alert blocks validation
 * @param state current lifecycle state
 * @param detail visible inconsistency description
 * @param resolvedByValidationResultId successful result used for resolution
 * @param discardJustification mandatory justification for a discarded alert
 * @param createdAt creation instant
 * @param createdBy authenticated creator
 * @param updatedAt last lifecycle-management instant
 * @param closedAt terminal-state instant
 */
public record EventValidationAlert(
        ValidationAlertId id,
        OperationalEventId eventId,
        ValidationResultId sourceValidationResultId,
        ValidationRuleCode causeRuleCode,
        ValidationSeverity severity,
        boolean blocking,
        ValidationAlertState state,
        String detail,
        ValidationResultId resolvedByValidationResultId,
        String discardJustification,
        Instant createdAt,
        AuditActor createdBy,
        Instant updatedAt,
        Instant closedAt) {

    public EventValidationAlert {
        requireNonNull(
                id,
                "id");

        requireNonNull(
                eventId,
                "eventId");

        requireNonNull(
                sourceValidationResultId,
                "sourceValidationResultId");

        requireNonNull(
                causeRuleCode,
                "causeRuleCode");

        requireNonNull(
                severity,
                "severity");

        requireNonNull(
                state,
                "state");

        requireNonNull(
                createdAt,
                "createdAt");

        requireNonNull(
                createdBy,
                "createdBy");

        requireNonNull(
                updatedAt,
                "updatedAt");

        if (causeRuleCode.scope()
                != ValidationRuleScope.EVENT) {

            throw new IllegalArgumentException(
                    "event validation alert requires an event-scoped rule");
        }

        if (severity
                != causeRuleCode.severity()) {

            throw new IllegalArgumentException(
                    "validation alert severity must match its cause rule");
        }

        detail =
                requireText(
                        detail,
                        "detail");

        if (updatedAt.isBefore(
                createdAt)) {

            throw new IllegalArgumentException(
                    "alert update instant must not be before creation");
        }

        if (closedAt != null
                && closedAt.isBefore(
                        createdAt)) {

            throw new IllegalArgumentException(
                    "alert close instant must not be before creation");
        }

        if (closedAt != null
                && closedAt.isAfter(
                        updatedAt)) {

            throw new IllegalArgumentException(
                    "alert close instant must not be after last update");
        }

        if (state == ValidationAlertState.RESOLVED) {
            requireNonNull(
                    resolvedByValidationResultId,
                    "resolvedByValidationResultId");

            if (discardJustification != null) {
                throw new IllegalArgumentException(
                        "resolved alert must not contain discard justification");
            }

            requireNonNull(
                    closedAt,
                    "closedAt");
        }
        else if (state == ValidationAlertState.DISCARDED) {
            if (resolvedByValidationResultId != null) {
                throw new IllegalArgumentException(
                        "discarded alert must not contain resolution result");
            }

            discardJustification =
                    requireText(
                            discardJustification,
                            "discardJustification");

            requireNonNull(
                    closedAt,
                    "closedAt");
        }
        else {
            if (resolvedByValidationResultId != null
                    || discardJustification != null
                    || closedAt != null) {

                throw new IllegalArgumentException(
                        "non-terminal alert must not contain closure metadata");
            }
        }
    }

    public static EventValidationAlert createFromFailedResult(
            ValidationAlertId id,
            EventValidationResult sourceResult,
            String detail,
            Instant createdAt,
            AuditActor createdBy) {

        requireNonNull(
                sourceResult,
                "sourceResult");

        requireNonNull(
                createdAt,
                "createdAt");

        if (sourceResult.outcome()
                != ValidationOutcome.FAILED) {

            throw new IllegalArgumentException(
                    "validation alert requires a failed source result");
        }

        if (!sourceResult.current()) {
            throw new IllegalArgumentException(
                    "validation alert requires a current source result");
        }

        if (createdAt.isBefore(
                sourceResult.evaluatedAt())) {

            throw new IllegalArgumentException(
                    "alert creation instant must not be before source evaluation");
        }

        return new EventValidationAlert(
                id,
                sourceResult.eventId(),
                sourceResult.id(),
                sourceResult.ruleCode(),
                sourceResult.ruleCode().severity(),
                true,
                ValidationAlertState.ACTIVE,
                detail,
                null,
                null,
                createdAt,
                createdBy,
                createdAt,
                null);
    }

    public EventValidationAlertChange acknowledge(
            ValidationAlertTransitionId transitionId,
            String transitionDetail,
            Instant occurredAt,
            AuditActor actor) {

        if (state != ValidationAlertState.ACTIVE) {
            throw new IllegalStateException(
                    "only active validation alert can be acknowledged");
        }

        requireLifecycleInstant(
                occurredAt);

        EventValidationAlert updatedAlert =
                new EventValidationAlert(
                        id,
                        eventId,
                        sourceValidationResultId,
                        causeRuleCode,
                        severity,
                        blocking,
                        ValidationAlertState.ACKNOWLEDGED,
                        detail,
                        null,
                        null,
                        createdAt,
                        createdBy,
                        occurredAt,
                        null);

        ValidationAlertTransition transition =
                ValidationAlertTransition.acknowledged(
                        transitionId,
                        id,
                        transitionDetail,
                        occurredAt,
                        actor);

        return new EventValidationAlertChange(
                updatedAlert,
                transition);
    }

    public EventValidationAlertChange startReview(
            ValidationAlertTransitionId transitionId,
            String transitionDetail,
            Instant occurredAt,
            AuditActor actor) {

        if (state != ValidationAlertState.ACTIVE
                && state
                        != ValidationAlertState.ACKNOWLEDGED) {

            throw new IllegalStateException(
                    "validation alert review requires active or acknowledged state");
        }

        requireLifecycleInstant(
                occurredAt);

        EventValidationAlert updatedAlert =
                new EventValidationAlert(
                        id,
                        eventId,
                        sourceValidationResultId,
                        causeRuleCode,
                        severity,
                        blocking,
                        ValidationAlertState.UNDER_REVIEW,
                        detail,
                        null,
                        null,
                        createdAt,
                        createdBy,
                        occurredAt,
                        null);

        ValidationAlertTransition transition =
                ValidationAlertTransition.underReview(
                        transitionId,
                        id,
                        state,
                        transitionDetail,
                        occurredAt,
                        actor);

        return new EventValidationAlertChange(
                updatedAlert,
                transition);
    }

    public EventValidationAlertChange resolve(
            ValidationAlertTransitionId transitionId,
            EventValidationResult resolutionResult,
            String transitionDetail,
            Instant occurredAt,
            AuditActor actor) {

        requireOpenState();
        requireNonNull(
                resolutionResult,
                "resolutionResult");

        if (!resolutionResult.current()) {
            throw new IllegalArgumentException(
                    "resolution requires a current validation result");
        }

        if (resolutionResult.outcome()
                != ValidationOutcome.SATISFIED) {

            throw new IllegalArgumentException(
                    "resolution requires a satisfied validation result");
        }

        if (!eventId.equals(
                resolutionResult.eventId())) {

            throw new IllegalArgumentException(
                    "resolution result must evaluate the affected event");
        }

        if (causeRuleCode
                != resolutionResult.ruleCode()) {

            throw new IllegalArgumentException(
                    "resolution result must satisfy the alert cause rule");
        }

        if (resolutionResult.evaluatedAt()
                .isBefore(
                        createdAt)) {

            throw new IllegalArgumentException(
                    "resolution result must not predate alert creation");
        }

        requireLifecycleInstant(
                occurredAt);

        if (occurredAt.isBefore(
                resolutionResult.evaluatedAt())) {

            throw new IllegalArgumentException(
                    "alert resolution instant must not be before revalidation");
        }

        EventValidationAlert updatedAlert =
                new EventValidationAlert(
                        id,
                        eventId,
                        sourceValidationResultId,
                        causeRuleCode,
                        severity,
                        blocking,
                        ValidationAlertState.RESOLVED,
                        detail,
                        resolutionResult.id(),
                        null,
                        createdAt,
                        createdBy,
                        occurredAt,
                        occurredAt);

        ValidationAlertTransition transition =
                ValidationAlertTransition.resolved(
                        transitionId,
                        id,
                        state,
                        transitionDetail,
                        resolutionResult.id(),
                        occurredAt,
                        actor);

        return new EventValidationAlertChange(
                updatedAlert,
                transition);
    }

    public EventValidationAlertChange discard(
            ValidationAlertTransitionId transitionId,
            String transitionDetail,
            String justification,
            Instant occurredAt,
            AuditActor actor) {

        requireOpenState();
        requireLifecycleInstant(
                occurredAt);

        String normalizedJustification =
                requireText(
                        justification,
                        "justification");

        EventValidationAlert updatedAlert =
                new EventValidationAlert(
                        id,
                        eventId,
                        sourceValidationResultId,
                        causeRuleCode,
                        severity,
                        blocking,
                        ValidationAlertState.DISCARDED,
                        detail,
                        null,
                        normalizedJustification,
                        createdAt,
                        createdBy,
                        occurredAt,
                        occurredAt);

        ValidationAlertTransition transition =
                ValidationAlertTransition.discarded(
                        transitionId,
                        id,
                        state,
                        transitionDetail,
                        normalizedJustification,
                        occurredAt,
                        actor);

        return new EventValidationAlertChange(
                updatedAlert,
                transition);
    }

    private void requireOpenState() {
        if (state.terminal()) {
            throw new IllegalStateException(
                    "terminal validation alert cannot change state");
        }
    }

    private void requireLifecycleInstant(
            Instant occurredAt) {

        requireNonNull(
                occurredAt,
                "occurredAt");

        if (occurredAt.isBefore(
                updatedAt)) {

            throw new IllegalArgumentException(
                    "alert lifecycle instant must not be before previous update");
        }
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