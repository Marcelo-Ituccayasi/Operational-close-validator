package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class EventValidationAlertTest {

    private static final Instant EVALUATED_AT =
            Instant.parse(
                    "2026-07-25T12:00:00Z");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-25T12:01:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Test
    void createsActiveBlockingAlertFromCurrentFailedResult() {
        EventValidationResult failedResult =
                failedResult(
                        ValidationRuleCode.VR_003);

        ValidationAlertId alertId =
                new ValidationAlertId(
                        UUID.randomUUID());

        EventValidationAlert alert =
                EventValidationAlert.createFromFailedResult(
                        alertId,
                        failedResult,
                        " Required evidence is not legible. ",
                        CREATED_AT,
                        ACTOR);

        assertThat(
                alert.id())
                .isEqualTo(
                        alertId);

        assertThat(
                alert.eventId())
                .isEqualTo(
                        failedResult.eventId());

        assertThat(
                alert.sourceValidationResultId())
                .isEqualTo(
                        failedResult.id());

        assertThat(
                alert.causeRuleCode())
                .isEqualTo(
                        ValidationRuleCode.VR_003);

        assertThat(
                alert.severity())
                .isEqualTo(
                        ValidationSeverity.HIGH);

        assertThat(
                alert.blocking())
                .isTrue();

        assertThat(
                alert.state())
                .isEqualTo(
                        ValidationAlertState.ACTIVE);

        assertThat(
                alert.detail())
                .isEqualTo(
                        "Required evidence is not legible.");

        assertThat(
                alert.createdAt())
                .isEqualTo(
                        CREATED_AT);

        assertThat(
                alert.updatedAt())
                .isEqualTo(
                        CREATED_AT);

        assertThat(
                alert.resolvedByValidationResultId())
                .isNull();

        assertThat(
                alert.discardJustification())
                .isNull();

        assertThat(
                alert.closedAt())
                .isNull();
    }

    @Test
    void rejectsSatisfiedSourceResult() {
        EventValidationResult satisfiedResult =
                EventValidationResult.create(
                        new ValidationResultId(
                                UUID.randomUUID()),
                        ValidationRuleCode.VR_001,
                        1,
                        new OperationalEventId(
                                UUID.randomUUID()),
                        ValidationOutcome.SATISFIED,
                        "Rule satisfied.",
                        EVALUATED_AT,
                        ACTOR,
                        1L);

        assertThatThrownBy(
                () -> EventValidationAlert.createFromFailedResult(
                        new ValidationAlertId(
                                UUID.randomUUID()),
                        satisfiedResult,
                        "Inconsistency.",
                        CREATED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation alert requires a failed source result");
    }

    @Test
    void rejectsInvalidatedSourceResult() {
        EventValidationResult invalidatedResult =
                failedResult(
                        ValidationRuleCode.VR_006)
                        .invalidate(
                                EVALUATED_AT.plusSeconds(
                                        30L),
                                "Event changed.");

        assertThatThrownBy(
                () -> EventValidationAlert.createFromFailedResult(
                        new ValidationAlertId(
                                UUID.randomUUID()),
                        invalidatedResult,
                        "Formal authorization is missing.",
                        CREATED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation alert requires a current source result");
    }

    @Test
    void rejectsCreationBeforeSourceEvaluation() {
        EventValidationResult failedResult =
                failedResult(
                        ValidationRuleCode.VR_002);

        assertThatThrownBy(
                () -> EventValidationAlert.createFromFailedResult(
                        new ValidationAlertId(
                                UUID.randomUUID()),
                        failedResult,
                        "Amounts differ.",
                        EVALUATED_AT.minusSeconds(
                                1L),
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "alert creation instant must not be before source evaluation");
    }

    @Test
    void rejectsSeverityThatDoesNotMatchCauseRule() {
        EventValidationResult sourceResult =
                failedResult(
                        ValidationRuleCode.VR_003);

        assertThatThrownBy(
                () -> new EventValidationAlert(
                        new ValidationAlertId(
                                UUID.randomUUID()),
                        sourceResult.eventId(),
                        sourceResult.id(),
                        ValidationRuleCode.VR_003,
                        ValidationSeverity.CRITICAL,
                        true,
                        ValidationAlertState.ACTIVE,
                        "Evidence is illegible.",
                        null,
                        null,
                        CREATED_AT,
                        ACTOR,
                        CREATED_AT,
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation alert severity must match its cause rule");
    }

    @Test
    void rejectsClosureMetadataForNonTerminalState() {
        EventValidationResult sourceResult =
                failedResult(
                        ValidationRuleCode.VR_001);

        assertThatThrownBy(
                () -> new EventValidationAlert(
                        new ValidationAlertId(
                                UUID.randomUUID()),
                        sourceResult.eventId(),
                        sourceResult.id(),
                        sourceResult.ruleCode(),
                        sourceResult.ruleCode().severity(),
                        true,
                        ValidationAlertState.ACTIVE,
                        "Authorization is missing.",
                        null,
                        null,
                        CREATED_AT,
                        ACTOR,
                        CREATED_AT,
                        CREATED_AT))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "non-terminal alert must not contain closure metadata");
    }

    @Test
    void exposesApprovedAlertStatesAndTerminalClassification() {
        assertThat(
                ValidationAlertState.values())
                .containsExactly(
                        ValidationAlertState.ACTIVE,
                        ValidationAlertState.ACKNOWLEDGED,
                        ValidationAlertState.UNDER_REVIEW,
                        ValidationAlertState.RESOLVED,
                        ValidationAlertState.DISCARDED);

        assertThat(
                ValidationAlertState.ACTIVE.terminal())
                .isFalse();

        assertThat(
                ValidationAlertState.UNDER_REVIEW.terminal())
                .isFalse();

        assertThat(
                ValidationAlertState.RESOLVED.terminal())
                .isTrue();

        assertThat(
                ValidationAlertState.DISCARDED.terminal())
                .isTrue();
    }

    @Test
    void validationAlertIdentifierRequiresUuid() {
        assertThatThrownBy(
                () -> new ValidationAlertId(
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation alert id must not be null");
    }

    private static EventValidationResult failedResult(
            ValidationRuleCode ruleCode) {

        return EventValidationResult.create(
                new ValidationResultId(
                        UUID.randomUUID()),
                ruleCode,
                1,
                new OperationalEventId(
                        UUID.randomUUID()),
                ValidationOutcome.FAILED,
                "Rule failed.",
                EVALUATED_AT,
                ACTOR,
                1L);
    }

}