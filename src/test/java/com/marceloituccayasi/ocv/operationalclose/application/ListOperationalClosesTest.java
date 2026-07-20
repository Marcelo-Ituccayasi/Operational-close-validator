package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

class ListOperationalClosesTest {

    @Test
    void mapsRepositoryOrderWithoutExposingDomainObjects() {
        UUID julyId =
                UUID.fromString(
                        "6c13876e-b9fe-49f8-a73b-1d32cbb01738");

        UUID juneId =
                UUID.fromString(
                        "5a92b790-20e2-4e47-9167-68739e90e1fd");

        OperationalClose july =
                operationalClose(
                        julyId,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31));

        OperationalClose june =
                operationalClose(
                        juneId,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30));

        ListOperationalCloses useCase =
                new ListOperationalCloses(
                        new QueryOperationalClosePort(
                                List.of(july, june)),
                        new DirectTransactionRunner());

        List<OperationalCloseView> result =
                useCase.execute();

        assertThat(result)
                .extracting(
                        OperationalCloseView::id)
                .containsExactly(
                        julyId,
                        juneId);

        assertThat(result)
                .extracting(
                        OperationalCloseView::state)
                .containsOnly("PREPARATION");
    }

    @Test
    void returnsAnUnmodifiableList() {
        ListOperationalCloses useCase =
                new ListOperationalCloses(
                        new QueryOperationalClosePort(List.of()),
                        new DirectTransactionRunner());

        List<OperationalCloseView> result =
                useCase.execute();

        assertThatThrownBy(result::clear)
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    private static OperationalClose operationalClose(
            UUID id,
            LocalDate periodStart,
            LocalDate periodEnd) {

        return OperationalClose.create(
                new OperationalCloseId(id),
                new OperationalPeriod(
                        periodStart,
                        periodEnd),
                new CurrencyCode("PEN"),
                new InitialBalance(
                        new BigDecimal("100.0000")),
                Instant.parse(
                        "2026-07-20T05:00:00Z"),
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
