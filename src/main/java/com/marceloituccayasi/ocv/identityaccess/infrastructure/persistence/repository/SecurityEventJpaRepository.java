package com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.entity.SecurityEventEntity;

/**
 * Internal append-only security event repository.
 */
public interface SecurityEventJpaRepository
        extends JpaRepository<SecurityEventEntity, UUID> {

    List<SecurityEventEntity> findAllByOrderByOccurredAtAsc();

}
