package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.EventAuthorizationEntity;

/**
 * Internal Spring Data repository for Event Authorization persistence.
 */
public interface EventAuthorizationJpaRepository
        extends JpaRepository<EventAuthorizationEntity, UUID> {

    List<EventAuthorizationEntity>
            findAllByEventIdOrderByAuthorizedAtDescCreatedAtDescIdDesc(
                    UUID eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select eventAuthorization
            from EventAuthorizationEntity eventAuthorization
            where eventAuthorization.id = :authorizationId
              and exists (
                  select operationalEvent.id
                  from OperationalEventEntity operationalEvent
                  where operationalEvent.id =
                        eventAuthorization.eventId
                    and operationalEvent.closeId = :closeId
              )
            """)
    Optional<EventAuthorizationEntity> findByIdForUpdate(
            @Param("closeId")
            UUID closeId,
            @Param("authorizationId")
            UUID authorizationId);

}