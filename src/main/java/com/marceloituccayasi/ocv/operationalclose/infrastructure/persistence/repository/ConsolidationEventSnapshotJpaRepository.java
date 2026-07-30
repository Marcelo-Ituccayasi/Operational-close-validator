package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ConsolidationEventSnapshotEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ConsolidationEventSnapshotEntityId;

/**
 * Internal Spring Data repository for immutable consolidation snapshots.
 */
public interface ConsolidationEventSnapshotJpaRepository
        extends JpaRepository<
                ConsolidationEventSnapshotEntity,
                ConsolidationEventSnapshotEntityId> {

    List<ConsolidationEventSnapshotEntity>
            findAllByConsolidationIdOrderByEventIdAsc(
                    UUID consolidationId);

}