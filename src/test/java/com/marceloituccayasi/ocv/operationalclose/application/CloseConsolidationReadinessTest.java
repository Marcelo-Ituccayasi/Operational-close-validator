package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

class CloseConsolidationReadinessTest {

    private static final OperationalEventId FIRST_EVENT_ID =
            eventId(
                    "49a2f434-d0f1-4cb9-b911-750000000001");

    private static final OperationalEventId SECOND_EVENT_ID =
            eventId(
                    "49a2f434-d0f1-4cb9-b911-750000000002");

    @Test
    void reportsNoEventsAsNotReady() {
        CloseConsolidationReadiness readiness =
                CloseConsolidationReadiness.noEvents();

        assertThat(
                readiness.eventsPresent())
                .isFalse();

        assertThat(
                readiness.ready())
                .isFalse();

        assertThat(
                readiness.affectedEventIds())
                .isEmpty();
    }

    @Test
    void reportsReadyWhenNoIssuesExist() {
        CloseConsolidationReadiness readiness =
                CloseConsolidationReadiness.evaluated(
                        List.of(),
                        List.of(),
                        List.of());

        assertThat(
                readiness.eventsPresent())
                .isTrue();

        assertThat(
                readiness.ready())
                .isTrue();

        assertThat(
                readiness.affectedEventIds())
                .isEmpty();
    }

    @Test
    void normalizesAndCombinesAffectedEvents() {
        CloseConsolidationReadiness readiness =
                CloseConsolidationReadiness.evaluated(
                        List.of(
                                SECOND_EVENT_ID,
                                FIRST_EVENT_ID),
                        List.of(
                                FIRST_EVENT_ID),
                        List.of(
                                SECOND_EVENT_ID));

        assertThat(
                readiness.ready())
                .isFalse();

        assertThat(
                readiness.notValidatedEventIds())
                .containsExactly(
                        FIRST_EVENT_ID,
                        SECOND_EVENT_ID);

        assertThat(
                readiness.affectedEventIds())
                .containsExactly(
                        FIRST_EVENT_ID,
                        SECOND_EVENT_ID);
    }

    @Test
    void rejectsIssuesWhenNoEventsExist() {
        assertThatThrownBy(
                () -> new CloseConsolidationReadiness(
                        false,
                        List.of(
                                FIRST_EVENT_ID),
                        List.of(),
                        List.of()))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "readiness without events must not "
                                + "contain event issues");
    }

    private static OperationalEventId eventId(
            String value) {

        return new OperationalEventId(
                UUID.fromString(
                        value));
    }

}