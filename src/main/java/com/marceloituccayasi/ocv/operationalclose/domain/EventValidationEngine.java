package com.marceloituccayasi.ocv.operationalclose.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class EventValidationEngine {

    public static final int MVP_RULE_VERSION =
            1;

    public List<EventValidationRuleEvaluation> evaluate(
            EventValidationContext context) {

        Objects.requireNonNull(
                context,
                "validation context must not be null");

        List<EventValidationRuleEvaluation> evaluations =
                new ArrayList<>();

        evaluateVr001(
                context)
                .ifPresent(
                        evaluations::add);

        evaluateVr002(
                context)
                .ifPresent(
                        evaluations::add);

        evaluateVr003(
                context)
                .ifPresent(
                        evaluations::add);

        evaluateVr006(
                context)
                .ifPresent(
                        evaluations::add);

        return List.copyOf(
                evaluations);
    }

    private Optional<EventValidationRuleEvaluation>
            evaluateVr001(
                    EventValidationContext context) {

        OperationalEvent event =
                context.event();

        boolean applies =
                event.authorizationRequired()
                        && (
                                event.eventType()
                                        == OperationalEventType.EXPENSE
                                || event.eventType()
                                        == OperationalEventType.DISCOUNT
                        );

        if (!applies) {
            return Optional.empty();
        }

        boolean satisfied =
                context.hasActiveAuthorization();

        return Optional.of(
                evaluation(
                        ValidationRuleCode.VR_001,
                        satisfied,
                        satisfied
                                ? "VR-001 satisfied: required formal authorization is active and linked."
                                : "VR-001 failed: required formal authorization is missing."));
    }

    private Optional<EventValidationRuleEvaluation>
            evaluateVr002(
                    EventValidationContext context) {

        OperationalEvent event =
                context.event();

        if (event.eventType()
                != OperationalEventType.INCOME) {

            return Optional.empty();
        }

        boolean satisfied =
                context.activeSupportingEvidence()
                        .stream()
                        .anyMatch(
                                evidence ->
                                        evidence.supportedAmount()
                                                != null
                                        && evidence.supportedAmount()
                                                .compareTo(
                                                        event.amount()
                                                                .value())
                                                == 0);

        return Optional.of(
                evaluation(
                        ValidationRuleCode.VR_002,
                        satisfied,
                        satisfied
                                ? "VR-002 satisfied: an active supporting evidence amount matches the registered income amount."
                                : "VR-002 failed: no active supporting evidence amount matches the registered income amount."));
    }

    private Optional<EventValidationRuleEvaluation>
            evaluateVr003(
                    EventValidationContext context) {

        OperationalEvent event =
                context.event();

        boolean applies =
                event.eventType()
                        == OperationalEventType.EXPENSE
                        && event.evidenceRequired();

        if (!applies) {
            return Optional.empty();
        }

        boolean satisfied =
                context.activeSupportingEvidence()
                        .stream()
                        .anyMatch(
                                evidence ->
                                        evidence.legibilityStatus()
                                                == SupportingEvidenceLegibilityStatus.LEGIBLE);

        return Optional.of(
                evaluation(
                        ValidationRuleCode.VR_003,
                        satisfied,
                        satisfied
                                ? "VR-003 satisfied: active legible supporting evidence is present."
                                : "VR-003 failed: active legible supporting evidence is missing."));
    }

    private Optional<EventValidationRuleEvaluation>
            evaluateVr006(
                    EventValidationContext context) {

        OperationalEventType eventType =
                context.event()
                        .eventType();

        boolean applies =
                eventType
                        == OperationalEventType.DISCOUNT
                        || eventType
                                == OperationalEventType.CANCELLATION;

        if (!applies) {
            return Optional.empty();
        }

        boolean satisfied =
                context.hasActiveAuthorization();

        return Optional.of(
                evaluation(
                        ValidationRuleCode.VR_006,
                        satisfied,
                        satisfied
                                ? "VR-006 satisfied: active formal authorization is linked to the discount or cancellation."
                                : "VR-006 failed: active formal authorization is missing for the discount or cancellation."));
    }

    private static EventValidationRuleEvaluation evaluation(
            ValidationRuleCode ruleCode,
            boolean satisfied,
            String detail) {

        return new EventValidationRuleEvaluation(
                ruleCode,
                MVP_RULE_VERSION,
                satisfied
                        ? ValidationOutcome.SATISFIED
                        : ValidationOutcome.FAILED,
                detail);
    }

}