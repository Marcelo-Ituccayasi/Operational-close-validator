package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

class GetOperationalCloseDetailTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "6c13876e-b9fe-49f8-a73b-1d32cbb01738");

    @Test
    void returnsMappedDetailWhenCloseExists() {
        OperationalClose operationalClose =
                operationalClose(CLOSE_ID);

        GetOperationalCloseDetail useCase =
                new GetOperationalCloseDetail(
                        new QueryOperationalClosePort(
                                List.of(operationalClose)),
                        new DirectTransactionRunner());

        GetOperationalCloseResult result =
                useCase.execute(CLOSE_ID);

        assertThat(result.status())
                .isEqualTo(
                        GetOperationalCloseResult.Status.FOUND);

        assertThat(result.operationalClose().id())
                .isEqualTo(CLOSE_ID);

        assertThat(result.operationalClose().currencyCode())
                .isEqualTo("PEN");

        assertThat(result.operationalClose().initialBalance())
                .isEqualByComparingTo("100.0000");

        assertThat(result.operationalClose().createdByUserId())
                .isEqualTo("responsible-user");
    }

    @Test
    void returnsNotFoundWhenCloseDoesNotExist() {
        GetOperationalCloseDetail useCase =
                new GetOperationalCloseDetail(
                        new QueryOperationalClosePort(List.of()),
                        new DirectTransactionRunner());

        GetOperationalCloseResult result =
                useCase.execute(CLOSE_ID);

        assertThat(result.status())
                .isEqualTo(
                        GetOperationalCloseResult.Status.NOT_FOUND);

        assertThat(result.operationalClose())
                .isNull();
    }

    private static OperationalClose operationalClose(
            UUID id) {

        Instant now =
                Instant.parse(
                        "2026-07-20T05:00:00Z");

        return OperationalClose.create(
                new OperationalCloseId(id),
                new OperationalPeriod(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31)),
                new CurrencyCode("PEN"),
                new InitialBalance(
                        new BigDecimal("100.0000")),
                now,
                new AuditActor(
                        "responsible-user",
                        "responsible"));
    }

    private static final class DirectTransactionRunner
            implements TransactionRunner {

        @Override
        public <T> T execute(
                Supplier<T> operation) {

            return operation.get();
        }

    }

    private record QueryOperationalClosePort(
            List<OperationalClose> closes)
            implements OperationalCloseRepository {

        @Override
        public boolean existsByPeriod(
                OperationalPeriod period) {

            return false;
        }

        @Override
        public void saveNew(
                OperationalClose operationalClose,
                CloseStateTransition initialTransition) {

            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OperationalClose> findById(
                OperationalCloseId closeId) {

            return closes.stream()
                    .filter(
                            operationalClose ->
                                    operationalClose.id()
                                            .equals(closeId))
                    .findFirst();
        }

        @Override
        public List<OperationalClose> findAllByPeriodDescending() {
            return List.copyOf(closes);
        }

    }

}
