package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.CloseStateTransitionEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.OperationalCloseEntity;

class OperationalClosePersistenceMapperTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "6c13876e-b9fe-49f8-a73b-1d32cbb01738");

    private static final UUID TRANSITION_ID =
            UUID.fromString(
                    "96bec50f-6525-46cb-b3b1-c5a7185a22db");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-20T05:00:00Z");

    private final OperationalClosePersistenceMapper mapper =
            new OperationalClosePersistenceMapper();

    @Test
    void mapsOperationalCloseFromDomainToEntity() {
        OperationalClose operationalClose =
                operationalClose();

        OperationalCloseEntity entity =
                mapper.toEntity(operationalClose);

        assertThat(entity.id())
                .isEqualTo(CLOSE_ID);

        assertThat(entity.periodStart())
                .isEqualTo(
                        LocalDate.of(2026, 7, 1));

        assertThat(entity.periodEnd())
                .isEqualTo(
                        LocalDate.of(2026, 7, 31));

        assertThat(entity.currencyCode())
                .isEqualTo("PEN");

        assertThat(entity.initialBalance())
                .isEqualByComparingTo("100.2500");

        assertThat(entity.state())
                .isEqualTo("PREPARATION");

        assertThat(entity.createdByUserId())
                .isEqualTo("responsible-user");

        assertThat(entity.createdByUsername())
                .isEqualTo("responsible");
    }

    @Test
    void mapsOperationalCloseFromEntityToDomain() {
        OperationalCloseEntity entity =
                OperationalCloseEntity.create(
                        CLOSE_ID,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        "PEN",
                        new BigDecimal("100.2500"),
                        "PREPARATION",
                        NOW,
                        NOW,
                        "responsible-user",
                        "responsible",
                        NOW,
                        "responsible-user",
                        "responsible");

        OperationalClose operationalClose =
                mapper.toDomain(entity);

        assertThat(operationalClose.id().value())
                .isEqualTo(CLOSE_ID);

        assertThat(operationalClose.period())
                .isEqualTo(
                        new OperationalPeriod(
                                LocalDate.of(2026, 7, 1),
                                LocalDate.of(2026, 7, 31)));

        assertThat(operationalClose.currencyCode())
                .isEqualTo(
                        new CurrencyCode("PEN"));

        assertThat(operationalClose.initialBalance().value())
                .isEqualByComparingTo("100.2500");

        assertThat(operationalClose.state())
                .isEqualTo(
                        OperationalCloseState.PREPARATION);

        assertThat(operationalClose.createdBy())
                .isEqualTo(
                        new AuditActor(
                                "responsible-user",
                                "responsible"));
    }

    @Test
    void mapsInitialStateTransitionToEntity() {
        CloseStateTransition transition =
                CloseStateTransition.initial(
                        new CloseStateTransitionId(
                                TRANSITION_ID),
                        new OperationalCloseId(
                                CLOSE_ID),
                        NOW,
                        new AuditActor(
                                "responsible-user",
                                "responsible"));

        CloseStateTransitionEntity entity =
                mapper.toEntity(transition);

        assertThat(entity.id())
                .isEqualTo(TRANSITION_ID);

        assertThat(entity.closeId())
                .isEqualTo(CLOSE_ID);

        assertThat(entity.fromState())
                .isNull();

        assertThat(entity.toState())
                .isEqualTo("PREPARATION");

        assertThat(entity.causeCode())
                .isEqualTo("CLOSE_CREATED");

        assertThat(entity.validationResultId())
                .isNull();

        assertThat(entity.consolidationId())
                .isNull();

        assertThat(entity.submissionAttemptId())
                .isNull();

        assertThat(entity.actorUserId())
                .isEqualTo("responsible-user");

        assertThat(entity.actorUsername())
                .isEqualTo("responsible");
    }

    private static OperationalClose operationalClose() {
        return OperationalClose.create(
                new OperationalCloseId(
                        CLOSE_ID),
                new OperationalPeriod(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31)),
                new CurrencyCode("PEN"),
                new InitialBalance(
                        new BigDecimal("100.2500")),
                NOW,
                new AuditActor(
                        "responsible-user",
                        "responsible"));
    }

}
