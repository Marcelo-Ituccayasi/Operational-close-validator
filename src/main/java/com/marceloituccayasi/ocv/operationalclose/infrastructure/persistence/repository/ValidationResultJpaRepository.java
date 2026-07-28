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

    @Query("""
            select validationResult
            from ValidationResultEntity validationResult
            where validationResult.current = true
              and validationResult.eventId in :eventIds
              and validationResult.closeId is null
              and exists (
                  select operationalEvent.id
                  from OperationalEventEntity operationalEvent
                  where operationalEvent.id =
                        validationResult.eventId
                    and operationalEvent.closeId = :closeId
              )
            order by
                validationResult.eventId asc,
                validationResult.ruleCode asc
            """)
    List<ValidationResultEntity>
            findAllCurrentEventResultsForInvalidation(
                    @Param("closeId")
                    UUID closeId,
                    @Param("eventIds")
                    List<UUID> eventIds);

}