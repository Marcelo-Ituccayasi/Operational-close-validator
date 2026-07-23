package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.EventStateTransitionEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.OperationalEventEntity;

/**
 * Explicit mapping between Operational Event domain objects
 * and JPA entities.
 */
@Component
public final class OperationalEventPersistenceMapper {

    public OperationalEventEntity toEntity(
            OperationalEvent operationalEvent) {

        Objects.requireNonNull(
                operationalEvent,
                "operationalEvent must not be null");

        return OperationalEventEntity.create(
                operationalEvent.id().value(),
                operationalEvent.closeId().value(),
                operationalEvent.eventType().name(),
                operationalEvent.amount().value(),
                operationalEvent.balanceEffect(),
                operationalEvent.reversedEventId() == null
                        ? null
                        : operationalEvent.reversedEventId().value(),
                operationalEvent.occurredAt(),
                operationalEvent.registeredAt(),
                operationalEvent.responsibleName(),
                operationalEvent.description(),
                operationalEvent.state().name(),
                operationalEvent.evidenceRequired(),
                operationalEvent.authorizationRequired(),
                operationalEvent.dataRevision(),
                operationalEvent.stateChangedAt(),
                operationalEvent.createdAt(),
                operationalEvent.createdBy().userId(),
                operationalEvent.createdBy().username(),
                operationalEvent.updatedAt(),
                operationalEvent.updatedBy().userId(),
                operationalEvent.updatedBy().username());
    }

    public OperationalEvent toDomain(
            OperationalEventEntity entity) {

        Objects.requireNonNull(
                entity,
                "entity must not be null");

        OperationalEventId reversedEventId =
                entity.reversedEventId() == null
                        ? null
                        : new OperationalEventId(
                                entity.reversedEventId());

        return new OperationalEvent(
                new OperationalEventId(
                        entity.id()),
                new OperationalCloseId(
                        entity.closeId()),
                OperationalEventType.valueOf(
                        entity.eventType()),
                new OperationalEventAmount(
                        entity.amount()),
                entity.balanceEffect(),
                reversedEventId,
                entity.occurredAt(),
                entity.registeredAt(),
                entity.responsibleName(),
                entity.description(),
                OperationalEventState.valueOf(
                        entity.state()),
                entity.evidenceRequired(),
                entity.authorizationRequired(),
                entity.dataRevision(),
                entity.stateChangedAt(),
                entity.createdAt(),
                new AuditActor(
                        entity.createdByUserId(),
                        entity.createdByUsername()),
                entity.updatedAt(),
                new AuditActor(
                        entity.updatedByUserId(),
                        entity.updatedByUsername()));
    }

    public EventStateTransitionEntity toEntity(
            EventStateTransition transition) {

        Objects.requireNonNull(
                transition,
                "transition must not be null");

        String fromState =
                transition.fromState() == null
                        ? null
                        : transition.fromState().name();

        return EventStateTransitionEntity.create(
                transition.id().value(),
                transition.eventId().value(),
                fromState,
                transition.toState().name(),
                transition.causeCode(),
                transition.detail(),
                transition.validationResultId(),
                transition.occurredAt(),
                transition.actor().userId(),
                transition.actor().username());
    }

}