package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.ValidationResultEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.EventValidationResultPersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.ValidationResultJpaRepository;

class EventValidationResultPersistenceAdapterTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "fb3ac9b3-1451-447c-9047-99d919a50001");

    private static final UUID FIRST_RESULT_ID =
            UUID.fromString(
                    "fb3ac9b3-1451-447c-9047-99d919a50002");

    private static final UUID SECOND_RESULT_ID =
            UUID.fromString(
                    "fb3ac9b3-1451-447c-9047-99d919a50003");

    private static final Instant EVALUATED_AT =
            Instant.parse(
                    "2026-07-26T19:00:00Z");

    private final ValidationResultJpaRepository
            validationResultJpaRepository =
                    mock(
                            ValidationResultJpaRepository.class);

    private final EventValidationResultPersistenceMapper mapper =
            new EventValidationResultPersistenceMapper();

    private final EventValidationResultPersistenceAdapter adapter =
            new EventValidationResultPersistenceAdapter(
                    validationResultJpaRepository,
                    mapper);

    @Test
    void savesNewResultAndSubsequentInvalidation() {
        EventValidationResult currentResult =
                result(
                        FIRST_RESULT_ID,
                        ValidationRuleCode.VR_003,
                        ValidationOutcome.FAILED);

        EventValidationResult invalidatedResult =
                currentResult.invalidate(
                        EVALUATED_AT.plusSeconds(
                                60L),
                        "Event data revision changed.");

        adapter.saveNew(
                currentResult);

        adapter.saveInvalidation(
                invalidatedResult);

        verify(
                validationResultJpaRepository,
                times(2))
                .saveAndFlush(
                        any(
                                ValidationResultEntity.class));
    }

    @Test
    void returnsMappedEventResultById() {
        EventValidationResult expected =
                result(
                        FIRST_RESULT_ID,
                        ValidationRuleCode.VR_003,
                        ValidationOutcome.FAILED);

        when(
                validationResultJpaRepository
                        .findEventResultById(
                                FIRST_RESULT_ID))
                .thenReturn(
                        Optional.of(
                                mapper.toEntity(
                                        expected)));

        Optional<EventValidationResult> result =
                adapter.findById(
                        new ValidationResultId(
                                FIRST_RESULT_ID));

        assertThat(result)
                .contains(
                        expected);

        verify(
                validationResultJpaRepository)
                .findEventResultById(
                        FIRST_RESULT_ID);
    }

    @Test
    void returnsCurrentResultByEventAndRule() {
        EventValidationResult expected =
                result(
                        FIRST_RESULT_ID,
                        ValidationRuleCode.VR_006,
                        ValidationOutcome.SATISFIED);

        when(
                validationResultJpaRepository
                        .findByEventIdAndRuleCodeAndCurrentTrue(
                                EVENT_ID,
                                "VR-006"))
                .thenReturn(
                        Optional.of(
                                mapper.toEntity(
                                        expected)));

        Optional<EventValidationResult> result =
                adapter.findCurrentByEventIdAndRuleCode(
                        new OperationalEventId(
                                EVENT_ID),
                        ValidationRuleCode.VR_006);

        assertThat(result)
                .contains(
                        expected);

        verify(
                validationResultJpaRepository)
                .findByEventIdAndRuleCodeAndCurrentTrue(
                        EVENT_ID,
                        "VR-006");
    }

    @Test
    void preservesRepositoryRuleOrder() {
        EventValidationResult first =
                result(
                        FIRST_RESULT_ID,
                        ValidationRuleCode.VR_001,
                        ValidationOutcome.SATISFIED);

        EventValidationResult second =
                result(
                        SECOND_RESULT_ID,
                        ValidationRuleCode.VR_003,
                        ValidationOutcome.FAILED);

        when(
                validationResultJpaRepository
                        .findAllByEventIdAndCurrentTrueOrderByRuleCodeAsc(
                                EVENT_ID))
                .thenReturn(
                        List.of(
                                mapper.toEntity(
                                        first),
                                mapper.toEntity(
                                        second)));

        List<EventValidationResult> results =
                adapter.findAllCurrentByEventIdOrderByRuleCode(
                        new OperationalEventId(
                                EVENT_ID));

        assertThat(results)
                .containsExactly(
                        first,
                        second);
    }

    @Test
    void returnsEmptyResultsWhenRepositoryFindsNothing() {
        when(
                validationResultJpaRepository
                        .findEventResultById(
                                FIRST_RESULT_ID))
                .thenReturn(
                        Optional.empty());

        when(
                validationResultJpaRepository
                        .findByEventIdAndRuleCodeAndCurrentTrue(
                                EVENT_ID,
                                "VR-002"))
                .thenReturn(
                        Optional.empty());

        assertThat(
                adapter.findById(
                        new ValidationResultId(
                                FIRST_RESULT_ID)))
                .isEmpty();

        assertThat(
                adapter.findCurrentByEventIdAndRuleCode(
                        new OperationalEventId(
                                EVENT_ID),
                        ValidationRuleCode.VR_002))
                .isEmpty();
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(
                () -> adapter.saveNew(
                        null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.findById(
                        null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.findCurrentByEventIdAndRuleCode(
                        null,
                        ValidationRuleCode.VR_001))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.findCurrentByEventIdAndRuleCode(
                        new OperationalEventId(
                                EVENT_ID),
                        null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter
                        .findAllCurrentByEventIdOrderByRuleCode(
                                null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.saveInvalidation(
                        null))
                .isInstanceOf(
                        NullPointerException.class);
    }

    private static EventValidationResult result(
            UUID resultId,
            ValidationRuleCode ruleCode,
            ValidationOutcome outcome) {

        return EventValidationResult.create(
                new ValidationResultId(
                        resultId),
                ruleCode,
                1,
                new OperationalEventId(
                        EVENT_ID),
                outcome,
                "Evaluation detail for "
                        + ruleCode.persistentValue()
                        + ".",
                EVALUATED_AT,
                actor(),
                3L);
    }

    private static AuditActor actor() {
        return new AuditActor(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
    }

}