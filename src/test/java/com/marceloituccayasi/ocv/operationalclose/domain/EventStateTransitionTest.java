package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class EventStateTransitionTest {

    private static final Instant NOW =
            Instant.parse("2026-07-22T10:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    "responsible-user",
                    "responsible");

    @Test
    void createsInitialRegisteredTransition() {
        EventStateTransition transition =
                EventStateTransition.initial(
                        transitionId(
                                "20ae2c4d-53ea-40da-8412-830a694d0001"),
                        eventId(
                                "20ae2c4d-53ea-40da-8412-830a694d0002"),
                        NOW,
                        ACTOR);

        assertThat(transition.fromState())
                .isNull();

        assertThat(transition.toState())
                .isEqualTo(
                        OperationalEventState.REGISTERED);

        assertThat(transition.causeCode())
                .isEqualTo(
                        EventStateTransition.EVENT_CREATED);

        assertThat(transition.validationResultId())
                .isNull();
    }

    @Test
    void rejectsTransitionWithoutStateChange() {
        assertThatThrownBy(
                () -> new EventStateTransition(
                        transitionId(
                                "20ae2c4d-53ea-40da-8412-830a694d0003"),
                        eventId(
                                "20ae2c4d-53ea-40da-8412-830a694d0004"),
                        OperationalEventState.REGISTERED,
                        OperationalEventState.REGISTERED,
                        "DATA_REVISED",
                        null,
                        null,
                        NOW,
                        ACTOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "must change the state");
    }

    @Test
    void rejectsBlankCauseCode() {
        assertThatThrownBy(
                () -> new EventStateTransition(
                        transitionId(
                                "20ae2c4d-53ea-40da-8412-830a694d0005"),
                        eventId(
                                "20ae2c4d-53ea-40da-8412-830a694d0006"),
                        null,
                        OperationalEventState.REGISTERED,
                        " ",
                        null,
                        null,
                        NOW,
                        ACTOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "cause code");
    }

    private static EventStateTransitionId transitionId(
            String value) {

        return new EventStateTransitionId(
                UUID.fromString(value));
    }

    private static OperationalEventId eventId(
            String value) {

        return new OperationalEventId(
                UUID.fromString(value));
    }

}