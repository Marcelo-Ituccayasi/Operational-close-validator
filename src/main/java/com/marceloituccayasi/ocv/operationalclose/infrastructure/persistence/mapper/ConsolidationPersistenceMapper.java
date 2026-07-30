package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.Consolidation;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationEventSnapshot;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ConsolidationEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ConsolidationEventSnapshotEntity;

/**
 * Explicit mapping between consolidation domain objects and JPA entities.
 */
@Component
public final class ConsolidationPersistenceMapper {

    public ConsolidationEntity toEntity(
            Consolidation consolidation) {

        Objects.requireNonNull(
                consolidation,
                "consolidation must not be null");

        return ConsolidationEntity.create(
                consolidation.id().value(),
                consolidation.closeId().value(),
                consolidation.currencyCode().value(),
                consolidation.eventCount(),
                consolidation.totalIncome(),
                consolidation.totalExpense(),
                consolidation.totalDiscount(),
                consolidation.totalCancellation(),
                consolidation.initialBalance().value(),
                consolidation.expectedBalance(),
                consolidation.actualBalance(),
                consolidation.difference(),
                consolidation.current(),
                consolidation.completedAt(),
                consolidation.completedBy().userId(),
                consolidation.completedBy().username(),
                consolidation.invalidatedAt(),
                consolidation.invalidationReason());
    }

    public List<ConsolidationEventSnapshotEntity>
            toSnapshotEntities(
                    Consolidation consolidation) {

        Objects.requireNonNull(
                consolidation,
                "consolidation must not be null");

        return consolidation.eventSnapshots()
                .stream()
                .map(
                        this::toSnapshotEntity)
                .toList();
    }

    public ConsolidationEventSnapshotEntity toSnapshotEntity(
            ConsolidationEventSnapshot snapshot) {

        Objects.requireNonNull(
                snapshot,
                "snapshot must not be null");

        return ConsolidationEventSnapshotEntity.create(
                snapshot.consolidationId().value(),
                snapshot.eventId().value(),
                snapshot.eventDataRevision(),
                snapshot.eventType().name(),
                snapshot.amount().value(),
                snapshot.balanceEffect(),
                snapshot.reversedEventId() == null
                        ? null
                        : snapshot.reversedEventId().value(),
                snapshot.eventState().name(),
                snapshot.capturedAt());
    }

    public Consolidation toDomain(
            ConsolidationEntity entity,
            List<ConsolidationEventSnapshotEntity>
                    snapshotEntities) {

        Objects.requireNonNull(
                entity,
                "entity must not be null");

        Objects.requireNonNull(
                snapshotEntities,
                "snapshotEntities must not be null");

        List<ConsolidationEventSnapshot> snapshots =
                snapshotEntities.stream()
                        .map(
                                this::toSnapshotDomain)
                        .toList();

        return new Consolidation(
                new ConsolidationId(
                        entity.id()),
                new OperationalCloseId(
                        entity.closeId()),
                new CurrencyCode(
                        entity.currencyCode()),
                entity.eventCount(),
                entity.totalIncome(),
                entity.totalExpense(),
                entity.totalDiscount(),
                entity.totalCancellation(),
                new InitialBalance(
                        entity.initialBalance()),
                entity.expectedBalance(),
                entity.actualBalance(),
                entity.difference(),
                entity.current(),
                entity.completedAt(),
                new AuditActor(
                        entity.completedByUserId(),
                        entity.completedByUsername()),
                entity.invalidatedAt(),
                entity.invalidationReason(),
                snapshots);
    }

    public ConsolidationEventSnapshot toSnapshotDomain(
            ConsolidationEventSnapshotEntity entity) {

        Objects.requireNonNull(
                entity,
                "entity must not be null");

        return new ConsolidationEventSnapshot(
                new ConsolidationId(
                        entity.consolidationId()),
                new OperationalEventId(
                        entity.eventId()),
                entity.eventDataRevision(),
                OperationalEventType.valueOf(
                        entity.eventType()),
                new OperationalEventAmount(
                        entity.amount()),
                entity.balanceEffect(),
                entity.reversedEventId() == null
                        ? null
                        : new OperationalEventId(
                                entity.reversedEventId()),
                OperationalEventState.valueOf(
                        entity.eventState()),
                entity.capturedAt());
    }

}