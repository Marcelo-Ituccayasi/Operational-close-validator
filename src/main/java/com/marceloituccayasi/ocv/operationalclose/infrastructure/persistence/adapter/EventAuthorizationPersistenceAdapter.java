package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventAuthorizationRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorizationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.EventAuthorizationPersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.EventAuthorizationJpaRepository;

/**
 * JPA implementation of Event Authorization persistence.
 */
@Repository
public class EventAuthorizationPersistenceAdapter
        implements EventAuthorizationRepository {

    private final EventAuthorizationJpaRepository
            eventAuthorizationJpaRepository;

    private final EventAuthorizationPersistenceMapper mapper;

    public EventAuthorizationPersistenceAdapter(
            EventAuthorizationJpaRepository
                    eventAuthorizationJpaRepository,
            EventAuthorizationPersistenceMapper mapper) {

        this.eventAuthorizationJpaRepository =
                Objects.requireNonNull(
                        eventAuthorizationJpaRepository);

        this.mapper =
                Objects.requireNonNull(
                        mapper);
    }

    @Override
    public void saveNew(
            EventAuthorization eventAuthorization) {

        Objects.requireNonNull(
                eventAuthorization,
                "eventAuthorization must not be null");

        eventAuthorizationJpaRepository.saveAndFlush(
                mapper.toEntity(
                        eventAuthorization));
    }

    @Override
    public Optional<EventAuthorization> findById(
            EventAuthorizationId authorizationId) {

        Objects.requireNonNull(
                authorizationId,
                "authorizationId must not be null");

        return eventAuthorizationJpaRepository
                .findById(
                        authorizationId.value())
                .map(
                        mapper::toDomain);
    }

    @Override
    public List<EventAuthorization>
            findAllByEventIdOrderByAuthorizedAtDescending(
                    OperationalEventId eventId) {

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        return eventAuthorizationJpaRepository
                .findAllByEventIdOrderByAuthorizedAtDescCreatedAtDescIdDesc(
                        eventId.value())
                .stream()
                .map(
                        mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<EventAuthorization> findByIdForUpdate(
            OperationalCloseId closeId,
            OperationalEventId eventId,
            EventAuthorizationId authorizationId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        Objects.requireNonNull(
                authorizationId,
                "authorizationId must not be null");

        return eventAuthorizationJpaRepository
                .findByIdForUpdate(
                        closeId.value(),
                        eventId.value(),
                        authorizationId.value())
                .map(
                        mapper::toDomain);
    }

    @Override
    public void saveRevision(
            EventAuthorization eventAuthorization) {

        Objects.requireNonNull(
                eventAuthorization,
                "eventAuthorization must not be null");

        eventAuthorizationJpaRepository.saveAndFlush(
                mapper.toEntity(
                        eventAuthorization));
    }

}