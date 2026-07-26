package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ValidationAlertTransitionTest {

    private static final Instant OCCURRED_AT =
            Instant.parse(
                    "2026-07-25T13:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Test
    void createsInitialActiveTransition() {
        ValidationAlertTransition transition =
                ValidationAlertTransition.initial(
                        transitionId(),
                        alertId(),
                        " Validation rule failed. ",
                        OCCURRED_AT,
                        ACTOR);

        assertThat(
                transition.fromState())
                .isNull();

        assertThat(
                transition.toState())
                .isEqualTo(
                        ValidationAlertState.ACTIVE);

        assertThat(
                transition.actionCode())
                .isEqualTo(
                        ValidationAlertTransition.ALERT_CREATED);

        assertThat(
                transition.detail())
                .isEqualTo(
                        "Validation rule failed.");

        assertThat(
                transition.justification())
                .isNull();

        assertThat(
                transition.validationResultId())
                .isNull();
    }

    @Test
    void createsAcknowledgementTransition() {
        ValidationAlertTransition transition =
                ValidationAlertTransition.acknowledged(
                        transitionId(),
                        alertId(),
                        "Alert acknowledged.",
                        OCCURRED_AT,
                        ACTOR);

        assertThat(
                transition.fromState())
                .isEqualTo(
                        ValidationAlertState.ACTIVE);

        assertThat(
                transition.toState())
                .isEqualTo(
                        ValidationAlertState.ACKNOWLEDGED);

        assertThat(
                transition.actionCode())
                .isEqualTo(
                        ValidationAlertTransition.ALERT_ACKNOWLEDGED);
    }

    @Test
    void createsReviewTransitionFromApprovedStates() {
        ValidationAlertTransition fromActive =
                ValidationAlertTransition.underReview(
                        transitionId(),
                        alertId(),
                        ValidationAlertState.ACTIVE,
                        null,
                        OCCURRED_AT,
                        ACTOR);

        ValidationAlertTransition fromAcknowledged =
                ValidationAlertTransition.underReview(
                        transitionId(),
                        alertId(),
                        ValidationAlertState.ACKNOWLEDGED,
                        "Correction is being reviewed.",
                        OCCURRED_AT,
                        ACTOR);

        assertThat(
                fromActive.toState())
                .isEqualTo(
                        ValidationAlertState.UNDER_REVIEW);

        assertThat(
                fromAcknowledged.fromState())
                .isEqualTo(
                        ValidationAlertState.ACKNOWLEDGED);

        assertThat(
                fromAcknowledged.actionCode())
                .isEqualTo(
                        ValidationAlertTransition.ALERT_UNDER_REVIEW);
    }

    @Test
    void rejectsReviewFromUnsupportedState() {
        assertThatThrownBy(
                () -> ValidationAlertTransition.underReview(
                        transitionId(),
                        alertId(),
                        ValidationAlertState.UNDER_REVIEW,
                        null,
                        OCCURRED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "review transition requires active or acknowledged state");
    }

    @Test
    void createsResolutionWithValidationResult() {
        ValidationResultId validationResultId =
                validationResultId();

        ValidationAlertTransition transition =
                ValidationAlertTransition.resolved(
                        transitionId(),
                        alertId(),
                        ValidationAlertState.UNDER_REVIEW,
                        "Revalidation satisfied the rule.",
                        validationResultId,
                        OCCURRED_AT,
                        ACTOR);

        assertThat(
                transition.toState())
                .isEqualTo(
                        ValidationAlertState.RESOLVED);

        assertThat(
                transition.actionCode())
                .isEqualTo(
                        ValidationAlertTransition.ALERT_RESOLVED);

        assertThat(
                transition.validationResultId())
                .isEqualTo(
                        validationResultId);

        assertThat(
                transition.justification())
                .isNull();
    }

    @Test
    void resolutionRequiresValidationResult() {
        assertThatThrownBy(
                () -> ValidationAlertTransition.resolved(
                        transitionId(),
                        alertId(),
                        ValidationAlertState.ACTIVE,
                        "Resolved.",
                        null,
                        OCCURRED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validationResultId must not be null");
    }

    @Test
    void createsDiscardWithMandatoryJustification() {
        ValidationAlertTransition transition =
                ValidationAlertTransition.discarded(
                        transitionId(),
                        alertId(),
                        ValidationAlertState.ACKNOWLEDGED,
                        "Alert is not applicable.",
                        " Approved operational exception. ",
                        OCCURRED_AT,
                        ACTOR);

        assertThat(
                transition.toState())
                .isEqualTo(
                        ValidationAlertState.DISCARDED);

        assertThat(
                transition.actionCode())
                .isEqualTo(
                        ValidationAlertTransition.ALERT_DISCARDED);

        assertThat(
                transition.justification())
                .isEqualTo(
                        "Approved operational exception.");

        assertThat(
                transition.validationResultId())
                .isNull();
    }

    @Test
    void discardRequiresJustification() {
        assertThatThrownBy(
                () -> ValidationAlertTransition.discarded(
                        transitionId(),
                        alertId(),
                        ValidationAlertState.ACTIVE,
                        null,
                        " ",
                        OCCURRED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "justification must not be blank");
    }

    @Test
    void rejectsTerminalSourceState() {
        assertThatThrownBy(
                () -> ValidationAlertTransition.resolved(
                        transitionId(),
                        alertId(),
                        ValidationAlertState.DISCARDED,
                        null,
                        validationResultId(),
                        OCCURRED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "resolution transition requires a non-terminal state");

        assertThatThrownBy(
                () -> ValidationAlertTransition.discarded(
                        transitionId(),
                        alertId(),
                        ValidationAlertState.RESOLVED,
                        null,
                        "Authorized exception.",
                        OCCURRED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "discard transition requires a non-terminal state");
    }

    @Test
    void rejectsTransitionWithoutStateChange() {
        assertThatThrownBy(
                () -> new ValidationAlertTransition(
                        transitionId(),
                        alertId(),
                        ValidationAlertState.ACTIVE,
                        ValidationAlertState.ACTIVE,
                        "INVALID",
                        null,
                        null,
                        null,
                        OCCURRED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "alert transition must change the state");
    }

    @Test
    void rejectsClosureMetadataForNonTerminalTransition() {
        assertThatThrownBy(
                () -> new ValidationAlertTransition(
                        transitionId(),
                        alertId(),
                        ValidationAlertState.ACTIVE,
                        ValidationAlertState.ACKNOWLEDGED,
                        ValidationAlertTransition.ALERT_ACKNOWLEDGED,
                        null,
                        null,
                        validationResultId(),
                        OCCURRED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "non-terminal alert transition must not contain closure metadata");
    }

    @Test
    void rejectsLongOrBlankActionCode() {
        assertThatThrownBy(
                () -> new ValidationAlertTransition(
                        transitionId(),
                        alertId(),
                        null,
                        ValidationAlertState.ACTIVE,
                        " ",
                        null,
                        null,
                        null,
                        OCCURRED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "actionCode must not be blank");

        assertThatThrownBy(
                () -> new ValidationAlertTransition(
                        transitionId(),
                        alertId(),
                        null,
                        ValidationAlertState.ACTIVE,
                        "A".repeat(
                                41),
                        null,
                        null,
                        null,
                        OCCURRED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "alert transition action code must not exceed 40 characters");
    }

    @Test
    void transitionIdentifierRequiresUuid() {
        assertThatThrownBy(
                () -> new ValidationAlertTransitionId(
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation alert transition id must not be null");
    }

    private static ValidationAlertTransitionId transitionId() {
        return new ValidationAlertTransitionId(
                UUID.randomUUID());
    }

    private static ValidationAlertId alertId() {
        return new ValidationAlertId(
                UUID.randomUUID());
    }

    private static ValidationResultId validationResultId() {
        return new ValidationResultId(
                UUID.randomUUID());
    }

}