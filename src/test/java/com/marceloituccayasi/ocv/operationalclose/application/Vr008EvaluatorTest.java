package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.Consolidation;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueType;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

class Vr008EvaluatorTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    uuid(
                            "ca000000-0000-0000-0000-000000000001"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    uuid(
                            "ca000000-0000-0000-0000-000000000002"));

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    uuid(
                            "ca000000-0000-0000-0000-000000000003"));

    private static final ValidationResultId RESULT_ID =
            new ValidationResultId(
                    uuid(
                            "ca000000-0000-0000-0000-000000000004"));

    private static final ValidationAlertId ALERT_ID =
            new ValidationAlertId(
                    uuid(
                            "ca000000-0000-0000-0000-000000000005"));

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-30T13:00:00Z");

    private static final Instant EVALUATED_AT =
            Instant.parse(
                    "2026-07-30T14:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final Vr008Evaluator evaluator =
            new Vr008Evaluator();

    @Test
    void satisfiesVr008ForCompleteCurrentData() {
        OperationalEvent event =
                event(
                        OperationalEventState.VALIDATED,
                        1);

        Consolidation consolidation =
                consolidation(
                        event);

        Vr008Evaluation evaluation =
                evaluator.evaluate(
                        CLOSE_ID,
                        List.of(
                                event),
                        ready(),
                        List.of(
                                satisfiedResult(
                                        event,
                                        1)),
                        List.of(),
                        consolidation);

        assertThat(
                evaluation.satisfied())
                .isTrue();

        assertThat(
                evaluation.consolidationId())
                .isEqualTo(
                        CONSOLIDATION_ID);

        assertThat(
                evaluation.issues())
                .isEmpty();
    }

    @Test
    void reportsEventResultAlertAndMissingConsolidation() {
        OperationalEvent event =
                event(
                        OperationalEventState.REGISTERED,
                        1);

        EventValidationResult failedResult =
                failedResult(
                        event,
                        1);

        EventValidationAlert alert =
                EventValidationAlert.createFromFailedResult(
                        ALERT_ID,
                        failedResult,
                        "A blocking inconsistency remains.",
                        EVALUATED_AT,
                        ACTOR);

        CloseConsolidationReadiness readiness =
                CloseConsolidationReadiness.evaluated(
                        List.of(
                                EVENT_ID),
                        List.of(
                                EVENT_ID),
                        List.of(
                                EVENT_ID));

        Vr008Evaluation evaluation =
                evaluator.evaluate(
                        CLOSE_ID,
                        List.of(
                                event),
                        readiness,
                        List.of(
                                failedResult),
                        List.of(
                                alert),
                        null);

        assertThat(
                evaluation.outcome())
                .isEqualTo(
                        ValidationOutcome.FAILED);

        assertThat(
                evaluation.issues())
                .extracting(
                        Vr008Issue::issueType)
                .containsExactlyInAnyOrder(
                        SubmissionAttemptIssueType
                                .EVENT_NOT_VALIDATED,
                        SubmissionAttemptIssueType
                                .VALIDATION_RESULT_FAILED,
                        SubmissionAttemptIssueType
                                .BLOCKING_ALERT,
                        SubmissionAttemptIssueType
                                .CONSOLIDATION_MISSING);
    }

    @Test
    void reportsStaleValidationResultRevision() {
        OperationalEvent event =
                event(
                        OperationalEventState.VALIDATED,
                        2);

        EventValidationResult staleResult =
                satisfiedResult(
                        event,
                        1);

        Vr008Evaluation evaluation =
                evaluator.evaluate(
                        CLOSE_ID,
                        List.of(
                                event),
                        CloseConsolidationReadiness.evaluated(
                                List.of(),
                                List.of(
                                        EVENT_ID),
                                List.of()),
                        List.of(
                                staleResult),
                        List.of(),
                        consolidation(
                                event));

        assertThat(
                evaluation.issues())
                .extracting(
                        Vr008Issue::issueType)
                .containsExactly(
                        SubmissionAttemptIssueType
                                .VALIDATION_RESULT_STALE);

        assertThat(
                evaluation.issues()
                        .getFirst()
                        .validationResultId())
                .isEqualTo(
                        RESULT_ID);
    }

    @Test
    void reportsIncompleteApplicableResultSet() {
        OperationalEvent event =
                event(
                        OperationalEventState.VALIDATED,
                        1);

        Vr008Evaluation evaluation =
                evaluator.evaluate(
                        CLOSE_ID,
                        List.of(
                                event),
                        CloseConsolidationReadiness.evaluated(
                                List.of(),
                                List.of(
                                        EVENT_ID),
                                List.of()),
                        List.of(
                                satisfiedResult(
                                        event,
                                        1)),
                        List.of(),
                        consolidation(
                                event));

        assertThat(
                evaluation.issues())
                .extracting(
                        Vr008Issue::issueType)
                .containsExactly(
                        SubmissionAttemptIssueType
                                .OTHER_CRITICAL_INCONSISTENCY);
    }

    @Test
    void reportsConsolidationBuiltFromOlderEventRevision() {
        OperationalEvent previousRevision =
                event(
                        OperationalEventState.VALIDATED,
                        1);

        OperationalEvent currentRevision =
                event(
                        OperationalEventState.VALIDATED,
                        2);

        Vr008Evaluation evaluation =
                evaluator.evaluate(
                        CLOSE_ID,
                        List.of(
                                currentRevision),
                        ready(),
                        List.of(
                                satisfiedResult(
                                        currentRevision,
                                        2)),
                        List.of(),
                        consolidation(
                                previousRevision));

        assertThat(
                evaluation.issues())
                .extracting(
                        Vr008Issue::issueType)
                .containsExactly(
                        SubmissionAttemptIssueType
                                .CONSOLIDATION_STALE);

        assertThat(
                evaluation.consolidationId())
                .isEqualTo(
                        CONSOLIDATION_ID);
    }

    @Test
    void rejectsReadinessThatReferencesAnotherEvent() {
        OperationalEvent event =
                event(
                        OperationalEventState.VALIDATED,
                        1);

        OperationalEventId otherEventId =
                new OperationalEventId(
                        uuid(
                                "ca000000-0000-0000-0000-000000000099"));

        assertThatThrownBy(
                () -> evaluator.evaluate(
                        CLOSE_ID,
                        List.of(
                                event),
                        CloseConsolidationReadiness.evaluated(
                                List.of(),
                                List.of(
                                        otherEventId),
                                List.of()),
                        List.of(),
                        List.of(),
                        consolidation(
                                event)))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "readiness must reference only evaluated Events");
    }

    private CloseConsolidationReadiness ready() {
        return CloseConsolidationReadiness.evaluated(
                List.of(),
                List.of(),
                List.of());
    }

    private OperationalClose close() {
        return OperationalClose.create(
                CLOSE_ID,
                new OperationalPeriod(
                        LocalDate.of(
                                2026,
                                7,
                                1),
                        LocalDate.of(
                                2026,
                                7,
                                31)),
                new CurrencyCode(
                        "PEN"),
                new InitialBalance(
                        decimal(
                                "1000.0000")),
                CREATED_AT,
                ACTOR);
    }

    private OperationalEvent event(
            OperationalEventState state,
            long dataRevision) {

        return new OperationalEvent(
                EVENT_ID,
                CLOSE_ID,
                OperationalEventType.INCOME,
                new OperationalEventAmount(
                        decimal(
                                "100.0000")),
                decimal(
                        "100.0000"),
                null,
                CREATED_AT,
                CREATED_AT,
                "Caja principal",
                "Evento para evaluación VR-008",
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

    private Consolidation consolidation(
            OperationalEvent event) {

        return Consolidation.complete(
                CONSOLIDATION_ID,
                close(),
                List.of(
                        event),
                decimal(
                        "1100.0000"),
                EVALUATED_AT,
                ACTOR);
    }

    private EventValidationResult satisfiedResult(
            OperationalEvent event,
            long evaluatedRevision) {

        return EventValidationResult.create(
                RESULT_ID,
                ValidationRuleCode.VR_001,
                1,
                event.id(),
                ValidationOutcome.SATISFIED,
                "Event rule passed.",
                EVALUATED_AT,
                ACTOR,
                evaluatedRevision);
    }

    private EventValidationResult failedResult(
            OperationalEvent event,
            long evaluatedRevision) {

        return EventValidationResult.create(
                RESULT_ID,
                ValidationRuleCode.VR_001,
                1,
                event.id(),
                ValidationOutcome.FAILED,
                "Event rule failed.",
                EVALUATED_AT,
                ACTOR,
                evaluatedRevision);
    }

    private static BigDecimal decimal(
            String value) {

        return new BigDecimal(
                value);
    }

    private static UUID uuid(
            String value) {

        return UUID.fromString(
                value);
    }

}