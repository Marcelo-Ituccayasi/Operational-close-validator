package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

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
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ValidationAlertEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ValidationAlertTransitionEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.EventValidationAlertPersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.ValidationAlertTransitionPersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.ValidationAlertJpaRepository;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.ValidationAlertTransitionJpaRepository;

class EventValidationAlertPersistenceAdapterTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "17ee7d03-ffec-4cd3-a853-81680a8a0001");

    private static final UUID RESULT_ID =
            UUID.fromString(
                    "17ee7d03-ffec-4cd3-a853-81680a8a0002");

    private static final UUID ALERT_ID =
            UUID.fromString(
                    "17ee7d03-ffec-4cd3-a853-81680a8a0003");

    private static final UUID INITIAL_TRANSITION_ID =
            UUID.fromString(
                    "17ee7d03-ffec-4cd3-a853-81680a8a0004");

    private static final UUID ACKNOWLEDGED_TRANSITION_ID =
            UUID.fromString(
                    "17ee7d03-ffec-4cd3-a853-81680a8a0005");

    private static final Instant EVALUATED_AT =
            Instant.parse(
                    "2026-07-27T01:00:00Z");

    private static final Instant CREATED_AT =
            EVALUATED_AT.plusSeconds(
                    60L);

    private final ValidationAlertJpaRepository
            validationAlertJpaRepository =
                    mock(
                            ValidationAlertJpaRepository.class);

    private final ValidationAlertTransitionJpaRepository
            validationAlertTransitionJpaRepository =
                    mock(
                            ValidationAlertTransitionJpaRepository.class);

    private final EventValidationAlertPersistenceMapper alertMapper =
            new EventValidationAlertPersistenceMapper();

    private final ValidationAlertTransitionPersistenceMapper
            transitionMapper =
                    new ValidationAlertTransitionPersistenceMapper();

    private final EventValidationAlertPersistenceAdapter adapter =
            new EventValidationAlertPersistenceAdapter(
                    validationAlertJpaRepository,
                    validationAlertTransitionJpaRepository,
                    alertMapper,
                    transitionMapper);

    @Test
    void savesNewAlertAndInitialTransition() {
        EventValidationAlert alert =
                activeAlert();

        ValidationAlertTransition transition =
                initialTransition();

        adapter.saveNew(
                alert,
                transition);

        verify(
                validationAlertJpaRepository)
                .saveAndFlush(
                        any(
                                ValidationAlertEntity.class));

        verify(
                validationAlertTransitionJpaRepository)
                .saveAndFlush(
                        any(
                                ValidationAlertTransitionEntity.class));
    }

    @Test
    void returnsMappedAlertByIdAndForUpdate() {
        EventValidationAlert expected =
                activeAlert();

        ValidationAlertEntity entity =
                alertMapper.toEntity(
                        expected);

        when(
                validationAlertJpaRepository
                        .findEventAlertById(
                                ALERT_ID))
                .thenReturn(
                        Optional.of(
                                entity));

        when(
                validationAlertJpaRepository
                        .findEventAlertByIdForUpdate(
                                ALERT_ID))
                .thenReturn(
                        Optional.of(
                                entity));

        assertThat(
                adapter.findById(
                        new ValidationAlertId(
                                ALERT_ID)))
                .contains(
                        expected);

        assertThat(
                adapter.findByIdForUpdate(
                        new ValidationAlertId(
                                ALERT_ID)))
                .contains(
                        expected);
    }

    @Test
    void findsLatestOpenAlertForEventRule() {
        EventValidationAlert expected =
                activeAlert();

        when(
                validationAlertJpaRepository
                        .findFirstByEventIdAndCauseCodeAndStateInOrderByCreatedAtDescIdDesc(
                                eq(
                                        EVENT_ID),
                                eq(
                                        "VR-003"),
                                eq(
                                        openStates())))
                .thenReturn(
                        Optional.of(
                                alertMapper.toEntity(
                                        expected)));

        Optional<EventValidationAlert> result =
                adapter.findOpenByEventIdAndCauseRuleCode(
                        new OperationalEventId(
                                EVENT_ID),
                        ValidationRuleCode.VR_003);

        assertThat(result)
                .contains(
                        expected);
    }

    @Test
    void preservesOpenAlertAndTransitionOrder() {
        EventValidationAlert alert =
                activeAlert();

        ValidationAlertTransition initial =
                initialTransition();

        EventValidationAlertChange acknowledged =
                alert.acknowledge(
                        new ValidationAlertTransitionId(
                                ACKNOWLEDGED_TRANSITION_ID),
                        "Responsible user acknowledged the alert.",
                        CREATED_AT.plusSeconds(
                                60L),
                        actor());

        when(
                validationAlertJpaRepository
                        .findAllByEventIdAndStateInOrderByCreatedAtAscIdAsc(
                                EVENT_ID,
                                openStates()))
                .thenReturn(
                        List.of(
                                alertMapper.toEntity(
                                        alert)));

        when(
                validationAlertTransitionJpaRepository
                        .findAllByAlertIdOrderByOccurredAtAscIdAsc(
                                ALERT_ID))
                .thenReturn(
                        List.of(
                                transitionMapper.toEntity(
                                        initial),
                                transitionMapper.toEntity(
                                        acknowledged.transition())));

        assertThat(
                adapter.findAllOpenByEventIdOrderByCreatedAt(
                        new OperationalEventId(
                                EVENT_ID)))
                .containsExactly(
                        alert);

        assertThat(
                adapter.findHistoryByAlertIdOrderByOccurredAt(
                        new ValidationAlertId(
                                ALERT_ID)))
                .containsExactly(
                        initial,
                        acknowledged.transition());
    }

    @Test
    void savesAlertLifecycleChange() {
        EventValidationAlertChange change =
                activeAlert().acknowledge(
                        new ValidationAlertTransitionId(
                                ACKNOWLEDGED_TRANSITION_ID),
                        "Responsible user acknowledged the alert.",
                        CREATED_AT.plusSeconds(
                                60L),
                        actor());

        adapter.saveChange(
                change);

        verify(
                validationAlertJpaRepository)
                .saveAndFlush(
                        any(
                                ValidationAlertEntity.class));

        verify(
                validationAlertTransitionJpaRepository)
                .saveAndFlush(
                        any(
                                ValidationAlertTransitionEntity.class));
    }

    @Test
    void rejectsInvalidInitialTransitionAndCloseRule() {
        EventValidationAlert alert =
                activeAlert();

        ValidationAlertTransition invalidTransition =
                ValidationAlertTransition.acknowledged(
                        new ValidationAlertTransitionId(
                                INITIAL_TRANSITION_ID),
                        alert.id(),
                        "Not an initial transition.",
                        CREATED_AT.plusSeconds(
                                60L),
                        actor());

        assertThatThrownBy(
                () -> adapter.saveNew(
                        alert,
                        invalidTransition))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "initial transition must create the persisted active alert");

        assertThatThrownBy(
                () -> adapter.findOpenByEventIdAndCauseRuleCode(
                        new OperationalEventId(
                                EVENT_ID),
                        ValidationRuleCode.VR_008))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "causeRuleCode must be event-scoped");
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(
                () -> adapter.saveNew(
                        null,
                        initialTransition()))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.saveNew(
                        activeAlert(),
                        null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.findById(
                        null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.findByIdForUpdate(
                        null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.findOpenByEventIdAndCauseRuleCode(
                        null,
                        ValidationRuleCode.VR_003))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.findOpenByEventIdAndCauseRuleCode(
                        new OperationalEventId(
                                EVENT_ID),
                        null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter
                        .findAllOpenByEventIdOrderByCreatedAt(
                                null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter
                        .findHistoryByAlertIdOrderByOccurredAt(
                                null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.saveChange(
                        null))
                .isInstanceOf(
                        NullPointerException.class);
    }

    private static EventValidationAlert activeAlert() {
        return EventValidationAlert.createFromFailedResult(
                new ValidationAlertId(
                        ALERT_ID),
                failedResult(),
                "Required supporting evidence is missing.",
                CREATED_AT,
                actor());
    }

    private static ValidationAlertTransition initialTransition() {
        return ValidationAlertTransition.initial(
                new ValidationAlertTransitionId(
                        INITIAL_TRANSITION_ID),
                new ValidationAlertId(
                        ALERT_ID),
                "Alert created from failed validation.",
                CREATED_AT,
                actor());
    }

    private static EventValidationResult failedResult() {
        return EventValidationResult.create(
                new ValidationResultId(
                        RESULT_ID),
                ValidationRuleCode.VR_003,
                1,
                new OperationalEventId(
                        EVENT_ID),
                ValidationOutcome.FAILED,
                "Required supporting evidence is missing.",
                EVALUATED_AT,
                actor(),
                3L);
    }

    private static List<String> openStates() {
        return List.of(
                ValidationAlertState.ACTIVE.name(),
                ValidationAlertState.ACKNOWLEDGED.name(),
                ValidationAlertState.UNDER_REVIEW.name());
    }

    private static AuditActor actor() {
        return new AuditActor(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
    }

}