package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ValidationResultEntity;

class EventValidationResultPersistenceMapperTest {

    private static final ValidationResultId RESULT_ID =
            new ValidationResultId(
                    UUID.fromString(
                            "745a95e4-6348-4e38-9128-874a41290001"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "745a95e4-6348-4e38-9128-874a41290002"));

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "745a95e4-6348-4e38-9128-874a41290003");

    private static final UUID CONSOLIDATION_ID =
            UUID.fromString(
                    "745a95e4-6348-4e38-9128-874a41290004");

    private static final Instant EVALUATED_AT =
            Instant.parse(
                    "2026-07-26T18:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final EventValidationResultPersistenceMapper mapper =
            new EventValidationResultPersistenceMapper();

    @Test
    void mapsCurrentEventValidationResultToEntity() {
        EventValidationResult validationResult =
                EventValidationResult.create(
                        RESULT_ID,
                        ValidationRuleCode.VR_003,
                        1,
                        EVENT_ID,
                        ValidationOutcome.FAILED,
                        "Required supporting evidence is missing.",
                        EVALUATED_AT,
                        ACTOR,
                        4L);

        ValidationResultEntity entity =
                mapper.toEntity(
                        validationResult);

        assertThat(entity.id())
                .isEqualTo(
                        RESULT_ID.value());

        assertThat(entity.ruleCode())
                .isEqualTo(
                        "VR-003");

        assertThat(entity.ruleVersion())
                .isEqualTo(
                        1);

        assertThat(entity.eventId())
                .isEqualTo(
                        EVENT_ID.value());

        assertThat(entity.closeId())
                .isNull();

        assertThat(entity.outcome())
                .isEqualTo(
                        "FAILED");

        assertThat(entity.detail())
                .isEqualTo(
                        "Required supporting evidence is missing.");

        assertThat(entity.evaluatedAt())
                .isEqualTo(
                        EVALUATED_AT);

        assertThat(entity.evaluatedByUserId())
                .isEqualTo(
                        AuditActor.RESPONSIBLE_USER_ID);

        assertThat(entity.evaluatedByUsername())
                .isEqualTo(
                        "responsible");

        assertThat(entity.eventDataRevision())
                .isEqualTo(
                        4L);

        assertThat(entity.consolidationId())
                .isNull();

        assertThat(entity.current())
                .isTrue();

        assertThat(entity.invalidatedAt())
                .isNull();

        assertThat(entity.invalidationReason())
                .isNull();
    }

    @Test
    void restoresInvalidatedEventValidationResult() {
        EventValidationResult validationResult =
                EventValidationResult.create(
                        RESULT_ID,
                        ValidationRuleCode.VR_006,
                        1,
                        EVENT_ID,
                        ValidationOutcome.SATISFIED,
                        "Required authorization is present.",
                        EVALUATED_AT,
                        ACTOR,
                        6L)
                        .invalidate(
                                EVALUATED_AT.plusSeconds(
                                        60L),
                                "Event data revision changed.");

        EventValidationResult restored =
                mapper.toDomain(
                        mapper.toEntity(
                                validationResult));

        assertThat(restored)
                .isEqualTo(
                        validationResult);
    }

    @Test
    void rejectsCloseScopedValidationResultEntity() {
        ValidationResultEntity closeResult =
                ValidationResultEntity.create(
                        RESULT_ID.value(),
                        "VR-008",
                        1,
                        null,
                        CLOSE_ID,
                        "SATISFIED",
                        "Known-event consolidation is complete.",
                        EVALUATED_AT,
                        AuditActor.RESPONSIBLE_USER_ID,
                        "responsible",
                        null,
                        CONSOLIDATION_ID,
                        true,
                        null,
                        null);

        assertThatThrownBy(
                () -> mapper.toDomain(
                        closeResult))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "entity must represent an event-scoped validation result");
    }

}