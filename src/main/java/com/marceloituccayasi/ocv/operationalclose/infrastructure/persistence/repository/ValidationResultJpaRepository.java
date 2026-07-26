package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ValidationResultEntity;

/**
 * Internal Spring Data repository for Validation Result persistence.
 */
public interface ValidationResultJpaRepository
        extends JpaRepository<ValidationResultEntity, UUID> {

    @Query("""
            select validationResult
            from ValidationResultEntity validationResult
            where validationResult.id = :validationResultId
              and validationResult.eventId is not null
              and validationResult.closeId is null
            """)
    Optional<ValidationResultEntity> findEventResultById(
            @Param("validationResultId")
            UUID validationResultId);

    Optional<ValidationResultEntity>
            findByEventIdAndRuleCodeAndCurrentTrue(
                    UUID eventId,
                    String ruleCode);

    List<ValidationResultEntity>
            findAllByEventIdAndCurrentTrueOrderByRuleCodeAsc(
                    UUID eventId);

}