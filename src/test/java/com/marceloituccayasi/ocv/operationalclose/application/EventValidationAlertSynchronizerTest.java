package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationAlertRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlertChange;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertState;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

class EventValidationAlertSynchronizerTest {

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "eaa3a380-e489-4685-b1bb-180000000001"));

    private static final ValidationResultId FAILED_RESULT_ID =
            new ValidationResultId(
                    UUID.fromString(
                            "eaa3a380-e489-4685-b1bb-180000000002"));

    private static final ValidationResultId SATISFIED_RESULT_ID =
            new ValidationResultId(
                    UUID.fromString(
                            "eaa3a380-e489-4685-b1bb-180000000003"));

    private static final UUID ALERT_UUID =
            UUID.fromString(
                    "eaa3a380-e489-4685-b1bb-180000000004");

    private static final UUID TRANSITION_UUID =
            UUID.fromString(
                    "eaa3a380-e489-4685-b1bb-180000000005");

    private static final ValidationAlertId ALERT_ID =
            new ValidationAlertId(
                    ALERT_UUID);

    private static final Instant FIRST_EVALUATION_AT =
            Instant.parse(
                    "2026-07-28T12:00:00Z");

    private static final Instant REVALIDATION_AT =
            Instant.parse(
                    "2026-07-28T13:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final EventValidationAlertRepository
            alertRepository =
                    mock(
                            EventValidationAlertRepository.class);

    private final UuidGenerator uuidGenerator =
            mock(
                    UuidGenerator.class);

    private final EventValidationAlertSynchronizer
            synchronizer =
                    new EventValidationAlertSynchronizer(
                            alertRepository,
                            uuidGenerator);

    @Test
    void createsAlertAndInitialTransitionForNewFailure() {
        EventValidationResult failedResult =
                failedResult();

        when(
                alertRepository
                        .findOpenByEventIdAndCauseRuleCode(
                                EVENT_ID,
                                ValidationRuleCode.VR_003))
                .thenReturn(
                        Optional.empty());

        when(
                uuidGenerator.next())
                .thenReturn(
                        ALERT_UUID,
                        TRANSITION_UUID);

        synchronizer.synchronize(
                List.of(
                        failedResult),
                FIRST_EVALUATION_AT,
                ACTOR);

        ArgumentCaptor<EventValidationAlert> alertCaptor =
                ArgumentCaptor.forClass(
                        EventValidationAlert.class);

        ArgumentCaptor<ValidationAlertTransition>
                transitionCaptor =
                        ArgumentCaptor.forClass(
                                ValidationAlertTransition.class);

        verify(
                alertRepository)
                .saveNew(
                        alertCaptor.capture(),
                        transitionCaptor.capture());

        EventValidationAlert alert =
                alertCaptor.getValue();

        ValidationAlertTransition transition =
                transitionCaptor.getValue();

        assertThat(
                alert.id())
                .isEqualTo(
                        ALERT_ID);

        assertThat(
                alert.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(
                alert.sourceValidationResultId())
                .isEqualTo(
                        FAILED_RESULT_ID);

        assertThat(
                alert.causeRuleCode())
                .isEqualTo(
                        ValidationRuleCode.VR_003);

        assertThat(
                alert.state())
                .isEqualTo(
                        ValidationAlertState.ACTIVE);

        assertThat(
                transition.id())
                .isEqualTo(
                        new ValidationAlertTransitionId(
                                TRANSITION_UUID));

        assertThat(
                transition.alertId())
                .isEqualTo(
                        ALERT_ID);

        assertThat(
                transition.toState())
                .isEqualTo(
                        ValidationAlertState.ACTIVE);

        assertThat(
                transition.occurredAt())
                .isEqualTo(
                        FIRST_EVALUATION_AT);

        verify(
                uuidGenerator,
                times(
                        2))
                .next();
    }

    @Test
    void maintainsExistingOpenAlertForRepeatedFailure() {
        EventValidationAlert existingAlert =
                existingOpenAlert();

        when(
                alertRepository
                        .findOpenByEventIdAndCauseRuleCode(
                                EVENT_ID,
                                ValidationRuleCode.VR_003))
                .thenReturn(
                        Optional.of(
                                existingAlert));

        synchronizer.synchronize(
                List.of(
                        failedResultAt(
                                REVALIDATION_AT)),
                REVALIDATION_AT,
                ACTOR);

        verify(
                alertRepository)
                .findOpenByEventIdAndCauseRuleCode(
                        EVENT_ID,
                        ValidationRuleCode.VR_003);

        verifyNoMoreInteractions(
                alertRepository);

        verifyNoInteractions(
                uuidGenerator);
    }

    @Test
    void resolvesExistingOpenAlertWithSatisfiedResult() {
        EventValidationAlert existingAlert =
                existingOpenAlert();

        EventValidationResult satisfiedResult =
                satisfiedResult();

        when(
                alertRepository
                        .findOpenByEventIdAndCauseRuleCode(
                                EVENT_ID,
                                ValidationRuleCode.VR_003))
                .thenReturn(
                        Optional.of(
                                existingAlert));

        when(
                alertRepository.findByIdForUpdate(
                        ALERT_ID))
                .thenReturn(
                        Optional.of(
                                existingAlert));

        when(
                uuidGenerator.next())
                .thenReturn(
                        TRANSITION_UUID);

        synchronizer.synchronize(
                List.of(
                        satisfiedResult),
                REVALIDATION_AT,
                ACTOR);

        ArgumentCaptor<EventValidationAlertChange>
                changeCaptor =
                        ArgumentCaptor.forClass(
                                EventValidationAlertChange.class);

        verify(
                alertRepository)
                .saveChange(
                        changeCaptor.capture());

        EventValidationAlertChange change =
                changeCaptor.getValue();

        assertThat(
                change.alert()
                        .state())
                .isEqualTo(
                        ValidationAlertState.RESOLVED);

        assertThat(
                change.alert()
                        .resolvedByValidationResultId())
                .isEqualTo(
                        SATISFIED_RESULT_ID);

        assertThat(
                change.transition()
                        .id())
                .isEqualTo(
                        new ValidationAlertTransitionId(
                                TRANSITION_UUID));

        assertThat(
                change.transition()
                        .fromState())
                .isEqualTo(
                        ValidationAlertState.ACTIVE);

        assertThat(
                change.transition()
                        .toState())
                .isEqualTo(
                        ValidationAlertState.RESOLVED);

        assertThat(
                change.transition()
                        .validationResultId())
                .isEqualTo(
                        SATISFIED_RESULT_ID);

        verify(
                uuidGenerator)
                .next();
    }

    @Test
    void doesNothingWhenSatisfiedResultHasNoOpenAlert() {
        when(
                alertRepository
                        .findOpenByEventIdAndCauseRuleCode(
                                EVENT_ID,
                                ValidationRuleCode.VR_003))
                .thenReturn(
                        Optional.empty());

        synchronizer.synchronize(
                List.of(
                        satisfiedResult()),
                REVALIDATION_AT,
                ACTOR);

        verify(
                alertRepository)
                .findOpenByEventIdAndCauseRuleCode(
                        EVENT_ID,
                        ValidationRuleCode.VR_003);

        verifyNoMoreInteractions(
                alertRepository);

        verifyNoInteractions(
                uuidGenerator);
    }

    @Test
    void rejectsNonCurrentResultBeforeReadingAlerts() {
        EventValidationResult invalidatedResult =
                failedResult()
                        .invalidate(
                                REVALIDATION_AT,
                                "Event data changed.");

        assertThatThrownBy(
                () -> synchronizer.synchronize(
                        List.of(
                                invalidatedResult),
                        REVALIDATION_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validation result must be current");

        verifyNoInteractions(
                alertRepository,
                uuidGenerator);
    }

    @Test
    void rejectsNullInputsBeforeReadingDependencies() {
        assertThatThrownBy(
                () -> synchronizer.synchronize(
                        null,
                        REVALIDATION_AT,
                        ACTOR))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "validation results must not be null");

        assertThatThrownBy(
                () -> synchronizer.synchronize(
                        List.of(),
                        null,
                        ACTOR))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "alert synchronization instant must not be null");

        assertThatThrownBy(
                () -> synchronizer.synchronize(
                        List.of(),
                        REVALIDATION_AT,
                        null))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "alert synchronization actor must not be null");

        verifyNoInteractions(
                alertRepository,
                uuidGenerator);
    }

    private static EventValidationResult failedResult() {
        return failedResultAt(
                FIRST_EVALUATION_AT);
    }

    private static EventValidationResult failedResultAt(
            Instant evaluatedAt) {

        return EventValidationResult.create(
                FAILED_RESULT_ID,
                ValidationRuleCode.VR_003,
                1,
                EVENT_ID,
                ValidationOutcome.FAILED,
                "VR-003 failed: active legible supporting evidence is missing.",
                evaluatedAt,
                ACTOR,
                1L);
    }

    private static EventValidationResult satisfiedResult() {
        return EventValidationResult.create(
                SATISFIED_RESULT_ID,
                ValidationRuleCode.VR_003,
                1,
                EVENT_ID,
                ValidationOutcome.SATISFIED,
                "VR-003 satisfied: active legible supporting evidence is present.",
                REVALIDATION_AT,
                ACTOR,
                1L);
    }

    private static EventValidationAlert existingOpenAlert() {
        EventValidationResult originalFailure =
                failedResult();

        return EventValidationAlert
                .createFromFailedResult(
                        ALERT_ID,
                        originalFailure,
                        originalFailure.detail(),
                        FIRST_EVALUATION_AT,
                        ACTOR);
    }

}