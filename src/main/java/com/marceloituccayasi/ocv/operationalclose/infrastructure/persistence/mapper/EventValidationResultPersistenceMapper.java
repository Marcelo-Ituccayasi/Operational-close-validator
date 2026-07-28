package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ValidationResultEntity;

/**
 * Explicit mapping between Event Validation Result domain objects
 * and the generic Validation Result JPA entity.
 */
@Component
public final class EventValidationResultPersistenceMapper {

    public ValidationResultEntity toEntity(
            EventValidationResult validationResult) {

        Objects.requireNonNull(
                validationResult,
                "validationResult must not be null");

        return ValidationResultEntity.create(
                validationResult.id().value(),
                validationResult.ruleCode().persistentValue(),
                validationResult.ruleVersion(),
                validationResult.eventId().value(),
                null,
                validationResult.outcome().name(),
                validationResult.detail(),
                validationResult.evaluatedAt(),
                validationResult.evaluatedBy().userId(),
                validationResult.evaluatedBy().username(),
                validationResult.eventDataRevision(),
                null,
                validationResult.current(),
                validationResult.invalidatedAt(),
                validationResult.invalidationReason());
    }

    public EventValidationResult toDomain(
            ValidationResultEntity entity) {

        Objects.requireNonNull(
                entity,
                "entity must not be null");

        if (entity.eventId() == null
                || entity.closeId() != null
                || entity.eventDataRevision() == null
                || entity.consolidationId() != null) {

            throw new IllegalArgumentException(
                    "entity must represent an event-scoped validation result");
        }

        return new EventValidationResult(
                new ValidationResultId(
                        entity.id()),
                ValidationRuleCode.fromPersistentValue(
                        entity.ruleCode()),
                entity.ruleVersion(),
                new OperationalEventId(
                        entity.eventId()),
                ValidationOutcome.valueOf(
                        entity.outcome()),
                entity.detail(),
                entity.evaluatedAt(),
                new AuditActor(
                        entity.evaluatedByUserId(),
                        entity.evaluatedByUsername()),
                entity.eventDataRevision(),
                entity.current(),
                entity.invalidatedAt(),
                entity.invalidationReason());
    }

}