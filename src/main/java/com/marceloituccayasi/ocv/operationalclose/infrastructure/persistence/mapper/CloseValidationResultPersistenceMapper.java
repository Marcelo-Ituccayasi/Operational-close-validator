package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ValidationResultEntity;

/**
 * Explicit mapping between Close Validation Results and the generic
 * Validation Result JPA entity.
 */
@Component
public final class CloseValidationResultPersistenceMapper {

    public ValidationResultEntity toEntity(
            CloseValidationResult validationResult) {

        Objects.requireNonNull(
                validationResult,
                "validationResult must not be null");

        return ValidationResultEntity.create(
                validationResult.id().value(),
                validationResult.ruleCode().persistentValue(),
                validationResult.ruleVersion(),
                null,
                validationResult.closeId().value(),
                validationResult.outcome().name(),
                validationResult.detail(),
                validationResult.evaluatedAt(),
                validationResult.evaluatedBy().userId(),
                validationResult.evaluatedBy().username(),
                null,
                validationResult.consolidationId() == null
                        ? null
                        : validationResult.consolidationId().value(),
                validationResult.current(),
                validationResult.invalidatedAt(),
                validationResult.invalidationReason());
    }

    public CloseValidationResult toDomain(
            ValidationResultEntity entity) {

        Objects.requireNonNull(
                entity,
                "entity must not be null");

        if (entity.eventId() != null
                || entity.closeId() == null
                || entity.eventDataRevision() != null) {

            throw new IllegalArgumentException(
                    "entity must represent a close-scoped validation result");
        }

        return new CloseValidationResult(
                new ValidationResultId(
                        entity.id()),
                ValidationRuleCode.fromPersistentValue(
                        entity.ruleCode()),
                entity.ruleVersion(),
                new OperationalCloseId(
                        entity.closeId()),
                ValidationOutcome.valueOf(
                        entity.outcome()),
                entity.detail(),
                entity.evaluatedAt(),
                new AuditActor(
                        entity.evaluatedByUserId(),
                        entity.evaluatedByUsername()),
                entity.consolidationId() == null
                        ? null
                        : new ConsolidationId(
                                entity.consolidationId()),
                entity.current(),
                entity.invalidatedAt(),
                entity.invalidationReason());
    }

}