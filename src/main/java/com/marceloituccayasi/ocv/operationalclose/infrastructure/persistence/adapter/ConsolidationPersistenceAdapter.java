package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.ConsolidationRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.Consolidation;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ConsolidationEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.ConsolidationPersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.ConsolidationEventSnapshotJpaRepository;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.ConsolidationJpaRepository;

/**
 * JPA implementation of consolidation persistence.
 */
@Repository
public class ConsolidationPersistenceAdapter
        implements ConsolidationRepository {

    private final ConsolidationJpaRepository
            consolidationJpaRepository;

    private final ConsolidationEventSnapshotJpaRepository
            snapshotJpaRepository;

    private final ConsolidationPersistenceMapper mapper;

    public ConsolidationPersistenceAdapter(
            ConsolidationJpaRepository
                    consolidationJpaRepository,
            ConsolidationEventSnapshotJpaRepository
                    snapshotJpaRepository,
            ConsolidationPersistenceMapper mapper) {

        this.consolidationJpaRepository =
                Objects.requireNonNull(
                        consolidationJpaRepository);

        this.snapshotJpaRepository =
                Objects.requireNonNull(
                        snapshotJpaRepository);

        this.mapper =
                Objects.requireNonNull(
                        mapper);
    }

    @Override
    public void saveNew(
            Consolidation consolidation) {

        Objects.requireNonNull(
                consolidation,
                "consolidation must not be null");

        if (!consolidation.current()) {
            throw new IllegalArgumentException(
                    "new consolidation must be current");
        }

        consolidationJpaRepository.saveAndFlush(
                mapper.toEntity(
                        consolidation));

        snapshotJpaRepository.saveAllAndFlush(
                mapper.toSnapshotEntities(
                        consolidation));
    }

    @Override
    public Optional<Consolidation> findById(
            ConsolidationId consolidationId) {

        Objects.requireNonNull(
                consolidationId,
                "consolidationId must not be null");

        return consolidationJpaRepository
                .findById(
                        consolidationId.value())
                .map(
                        this::toDomain);
    }

    @Override
    public Optional<Consolidation> findCurrentByCloseId(
            OperationalCloseId closeId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        return consolidationJpaRepository
                .findByCloseIdAndCurrentTrue(
                        closeId.value())
                .map(
                        this::toDomain);
    }

    @Override
    public List<Consolidation>
            findAllByCloseIdOrderByCompletedAt(
                    OperationalCloseId closeId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        return consolidationJpaRepository
                .findAllByCloseIdOrderByCompletedAtAscIdAsc(
                        closeId.value())
                .stream()
                .map(
                        this::toDomain)
                .toList();
    }

    @Override
    public void saveInvalidation(
            Consolidation consolidation) {

        Objects.requireNonNull(
                consolidation,
                "consolidation must not be null");

        if (consolidation.current()) {
            throw new IllegalArgumentException(
                    "invalidation persistence requires "
                            + "an invalidated consolidation");
        }

        consolidationJpaRepository.saveAndFlush(
                mapper.toEntity(
                        consolidation));
    }

    private Consolidation toDomain(
            ConsolidationEntity entity) {

        return mapper.toDomain(
                entity,
                snapshotJpaRepository
                        .findAllByConsolidationIdOrderByEventIdAsc(
                                entity.id()));
    }

}