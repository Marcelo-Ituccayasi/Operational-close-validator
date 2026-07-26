package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.EventValidationResultPersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.ValidationResultJpaRepository;

/**
 * JPA implementation of Event Validation Result persistence.
 */
@Repository
public class EventValidationResultPersistenceAdapter
        implements EventValidationResultRepository {

    private final ValidationResultJpaRepository
            validationResultJpaRepository;

    private final EventValidationResultPersistenceMapper mapper;

    public EventValidationResultPersistenceAdapter(
            ValidationResultJpaRepository
                    validationResultJpaRepository,
            EventValidationResultPersistenceMapper mapper) {

        this.validationResultJpaRepository =
                Objects.requireNonNull(
                        validationResultJpaRepository);

        this.mapper =
                Objects.requireNonNull(
                        mapper);
    }

    @Override
    public void saveNew(
            EventValidationResult validationResult) {

        Objects.requireNonNull(
                validationResult,
                "validationResult must not be null");

        validationResultJpaRepository.saveAndFlush(
                mapper.toEntity(
                        validationResult));
    }

    @Override
    public Optional<EventValidationResult> findById(
            ValidationResultId validationResultId) {

        Objects.requireNonNull(
                validationResultId,
                "validationResultId must not be null");

        return validationResultJpaRepository
                .findEventResultById(
                        validationResultId.value())
                .map(
                        mapper::toDomain);
    }

    @Override
    public Optional<EventValidationResult>
            findCurrentByEventIdAndRuleCode(
                    OperationalEventId eventId,
                    ValidationRuleCode ruleCode) {

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        Objects.requireNonNull(
                ruleCode,
                "ruleCode must not be null");

        return validationResultJpaRepository
                .findByEventIdAndRuleCodeAndCurrentTrue(
                        eventId.value(),
                        ruleCode.persistentValue())
                .map(
                        mapper::toDomain);
    }

    @Override
    public List<EventValidationResult>
            findAllCurrentByEventIdOrderByRuleCode(
                    OperationalEventId eventId) {

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        return validationResultJpaRepository
                .findAllByEventIdAndCurrentTrueOrderByRuleCodeAsc(
                        eventId.value())
                .stream()
                .map(
                        mapper::toDomain)
                .toList();
    }

    @Override
    public void saveInvalidation(
            EventValidationResult validationResult) {

        Objects.requireNonNull(
                validationResult,
                "validationResult must not be null");

        validationResultJpaRepository.saveAndFlush(
                mapper.toEntity(
                        validationResult));
    }

}