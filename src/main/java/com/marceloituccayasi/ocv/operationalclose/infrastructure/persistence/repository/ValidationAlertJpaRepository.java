package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ValidationAlertEntity;

/**
 * Internal Spring Data repository for Validation Alert persistence.
 */
public interface ValidationAlertJpaRepository
        extends JpaRepository<ValidationAlertEntity, UUID> {

    @Query("""
            select validationAlert
            from ValidationAlertEntity validationAlert
            where validationAlert.id = :alertId
              and validationAlert.eventId is not null
              and validationAlert.closeId is null
            """)
    Optional<ValidationAlertEntity> findEventAlertById(
            @Param("alertId")
            UUID alertId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select validationAlert
            from ValidationAlertEntity validationAlert
            where validationAlert.id = :alertId
              and validationAlert.eventId is not null
              and validationAlert.closeId is null
            """)
    Optional<ValidationAlertEntity> findEventAlertByIdForUpdate(
            @Param("alertId")
            UUID alertId);

    Optional<ValidationAlertEntity>
            findFirstByEventIdAndCauseCodeAndStateInOrderByCreatedAtDescIdDesc(
                    UUID eventId,
                    String causeCode,
                    Collection<String> states);

    List<ValidationAlertEntity>
            findAllByEventIdAndStateInOrderByCreatedAtAscIdAsc(
                    UUID eventId,
                    Collection<String> states);

}