package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationAlertRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationContext;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationEngine;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationRuleEvaluation;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

/**
 * Reloads and verifies every Event-level condition required by
 * Operational Close consolidation.
 */
public final class CloseConsolidationReadinessEvaluator {

    private final EventValidationContextLoader contextLoader;

    private final EventValidationEngine validationEngine;

    private final EventValidationResultRepository
            validationResultRepository;

    private final EventValidationAlertRepository
            alertRepository;

    public CloseConsolidationReadinessEvaluator(
            EventValidationContextLoader contextLoader,
            EventValidationEngine validationEngine,
            EventValidationResultRepository validationResultRepository,
            EventValidationAlertRepository alertRepository) {

        this.contextLoader =
                Objects.requireNonNull(
                        contextLoader);

        this.validationEngine =
                Objects.requireNonNull(
                        validationEngine);

        this.validationResultRepository =
                Objects.requireNonNull(
                        validationResultRepository);

        this.alertRepository =
                Objects.requireNonNull(
                        alertRepository);
    }

    public CloseConsolidationReadiness evaluate(
            OperationalCloseId closeId,
            List<OperationalEvent> events) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        Objects.requireNonNull(
                events,
                "events must not be null");

        if (events.isEmpty()) {
            return CloseConsolidationReadiness.noEvents();
        }

        validateEventCollection(
                closeId,
                events);

        List<OperationalEventId> notValidatedEventIds =
                new ArrayList<>();

        List<OperationalEventId> invalidResultEventIds =
                new ArrayList<>();

        List<OperationalEventId> blockingAlertEventIds =
                new ArrayList<>();

        for (OperationalEvent event : events) {
            if (event.state()
                    != OperationalEventState.VALIDATED) {

                notValidatedEventIds.add(
                        event.id());
            }

            if (!hasCompleteCurrentSatisfiedResults(
                    closeId,
                    event)) {

                invalidResultEventIds.add(
                        event.id());
            }

            if (hasOpenBlockingAlert(
                    event.id())) {

                blockingAlertEventIds.add(
                        event.id());
            }
        }

        return CloseConsolidationReadiness.evaluated(
                notValidatedEventIds,
                invalidResultEventIds,
                blockingAlertEventIds);
    }

    private boolean hasCompleteCurrentSatisfiedResults(
            OperationalCloseId closeId,
            OperationalEvent event) {

        EventValidationContext loadedContext =
                contextLoader.load(
                        closeId,
                        event.id())
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "persisted event disappeared while "
                                                + "evaluating consolidation"));

        EventValidationContext currentContext =
                new EventValidationContext(
                        event,
                        loadedContext.supportingEvidence(),
                        loadedContext.authorizations());

        List<EventValidationRuleEvaluation> evaluations =
                Objects.requireNonNull(
                        validationEngine.evaluate(
                                currentContext),
                        "validation evaluations must not be null");

        List<EventValidationResult> currentResults =
                Objects.requireNonNull(
                        validationResultRepository
                                .findAllCurrentByEventIdOrderByRuleCode(
                                        event.id()),
                        "current validation results must not be null");

        Map<ValidationRuleCode, EventValidationResult> resultsByRule =
                new EnumMap<>(
                        ValidationRuleCode.class);

        for (EventValidationResult result : currentResults) {
            Objects.requireNonNull(
                    result,
                    "current validation result must not be null");

            EventValidationResult previous =
                    resultsByRule.put(
                            result.ruleCode(),
                            result);

            if (previous != null) {
                return false;
            }
        }

        if (resultsByRule.size()
                != evaluations.size()) {

            return false;
        }

        Set<ValidationRuleCode> evaluatedRules =
                new HashSet<>();

        for (EventValidationRuleEvaluation evaluation
                : evaluations) {

            Objects.requireNonNull(
                    evaluation,
                    "validation evaluation must not be null");

            if (!evaluatedRules.add(
                    evaluation.ruleCode())) {

                return false;
            }

            if (evaluation.outcome()
                    != ValidationOutcome.SATISFIED) {

                return false;
            }

            EventValidationResult result =
                    resultsByRule.get(
                            evaluation.ruleCode());

            if (result == null
                    || !result.current()
                    || !result.eventId().equals(
                            event.id())
                    || result.eventDataRevision()
                            != event.dataRevision()
                    || result.ruleVersion()
                            != evaluation.ruleVersion()
                    || result.outcome()
                            != ValidationOutcome.SATISFIED) {

                return false;
            }
        }

        return true;
    }

    private boolean hasOpenBlockingAlert(
            OperationalEventId eventId) {

        List<EventValidationAlert> openAlerts =
                Objects.requireNonNull(
                        alertRepository
                                .findAllOpenByEventIdOrderByCreatedAt(
                                        eventId),
                        "open validation alerts must not be null");

        for (EventValidationAlert openAlert : openAlerts) {
            Objects.requireNonNull(
                    openAlert,
                    "open validation alert must not be null");
        }

        return openAlerts.stream()
                .anyMatch(
                        EventValidationAlert::blocking);
    }

    private static void validateEventCollection(
            OperationalCloseId closeId,
            List<OperationalEvent> events) {

        Set<OperationalEventId> eventIds =
                new HashSet<>();

        for (OperationalEvent event : events) {
            Objects.requireNonNull(
                    event,
                    "events must not contain null values");

            if (!closeId.equals(
                    event.closeId())) {

                throw new IllegalArgumentException(
                        "all events must belong to the evaluated close");
            }

            if (!eventIds.add(
                    event.id())) {

                throw new IllegalArgumentException(
                        "events must not contain duplicate identifiers");
            }
        }
    }

}