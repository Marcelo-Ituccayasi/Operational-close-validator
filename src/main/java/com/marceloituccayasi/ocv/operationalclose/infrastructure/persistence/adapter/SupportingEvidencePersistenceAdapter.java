package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.SupportingEvidenceRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.SupportingEvidencePersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.SupportingEvidenceJpaRepository;

/**
 * JPA implementation of Supporting Evidence persistence.
 */
@Repository
public class SupportingEvidencePersistenceAdapter
        implements SupportingEvidenceRepository {

    private final SupportingEvidenceJpaRepository
            supportingEvidenceJpaRepository;

    private final SupportingEvidencePersistenceMapper mapper;

    public SupportingEvidencePersistenceAdapter(
            SupportingEvidenceJpaRepository
                    supportingEvidenceJpaRepository,
            SupportingEvidencePersistenceMapper mapper) {

        this.supportingEvidenceJpaRepository =
                Objects.requireNonNull(
                        supportingEvidenceJpaRepository);

        this.mapper =
                Objects.requireNonNull(
                        mapper);
    }

    @Override
    public void saveNew(
            SupportingEvidence supportingEvidence) {

        Objects.requireNonNull(
                supportingEvidence,
                "supportingEvidence must not be null");

        supportingEvidenceJpaRepository.saveAndFlush(
                mapper.toEntity(
                        supportingEvidence));
    }

    @Override
    public Optional<SupportingEvidence> findById(
            SupportingEvidenceId evidenceId) {

        Objects.requireNonNull(
                evidenceId,
                "evidenceId must not be null");

        return supportingEvidenceJpaRepository
                .findById(
                        evidenceId.value())
                .map(
                        mapper::toDomain);
    }

    @Override
    public List<SupportingEvidence>
            findAllByEventIdOrderByEvidenceDateDescending(
                    OperationalEventId eventId) {

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        return supportingEvidenceJpaRepository
                .findAllByEventIdOrderByEvidenceDateDescCreatedAtDescIdDesc(
                        eventId.value())
                .stream()
                .map(
                        mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<SupportingEvidence> findByIdForUpdate(
            OperationalCloseId closeId,
            SupportingEvidenceId evidenceId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        Objects.requireNonNull(
                evidenceId,
                "evidenceId must not be null");

        return supportingEvidenceJpaRepository
                .findByIdForUpdate(
                        closeId.value(),
                        evidenceId.value())
                .map(
                        mapper::toDomain);
    }

    @Override
    public void saveRevision(
            SupportingEvidence supportingEvidence) {

        Objects.requireNonNull(
                supportingEvidence,
                "supportingEvidence must not be null");

        supportingEvidenceJpaRepository.saveAndFlush(
                mapper.toEntity(
                        supportingEvidence));
    }

}