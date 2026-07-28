package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationAlertRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationStateResolver;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;

class EventValidationOpenAlertInvariantTest {

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "648d04a8-006e-41fb-a475-250000000001"));

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
    void detectsOpenBlockingAlert() {
        EventValidationAlert nonBlockingAlert =
                mock(
                        EventValidationAlert.class);

        EventValidationAlert blockingAlert =
                mock(
                        EventValidationAlert.class);

        when(
                nonBlockingAlert.blocking())
                .thenReturn(
                        false);

        when(
                blockingAlert.blocking())
                .thenReturn(
                        true);

        when(
                alertRepository
                        .findAllOpenByEventIdOrderByCreatedAt(
                                EVENT_ID))
                .thenReturn(
                        List.of(
                                nonBlockingAlert,
                                blockingAlert));

        assertThat(
                synchronizer.hasOpenBlockingAlert(
                        EVENT_ID))
                .isTrue();

        verify(
                alertRepository)
                .findAllOpenByEventIdOrderByCreatedAt(
                        EVENT_ID);
    }

    @Test
    void reportsNoBlockingAlertWhenOpenCollectionIsEmpty() {
        when(
                alertRepository
                        .findAllOpenByEventIdOrderByCreatedAt(
                                EVENT_ID))
                .thenReturn(
                        List.of());

        assertThat(
                synchronizer.hasOpenBlockingAlert(
                        EVENT_ID))
                .isFalse();
    }

    @Test
    void preventsValidatedStateWhileBlockingAlertRemainsOpen() {
        assertThat(
                EventValidationStateResolver
                        .enforceOpenBlockingAlertInvariant(
                                OperationalEventState.VALIDATED,
                                true))
                .isEqualTo(
                        OperationalEventState.OBSERVED);
    }

    @Test
    void preservesRuleDerivedFailureState() {
        assertThat(
                EventValidationStateResolver
                        .enforceOpenBlockingAlertInvariant(
                                OperationalEventState
                                        .PENDING_SUPPORT,
                                true))
                .isEqualTo(
                        OperationalEventState
                                .PENDING_SUPPORT);
    }

    @Test
    void permitsValidatedStateWithoutOpenBlockingAlert() {
        assertThat(
                EventValidationStateResolver
                        .enforceOpenBlockingAlertInvariant(
                                OperationalEventState.VALIDATED,
                                false))
                .isEqualTo(
                        OperationalEventState.VALIDATED);
    }

    @Test
    void rejectsNullResolvedState() {
        assertThatThrownBy(
                () -> EventValidationStateResolver
                        .enforceOpenBlockingAlertInvariant(
                                null,
                                false))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "resolved event state must not be null");
    }

}