package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.CloseStateTransitionEntity;

/**
 * Internal Spring Data repository for Operational Close state transitions.
 */
public interface CloseStateTransitionJpaRepository
        extends JpaRepository<CloseStateTransitionEntity, UUID> {

    List<CloseStateTransitionEntity>
            findAllByCloseIdOrderByOccurredAtAsc(
                    UUID closeId);

}
