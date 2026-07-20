package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.OperationalCloseEntity;

/**
 * Internal Spring Data repository for Operational Close persistence.
 */
public interface OperationalCloseJpaRepository
        extends JpaRepository<OperationalCloseEntity, UUID> {

    boolean existsByPeriodStartAndPeriodEnd(
            LocalDate periodStart,
            LocalDate periodEnd);

    List<OperationalCloseEntity>
            findAllByOrderByPeriodEndDescPeriodStartDesc();

}
