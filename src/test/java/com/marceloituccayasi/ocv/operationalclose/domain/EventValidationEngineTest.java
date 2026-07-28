package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class EventValidationEngineTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "c372ec7c-714f-47ad-a8ba-b13e02010001"));

    private static final Instant REGISTERED_AT =
            Instant.parse(
                    "2026-07-27T12:00:00Z");

    private static final EventValidationEngine ENGINE =
            new EventValidationEngine();

    @Test
    void evaluatesOnlyApplicableIncomeRule() {
        OperationalEvent income =
                regularEvent(
                        OperationalEventType.INCOME,
                        false,
                        false);

        SupportingEvidence matchingEvidence =
                evidence(
                        income,
                        new BigDecimal(
                                "100.0000"),
                        SupportingEvidenceLegibilityStatus.UNVERIFIED,
                        true);

        List<EventValidationRuleEvaluation> evaluations =
                ENGINE.evaluate(
                        new EventValidationContext(
                                income,
                                List.of(
                                        matchingEvidence),
                                List.of()));

        assertThat(evaluations)
                .extracting(
                        EventValidationRuleEvaluation::ruleCode)
                .containsExactly(
                        ValidationRuleCode.VR_002);

        assertThat(
                evaluations.get(0)
                        .outcome())
                .isEqualTo(
                        ValidationOutcome.SATISFIED);
    }

    @Test
    void vr002IgnoresInactiveAndNonMatchingEvidence() {
        OperationalEvent income =
                regularEvent(
                        OperationalEventType.INCOME,
                        false,
                        false);

        SupportingEvidence inactiveMatchingEvidence =
                evidence(
                        income,
                        new BigDecimal(
                                "100.0000"),
                        SupportingEvidenceLegibilityStatus.LEGIBLE,
                        false);

        SupportingEvidence activeNonMatchingEvidence =
                evidence(
                        income,
                        new BigDecimal(
                                "90.0000"),
                        SupportingEvidenceLegibilityStatus.LEGIBLE,
                        true);

        EventValidationRuleEvaluation evaluation =
                ENGINE.evaluate(
                        new EventValidationContext(
                                income,
                                List.of(
                                        inactiveMatchingEvidence,
                                        activeNonMatchingEvidence),
                                List.of()))
                        .get(0);

        assertThat(evaluation.ruleCode())
                .isEqualTo(
                        ValidationRuleCode.VR_002);

        assertThat(evaluation.outcome())
                .isEqualTo(
                        ValidationOutcome.FAILED);
    }

    @Test
    void vr003RequiresActiveLegibleEvidence() {
        OperationalEvent expense =
                regularEvent(
                        OperationalEventType.EXPENSE,
                        true,
                        false);

        SupportingEvidence unverifiedEvidence =
                evidence(
                        expense,
                        null,
                        SupportingEvidenceLegibilityStatus.UNVERIFIED,
                        true);

        EventValidationRuleEvaluation failed =
                ENGINE.evaluate(
                        new EventValidationContext(
                                expense,
                                List.of(
                                        unverifiedEvidence),
                                List.of()))
                        .get(0);

        SupportingEvidence legibleEvidence =
                evidence(
                        expense,
                        null,
                        SupportingEvidenceLegibilityStatus.LEGIBLE,
                        true);

        EventValidationRuleEvaluation satisfied =
                ENGINE.evaluate(
                        new EventValidationContext(
                                expense,
                                List.of(
                                        legibleEvidence),
                                List.of()))
                        .get(0);

        assertThat(failed.ruleCode())
                .isEqualTo(
                        ValidationRuleCode.VR_003);

        assertThat(failed.outcome())
                .isEqualTo(
                        ValidationOutcome.FAILED);

        assertThat(satisfied.outcome())
                .isEqualTo(
                        ValidationOutcome.SATISFIED);
    }

    @Test
    void vr001RequiresAuthorizationForMarkedExpense() {
        OperationalEvent expense =
                regularEvent(
                        OperationalEventType.EXPENSE,
                        false,
                        true);

        EventValidationRuleEvaluation failed =
                ENGINE.evaluate(
                        new EventValidationContext(
                                expense,
                                List.of(),
                                List.of()))
                        .get(0);

        EventAuthorization authorization =
                authorization(
                        expense,
                        true);

        EventValidationRuleEvaluation satisfied =
                ENGINE.evaluate(
                        new EventValidationContext(
                                expense,
                                List.of(),
                                List.of(
                                        authorization)))
                        .get(0);

        assertThat(failed.ruleCode())
                .isEqualTo(
                        ValidationRuleCode.VR_001);

        assertThat(failed.outcome())
                .isEqualTo(
                        ValidationOutcome.FAILED);

        assertThat(satisfied.outcome())
                .isEqualTo(
                        ValidationOutcome.SATISFIED);
    }

    @Test
    void discountEvaluatesVr001AndVr006InCatalogOrder() {
        OperationalEvent discount =
                regularEvent(
                        OperationalEventType.DISCOUNT,
                        false,
                        true);

        EventAuthorization authorization =
                authorization(
                        discount,
                        true);

        List<EventValidationRuleEvaluation> evaluations =
                ENGINE.evaluate(
                        new EventValidationContext(
                                discount,
                                List.of(),
                                List.of(
                                        authorization)));

        assertThat(evaluations)
                .extracting(
                        EventValidationRuleEvaluation::ruleCode)
                .containsExactly(
                        ValidationRuleCode.VR_001,
                        ValidationRuleCode.VR_006);

        assertThat(evaluations)
                .extracting(
                        EventValidationRuleEvaluation::outcome)
                .containsExactly(
                        ValidationOutcome.SATISFIED,
                        ValidationOutcome.SATISFIED);
    }

    @Test
    void vr006AppliesToCancellationRegardlessOfRequirementFlag() {
        OperationalEvent cancellation =
                cancellationEvent(
                        false);

        EventValidationRuleEvaluation failed =
                ENGINE.evaluate(
                        new EventValidationContext(
                                cancellation,
                                List.of(),
                                List.of()))
                        .get(0);

        EventValidationRuleEvaluation satisfied =
                ENGINE.evaluate(
                        new EventValidationContext(
                                cancellation,
                                List.of(),
                                List.of(
                                        authorization(
                                                cancellation,
                                                true))))
                        .get(0);

        assertThat(failed.ruleCode())
                .isEqualTo(
                        ValidationRuleCode.VR_006);

        assertThat(failed.outcome())
                .isEqualTo(
                        ValidationOutcome.FAILED);

        assertThat(satisfied.outcome())
                .isEqualTo(
                        ValidationOutcome.SATISFIED);
    }

    @Test
    void contextRejectsDependenciesFromAnotherEvent() {
        OperationalEvent evaluatedEvent =
                regularEvent(
                        OperationalEventType.EXPENSE,
                        true,
                        false);

        OperationalEvent otherEvent =
                regularEvent(
                        OperationalEventType.EXPENSE,
                        true,
                        false);

        SupportingEvidence foreignEvidence =
                evidence(
                        otherEvent,
                        null,
                        SupportingEvidenceLegibilityStatus.LEGIBLE,
                        true);

        EventAuthorization foreignAuthorization =
                authorization(
                        otherEvent,
                        true);

        assertThatThrownBy(
                () -> new EventValidationContext(
                        evaluatedEvent,
                        List.of(
                                foreignEvidence),
                        List.of()))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "supporting evidence must belong to evaluated event");

        assertThatThrownBy(
                () -> new EventValidationContext(
                        evaluatedEvent,
                        List.of(),
                        List.of(
                                foreignAuthorization)))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "authorization must belong to evaluated event");
    }

    @Test
    void evaluationRejectsCloseRuleAndInvalidContent() {
        assertThatThrownBy(
                () -> new EventValidationRuleEvaluation(
                        ValidationRuleCode.VR_008,
                        1,
                        ValidationOutcome.FAILED,
                        "Close-scoped rule."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation rule must be event-scoped");

        assertThatThrownBy(
                () -> new EventValidationRuleEvaluation(
                        ValidationRuleCode.VR_001,
                        0,
                        ValidationOutcome.FAILED,
                        "Invalid version."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation rule version must be at least one");

        assertThatThrownBy(
                () -> new EventValidationRuleEvaluation(
                        ValidationRuleCode.VR_001,
                        1,
                        ValidationOutcome.FAILED,
                        "   "))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation detail must not be blank");
    }

    private static OperationalEvent regularEvent(
            OperationalEventType eventType,
            boolean evidenceRequired,
            boolean authorizationRequired) {

        return OperationalEvent.create(
                new OperationalEventId(
                        UUID.randomUUID()),
                CLOSE_ID,
                eventType,
                new OperationalEventAmount(
                        new BigDecimal(
                                "100.0000")),
                REGISTERED_AT.minusSeconds(
                        60L),
                "Caja principal",
                "Evento para probar el motor de validación",
                evidenceRequired,
                authorizationRequired,
                REGISTERED_AT,
                actor());
    }

    private static OperationalEvent cancellationEvent(
            boolean authorizationRequired) {

        OperationalEvent originalEvent =
                regularEvent(
                        OperationalEventType.INCOME,
                        false,
                        false);

        return OperationalEvent.createCancellation(
                new OperationalEventId(
                        UUID.randomUUID()),
                CLOSE_ID,
                originalEvent,
                REGISTERED_AT.minusSeconds(
                        30L),
                "Caja principal",
                "Anulación para probar VR-006",
                false,
                authorizationRequired,
                REGISTERED_AT.plusSeconds(
                        30L),
                actor());
    }

    private static SupportingEvidence evidence(
            OperationalEvent event,
            BigDecimal supportedAmount,
            SupportingEvidenceLegibilityStatus legibilityStatus,
            boolean active) {

        SupportingEvidence evidence =
                SupportingEvidence.create(
                        new SupportingEvidenceId(
                                UUID.randomUUID()),
                        event.id(),
                        "RECEIPT",
                        "reference:evidence-"
                                + UUID.randomUUID(),
                        supportedAmount,
                        LocalDate.of(
                                2026,
                                7,
                                27),
                        legibilityStatus,
                        REGISTERED_AT.plusSeconds(
                                60L),
                        actor());

        return active
                ? evidence
                : evidence.deactivate(
                        REGISTERED_AT.plusSeconds(
                                120L),
                        actor());
    }

    private static EventAuthorization authorization(
            OperationalEvent event,
            boolean active) {

        EventAuthorization authorization =
                EventAuthorization.create(
                        new EventAuthorizationId(
                                UUID.randomUUID()),
                        event.id(),
                        "Gerencia",
                        "Autorización formal del movimiento",
                        REGISTERED_AT,
                        "AUTH-"
                                + UUID.randomUUID(),
                        REGISTERED_AT.plusSeconds(
                                60L),
                        actor());

        return active
                ? authorization
                : authorization.deactivate(
                        REGISTERED_AT.plusSeconds(
                                120L),
                        actor());
    }

    private static AuditActor actor() {
        return new AuditActor(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
    }

}