package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import java.util.List;
import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlertChange;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

/**
 * Persistence contract required by Event Validation Alert use cases.
 *
 * <p>Mutation use cases must lock the owning Operational Close and
 * Operational Event before locking an existing Validation Alert.</p>
 */
public interface EventValidationAlertRepository {

    void saveNew(
            EventValidationAlert alert,
            ValidationAlertTransition initialTransition);

    Optional<EventValidationAlert> findById(
            ValidationAlertId alertId);

    Optional<EventValidationAlert> findByIdForUpdate(
            ValidationAlertId alertId);

    Optional<EventValidationAlert>
            findOpenByEventIdAndCauseRuleCode(
                    OperationalEventId eventId,
                    ValidationRuleCode causeRuleCode);

    List<EventValidationAlert>
            findAllOpenByEventIdOrderByCreatedAt(
                    OperationalEventId eventId);

    List<ValidationAlertTransition>
            findHistoryByAlertIdOrderByOccurredAt(
                    ValidationAlertId alertId);

    void saveChange(
            EventValidationAlertChange change);

}