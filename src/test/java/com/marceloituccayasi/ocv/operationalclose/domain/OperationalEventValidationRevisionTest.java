package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OperationalEventValidationRevisionTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "15b81708-03bf-498f-a899-190000000001"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "15b81708-03bf-498f-a899-190000000002"));

    private static final EventStateTransitionId TRANSITION_ID =
            new EventStateTransitionId(
                    UUID.fromString(
                            "15b81708-03bf-498f-a899-190000000003"));

    private static final Instant REGISTERED_AT =
            Instant.parse(
                    "2026-07-28T14:00:00Z");

    private static final Instant VALIDATED_AT =
            Instant.parse(
                    "2026-07-28T15:00:00Z");

    private static final Instant REVALIDATED_AT =
            Instant.parse(
                    "2026-07-28T16:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Test
    void appliesValidationStateAndCreatesTransition() {
        OperationalEvent originalEvent =
                event();

        OperationalEventValidationRevision revision =
                OperationalEventValidationRevision.apply(
                        originalEvent,
                        OperationalEventState.VALIDATED,
                        TRANSITION_ID,
                        VALIDATED_AT,
                        ACTOR);

        assertThat(
                revision.stateChanged())
                .isTrue();

        assertThat(
                revision.revisedEvent()
                        .state())
                .isEqualTo(
                        OperationalEventState.VALIDATED);

        assertThat(
                revision.revisedEvent()
                        .stateChangedAt())
                .isEqualTo(
                        VALIDATED_AT);

        assertThat(
                revision.revisedEvent()
                        .updatedAt())
                .isEqualTo(
                        VALIDATED_AT);

        assertThat(
                revision.revisedEvent()
                        .updatedBy())
                .isEqualTo(
                        ACTOR);

        assertThat(
                revision.revisedEvent()
                        .dataRevision())
                .isEqualTo(
                        originalEvent.dataRevision());

        assertThat(
                revision.revisedEvent()
                        .amount())
                .isEqualTo(
                        originalEvent.amount());

        assertThat(
                revision.revisedEvent()
                        .balanceEffect())
                .isEqualTo(
                        originalEvent.balanceEffect());

        EventStateTransition transition =
                revision.stateTransition();

        assertThat(transition)
                .isNotNull();

        assertThat(
                transition.id())
                .isEqualTo(
                        TRANSITION_ID);

        assertThat(
                transition.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(
                transition.fromState())
                .isEqualTo(
                        OperationalEventState.REGISTERED);

        assertThat(
                transition.toState())
                .isEqualTo(
                        OperationalEventState.VALIDATED);

        assertThat(
                transition.causeCode())
                .isEqualTo(
                        OperationalEventValidationRevision
                                .EVENT_VALIDATION_APPLIED);

        assertThat(
                transition.validationResultId())
                .isNull();

        assertThat(
                transition.occurredAt())
                .isEqualTo(
                        VALIDATED_AT);
    }

    @Test
    void appliesFailedValidationState() {
        OperationalEventValidationRevision revision =
                OperationalEventValidationRevision.apply(
                        event(),
                        OperationalEventState.PENDING_SUPPORT,
                        TRANSITION_ID,
                        VALIDATED_AT,
                        ACTOR);

        assertThat(
                revision.revisedEvent()
                        .state())
                .isEqualTo(
                        OperationalEventState
                                .PENDING_SUPPORT);

        assertThat(
                revision.stateTransition()
                        .toState())
                .isEqualTo(
                        OperationalEventState
                                .PENDING_SUPPORT);
    }

    @Test
    void returnsOriginalEventWhenStateDoesNotChange() {
        OperationalEvent validatedEvent =
                OperationalEventValidationRevision
                        .apply(
                                event(),
                                OperationalEventState.VALIDATED,
                                TRANSITION_ID,
                                VALIDATED_AT,
                                ACTOR)
                        .revisedEvent();

        OperationalEventValidationRevision revision =
                OperationalEventValidationRevision.apply(
                        validatedEvent,
                        OperationalEventState.VALIDATED,
                        null,
                        REVALIDATED_AT,
                        ACTOR);

        assertThat(
                revision.stateChanged())
                .isFalse();

        assertThat(
                revision.revisedEvent())
                .isSameAs(
                        validatedEvent);

        assertThat(
                revision.stateTransition())
                .isNull();
    }

    @Test
    void rejectsRegisteredAsValidationResultingState() {
        assertThatThrownBy(
                () -> OperationalEventValidationRevision
                        .apply(
                                event(),
                                OperationalEventState.REGISTERED,
                                TRANSITION_ID,
                                VALIDATED_AT,
                                ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation resulting state must not be registered");
    }

    @Test
    void requiresTransitionIdentifierWhenStateChanges() {
        assertThatThrownBy(
                () -> OperationalEventValidationRevision
                        .apply(
                                event(),
                                OperationalEventState.VALIDATED,
                                null,
                                VALIDATED_AT,
                                ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "transitionId must not be null");
    }

    @Test
    void rejectsValidationInstantBeforeEventUpdate() {
        assertThatThrownBy(
                () -> OperationalEventValidationRevision
                        .apply(
                                event(),
                                OperationalEventState.VALIDATED,
                                TRANSITION_ID,
                                REGISTERED_AT.minusSeconds(
                                        1L),
                                ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation state instant must not be before event update");
    }

    private static OperationalEvent event() {
        return OperationalEvent.create(
                EVENT_ID,
                CLOSE_ID,
                OperationalEventType.EXPENSE,
                new OperationalEventAmount(
                        new BigDecimal(
                                "75.0000")),
                REGISTERED_AT.minusSeconds(
                        60L),
                "Caja principal",
                "Evento para aplicar estado de validación",
                true,
                false,
                REGISTERED_AT,
                ACTOR);
    }

}