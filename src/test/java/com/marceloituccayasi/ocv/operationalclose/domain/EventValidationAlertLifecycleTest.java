package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class EventValidationAlertLifecycleTest {

    private static final Instant EVALUATED_AT =
            Instant.parse(
                    "2026-07-25T14:00:00Z");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-25T14:01:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Test
    void acknowledgesActiveAlertAndProducesTransition() {
        EventValidationAlert alert =
                activeAlert(
                        ValidationRuleCode.VR_001);

        Instant occurredAt =
                CREATED_AT.plusSeconds(
                        60L);

        EventValidationAlertChange change =
                alert.acknowledge(
                        transitionId(),
                        " Alert acknowledged. ",
                        occurredAt,
                        ACTOR);

        assertThat(
                change.alert().state())
                .isEqualTo(
                        ValidationAlertState.ACKNOWLEDGED);

        assertThat(
                change.alert().updatedAt())
                .isEqualTo(
                        occurredAt);

        assertThat(
                change.alert().closedAt())
                .isNull();

        assertThat(
                change.transition().fromState())
                .isEqualTo(
                        ValidationAlertState.ACTIVE);

        assertThat(
                change.transition().toState())
                .isEqualTo(
                        ValidationAlertState.ACKNOWLEDGED);

        assertThat(
                change.transition().actionCode())
                .isEqualTo(
                        ValidationAlertTransition.ALERT_ACKNOWLEDGED);
    }

    @Test
    void startsReviewFromAcknowledgedAlert() {
        EventValidationAlert acknowledged =
                activeAlert(
                        ValidationRuleCode.VR_003)
                        .acknowledge(
                                transitionId(),
                                null,
                                CREATED_AT.plusSeconds(
                                        30L),
                                ACTOR)
                        .alert();

        Instant reviewAt =
                CREATED_AT.plusSeconds(
                        60L);

        EventValidationAlertChange change =
                acknowledged.startReview(
                        transitionId(),
                        "Evidence correction is being reviewed.",
                        reviewAt,
                        ACTOR);

        assertThat(
                change.alert().state())
                .isEqualTo(
                        ValidationAlertState.UNDER_REVIEW);

        assertThat(
                change.transition().fromState())
                .isEqualTo(
                        ValidationAlertState.ACKNOWLEDGED);

        assertThat(
                change.transition().toState())
                .isEqualTo(
                        ValidationAlertState.UNDER_REVIEW);
    }

    @Test
    void resolvesAlertWithCurrentSatisfiedResultForSameRuleAndEvent() {
        EventValidationAlert alert =
                activeAlert(
                        ValidationRuleCode.VR_006);

        EventValidationResult resolutionResult =
                EventValidationResult.create(
                        new ValidationResultId(
                                UUID.randomUUID()),
                        ValidationRuleCode.VR_006,
                        1,
                        alert.eventId(),
                        ValidationOutcome.SATISFIED,
                        "Formal authorization is present.",
                        CREATED_AT.plusSeconds(
                                60L),
                        ACTOR,
                        2L);

        Instant resolvedAt =
                CREATED_AT.plusSeconds(
                        120L);

        EventValidationAlertChange change =
                alert.resolve(
                        transitionId(),
                        resolutionResult,
                        "Revalidation satisfied the rule.",
                        resolvedAt,
                        ACTOR);

        assertThat(
                change.alert().state())
                .isEqualTo(
                        ValidationAlertState.RESOLVED);

        assertThat(
                change.alert().resolvedByValidationResultId())
                .isEqualTo(
                        resolutionResult.id());

        assertThat(
                change.alert().closedAt())
                .isEqualTo(
                        resolvedAt);

        assertThat(
                change.transition().validationResultId())
                .isEqualTo(
                        resolutionResult.id());

        assertThat(
                change.transition().toState())
                .isEqualTo(
                        ValidationAlertState.RESOLVED);
    }

    @Test
    void rejectsFailedInvalidatedOrIncompatibleResolutionResult() {
        EventValidationAlert alert =
                activeAlert(
                        ValidationRuleCode.VR_002);

        EventValidationResult failed =
                result(
                        alert.eventId(),
                        ValidationRuleCode.VR_002,
                        ValidationOutcome.FAILED);

        assertThatThrownBy(
                () -> alert.resolve(
                        transitionId(),
                        failed,
                        null,
                        CREATED_AT.plusSeconds(
                                120L),
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "resolution requires a satisfied validation result");

        EventValidationResult invalidated =
                result(
                        alert.eventId(),
                        ValidationRuleCode.VR_002,
                        ValidationOutcome.SATISFIED)
                        .invalidate(
                                CREATED_AT.plusSeconds(
                                        90L),
                                "Event changed.");

        assertThatThrownBy(
                () -> alert.resolve(
                        transitionId(),
                        invalidated,
                        null,
                        CREATED_AT.plusSeconds(
                                120L),
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "resolution requires a current validation result");

        EventValidationResult differentRule =
                result(
                        alert.eventId(),
                        ValidationRuleCode.VR_001,
                        ValidationOutcome.SATISFIED);

        assertThatThrownBy(
                () -> alert.resolve(
                        transitionId(),
                        differentRule,
                        null,
                        CREATED_AT.plusSeconds(
                                120L),
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "resolution result must satisfy the alert cause rule");

        EventValidationResult differentEvent =
                result(
                        new OperationalEventId(
                                UUID.randomUUID()),
                        ValidationRuleCode.VR_002,
                        ValidationOutcome.SATISFIED);

        assertThatThrownBy(
                () -> alert.resolve(
                        transitionId(),
                        differentEvent,
                        null,
                        CREATED_AT.plusSeconds(
                                120L),
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "resolution result must evaluate the affected event");
    }

    @Test
    void discardsOpenAlertWithJustification() {
        EventValidationAlert alert =
                activeAlert(
                        ValidationRuleCode.VR_003);

        Instant discardedAt =
                CREATED_AT.plusSeconds(
                        60L);

        EventValidationAlertChange change =
                alert.discard(
                        transitionId(),
                        "Alert classified as not applicable.",
                        " Authorized operational exception. ",
                        discardedAt,
                        ACTOR);

        assertThat(
                change.alert().state())
                .isEqualTo(
                        ValidationAlertState.DISCARDED);

        assertThat(
                change.alert().discardJustification())
                .isEqualTo(
                        "Authorized operational exception.");

        assertThat(
                change.alert().closedAt())
                .isEqualTo(
                        discardedAt);

        assertThat(
                change.transition().justification())
                .isEqualTo(
                        "Authorized operational exception.");
    }

    @Test
    void rejectsLifecycleChangesAfterTerminalState() {
        EventValidationAlert discarded =
                activeAlert(
                        ValidationRuleCode.VR_001)
                        .discard(
                                transitionId(),
                                null,
                                "Authorized exception.",
                                CREATED_AT.plusSeconds(
                                        60L),
                                ACTOR)
                        .alert();

        assertThatThrownBy(
                () -> discarded.startReview(
                        transitionId(),
                        null,
                        CREATED_AT.plusSeconds(
                                120L),
                        ACTOR))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "validation alert review requires active or acknowledged state");

        assertThatThrownBy(
                () -> discarded.discard(
                        transitionId(),
                        null,
                        "Second discard.",
                        CREATED_AT.plusSeconds(
                                120L),
                        ACTOR))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "terminal validation alert cannot change state");
    }

    @Test
    void rejectsLifecycleInstantBeforePreviousUpdate() {
        EventValidationAlert acknowledged =
                activeAlert(
                        ValidationRuleCode.VR_006)
                        .acknowledge(
                                transitionId(),
                                null,
                                CREATED_AT.plusSeconds(
                                        60L),
                                ACTOR)
                        .alert();

        assertThatThrownBy(
                () -> acknowledged.startReview(
                        transitionId(),
                        null,
                        CREATED_AT.plusSeconds(
                                30L),
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "alert lifecycle instant must not be before previous update");
    }

    @Test
    void changeRequiresMatchingAlertTransitionAndInstant() {
        EventValidationAlert alert =
                activeAlert(
                        ValidationRuleCode.VR_001);

        ValidationAlertTransition unrelatedTransition =
                ValidationAlertTransition.acknowledged(
                        transitionId(),
                        new ValidationAlertId(
                                UUID.randomUUID()),
                        null,
                        CREATED_AT.plusSeconds(
                                60L),
                        ACTOR);

        assertThatThrownBy(
                () -> new EventValidationAlertChange(
                        alert,
                        unrelatedTransition))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation alert transition must belong to updated alert");
    }

    private static EventValidationAlert activeAlert(
            ValidationRuleCode ruleCode) {

        EventValidationResult failedResult =
                EventValidationResult.create(
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

        return EventValidationAlert.createFromFailedResult(
                new ValidationAlertId(
                        UUID.randomUUID()),
                failedResult,
                "Validation inconsistency.",
                CREATED_AT,
                ACTOR);
    }

    private static EventValidationResult result(
            OperationalEventId eventId,
            ValidationRuleCode ruleCode,
            ValidationOutcome outcome) {

        return EventValidationResult.create(
                new ValidationResultId(
                        UUID.randomUUID()),
                ruleCode,
                1,
                eventId,
                outcome,
                "Revalidation result.",
                CREATED_AT.plusSeconds(
                        60L),
                ACTOR,
                2L);
    }

    private static ValidationAlertTransitionId transitionId() {
        return new ValidationAlertTransitionId(
                UUID.randomUUID());
    }

}