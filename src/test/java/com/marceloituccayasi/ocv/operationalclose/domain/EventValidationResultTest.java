package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class EventValidationResultTest {

    private static final Instant EVALUATED_AT =
            Instant.parse(
                    "2026-07-25T10:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Test
    void createsCurrentImmutableEventResult() {
        ValidationResultId resultId =
                new ValidationResultId(
                        UUID.randomUUID());

        OperationalEventId eventId =
                new OperationalEventId(
                        UUID.randomUUID());

        EventValidationResult result =
                EventValidationResult.create(
                        resultId,
                        ValidationRuleCode.VR_002,
                        1,
                        eventId,
                        ValidationOutcome.FAILED,
                        " Registered amount differs from evidence amount. ",
                        EVALUATED_AT,
                        ACTOR,
                        7L);

        assertThat(
                result.id())
                .isEqualTo(
                        resultId);

        assertThat(
                result.ruleCode())
                .isEqualTo(
                        ValidationRuleCode.VR_002);

        assertThat(
                result.ruleVersion())
                .isEqualTo(
                        1);

        assertThat(
                result.eventId())
                .isEqualTo(
                        eventId);

        assertThat(
                result.outcome())
                .isEqualTo(
                        ValidationOutcome.FAILED);

        assertThat(
                result.detail())
                .isEqualTo(
                        "Registered amount differs from evidence amount.");

        assertThat(
                result.eventDataRevision())
                .isEqualTo(
                        7L);

        assertThat(
                result.current())
                .isTrue();

        assertThat(
                result.invalidatedAt())
                .isNull();

        assertThat(
                result.invalidationReason())
                .isNull();
    }

    @Test
    void rejectsCloseScopedRuleForEventResult() {
        assertThatThrownBy(
                () -> EventValidationResult.create(
                        new ValidationResultId(
                                UUID.randomUUID()),
                        ValidationRuleCode.VR_008,
                        1,
                        new OperationalEventId(
                                UUID.randomUUID()),
                        ValidationOutcome.SATISFIED,
                        "Close control passed.",
                        EVALUATED_AT,
                        ACTOR,
                        1L))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "event validation result requires an event-scoped rule");
    }

    @Test
    void rejectsInvalidRuleVersionRevisionAndDetail() {
        assertThatThrownBy(
                () -> EventValidationResult.create(
                        new ValidationResultId(
                                UUID.randomUUID()),
                        ValidationRuleCode.VR_001,
                        0,
                        new OperationalEventId(
                                UUID.randomUUID()),
                        ValidationOutcome.SATISFIED,
                        "Valid.",
                        EVALUATED_AT,
                        ACTOR,
                        1L))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation rule version must be at least one");

        assertThatThrownBy(
                () -> EventValidationResult.create(
                        new ValidationResultId(
                                UUID.randomUUID()),
                        ValidationRuleCode.VR_001,
                        1,
                        new OperationalEventId(
                                UUID.randomUUID()),
                        ValidationOutcome.SATISFIED,
                        "Valid.",
                        EVALUATED_AT,
                        ACTOR,
                        0L))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "event data revision must be at least one");

        assertThatThrownBy(
                () -> EventValidationResult.create(
                        new ValidationResultId(
                                UUID.randomUUID()),
                        ValidationRuleCode.VR_001,
                        1,
                        new OperationalEventId(
                                UUID.randomUUID()),
                        ValidationOutcome.SATISFIED,
                        " ",
                        EVALUATED_AT,
                        ACTOR,
                        1L))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "detail must not be blank");
    }

    @Test
    void invalidatesWithoutRewritingEvaluationContent() {
        EventValidationResult currentResult =
                EventValidationResult.create(
                        new ValidationResultId(
                                UUID.randomUUID()),
                        ValidationRuleCode.VR_003,
                        1,
                        new OperationalEventId(
                                UUID.randomUUID()),
                        ValidationOutcome.FAILED,
                        "Required evidence is not legible.",
                        EVALUATED_AT,
                        ACTOR,
                        4L);

        Instant invalidatedAt =
                Instant.parse(
                        "2026-07-25T11:00:00Z");

        EventValidationResult invalidatedResult =
                currentResult.invalidate(
                        invalidatedAt,
                        " Event supporting information changed. ");

        assertThat(
                invalidatedResult.id())
                .isEqualTo(
                        currentResult.id());

        assertThat(
                invalidatedResult.ruleCode())
                .isEqualTo(
                        currentResult.ruleCode());

        assertThat(
                invalidatedResult.outcome())
                .isEqualTo(
                        currentResult.outcome());

        assertThat(
                invalidatedResult.detail())
                .isEqualTo(
                        currentResult.detail());

        assertThat(
                invalidatedResult.evaluatedAt())
                .isEqualTo(
                        currentResult.evaluatedAt());

        assertThat(
                invalidatedResult.evaluatedBy())
                .isEqualTo(
                        currentResult.evaluatedBy());

        assertThat(
                invalidatedResult.eventDataRevision())
                .isEqualTo(
                        currentResult.eventDataRevision());

        assertThat(
                invalidatedResult.current())
                .isFalse();

        assertThat(
                invalidatedResult.invalidatedAt())
                .isEqualTo(
                        invalidatedAt);

        assertThat(
                invalidatedResult.invalidationReason())
                .isEqualTo(
                        "Event supporting information changed.");
    }

    @Test
    void rejectsInvalidationBeforeEvaluationOrRepeatedInvalidation() {
        EventValidationResult result =
                EventValidationResult.create(
                        new ValidationResultId(
                                UUID.randomUUID()),
                        ValidationRuleCode.VR_006,
                        1,
                        new OperationalEventId(
                                UUID.randomUUID()),
                        ValidationOutcome.FAILED,
                        "Formal authorization is missing.",
                        EVALUATED_AT,
                        ACTOR,
                        2L);

        assertThatThrownBy(
                () -> result.invalidate(
                        EVALUATED_AT.minusSeconds(
                                1L),
                        "Event changed."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "invalidation instant must not be before evaluation");

        EventValidationResult invalidated =
                result.invalidate(
                        EVALUATED_AT.plusSeconds(
                                1L),
                        "Event changed.");

        assertThatThrownBy(
                () -> invalidated.invalidate(
                        EVALUATED_AT.plusSeconds(
                                2L),
                        "Changed again."))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "validation result is already invalidated");
    }

    @Test
    void validationResultIdentifierRequiresUuid() {
        assertThatThrownBy(
                () -> new ValidationResultId(
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation result id must not be null");
    }

}