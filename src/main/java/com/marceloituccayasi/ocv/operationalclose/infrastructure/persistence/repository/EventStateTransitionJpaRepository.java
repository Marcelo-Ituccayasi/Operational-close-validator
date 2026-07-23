package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.EventStateTransitionEntity;

/**
 * Internal Spring Data repository for immutable Operational Event
 * state transitions.
 */
public interface EventStateTransitionJpaRepository
        extends JpaRepository<EventStateTransitionEntity, UUID> {
}