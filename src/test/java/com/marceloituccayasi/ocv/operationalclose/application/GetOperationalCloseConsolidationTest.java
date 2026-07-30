package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

class GetOperationalCloseConsolidationTest {

    private static final UUID CLOSE_UUID =
            UUID.fromString(
                    "18acf171-9c69-4086-b080-790000000001");

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    CLOSE_UUID);

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "18acf171-9c69-4086-b080-790000000002"));

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-30T12:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final OperationalCloseRepository closeRepository =
            mock(
                    OperationalCloseRepository.class);

    private final OperationalEventRepository eventRepository =
            mock(
                    OperationalEventRepository.class);

    private final CloseConsolidationReadinessEvaluator
            readinessEvaluator =
                    mock(
                            CloseConsolidationReadinessEvaluator.class);

    private final TransactionRunner transactionRunner =
            new TransactionRunner() {

                @Override
                public <T> T execute(
                        Supplier<T> operation) {

                    return Objects.requireNonNull(
                            operation)
                            .get();
                }
            };

    private final GetOperationalCloseConsolidation query =
            new GetOperationalCloseConsolidation(
                    closeRepository,
                    eventRepository,
                    readinessEvaluator,
                    transactionRunner);

    @Test
    void returnsCurrentPreviewForPreparationClose() {
        OperationalClose operationalClose =
                close(
                        OperationalCloseState.PREPARATION);

        OperationalEvent event =
                event();

        when(closeRepository.findById(
                CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                operationalClose));

        when(eventRepository
                .findAllByCloseIdOrderByOccurredAtDescending(
                        CLOSE_ID))
                .thenReturn(
                        List.of(
                                event));

        when(readinessEvaluator.evaluate(
                CLOSE_ID,
                List.of(
                        event)))
                .thenReturn(
                        CloseConsolidationReadiness.evaluated(
                                List.of(),
                                List.of(),
                                List.of()));

        GetOperationalCloseConsolidationResult result =
                query.execute(
                        CLOSE_UUID);

        assertThat(
                result.status())
                .isEqualTo(
                        GetOperationalCloseConsolidationResult
                                .Status.FOUND);

        assertThat(
                result.preview())
                .isNotNull();

        assertThat(
                result.preview()
                        .ready())
                .isTrue();

        assertThat(
                result.preview()
                        .expectedBalance())
                .isEqualByComparingTo(
                        new BigDecimal(
                                "1125.0000"));

        assertThat(
                result.message())
                .isNull();
    }

    @Test
    void returnsNotFoundWithoutLoadingEvents() {
        when(closeRepository.findById(
                CLOSE_ID))
                .thenReturn(
                        Optional.empty());

        GetOperationalCloseConsolidationResult result =
                query.execute(
                        CLOSE_UUID);

        assertThat(
                result.status())
                .isEqualTo(
                        GetOperationalCloseConsolidationResult
                                .Status.NOT_FOUND);

        assertThat(
                result.preview())
                .isNull();

        verifyNoInteractions(
                eventRepository,
                readinessEvaluator);
    }

    @Test
    void rejectsAValidatedCloseWithoutLoadingEvents() {
        when(closeRepository.findById(
                CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                close(
                                        OperationalCloseState.VALIDATED)));

        GetOperationalCloseConsolidationResult result =
                query.execute(
                        CLOSE_UUID);

        assertThat(
                result.status())
                .isEqualTo(
                        GetOperationalCloseConsolidationResult
                                .Status.NOT_AVAILABLE);

        assertThat(
                result.preview())
                .isNull();

        verifyNoInteractions(
                eventRepository,
                readinessEvaluator);
    }

    private OperationalClose close(
            OperationalCloseState state) {

        return new OperationalClose(
                CLOSE_ID,
                new OperationalPeriod(
                        LocalDate.of(
                                2026,
                                7,
                                1),
                        LocalDate.of(
                                2026,
                                7,
                                31)),
                new CurrencyCode(
                        "PEN"),
                new InitialBalance(
                        new BigDecimal(
                                "1000.0000")),
                state,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

    private OperationalEvent event() {
        return new OperationalEvent(
                EVENT_ID,
                CLOSE_ID,
                OperationalEventType.INCOME,
                new OperationalEventAmount(
                        new BigDecimal(
                                "125.0000")),
                new BigDecimal(
                        "125.0000"),
                null,
                CREATED_AT,
                CREATED_AT,
                "Caja principal",
                "Evento para consulta de consolidación",
                OperationalEventState.VALIDATED,
                false,
                false,
                4,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

}