package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OperationalCloseTest {

    @Test
    void createsCloseInPreparationWithAuditMetadata() {
        Instant now =
                Instant.parse(
                        "2026-07-20T05:00:00Z");

        AuditActor actor =
                new AuditActor(
                        "responsible-user",
                        "responsible");

        OperationalClose operationalClose =
                OperationalClose.create(
                        new OperationalCloseId(
                                UUID.fromString(
                                        "6c13876e-b9fe-49f8-a73b-1d32cbb01738")),
                        new OperationalPeriod(
                                LocalDate.of(2026, 7, 1),
                                LocalDate.of(2026, 7, 31)),
                        new CurrencyCode("PEN"),
                        new InitialBalance(
                                new BigDecimal(
                                        "100.0000")),
                        now,
                        actor);

        assertThat(operationalClose.state())
                .isEqualTo(
                        OperationalCloseState.PREPARATION);

        assertThat(operationalClose.createdAt())
                .isEqualTo(now);

        assertThat(operationalClose.updatedAt())
                .isEqualTo(now);

        assertThat(operationalClose.createdBy())
                .isEqualTo(actor);

        assertThat(operationalClose.updatedBy())
                .isEqualTo(actor);
    }

    @Test
    void rejectsActorOtherThanResponsibleUser() {
        assertThatThrownBy(
                () -> new AuditActor(
                        "another-user",
                        "another"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "responsible user");
    }

    @Test
    void rejectsStateTimelineBeforeCreation() {
        Instant createdAt =
                Instant.parse(
                        "2026-07-20T05:00:00Z");

        AuditActor actor =
                new AuditActor(
                        "responsible-user",
                        "responsible");

        assertThatThrownBy(
                () -> new OperationalClose(
                        new OperationalCloseId(
                                UUID.randomUUID()),
                        new OperationalPeriod(
                                LocalDate.of(2026, 7, 1),
                                LocalDate.of(2026, 7, 31)),
                        new CurrencyCode("PEN"),
                        new InitialBalance(
                                BigDecimal.ZERO),
                        OperationalCloseState.PREPARATION,
                        createdAt.minusSeconds(1),
                        createdAt,
                        actor,
                        createdAt,
                        actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "state change instant");
    }

}
