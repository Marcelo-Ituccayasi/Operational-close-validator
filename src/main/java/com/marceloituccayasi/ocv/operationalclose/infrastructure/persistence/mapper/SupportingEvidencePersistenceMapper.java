package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceLegibilityStatus;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.SupportingEvidenceEntity;

/**
 * Explicit mapping between Supporting Evidence domain objects
 * and JPA entities.
 */
@Component
public final class SupportingEvidencePersistenceMapper {

    public SupportingEvidenceEntity toEntity(
            SupportingEvidence supportingEvidence) {

        Objects.requireNonNull(
                supportingEvidence,
                "supportingEvidence must not be null");

        return SupportingEvidenceEntity.create(
                supportingEvidence.id().value(),
                supportingEvidence.eventId().value(),
                supportingEvidence.evidenceType(),
                supportingEvidence.contentReference(),
                supportingEvidence.supportedAmount(),
                supportingEvidence.evidenceDate(),
                supportingEvidence.legibilityStatus().name(),
                supportingEvidence.active(),
                supportingEvidence.revision(),
                supportingEvidence.createdAt(),
                supportingEvidence.createdBy().userId(),
                supportingEvidence.createdBy().username(),
                supportingEvidence.updatedAt(),
                supportingEvidence.updatedBy().userId(),
                supportingEvidence.updatedBy().username(),
                supportingEvidence.deactivatedAt());
    }

    public SupportingEvidence toDomain(
            SupportingEvidenceEntity entity) {

        Objects.requireNonNull(
                entity,
                "entity must not be null");

        return new SupportingEvidence(
                new SupportingEvidenceId(
                        entity.id()),
                new OperationalEventId(
                        entity.eventId()),
                entity.evidenceType(),
                entity.contentReference(),
                entity.supportedAmount(),
                entity.evidenceDate(),
                SupportingEvidenceLegibilityStatus.valueOf(
                        entity.legibilityStatus()),
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