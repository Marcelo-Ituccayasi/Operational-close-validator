package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationAlertRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationContext;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationEngine;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationRuleEvaluation;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

class CloseConsolidationReadinessEvaluatorTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "2aa10306-0b75-46e5-8d5a-760000000001"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "2aa10306-0b75-46e5-8d5a-760000000002"));

    private static final ValidationResultId RESULT_ID =
            new ValidationResultId(
                    UUID.fromString(
                            "2aa10306-0b75-46e5-8d5a-760000000003"));

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-30T12:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final EventValidationContextLoader contextLoader =
            mock(
                    EventValidationContextLoader.class);

    private final EventValidationEngine validationEngine =
            mock(
                    EventValidationEngine.class);

    private final EventValidationResultRepository
            validationResultRepository =
                    mock(
                            EventValidationResultRepository.class);

    private final EventValidationAlertRepository alertRepository =
            mock(
                    EventValidationAlertRepository.class);

    private final CloseConsolidationReadinessEvaluator evaluator =
            new CloseConsolidationReadinessEvaluator(
                    contextLoader,
                    validationEngine,
                    validationResultRepository,
                    alertRepository);

    @Test
    void reportsNoEventsWithoutReadingDependentData() {
        CloseConsolidationReadiness readiness =
                evaluator.evaluate(
                        CLOSE_ID,
                        List.of());

        assertThat(
                readiness.eventsPresent())
                .isFalse();

        assertThat(
                readiness.ready())
                .isFalse();

        verifyNoInteractions(
                contextLoader,
                validationEngine,
                validationResultRepository,
                alertRepository);
    }

    @Test
    void reportsReadyForCompleteCurrentSatisfiedValidation() {
        OperationalEvent event =
                event(
                        OperationalEventState.VALIDATED,
                        4);

        EventValidationRuleEvaluation evaluation =
                satisfiedEvaluation();

        EventValidationResult result =
                satisfiedResult(
                        4);

        configureValidation(
                event,
                List.of(
                        evaluation),
                List.of(
                        result));

        when(alertRepository
                .findAllOpenByEventIdOrderByCreatedAt(
                        EVENT_ID))
                .thenReturn(
                        List.of());

        CloseConsolidationReadiness readiness =
                evaluator.evaluate(
                        CLOSE_ID,
                        List.of(
                                event));

        assertThat(
                readiness.ready())
                .isTrue();

        assertThat(
                readiness.notValidatedEventIds())
                .isEmpty();

        assertThat(
                readiness.invalidResultEventIds())
                .isEmpty();

        assertThat(
                readiness.blockingAlertEventIds())
                .isEmpty();
    }

    @Test
    void rejectsAResultFromAnOlderEventRevision() {
        OperationalEvent event =
                event(
                        OperationalEventState.VALIDATED,
                        4);

        configureValidation(
                event,
                List.of(
                        satisfiedEvaluation()),
                List.of(
                        satisfiedResult(
                                3)));

        when(alertRepository
                .findAllOpenByEventIdOrderByCreatedAt(
                        EVENT_ID))
                .thenReturn(
                        List.of());

        CloseConsolidationReadiness readiness =
                evaluator.evaluate(
                        CLOSE_ID,
                        List.of(
                                event));

        assertThat(
                readiness.ready())
                .isFalse();

        assertThat(
                readiness.invalidResultEventIds())
                .containsExactly(
                        EVENT_ID);

        assertThat(
                readiness.affectedEventIds())
                .containsExactly(
                        EVENT_ID);
    }

    @Test
    void reportsNonValidatedEventAndOpenBlockingAlert() {
        OperationalEvent event =
                event(
                        OperationalEventState.OBSERVED,
                        4);

        configureValidation(
                event,
                List.of(),
                List.of());

        EventValidationAlert blockingAlert =
                mock(
                        EventValidationAlert.class);

        when(blockingAlert.blocking())
                .thenReturn(
                        true);

        when(alertRepository
                .findAllOpenByEventIdOrderByCreatedAt(
                        EVENT_ID))
                .thenReturn(
                        List.of(
                                blockingAlert));

        CloseConsolidationReadiness readiness =
                evaluator.evaluate(
                        CLOSE_ID,
                        List.of(
                                event));

        assertThat(
                readiness.ready())
                .isFalse();

        assertThat(
                readiness.notValidatedEventIds())
                .containsExactly(
                        EVENT_ID);

        assertThat(
                readiness.invalidResultEventIds())
                .isEmpty();

        assertThat(
                readiness.blockingAlertEventIds())
                .containsExactly(
                        EVENT_ID);

        assertThat(
                readiness.affectedEventIds())
                .containsExactly(
                        EVENT_ID);
    }

    private void configureValidation(
            OperationalEvent event,
            List<EventValidationRuleEvaluation> evaluations,
            List<EventValidationResult> results) {

        when(contextLoader.load(
                CLOSE_ID,
                EVENT_ID))
                .thenReturn(
                        Optional.of(
                                new EventValidationContext(
                                        event,
                                        List.of(),
                                        List.of())));

        when(validationEngine.evaluate(
                any(
                        EventValidationContext.class)))
                .thenReturn(
                        evaluations);

        when(validationResultRepository
                .findAllCurrentByEventIdOrderByRuleCode(
                        EVENT_ID))
                .thenReturn(
                        results);
    }

    private EventValidationRuleEvaluation
            satisfiedEvaluation() {

        return new EventValidationRuleEvaluation(
                ValidationRuleCode.VR_003,
                EventValidationEngine.MVP_RULE_VERSION,
                ValidationOutcome.SATISFIED,
                "Required evidence validation is satisfied.");
    }

    private EventValidationResult satisfiedResult(
            long eventDataRevision) {

        return EventValidationResult.create(
                RESULT_ID,
                ValidationRuleCode.VR_003,
                EventValidationEngine.MVP_RULE_VERSION,
                EVENT_ID,
                ValidationOutcome.SATISFIED,
                "Required evidence validation is satisfied.",
                CREATED_AT,
                ACTOR,
                eventDataRevision);
    }

    private OperationalEvent event(
            OperationalEventState state,
            long dataRevision) {

        return new OperationalEvent(
                EVENT_ID,
                CLOSE_ID,
                OperationalEventType.EXPENSE,
                new OperationalEventAmount(
                        new BigDecimal(
                                "50.0000")),
                new BigDecimal(
                        "-50.0000"),
                null,
                CREATED_AT,
                CREATED_AT,
                "Caja principal",
                "Evento evaluado para consolidación",
                state,
                false,
                false,
                dataRevision,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

}