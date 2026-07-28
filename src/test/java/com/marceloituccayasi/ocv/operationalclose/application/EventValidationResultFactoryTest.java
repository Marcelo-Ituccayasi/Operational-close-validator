package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationContext;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationRuleEvaluation;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

class EventValidationResultFactoryTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "e88344a0-59cd-4de4-a247-170000000001"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "e88344a0-59cd-4de4-a247-170000000002"));

    private static final UUID FIRST_RESULT_UUID =
            UUID.fromString(
                    "e88344a0-59cd-4de4-a247-170000000003");

    private static final UUID SECOND_RESULT_UUID =
            UUID.fromString(
                    "e88344a0-59cd-4de4-a247-170000000004");

    private static final Instant REGISTERED_AT =
            Instant.parse(
                    "2026-07-28T10:00:00Z");

    private static final Instant EVALUATED_AT =
            Instant.parse(
                    "2026-07-28T11:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final UuidGenerator uuidGenerator =
            mock(
                    UuidGenerator.class);

    private final EventValidationResultFactory factory =
            new EventValidationResultFactory(
                    uuidGenerator);

    @Test
    void createsOneImmutableResultPerEvaluation() {
        when(
                uuidGenerator.next())
                .thenReturn(
                        FIRST_RESULT_UUID,
                        SECOND_RESULT_UUID);

        EventValidationRuleEvaluation satisfiedEvaluation =
                new EventValidationRuleEvaluation(
                        ValidationRuleCode.VR_001,
                        1,
                        ValidationOutcome.SATISFIED,
                        "VR-001 satisfied.");

        EventValidationRuleEvaluation failedEvaluation =
                new EventValidationRuleEvaluation(
                        ValidationRuleCode.VR_003,
                        1,
                        ValidationOutcome.FAILED,
                        "VR-003 failed.");

        List<EventValidationResult> results =
                factory.createAll(
                        context(),
                        List.of(
                                satisfiedEvaluation,
                                failedEvaluation),
                        EVALUATED_AT,
                        ACTOR);

        assertThat(results)
                .hasSize(
                        2);

        assertThat(results)
                .extracting(
                        EventValidationResult::id)
                .containsExactly(
                        new ValidationResultId(
                                FIRST_RESULT_UUID),
                        new ValidationResultId(
                                SECOND_RESULT_UUID));

        assertThat(results)
                .extracting(
                        EventValidationResult::ruleCode)
                .containsExactly(
                        ValidationRuleCode.VR_001,
                        ValidationRuleCode.VR_003);

        assertThat(results)
                .extracting(
                        EventValidationResult::outcome)
                .containsExactly(
                        ValidationOutcome.SATISFIED,
                        ValidationOutcome.FAILED);

        assertThat(results)
                .allSatisfy(
                        result -> {
                            assertThat(
                                    result.eventId())
                                    .isEqualTo(
                                            EVENT_ID);

                            assertThat(
                                    result.ruleVersion())
                                    .isEqualTo(
                                            1);

                            assertThat(
                                    result.evaluatedAt())
                                    .isEqualTo(
                                            EVALUATED_AT);

                            assertThat(
                                    result.evaluatedBy())
                                    .isEqualTo(
                                            ACTOR);

                            assertThat(
                                    result.eventDataRevision())
                                    .isEqualTo(
                                            1L);

                            assertThat(
                                    result.current())
                                    .isTrue();

                            assertThat(
                                    result.invalidatedAt())
                                    .isNull();

                            assertThat(
                                    result.invalidationReason())
                                    .isNull();
                        });

        verify(
                uuidGenerator,
                times(
                        2))
                .next();
    }

    @Test
    void supportsAnEventWithoutApplicableRules() {
        List<EventValidationResult> results =
                factory.createAll(
                        context(),
                        List.of(),
                        EVALUATED_AT,
                        ACTOR);

        assertThat(results)
                .isEmpty();

        verifyNoInteractions(
                uuidGenerator);
    }

    @Test
    void rejectsNullInputsBeforeGeneratingIdentifiers() {
        assertThatThrownBy(
                () -> factory.createAll(
                        null,
                        List.of(),
                        EVALUATED_AT,
                        ACTOR))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "validation context must not be null");

        assertThatThrownBy(
                () -> factory.createAll(
                        context(),
                        null,
                        EVALUATED_AT,
                        ACTOR))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "validation evaluations must not be null");

        assertThatThrownBy(
                () -> factory.createAll(
                        context(),
                        List.of(),
                        null,
                        ACTOR))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "evaluation instant must not be null");

        assertThatThrownBy(
                () -> factory.createAll(
                        context(),
                        List.of(),
                        EVALUATED_AT,
                        null))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "evaluation actor must not be null");

        verifyNoInteractions(
                uuidGenerator);
    }

    @Test
    void rejectsNullEvaluation() {
        List<EventValidationRuleEvaluation> evaluations =
                new java.util.ArrayList<>();

        evaluations.add(
                null);

        assertThatThrownBy(
                () -> factory.createAll(
                        context(),
                        evaluations,
                        EVALUATED_AT,
                        ACTOR))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "validation evaluation must not be null");

        verifyNoInteractions(
                uuidGenerator);
    }

    @Test
    void rejectsNullGeneratedIdentifier() {
        when(
                uuidGenerator.next())
                .thenReturn(
                        null);

        EventValidationRuleEvaluation evaluation =
                new EventValidationRuleEvaluation(
                        ValidationRuleCode.VR_001,
                        1,
                        ValidationOutcome.SATISFIED,
                        "VR-001 satisfied.");

        assertThatThrownBy(
                () -> factory.createAll(
                        context(),
                        List.of(
                                evaluation),
                        EVALUATED_AT,
                        ACTOR))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "generated validation result UUID must not be null");
    }

    private static EventValidationContext context() {
        return new EventValidationContext(
                OperationalEvent.create(
                        EVENT_ID,
                        CLOSE_ID,
                        OperationalEventType.EXPENSE,
                        new OperationalEventAmount(
                                new BigDecimal(
                                        "50.0000")),
                        REGISTERED_AT.minusSeconds(
                                60L),
                        "Caja principal",
                        "Evento para crear resultados de validación",
                        true,
                        true,
                        REGISTERED_AT,
                        ACTOR),
                List.of(),
                List.of());
    }

}