package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OperationalDependencyRevisionTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "93fcf699-017e-4636-bf58-34b9cb050001"));

    private static final OperationalCloseId OTHER_CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "93fcf699-017e-4636-bf58-34b9cb050002"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "93fcf699-017e-4636-bf58-34b9cb050003"));

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-23T08:00:00Z");

    private static final Instant REVISED_AT =
            Instant.parse(
                    "2026-07-23T10:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Test
    void incrementsRevisionWithoutChangingRegisteredStates() {
        OperationalClose operationalClose =
                closeWithState(
                        CLOSE_ID,
                        OperationalCloseState.PREPARATION);

        OperationalEvent operationalEvent =
                eventWithState(
                        CLOSE_ID,
                        OperationalEventState.REGISTERED);

        OperationalDependencyRevision revision =
                OperationalDependencyRevision.apply(
                        operationalClose,
                        operationalEvent,
                        REVISED_AT,
                        ACTOR);

        assertThat(
                revision.revisedEvent()
                        .dataRevision())
                .isEqualTo(2L);

        assertThat(
                revision.revisedEvent()
                        .state())
                .isEqualTo(
                        OperationalEventState.REGISTERED);

        assertThat(
                revision.revisedEvent()
                        .stateChangedAt())
                .isEqualTo(
                        operationalEvent.stateChangedAt());

        assertThat(
                revision.revisedEvent()
                        .updatedAt())
                .isEqualTo(REVISED_AT);

        assertThat(
                revision.revisedEvent()
                        .updatedBy())
                .isEqualTo(ACTOR);

        assertThat(revision.revisedClose())
                .isSameAs(operationalClose);

        assertThat(revision.eventStateChanged())
                .isFalse();

        assertThat(revision.closeStateChanged())
                .isFalse();
    }

    @Test
    void resetsValidatedEventAndBlocksValidatedClose() {
        OperationalClose operationalClose =
                closeWithState(
                        CLOSE_ID,
                        OperationalCloseState.VALIDATED);

        OperationalEvent operationalEvent =
                eventWithState(
                        CLOSE_ID,
                        OperationalEventState.VALIDATED);

        OperationalDependencyRevision revision =
                OperationalDependencyRevision.apply(
                        operationalClose,
                        operationalEvent,
                        REVISED_AT,
                        ACTOR);

        assertThat(
                revision.revisedEvent()
                        .state())
                .isEqualTo(
                        OperationalEventState.REGISTERED);

        assertThat(
                revision.revisedEvent()
                        .stateChangedAt())
                .isEqualTo(REVISED_AT);

        assertThat(
                revision.revisedEvent()
                        .dataRevision())
                .isEqualTo(2L);

        assertThat(
                revision.revisedClose()
                        .state())
                .isEqualTo(
                        OperationalCloseState.BLOCKED);

        assertThat(
                revision.revisedClose()
                        .stateChangedAt())
                .isEqualTo(REVISED_AT);

        assertThat(
                revision.revisedClose()
                        .updatedAt())
                .isEqualTo(REVISED_AT);

        assertThat(
                revision.revisedClose()
                        .updatedBy())
                .isEqualTo(ACTOR);

        assertThat(revision.eventStateChanged())
                .isTrue();

        assertThat(revision.closeStateChanged())
                .isTrue();
    }

    @Test
    void preservesBlockedCloseWhileRevisingEvent() {
        OperationalClose operationalClose =
                closeWithState(
                        CLOSE_ID,
                        OperationalCloseState.BLOCKED);

        OperationalEvent operationalEvent =
                eventWithState(
                        CLOSE_ID,
                        OperationalEventState.OBSERVED);

        OperationalDependencyRevision revision =
                OperationalDependencyRevision.apply(
                        operationalClose,
                        operationalEvent,
                        REVISED_AT,
                        ACTOR);

        assertThat(revision.revisedClose())
                .isSameAs(operationalClose);

        assertThat(
                revision.revisedEvent()
                        .state())
                .isEqualTo(
                        OperationalEventState.OBSERVED);

        assertThat(
                revision.revisedEvent()
                        .dataRevision())
                .isEqualTo(2L);

        assertThat(revision.eventStateChanged())
                .isFalse();

        assertThat(revision.closeStateChanged())
                .isFalse();
    }

    @Test
    void rejectsSentClose() {
        OperationalClose operationalClose =
                closeWithState(
                        CLOSE_ID,
                        OperationalCloseState.SENT_TO_ACCOUNTING);

        OperationalEvent operationalEvent =
                eventWithState(
                        CLOSE_ID,
                        OperationalEventState.VALIDATED);

        assertThatThrownBy(
                () -> OperationalDependencyRevision.apply(
                        operationalClose,
                        operationalEvent,
                        REVISED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessageContaining(
                        "immutable");
    }

    @Test
    void rejectsEventFromAnotherClose() {
        OperationalClose operationalClose =
                closeWithState(
                        CLOSE_ID,
                        OperationalCloseState.PREPARATION);

        OperationalEvent operationalEvent =
                eventWithState(
                        OTHER_CLOSE_ID,
                        OperationalEventState.REGISTERED);

        assertThatThrownBy(
                () -> OperationalDependencyRevision.apply(
                        operationalClose,
                        operationalEvent,
                        REVISED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "belong");
    }

    @Test
    void rejectsTemporalRegression() {
        OperationalClose operationalClose =
                closeWithState(
                        CLOSE_ID,
                        OperationalCloseState.PREPARATION);

        OperationalEvent operationalEvent =
                eventWithState(
                        CLOSE_ID,
                        OperationalEventState.REGISTERED);

        assertThatThrownBy(
                () -> OperationalDependencyRevision.apply(
                        operationalClose,
                        operationalEvent,
                        CREATED_AT.minusSeconds(1),
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "event update");
    }

    private static OperationalClose closeWithState(
            OperationalCloseId closeId,
            OperationalCloseState state) {

        return new OperationalClose(
                closeId,
                new OperationalPeriod(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31)),
                new CurrencyCode("PEN"),
                new InitialBalance(
                        new BigDecimal("1000.0000")),
                state,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

    private static OperationalEvent eventWithState(
            OperationalCloseId closeId,
            OperationalEventState state) {

        return new OperationalEvent(
                EVENT_ID,
                closeId,
                OperationalEventType.EXPENSE,
                new OperationalEventAmount(
                        new BigDecimal("80.0000")),
                new BigDecimal("-80.0000"),
                null,
                CREATED_AT.minusSeconds(60),
                CREATED_AT,
                "Caja principal",
                "Gasto operativo",
                state,
                false,
                false,
                1L,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

}