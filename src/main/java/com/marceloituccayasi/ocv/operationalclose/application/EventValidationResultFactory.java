package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationContext;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationRuleEvaluation;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;

/**
 * Converts pure rule evaluations into immutable persisted Validation Results.
 */
public final class EventValidationResultFactory {

    private final UuidGenerator
            uuidGenerator;

    public EventValidationResultFactory(
            UuidGenerator uuidGenerator) {

        this.uuidGenerator =
                Objects.requireNonNull(
                        uuidGenerator);
    }

    public List<EventValidationResult> createAll(
            EventValidationContext context,
            List<EventValidationRuleEvaluation> evaluations,
            Instant evaluatedAt,
            AuditActor actor) {

        Objects.requireNonNull(
                context,
                "validation context must not be null");

        Objects.requireNonNull(
                evaluations,
                "validation evaluations must not be null");

        Objects.requireNonNull(
                evaluatedAt,
                "evaluation instant must not be null");

        Objects.requireNonNull(
                actor,
                "evaluation actor must not be null");

        List<EventValidationResult> results =
                new ArrayList<>(
                        evaluations.size());

        for (EventValidationRuleEvaluation evaluation
                : evaluations) {

            Objects.requireNonNull(
                    evaluation,
                    "validation evaluation must not be null");

            UUID resultUuid =
                    Objects.requireNonNull(
                            uuidGenerator.next(),
                            "generated validation result UUID must not be null");

            results.add(
                    EventValidationResult.create(
                            new ValidationResultId(
                                    resultUuid),
                            evaluation.ruleCode(),
                            evaluation.ruleVersion(),
                            context.event()
                                    .id(),
                            evaluation.outcome(),
                            evaluation.detail(),
                            evaluatedAt,
                            actor,
                            context.event()
                                    .dataRevision()));
        }

        return List.copyOf(
                results);
    }

}