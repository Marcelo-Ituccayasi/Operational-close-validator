package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ConsolidationEntity;

/**
 * Internal Spring Data repository for consolidation persistence.
 */
public interface ConsolidationJpaRepository
        extends JpaRepository<ConsolidationEntity, UUID> {

    Optional<ConsolidationEntity>
            findByCloseIdAndCurrentTrue(
                    UUID closeId);

    List<ConsolidationEntity>
            findAllByCloseIdOrderByCompletedAtAscIdAsc(
                    UUID closeId);

}