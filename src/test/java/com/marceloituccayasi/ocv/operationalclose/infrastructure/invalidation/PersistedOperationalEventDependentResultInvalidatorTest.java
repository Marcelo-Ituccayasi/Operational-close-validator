package com.marceloituccayasi.ocv.operationalclose.infrastructure.invalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

class PersistedOperationalEventDependentResultInvalidatorTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "1e55997a-4cbe-4e09-bded-3f4f62610001"));

    private static final OperationalEventId FIRST_EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "1e55997a-4cbe-4e09-bded-3f4f62610002"));

    private static final OperationalEventId SECOND_EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "1e55997a-4cbe-4e09-bded-3f4f62610003"));

    private static final Instant EVALUATED_AT =
            Instant.parse(
                    "2026-07-27T10:00:00Z");

    private static final Instant INVALIDATED_AT =
            Instant.parse(
                    "2026-07-27T11:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final EventValidationResultRepository
            validationResultRepository =
                    mock(
                            EventValidationResultRepository.class);

    private final ApplicationClock applicationClock =
            mock(
                    ApplicationClock.class);

    private final PersistedOperationalEventDependentResultInvalidator
            invalidator =
                    new PersistedOperationalEventDependentResultInvalidator(
                            validationResultRepository,
                            applicationClock);

    @Test
    void invalidatesAllCurrentResultsUsingOneInstantAndDistinctEventIds() {
        EventValidationResult firstResult =
                result(
                        "1e55997a-4cbe-4e09-bded-3f4f62610004",
                        FIRST_EVENT_ID,
                        ValidationRuleCode.VR_001);

        EventValidationResult secondResult =
                result(
                        "1e55997a-4cbe-4e09-bded-3f4f62610005",
                        SECOND_EVENT_ID,
                        ValidationRuleCode.VR_003);

        List<OperationalEventId> distinctEventIds =
                List.of(
                        FIRST_EVENT_ID,
                        SECOND_EVENT_ID);

        when(
                applicationClock.now())
                .thenReturn(
                        INVALIDATED_AT);

        when(
                validationResultRepository
                        .findAllCurrentForInvalidation(
                                CLOSE_ID,
                                distinctEventIds))
                .thenReturn(
                        List.of(
                                firstResult,
                                secondResult));

        invalidator.invalidateForRevisions(
                CLOSE_ID,
                List.of(
                        FIRST_EVENT_ID,
                        FIRST_EVENT_ID,
                        SECOND_EVENT_ID));

        verify(
                applicationClock,
                times(
                        1))
                .now();

        verify(
                validationResultRepository)
                .findAllCurrentForInvalidation(
                        CLOSE_ID,
                        distinctEventIds);

        ArgumentCaptor<EventValidationResult>
                invalidatedResultCaptor =
                        ArgumentCaptor.forClass(
                                EventValidationResult.class);

        verify(
                validationResultRepository,
                times(
                        2))
                .saveInvalidation(
                        invalidatedResultCaptor.capture());

        assertThat(
                invalidatedResultCaptor
                        .getAllValues())
                .allSatisfy(
                        invalidatedResult -> {
                            assertThat(
                                    invalidatedResult.current())
                                    .isFalse();

                            assertThat(
                                    invalidatedResult.invalidatedAt())
                                    .isEqualTo(
                                            INVALIDATED_AT);

                            assertThat(
                                    invalidatedResult
                                            .invalidationReason())
                                    .isEqualTo(
                                            PersistedOperationalEventDependentResultInvalidator
                                                    .EVENT_DATA_REVISION_CHANGED);
                        });

        assertThat(
                invalidatedResultCaptor
                        .getAllValues())
                .extracting(
                        EventValidationResult::id)
                .containsExactly(
                        firstResult.id(),
                        secondResult.id());
    }

    @Test
    void rejectsInvalidRequestsBeforeReadingClockOrRepository() {
        assertThatThrownBy(
                () -> invalidator.invalidateForRevisions(
                        null,
                        List.of(
                                FIRST_EVENT_ID)))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "closeId must not be null");

        assertThatThrownBy(
                () -> invalidator.invalidateForRevisions(
                        CLOSE_ID,
                        null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> invalidator.invalidateForRevisions(
                        CLOSE_ID,
                        List.of()))
                .isInstanceOf(
                        IllegalArgumentException.class);

        assertThatThrownBy(
                () -> invalidator.invalidateForRevisions(
                        CLOSE_ID,
                        Arrays.asList(
                                FIRST_EVENT_ID,
                                null)))
                .isInstanceOf(
                        NullPointerException.class);

        verifyNoInteractions(
                applicationClock,
                validationResultRepository);
    }

    @Test
    void rejectsNullApplicationTimeBeforeLoadingResults() {
        when(
                applicationClock.now())
                .thenReturn(
                        null);

        assertThatThrownBy(
                () -> invalidator.invalidateForRevisions(
                        CLOSE_ID,
                        List.of(
                                FIRST_EVENT_ID)))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "application clock must not return null");

        verify(
                applicationClock)
                .now();

        verifyNoInteractions(
                validationResultRepository);
    }

    private static EventValidationResult result(
            String resultId,
            OperationalEventId eventId,
            ValidationRuleCode ruleCode) {

        return EventValidationResult.create(
                new ValidationResultId(
                        UUID.fromString(
                                resultId)),
                ruleCode,
                1,
                eventId,
                ValidationOutcome.FAILED,
                "Validation rule failed.",
                EVALUATED_AT,
                ACTOR,
                2L);
    }

}