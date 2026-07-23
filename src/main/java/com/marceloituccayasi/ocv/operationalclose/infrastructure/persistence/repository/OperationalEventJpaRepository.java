package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.OperationalEventEntity;

/**
 * Internal Spring Data repository for Operational Event persistence.
 */
public interface OperationalEventJpaRepository
        extends JpaRepository<OperationalEventEntity, UUID> {

    List<OperationalEventEntity>
            findAllByCloseIdOrderByOccurredAtDescRegisteredAtDescIdDesc(
                    UUID closeId);

    boolean existsByReversedEventId(
            UUID reversedEventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select operationalEvent
            from OperationalEventEntity operationalEvent
            where operationalEvent.closeId = :closeId
              and operationalEvent.id = :eventId
            """)
    Optional<OperationalEventEntity> findByIdForUpdate(
            @Param("closeId")
            UUID closeId,
            @Param("eventId")
            UUID eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select operationalEvent
            from OperationalEventEntity operationalEvent
            where operationalEvent.closeId = :closeId
              and operationalEvent.reversedEventId = :reversedEventId
            """)
    Optional<OperationalEventEntity>
            findCancellationByReversedEventIdForUpdate(
                    @Param("closeId")
                    UUID closeId,
                    @Param("reversedEventId")
                    UUID reversedEventId);

}