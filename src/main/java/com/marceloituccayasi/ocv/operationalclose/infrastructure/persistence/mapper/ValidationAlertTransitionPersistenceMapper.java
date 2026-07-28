package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertState;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ValidationAlertTransitionEntity;

/**
 * Explicit mapping between Validation Alert transitions
 * and append-only JPA entities.
 */
@Component
public final class ValidationAlertTransitionPersistenceMapper {

    public ValidationAlertTransitionEntity toEntity(
            ValidationAlertTransition transition) {

        Objects.requireNonNull(
                transition,
                "transition must not be null");

        return ValidationAlertTransitionEntity.create(
                transition.id().value(),
                transition.alertId().value(),
                optionalStateName(
                        transition.fromState()),
                transition.toState().name(),
                transition.actionCode(),
                transition.detail(),
                transition.justification(),
                optionalResultValue(
                        transition.validationResultId()),
                transition.occurredAt(),
                transition.actor().userId(),
                transition.actor().username());
    }

    public ValidationAlertTransition toDomain(
            ValidationAlertTransitionEntity entity) {

        Objects.requireNonNull(
                entity,
                "entity must not be null");

        return new ValidationAlertTransition(
                new ValidationAlertTransitionId(
                        entity.id()),
                new ValidationAlertId(
                        entity.alertId()),
                optionalState(
                        entity.fromState()),
                ValidationAlertState.valueOf(
                        entity.toState()),
                entity.actionCode(),
                entity.detail(),
                entity.justification(),
                optionalResultId(
                        entity.validationResultId()),
                entity.occurredAt(),
                new AuditActor(
                        entity.actorUserId(),
                        entity.actorUsername()));
    }

    private static String optionalStateName(
            ValidationAlertState state) {

        return state == null
                ? null
                : state.name();
    }

    private static ValidationAlertState optionalState(
            String state) {

        return state == null
                ? null
                : ValidationAlertState.valueOf(
                        state);
    }

    private static UUID optionalResultValue(
            ValidationResultId resultId) {

        return resultId == null
                ? null
                : resultId.value();
    }

    private static ValidationResultId optionalResultId(
            UUID resultId) {

        return resultId == null
                ? null
                : new ValidationResultId(
                        resultId);
    }

}