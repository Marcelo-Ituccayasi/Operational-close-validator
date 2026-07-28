package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationAlertRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlertChange;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertState;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleScope;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.EventValidationAlertPersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.ValidationAlertTransitionPersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.ValidationAlertJpaRepository;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.ValidationAlertTransitionJpaRepository;

/**
 * JPA implementation of Event Validation Alert persistence.
 */
@Repository
public class EventValidationAlertPersistenceAdapter
        implements EventValidationAlertRepository {

    private static final List<String> OPEN_STATES =
            List.of(
                    ValidationAlertState.ACTIVE.name(),
                    ValidationAlertState.ACKNOWLEDGED.name(),
                    ValidationAlertState.UNDER_REVIEW.name());

    private final ValidationAlertJpaRepository
            validationAlertJpaRepository;

    private final ValidationAlertTransitionJpaRepository
            validationAlertTransitionJpaRepository;

    private final EventValidationAlertPersistenceMapper
            alertMapper;

    private final ValidationAlertTransitionPersistenceMapper
            transitionMapper;

    public EventValidationAlertPersistenceAdapter(
            ValidationAlertJpaRepository
                    validationAlertJpaRepository,
            ValidationAlertTransitionJpaRepository
                    validationAlertTransitionJpaRepository,
            EventValidationAlertPersistenceMapper alertMapper,
            ValidationAlertTransitionPersistenceMapper
                    transitionMapper) {

        this.validationAlertJpaRepository =
                Objects.requireNonNull(
                        validationAlertJpaRepository);

        this.validationAlertTransitionJpaRepository =
                Objects.requireNonNull(
                        validationAlertTransitionJpaRepository);

        this.alertMapper =
                Objects.requireNonNull(
                        alertMapper);

        this.transitionMapper =
                Objects.requireNonNull(
                        transitionMapper);
    }

    @Override
    @Transactional
    public void saveNew(
            EventValidationAlert alert,
            ValidationAlertTransition initialTransition) {

        Objects.requireNonNull(
                alert,
                "alert must not be null");

        Objects.requireNonNull(
                initialTransition,
                "initialTransition must not be null");

        requireInitialTransition(
                alert,
                initialTransition);

        validationAlertJpaRepository.saveAndFlush(
                alertMapper.toEntity(
                        alert));

        validationAlertTransitionJpaRepository.saveAndFlush(
                transitionMapper.toEntity(
                        initialTransition));
    }

    @Override
    public Optional<EventValidationAlert> findById(
            ValidationAlertId alertId) {

        Objects.requireNonNull(
                alertId,
                "alertId must not be null");

        return validationAlertJpaRepository
                .findEventAlertById(
                        alertId.value())
                .map(
                        alertMapper::toDomain);
    }

    @Override
    public Optional<EventValidationAlert> findByIdForUpdate(
            ValidationAlertId alertId) {

        Objects.requireNonNull(
                alertId,
                "alertId must not be null");

        return validationAlertJpaRepository
                .findEventAlertByIdForUpdate(
                        alertId.value())
                .map(
                        alertMapper::toDomain);
    }

    @Override
    public Optional<EventValidationAlert>
            findOpenByEventIdAndCauseRuleCode(
                    OperationalEventId eventId,
                    ValidationRuleCode causeRuleCode) {

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        requireEventRule(
                causeRuleCode);

        return validationAlertJpaRepository
                .findFirstByEventIdAndCauseCodeAndStateInOrderByCreatedAtDescIdDesc(
                        eventId.value(),
                        causeRuleCode.persistentValue(),
                        OPEN_STATES)
                .map(
                        alertMapper::toDomain);
    }

    @Override
    public List<EventValidationAlert>
            findAllOpenByEventIdOrderByCreatedAt(
                    OperationalEventId eventId) {

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        return validationAlertJpaRepository
                .findAllByEventIdAndStateInOrderByCreatedAtAscIdAsc(
                        eventId.value(),
                        OPEN_STATES)
                .stream()
                .map(
                        alertMapper::toDomain)
                .toList();
    }

    @Override
    public List<ValidationAlertTransition>
            findHistoryByAlertIdOrderByOccurredAt(
                    ValidationAlertId alertId) {

        Objects.requireNonNull(
                alertId,
                "alertId must not be null");

        return validationAlertTransitionJpaRepository
                .findAllByAlertIdOrderByOccurredAtAscIdAsc(
                        alertId.value())
                .stream()
                .map(
                        transitionMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void saveChange(
            EventValidationAlertChange change) {

        Objects.requireNonNull(
                change,
                "change must not be null");

        validationAlertJpaRepository.saveAndFlush(
                alertMapper.toEntity(
                        change.alert()));

        validationAlertTransitionJpaRepository.saveAndFlush(
                transitionMapper.toEntity(
                        change.transition()));
    }

    private static void requireInitialTransition(
            EventValidationAlert alert,
            ValidationAlertTransition transition) {

        if (!alert.id().equals(
                transition.alertId())
                || transition.fromState() != null
                || transition.toState()
                        != ValidationAlertState.ACTIVE
                || alert.state()
                        != ValidationAlertState.ACTIVE
                || !alert.createdAt().equals(
                        transition.occurredAt())
                || !alert.updatedAt().equals(
                        transition.occurredAt())) {

            throw new IllegalArgumentException(
                    "initial transition must create the persisted active alert");
        }
    }

    private static void requireEventRule(
            ValidationRuleCode ruleCode) {

        Objects.requireNonNull(
                ruleCode,
                "causeRuleCode must not be null");

        if (ruleCode.scope()
                != ValidationRuleScope.EVENT) {

            throw new IllegalArgumentException(
                    "causeRuleCode must be event-scoped");
        }
    }

}