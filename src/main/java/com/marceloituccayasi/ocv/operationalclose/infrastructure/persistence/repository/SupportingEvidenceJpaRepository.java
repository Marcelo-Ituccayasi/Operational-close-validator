package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.SupportingEvidenceEntity;

/**
 * Internal Spring Data repository for Supporting Evidence persistence.
 */
public interface SupportingEvidenceJpaRepository
        extends JpaRepository<SupportingEvidenceEntity, UUID> {

    List<SupportingEvidenceEntity>
            findAllByEventIdOrderByEvidenceDateDescCreatedAtDescIdDesc(
                    UUID eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select supportingEvidence
            from SupportingEvidenceEntity supportingEvidence
            where supportingEvidence.id = :evidenceId
              and supportingEvidence.eventId = :eventId
              and exists (
                  select operationalEvent.id
                  from OperationalEventEntity operationalEvent
                  where operationalEvent.id = :eventId
                    and operationalEvent.closeId = :closeId
              )
            """)
    Optional<SupportingEvidenceEntity> findByIdForUpdate(
            @Param("closeId")
            UUID closeId,
            @Param("eventId")
            UUID eventId,
            @Param("evidenceId")
            UUID evidenceId);

}