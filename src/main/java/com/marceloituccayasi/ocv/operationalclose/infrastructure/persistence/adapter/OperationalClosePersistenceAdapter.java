package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.marceloituccayasi.ocv.operationalclose.application.DuplicateOperationalClosePeriodException;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.CloseStateTransitionEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.OperationalCloseEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.OperationalClosePersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.CloseStateTransitionJpaRepository;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.OperationalCloseJpaRepository;

/**
 * JPA implementation of the Operational Close persistence ports.
 */
@Repository
public class OperationalClosePersistenceAdapter
        implements OperationalCloseRepository,
        OperationalCloseLockRepository {

    private static final String UNIQUE_PERIOD_CONSTRAINT =
            "uq_operational_close_period";

    private final OperationalCloseJpaRepository
            operationalCloseJpaRepository;

    private final CloseStateTransitionJpaRepository
            transitionJpaRepository;

    private final OperationalClosePersistenceMapper mapper;

    public OperationalClosePersistenceAdapter(
            OperationalCloseJpaRepository
                    operationalCloseJpaRepository,
            CloseStateTransitionJpaRepository
                    transitionJpaRepository,
            OperationalClosePersistenceMapper mapper) {

        this.operationalCloseJpaRepository =
                Objects.requireNonNull(
                        operationalCloseJpaRepository);

        this.transitionJpaRepository =
                Objects.requireNonNull(
                        transitionJpaRepository);

        this.mapper =
                Objects.requireNonNull(mapper);
    }

    @Override
    public boolean existsByPeriod(
            OperationalPeriod period) {

        Objects.requireNonNull(
                period,
                "period must not be null");

        return operationalCloseJpaRepository
                .existsByPeriodStartAndPeriodEnd(
                        period.startDate(),
                        period.endDate());
    }

    @Override
    public void saveNew(
            OperationalClose operationalClose,
            CloseStateTransition initialTransition) {

        Objects.requireNonNull(
                operationalClose,
                "operationalClose must not be null");

        Objects.requireNonNull(
                initialTransition,
                "initialTransition must not be null");

        OperationalCloseEntity operationalCloseEntity =
                mapper.toEntity(operationalClose);

        CloseStateTransitionEntity transitionEntity =
                mapper.toEntity(initialTransition);

        try {
            operationalCloseJpaRepository.saveAndFlush(
                    operationalCloseEntity);

            transitionJpaRepository.saveAndFlush(
                    transitionEntity);
        }
        catch (DataIntegrityViolationException exception) {
            if (containsConstraintName(
                    exception,
                    UNIQUE_PERIOD_CONSTRAINT)) {

                throw new DuplicateOperationalClosePeriodException(
                        exception);
            }

            throw exception;
        }
    }

    @Override
    public Optional<OperationalClose> findById(
            OperationalCloseId closeId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        return operationalCloseJpaRepository
                .findById(closeId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<OperationalClose> findByIdForUpdate(
            OperationalCloseId closeId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        return operationalCloseJpaRepository
                .findByIdForUpdate(closeId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<OperationalClose>
            findAllByPeriodDescending() {

        return operationalCloseJpaRepository
                .findAllByOrderByPeriodEndDescPeriodStartDesc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    private static boolean containsConstraintName(
            Throwable throwable,
            String constraintName) {

        Throwable current = throwable;

        while (current != null) {
            String message = current.getMessage();

            if (message != null
                    && message.contains(constraintName)) {

                return true;
            }

            current = current.getCause();
        }

        return false;
    }

}