package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceLegibilityStatus;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.SupportingEvidenceEntity;

class SupportingEvidencePersistenceMapperTest {

    private static final UUID EVIDENCE_ID =
            UUID.fromString(
                    "4352540f-2d5c-4a85-bd36-d67241110001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "4352540f-2d5c-4a85-bd36-d67241110002");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-23T16:00:00Z");

    private static final Instant UPDATED_AT =
            Instant.parse(
                    "2026-07-23T17:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    "responsible-user",
                    "responsible");

    private final SupportingEvidencePersistenceMapper mapper =
            new SupportingEvidencePersistenceMapper();

    @Test
    void mapsDomainEvidenceToEntity() {
        SupportingEvidence evidence =
                SupportingEvidence.create(
                        new SupportingEvidenceId(
                                EVIDENCE_ID),
                        new OperationalEventId(
                                EVENT_ID),
                        "RECEIPT",
                        "stored:evidence/content.pdf",
                        new BigDecimal(
                                "125.5000"),
                        LocalDate.of(
                                2026,
                                7,
                                22),
                        SupportingEvidenceLegibilityStatus.LEGIBLE,
                        CREATED_AT,
                        ACTOR);

        SupportingEvidenceEntity entity =
                mapper.toEntity(
                        evidence);

        assertThat(entity.id())
                .isEqualTo(
                        EVIDENCE_ID);

        assertThat(entity.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(entity.evidenceType())
                .isEqualTo(
                        "RECEIPT");

        assertThat(entity.contentReference())
                .isEqualTo(
                        "stored:evidence/content.pdf");

        assertThat(entity.supportedAmount())
                .isEqualByComparingTo(
                        "125.5000");

        assertThat(entity.evidenceDate())
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                7,
                                22));

        assertThat(entity.legibilityStatus())
                .isEqualTo(
                        "LEGIBLE");

        assertThat(entity.active())
                .isTrue();

        assertThat(entity.revision())
                .isEqualTo(1L);

        assertThat(entity.createdByUserId())
                .isEqualTo(
                        "responsible-user");

        assertThat(entity.createdByUsername())
                .isEqualTo(
                        "responsible");

        assertThat(entity.deactivatedAt())
                .isNull();
    }

    @Test
    void mapsInactiveEntityToDomainEvidence() {
        SupportingEvidenceEntity entity =
                SupportingEvidenceEntity.create(
                        EVIDENCE_ID,
                        EVENT_ID,
                        "BANK_CONFIRMATION",
                        "reference:confirmation-2026-007",
                        null,
                        LocalDate.of(
                                2026,
                                7,
                                21),
                        "ILLEGIBLE",
                        false,
                        2L,
                        CREATED_AT,
                        "responsible-user",
                        "responsible",
                        UPDATED_AT,
                        "responsible-user",
                        "responsible",
                        UPDATED_AT);

        SupportingEvidence evidence =
                mapper.toDomain(
                        entity);

        assertThat(evidence.id().value())
                .isEqualTo(
                        EVIDENCE_ID);

        assertThat(evidence.eventId().value())
                .isEqualTo(
                        EVENT_ID);

        assertThat(evidence.evidenceType())
                .isEqualTo(
                        "BANK_CONFIRMATION");

        assertThat(evidence.contentReference())
                .isEqualTo(
                        "reference:confirmation-2026-007");

        assertThat(evidence.supportedAmount())
                .isNull();

        assertThat(evidence.legibilityStatus())
                .isEqualTo(
                        SupportingEvidenceLegibilityStatus.ILLEGIBLE);

        assertThat(evidence.active())
                .isFalse();

        assertThat(evidence.revision())
                .isEqualTo(2L);

        assertThat(evidence.createdAt())
                .isEqualTo(
                        CREATED_AT);

        assertThat(evidence.updatedAt())
                .isEqualTo(
                        UPDATED_AT);

        assertThat(evidence.createdBy())
                .isEqualTo(
                        ACTOR);

        assertThat(evidence.updatedBy())
                .isEqualTo(
                        ACTOR);

        assertThat(evidence.deactivatedAt())
                .isEqualTo(
                        UPDATED_AT);
    }

    @Test
    void rejectsNullMappingArguments() {
        assertThatThrownBy(
                () -> mapper.toEntity(
                        null))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessageContaining(
                        "supportingEvidence");

        assertThatThrownBy(
                () -> mapper.toDomain(
                        null))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessageContaining(
                        "entity");
    }

}