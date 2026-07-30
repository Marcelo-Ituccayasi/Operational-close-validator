package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.Consolidation;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ConsolidationEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ConsolidationEventSnapshotEntity;

class ConsolidationPersistenceMapperTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "d171ed93-8e27-4ba6-a667-730000000001"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "d171ed93-8e27-4ba6-a667-730000000002"));

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    UUID.fromString(
                            "d171ed93-8e27-4ba6-a667-730000000003"));

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-30T10:00:00Z");

    private static final Instant COMPLETED_AT =
            Instant.parse(
                    "2026-07-30T11:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final ConsolidationPersistenceMapper mapper =
            new ConsolidationPersistenceMapper();

    @Test
    void mapsAndReconstructsTheCompleteAggregate() {
        Consolidation expected =
                consolidation();

        ConsolidationEntity entity =
                mapper.toEntity(
                        expected);

        List<ConsolidationEventSnapshotEntity> snapshots =
                mapper.toSnapshotEntities(
                        expected);

        Consolidation reconstructed =
                mapper.toDomain(
                        entity,
                        snapshots);

        assertThat(
                entity.id())
                .isEqualTo(
                        CONSOLIDATION_ID.value());

        assertThat(
                entity.closeId())
                .isEqualTo(
                        CLOSE_ID.value());

        assertThat(
                entity.currencyCode())
                .isEqualTo(
                        "PEN");

        assertThat(
                entity.expectedBalance())
                .isEqualByComparingTo(
                        decimal(
                                "1125.5000"));

        assertThat(
                entity.difference())
                .isEqualByComparingTo(
                        BigDecimal.ZERO);

        assertThat(
                snapshots)
                .hasSize(
                        1);

        assertThat(
                snapshots.getFirst()
                        .eventId())
                .isEqualTo(
                        EVENT_ID.value());

        assertThat(
                snapshots.getFirst()
                        .eventDataRevision())
                .isEqualTo(
                        3);

        assertThat(
                reconstructed)
                .isEqualTo(
                        expected);
    }

    @Test
    void rejectsAReconstructionWithoutItsRequiredSnapshots() {
        Consolidation expected =
                consolidation();

        ConsolidationEntity entity =
                mapper.toEntity(
                        expected);

        assertThatThrownBy(
                () -> mapper.toDomain(
                        entity,
                        List.of()))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "event count must match snapshot count");
    }

    private Consolidation consolidation() {
        return Consolidation.complete(
                CONSOLIDATION_ID,
                close(),
                List.of(
                        event()),
                decimal(
                        "1125.5000"),
                COMPLETED_AT,
                ACTOR);
    }

    private OperationalClose close() {
        return OperationalClose.create(
                CLOSE_ID,
                new OperationalPeriod(
                        LocalDate.of(
                                2026,
                                7,
                                1),
                        LocalDate.of(
                                2026,
                                7,
                                31)),
                new CurrencyCode(
                        "PEN"),
                new InitialBalance(
                        decimal(
                                "1000.0000")),
                CREATED_AT,
                ACTOR);
    }

    private OperationalEvent event() {
        return new OperationalEvent(
                EVENT_ID,
                CLOSE_ID,
                OperationalEventType.INCOME,
                new OperationalEventAmount(
                        decimal(
                                "125.5000")),
                decimal(
                        "125.5000"),
                null,
                CREATED_AT,
                CREATED_AT,
                "Caja principal",
                "Evento para prueba de mapper",
                OperationalEventState.VALIDATED,
                false,
                false,
                3,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

    private static BigDecimal decimal(
            String value) {

        return new BigDecimal(
                value);
    }

}