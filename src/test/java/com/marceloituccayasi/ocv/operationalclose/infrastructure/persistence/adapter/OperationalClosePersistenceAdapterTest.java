package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.marceloituccayasi.ocv.operationalclose.application.DuplicateOperationalClosePeriodException;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.OperationalCloseEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.OperationalClosePersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.CloseStateTransitionJpaRepository;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.OperationalCloseJpaRepository;

class OperationalClosePersistenceAdapterTest {

    private static final UUID JULY_CLOSE_ID =
            UUID.fromString(
                    "6c13876e-b9fe-49f8-a73b-1d32cbb01738");

    private static final UUID JUNE_CLOSE_ID =
            UUID.fromString(
                    "5a92b790-20e2-4e47-9167-68739e90e1fd");

    private static final UUID TRANSITION_ID =
            UUID.fromString(
                    "96bec50f-6525-46cb-b3b1-c5a7185a22db");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-20T05:00:00Z");

    private final OperationalCloseJpaRepository
            operationalCloseJpaRepository =
                    mock(
                            OperationalCloseJpaRepository.class);

    private final CloseStateTransitionJpaRepository
            transitionJpaRepository =
                    mock(
                            CloseStateTransitionJpaRepository.class);

    private final OperationalClosePersistenceMapper mapper =
            new OperationalClosePersistenceMapper();

    private final OperationalClosePersistenceAdapter adapter =
            new OperationalClosePersistenceAdapter(
                    operationalCloseJpaRepository,
                    transitionJpaRepository,
                    mapper);

    @Test
    void checksWhetherPeriodAlreadyExists() {
        OperationalPeriod period =
                new OperationalPeriod(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31));

        when(
                operationalCloseJpaRepository
                        .existsByPeriodStartAndPeriodEnd(
                                period.startDate(),
                                period.endDate()))
                .thenReturn(true);

        assertThat(adapter.existsByPeriod(period))
                .isTrue();

        verify(operationalCloseJpaRepository)
                .existsByPeriodStartAndPeriodEnd(
                        period.startDate(),
                        period.endDate());
    }

    @Test
    void savesCloseAndInitialTransition() {
        OperationalClose operationalClose =
                operationalClose(
                        JULY_CLOSE_ID,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31));

        CloseStateTransition transition =
                CloseStateTransition.initial(
                        new CloseStateTransitionId(
                                TRANSITION_ID),
                        operationalClose.id(),
                        NOW,
                        actor());

        adapter.saveNew(
                operationalClose,
                transition);

        verify(operationalCloseJpaRepository)
                .saveAndFlush(
                        any(OperationalCloseEntity.class));

        verify(transitionJpaRepository)
                .saveAndFlush(any());
    }

    @Test
    void translatesUniquePeriodViolation() {
        OperationalClose operationalClose =
                operationalClose(
                        JULY_CLOSE_ID,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31));

        CloseStateTransition transition =
                CloseStateTransition.initial(
                        new CloseStateTransitionId(
                                TRANSITION_ID),
                        operationalClose.id(),
                        NOW,
                        actor());

        DataIntegrityViolationException persistenceFailure =
                new DataIntegrityViolationException(
                        "duplicate constraint "
                                + "uq_operational_close_period");

        when(
                operationalCloseJpaRepository.saveAndFlush(
                        any(OperationalCloseEntity.class)))
                .thenThrow(persistenceFailure);

        assertThatThrownBy(
                () -> adapter.saveNew(
                        operationalClose,
                        transition))
                .isInstanceOf(
                        DuplicateOperationalClosePeriodException.class)
                .hasCause(persistenceFailure);
    }

    @Test
    void preservesUnrelatedIntegrityViolation() {
        OperationalClose operationalClose =
                operationalClose(
                        JULY_CLOSE_ID,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31));

        CloseStateTransition transition =
                CloseStateTransition.initial(
                        new CloseStateTransitionId(
                                TRANSITION_ID),
                        operationalClose.id(),
                        NOW,
                        actor());

        DataIntegrityViolationException persistenceFailure =
                new DataIntegrityViolationException(
                        "another database constraint");

        when(
                operationalCloseJpaRepository.saveAndFlush(
                        any(OperationalCloseEntity.class)))
                .thenThrow(persistenceFailure);

        assertThatThrownBy(
                () -> adapter.saveNew(
                        operationalClose,
                        transition))
                .isSameAs(persistenceFailure);
    }

    @Test
    void returnsMappedCloseById() {
        OperationalClose expected =
                operationalClose(
                        JULY_CLOSE_ID,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31));

        when(
                operationalCloseJpaRepository.findById(
                        JULY_CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                mapper.toEntity(expected)));

        Optional<OperationalClose> result =
                adapter.findById(
                        new OperationalCloseId(
                                JULY_CLOSE_ID));

        assertThat(result)
                .isPresent();

        assertThat(result.orElseThrow().id().value())
                .isEqualTo(JULY_CLOSE_ID);

        assertThat(
                result.orElseThrow()
                        .currencyCode()
                        .value())
                .isEqualTo("PEN");
    }

    @Test
    void returnsEmptyWhenCloseDoesNotExist() {
        when(
                operationalCloseJpaRepository.findById(
                        JULY_CLOSE_ID))
                .thenReturn(Optional.empty());

        Optional<OperationalClose> result =
                adapter.findById(
                        new OperationalCloseId(
                                JULY_CLOSE_ID));

        assertThat(result)
                .isEmpty();
    }

    @Test
    void preservesRepositoryPeriodOrder() {
        OperationalClose july =
                operationalClose(
                        JULY_CLOSE_ID,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31));

        OperationalClose june =
                operationalClose(
                        JUNE_CLOSE_ID,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30));

        when(
                operationalCloseJpaRepository
                        .findAllByOrderByPeriodEndDescPeriodStartDesc())
                .thenReturn(
                        List.of(
                                mapper.toEntity(july),
                                mapper.toEntity(june)));

        List<OperationalClose> result =
                adapter.findAllByPeriodDescending();

        assertThat(result)
                .hasSize(2);

        assertThat(result.get(0).id().value())
                .isEqualTo(JULY_CLOSE_ID);

        assertThat(result.get(1).id().value())
                .isEqualTo(JUNE_CLOSE_ID);
    }

    private static OperationalClose operationalClose(
            UUID closeId,
            LocalDate periodStart,
            LocalDate periodEnd) {

        return OperationalClose.create(
                new OperationalCloseId(closeId),
                new OperationalPeriod(
                        periodStart,
                        periodEnd),
                new CurrencyCode("PEN"),
                new InitialBalance(
                        new BigDecimal("100.0000")),
                NOW,
                actor());
    }

    private static AuditActor actor() {
        return new AuditActor(
                "responsible-user",
                "responsible");
    }

}
