package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.marceloituccayasi.ocv.operationalclose.application.DuplicateOperationalEventCancellationException;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.EventStateTransitionEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.OperationalEventEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.OperationalEventPersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.EventStateTransitionJpaRepository;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.OperationalEventJpaRepository;

/**
 * JPA implementation of Operational Event persistence ports.
 */
@Repository
public class OperationalEventPersistenceAdapter
        implements OperationalEventRepository,
        OperationalEventRevisionRepository {

    private static final String UNIQUE_CANCELLATION_CONSTRAINT =
            "uq_operational_event_reversed_event";

    private final OperationalEventJpaRepository
            operationalEventJpaRepository;

    private final EventStateTransitionJpaRepository
            transitionJpaRepository;

    private final OperationalEventPersistenceMapper mapper;

    public OperationalEventPersistenceAdapter(
            OperationalEventJpaRepository
                    operationalEventJpaRepository,
            EventStateTransitionJpaRepository
                    transitionJpaRepository,
            OperationalEventPersistenceMapper mapper) {

        this.operationalEventJpaRepository =
                Objects.requireNonNull(
                        operationalEventJpaRepository);

        this.transitionJpaRepository =
                Objects.requireNonNull(
                        transitionJpaRepository);

        this.mapper =
                Objects.requireNonNull(
                        mapper);
    }

    @Override
    public void saveNew(
            OperationalEvent operationalEvent,
            EventStateTransition initialTransition) {

        Objects.requireNonNull(
                operationalEvent,
                "operationalEvent must not be null");

        Objects.requireNonNull(
                initialTransition,
                "initialTransition must not be null");

        OperationalEventEntity operationalEventEntity =
                mapper.toEntity(
                        operationalEvent);

        EventStateTransitionEntity transitionEntity =
                mapper.toEntity(
                        initialTransition);

        try {
            operationalEventJpaRepository.saveAndFlush(
                    operationalEventEntity);

            transitionJpaRepository.saveAndFlush(
                    transitionEntity);
        }
        catch (DataIntegrityViolationException exception) {
            translateCancellationConflict(
                    exception);
        }
    }

    @Override
    public Optional<OperationalEvent> findById(
            OperationalEventId eventId) {

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        return operationalEventJpaRepository
                .findById(
                        eventId.value())
                .map(
                        mapper::toDomain);
    }

    @Override
    public List<OperationalEvent>
            findAllByCloseIdOrderByOccurredAtDescending(
                    OperationalCloseId closeId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        return operationalEventJpaRepository
                .findAllByCloseIdOrderByOccurredAtDescRegisteredAtDescIdDesc(
                        closeId.value())
                .stream()
                .map(
                        mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsCancellationFor(
            OperationalEventId reversedEventId) {

        Objects.requireNonNull(
                reversedEventId,
                "reversedEventId must not be null");

        return operationalEventJpaRepository
                .existsByReversedEventId(
                        reversedEventId.value());
    }

    @Override
    public Optional<OperationalEvent> findByIdForUpdate(
            OperationalCloseId closeId,
            OperationalEventId eventId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        return operationalEventJpaRepository
                .findByIdForUpdate(
                        closeId.value(),
                        eventId.value())
                .map(
                        mapper::toDomain);
    }

    @Override
    public Optional<OperationalEvent>
            findCancellationByReversedEventIdForUpdate(
                    OperationalCloseId closeId,
                    OperationalEventId reversedEventId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        Objects.requireNonNull(
                reversedEventId,
                "reversedEventId must not be null");

        return operationalEventJpaRepository
                .findCancellationByReversedEventIdForUpdate(
                        closeId.value(),
                        reversedEventId.value())
                .map(
                        mapper::toDomain);
    }

    @Override
    public void saveRevision(
            OperationalEvent operationalEvent) {

        Objects.requireNonNull(
                operationalEvent,
                "operationalEvent must not be null");

        try {
            operationalEventJpaRepository.saveAndFlush(
                    mapper.toEntity(
                            operationalEvent));
        }
        catch (DataIntegrityViolationException exception) {
            translateCancellationConflict(
                    exception);
        }
    }

    @Override
    public void appendStateTransition(
            EventStateTransition stateTransition) {

        Objects.requireNonNull(
                stateTransition,
                "stateTransition must not be null");

        transitionJpaRepository.saveAndFlush(
                mapper.toEntity(
                        stateTransition));
    }

    private static void translateCancellationConflict(
            DataIntegrityViolationException exception) {

        if (containsConstraintName(
                exception,
                UNIQUE_CANCELLATION_CONSTRAINT)) {

            throw new DuplicateOperationalEventCancellationException(
                    exception);
        }

        throw exception;
    }

    private static boolean containsConstraintName(
            Throwable throwable,
            String constraintName) {

        Throwable current =
                throwable;

        while (current != null) {
            String message =
                    current.getMessage();

            if (message != null
                    && message.contains(
                            constraintName)) {

                return true;
            }

            current =
                    current.getCause();
        }

        return false;
    }

}