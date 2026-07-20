package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CloseStateTransitionTest {

    @Test
    void createsInitialTransitionToPreparation() {
        AuditActor actor =
                new AuditActor(
                        "responsible-user",
                        "responsible");

        Instant occurredAt =
                Instant.parse(
                        "2026-07-20T05:00:00Z");

        CloseStateTransition transition =
                CloseStateTransition.initial(
                        new CloseStateTransitionId(
                                UUID.randomUUID()),
                        new OperationalCloseId(
                                UUID.randomUUID()),
                        occurredAt,
                        actor);

        assertThat(transition.fromState())
                .isNull();

        assertThat(transition.toState())
                .isEqualTo(
                        OperationalCloseState.PREPARATION);

        assertThat(transition.causeCode())
                .isEqualTo(
                        CloseStateTransition.CLOSE_CREATED);

        assertThat(transition.occurredAt())
                .isEqualTo(occurredAt);
    }

    @Test
    void rejectsTransitionWithoutStateChange() {
        assertThatThrownBy(
                () -> new CloseStateTransition(
                        new CloseStateTransitionId(
                                UUID.randomUUID()),
                        new OperationalCloseId(
                                UUID.randomUUID()),
                        OperationalCloseState.BLOCKED,
                        OperationalCloseState.BLOCKED,
                        "REVALIDATION_FAILED",
                        null,
                        Instant.now(),
                        new AuditActor(
                                "responsible-user",
                                "responsible")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "must change the state");
    }

}
