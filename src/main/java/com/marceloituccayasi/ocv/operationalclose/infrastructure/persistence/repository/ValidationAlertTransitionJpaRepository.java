package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ValidationAlertTransitionEntity;

/**
 * Internal Spring Data repository for append-only Alert transitions.
 */
public interface ValidationAlertTransitionJpaRepository
        extends JpaRepository<ValidationAlertTransitionEntity, UUID> {

    List<ValidationAlertTransitionEntity>
            findAllByAlertIdOrderByOccurredAtAscIdAsc(
                    UUID alertId);

}