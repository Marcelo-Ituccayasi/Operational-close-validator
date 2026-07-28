package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertState;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleScope;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationSeverity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ValidationAlertEntity;

/**
 * Explicit mapping between Event Validation Alert domain objects
 * and the generic Validation Alert JPA entity.
 */
@Component
public final class EventValidationAlertPersistenceMapper {

    public ValidationAlertEntity toEntity(
            EventValidationAlert alert) {

        Objects.requireNonNull(
                alert,
                "alert must not be null");

        return ValidationAlertEntity.create(
                alert.id().value(),
                alert.eventId().value(),
                null,
                alert.sourceValidationResultId().value(),
                alert.causeRuleCode().persistentValue(),
                alert.severity().name(),
                alert.blocking(),
                alert.state().name(),
                alert.detail(),
                optionalValue(
                        alert.resolvedByValidationResultId()),
                alert.discardJustification(),
                alert.createdAt(),
                alert.createdBy().userId(),
                alert.createdBy().username(),
                alert.updatedAt(),
                alert.closedAt());
    }

    public EventValidationAlert toDomain(
            ValidationAlertEntity entity) {

        Objects.requireNonNull(
                entity,
                "entity must not be null");

        if (entity.eventId() == null
                || entity.closeId() != null
                || entity.sourceValidationResultId() == null) {

            throw new IllegalArgumentException(
                    "entity must represent an event-scoped validation alert");
        }

        ValidationRuleCode causeRuleCode =
                ValidationRuleCode.fromPersistentValue(
                        entity.causeCode());

        if (causeRuleCode.scope()
                != ValidationRuleScope.EVENT) {

            throw new IllegalArgumentException(
                    "entity must contain an event-scoped alert cause");
        }

        return new EventValidationAlert(
                new ValidationAlertId(
                        entity.id()),
                new OperationalEventId(
                        entity.eventId()),
                new ValidationResultId(
                        entity.sourceValidationResultId()),
                causeRuleCode,
                ValidationSeverity.valueOf(
                        entity.severity()),
                entity.blocking(),
                ValidationAlertState.valueOf(
                        entity.state()),
                entity.detail(),
                optionalId(
                        entity.resolvedByValidationResultId()),
                entity.discardJustification(),
                entity.createdAt(),
                new AuditActor(
                        entity.createdByUserId(),
                        entity.createdByUsername()),
                entity.updatedAt(),
                entity.closedAt());
    }

    private static java.util.UUID optionalValue(
            ValidationResultId validationResultId) {

        return validationResultId == null
                ? null
                : validationResultId.value();
    }

    private static ValidationResultId optionalId(
            java.util.UUID validationResultId) {

        return validationResultId == null
                ? null
                : new ValidationResultId(
                        validationResultId);
    }

}