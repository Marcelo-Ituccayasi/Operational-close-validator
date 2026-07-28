package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ValidationAlertEntity;

class EventValidationAlertPersistenceMapperTest {

    private static final UUID ALERT_ID =
            UUID.fromString(
                    "bbf6acd5-f779-4171-aa84-adff06630001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "bbf6acd5-f779-4171-aa84-adff06630002");

    private static final UUID FAILED_RESULT_ID =
            UUID.fromString(
                    "bbf6acd5-f779-4171-aa84-adff06630003");

    private static final UUID SATISFIED_RESULT_ID =
            UUID.fromString(
                    "bbf6acd5-f779-4171-aa84-adff06630004");

    private static final UUID TRANSITION_ID =
            UUID.fromString(
                    "bbf6acd5-f779-4171-aa84-adff06630005");

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "bbf6acd5-f779-4171-aa84-adff06630006");

    private static final Instant EVALUATED_AT =
            Instant.parse(
                    "2026-07-26T21:00:00Z");

    private final EventValidationAlertPersistenceMapper mapper =
            new EventValidationAlertPersistenceMapper();

    @Test
    void roundTripsActiveEventValidationAlert() {
        EventValidationAlert alert =
                activeAlert();

        EventValidationAlert restored =
                mapper.toDomain(
                        mapper.toEntity(
                                alert));

        assertThat(restored)
                .isEqualTo(
                        alert);
    }

    @Test
    void roundTripsResolvedEventValidationAlert() {
        EventValidationAlert activeAlert =
                activeAlert();

        EventValidationResult satisfiedResult =
                EventValidationResult.create(
                        new ValidationResultId(
                                SATISFIED_RESULT_ID),
                        ValidationRuleCode.VR_003,
                        1,
                        new OperationalEventId(
                                EVENT_ID),
                        ValidationOutcome.SATISFIED,
                        "Required supporting evidence is present.",
                        EVALUATED_AT.plusSeconds(
                                120L),
                        actor(),
                        3L);

        EventValidationAlert resolvedAlert =
                activeAlert.resolve(
                        new ValidationAlertTransitionId(
                                TRANSITION_ID),
                        satisfiedResult,
                        "Resolved after satisfactory revalidation.",
                        EVALUATED_AT.plusSeconds(
                                180L),
                        actor())
                        .alert();

        EventValidationAlert restored =
                mapper.toDomain(
                        mapper.toEntity(
                                resolvedAlert));

        assertThat(restored)
                .isEqualTo(
                        resolvedAlert);
    }

    @Test
    void rejectsCloseScopedValidationAlertEntity() {
        ValidationAlertEntity closeAlert =
                ValidationAlertEntity.create(
                        ALERT_ID,
                        null,
                        CLOSE_ID,
                        null,
                        "VR-008",
                        "CRITICAL",
                        true,
                        "ACTIVE",
                        "Close-level inconsistency.",
                        null,
                        null,
                        EVALUATED_AT,
                        AuditActor.RESPONSIBLE_USER_ID,
                        "responsible",
                        EVALUATED_AT,
                        null);

        assertThatThrownBy(
                () -> mapper.toDomain(
                        closeAlert))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "entity must represent an event-scoped validation alert");
    }

    private static EventValidationAlert activeAlert() {
        EventValidationResult failedResult =
                EventValidationResult.create(
                        new ValidationResultId(
                                FAILED_RESULT_ID),
                        ValidationRuleCode.VR_003,
                        1,
                        new OperationalEventId(
                                EVENT_ID),
                        ValidationOutcome.FAILED,
                        "Required supporting evidence is missing.",
                        EVALUATED_AT,
                        actor(),
                        2L);

        return EventValidationAlert.createFromFailedResult(
                new ValidationAlertId(
                        ALERT_ID),
                failedResult,
                "Required supporting evidence is missing.",
                EVALUATED_AT.plusSeconds(
                        60L),
                actor());
    }

    private static AuditActor actor() {
        return new AuditActor(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
    }

}