package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertState;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;

class ValidationAlertTransitionPersistenceMapperTest {

    private static final ValidationAlertTransitionId TRANSITION_ID =
            new ValidationAlertTransitionId(
                    UUID.fromString(
                            "7fd498bd-7437-45f9-982c-b412bfb70001"));

    private static final ValidationAlertId ALERT_ID =
            new ValidationAlertId(
                    UUID.fromString(
                            "7fd498bd-7437-45f9-982c-b412bfb70002"));

    private static final ValidationResultId RESULT_ID =
            new ValidationResultId(
                    UUID.fromString(
                            "7fd498bd-7437-45f9-982c-b412bfb70003"));

    private static final Instant OCCURRED_AT =
            Instant.parse(
                    "2026-07-26T22:00:00Z");

    private final ValidationAlertTransitionPersistenceMapper mapper =
            new ValidationAlertTransitionPersistenceMapper();

    @Test
    void roundTripsInitialTransitionWithNullFromState() {
        ValidationAlertTransition transition =
                ValidationAlertTransition.initial(
                        TRANSITION_ID,
                        ALERT_ID,
                        "Alert created from failed validation.",
                        OCCURRED_AT,
                        actor());

        ValidationAlertTransition restored =
                mapper.toDomain(
                        mapper.toEntity(
                                transition));

        assertThat(restored)
                .isEqualTo(
                        transition);

        assertThat(restored.fromState())
                .isNull();
    }

    @Test
    void roundTripsResolvedTransitionWithValidationResult() {
        ValidationAlertTransition transition =
                ValidationAlertTransition.resolved(
                        TRANSITION_ID,
                        ALERT_ID,
                        ValidationAlertState.UNDER_REVIEW,
                        "Resolved after satisfactory revalidation.",
                        RESULT_ID,
                        OCCURRED_AT,
                        actor());

        ValidationAlertTransition restored =
                mapper.toDomain(
                        mapper.toEntity(
                                transition));

        assertThat(restored)
                .isEqualTo(
                        transition);

        assertThat(restored.validationResultId())
                .isEqualTo(
                        RESULT_ID);
    }

    private static AuditActor actor() {
        return new AuditActor(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
    }

}