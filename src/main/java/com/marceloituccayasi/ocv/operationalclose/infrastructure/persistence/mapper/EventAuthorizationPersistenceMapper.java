package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorizationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.EventAuthorizationEntity;

/**
 * Explicit mapping between Event Authorization domain objects
 * and JPA entities.
 */
@Component
public final class EventAuthorizationPersistenceMapper {

    public EventAuthorizationEntity toEntity(
            EventAuthorization eventAuthorization) {

        Objects.requireNonNull(
                eventAuthorization,
                "eventAuthorization must not be null");

        return EventAuthorizationEntity.create(
                eventAuthorization.id().value(),
                eventAuthorization.eventId().value(),
                eventAuthorization.authorizedByName(),
                eventAuthorization.reason(),
                eventAuthorization.authorizedAt(),
                eventAuthorization.formalReference(),
                eventAuthorization.active(),
                eventAuthorization.revision(),
                eventAuthorization.createdAt(),
                eventAuthorization.createdBy().userId(),
                eventAuthorization.createdBy().username(),
                eventAuthorization.updatedAt(),
                eventAuthorization.updatedBy().userId(),
                eventAuthorization.updatedBy().username(),
                eventAuthorization.deactivatedAt());
    }

    public EventAuthorization toDomain(
            EventAuthorizationEntity entity) {

        Objects.requireNonNull(
                entity,
                "entity must not be null");

        return new EventAuthorization(
                new EventAuthorizationId(
                        entity.id()),
                new OperationalEventId(
                        entity.eventId()),
                entity.authorizedByName(),
                entity.reason(),
                entity.authorizedAt(),
                entity.formalReference(),
                entity.active(),
                entity.revision(),
                entity.createdAt(),
                new AuditActor(
                        entity.createdByUserId(),
                        entity.createdByUsername()),
                entity.updatedAt(),
                new AuditActor(
                        entity.updatedByUserId(),
                        entity.updatedByUsername()),
                entity.deactivatedAt());
    }

}