package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ValidationResultEntity;

class CloseValidationResultPersistenceMapperTest {

    private static final ValidationResultId RESULT_ID =
            new ValidationResultId(
                    uuid(
                            "b7000000-0000-0000-0000-000000000001"));

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    uuid(
                            "b7000000-0000-0000-0000-000000000002"));

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    uuid(
                            "b7000000-0000-0000-0000-000000000003"));

    private static final Instant EVALUATED_AT =
            Instant.parse(
                    "2026-07-30T19:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final CloseValidationResultPersistenceMapper mapper =
            new CloseValidationResultPersistenceMapper();

    @Test
    void mapsAndReconstructsSatisfiedResult() {
        CloseValidationResult expected =
                CloseValidationResult.create(
                        RESULT_ID,
                        ValidationRuleCode.VR_008,
                        1,
                        CLOSE_ID,
                        ValidationOutcome.SATISFIED,
                        "Final control passed.",
                        EVALUATED_AT,
                        ACTOR,
                        CONSOLIDATION_ID);

        ValidationResultEntity entity =
                mapper.toEntity(
                        expected);

        CloseValidationResult reconstructed =
                mapper.toDomain(
                        entity);

        assertThat(
                entity.eventId())
                .isNull();

        assertThat(
                entity.closeId())
                .isEqualTo(
                        CLOSE_ID.value());

        assertThat(
                entity.eventDataRevision())
                .isNull();

        assertThat(
                entity.consolidationId())
                .isEqualTo(
                        CONSOLIDATION_ID.value());

        assertThat(
                reconstructed)
                .isEqualTo(
                        expected);
    }

    @Test
    void mapsAndReconstructsFailedResultWithoutConsolidation() {
        CloseValidationResult expected =
                CloseValidationResult.create(
                        RESULT_ID,
                        ValidationRuleCode.VR_008,
                        1,
                        CLOSE_ID,
                        ValidationOutcome.FAILED,
                        "Current Consolidation is missing.",
                        EVALUATED_AT,
                        ACTOR,
                        null);

        CloseValidationResult reconstructed =
                mapper.toDomain(
                        mapper.toEntity(
                                expected));

        assertThat(
                reconstructed)
                .isEqualTo(
                        expected);
    }

    @Test
    void rejectsEventScopedEntity() {
        ValidationResultEntity entity =
                ValidationResultEntity.create(
                        RESULT_ID.value(),
                        "VR-001",
                        1,
                        uuid(
                                "b7000000-0000-0000-0000-000000000004"),
                        null,
                        "SATISFIED",
                        "Event control passed.",
                        EVALUATED_AT,
                        ACTOR.userId(),
                        ACTOR.username(),
                        1L,
                        null,
                        true,
                        null,
                        null);

        assertThatThrownBy(
                () -> mapper.toDomain(
                        entity))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "entity must represent "
                                + "a close-scoped validation result");
    }

    private static UUID uuid(
            String value) {

        return UUID.fromString(
                value);
    }

}