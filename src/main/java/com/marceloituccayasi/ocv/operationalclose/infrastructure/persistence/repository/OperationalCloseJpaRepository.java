package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.OperationalCloseEntity;

/**
 * Internal Spring Data repository for Operational Close persistence.
 */
public interface OperationalCloseJpaRepository
        extends JpaRepository<OperationalCloseEntity, UUID> {

    boolean existsByPeriodStartAndPeriodEnd(
            LocalDate periodStart,
            LocalDate periodEnd);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select operationalClose
            from OperationalCloseEntity operationalClose
            where operationalClose.id = :closeId
            """)
    Optional<OperationalCloseEntity> findByIdForUpdate(
            @Param("closeId") UUID closeId);

    List<OperationalCloseEntity>
            findAllByOrderByPeriodEndDescPeriodStartDesc();

}