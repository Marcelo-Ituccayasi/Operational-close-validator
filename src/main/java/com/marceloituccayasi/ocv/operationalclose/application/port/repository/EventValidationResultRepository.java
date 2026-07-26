package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import java.util.List;
import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

/**
 * Persistence contract required by Event Validation use cases.
 *
 * <p>Mutation use cases must lock the owning Operational Close and
 * Operational Event before changing current Validation Results.</p>
 */
public interface EventValidationResultRepository {

    void saveNew(
            EventValidationResult validationResult);

    Optional<EventValidationResult> findById(
            ValidationResultId validationResultId);

    Optional<EventValidationResult>
            findCurrentByEventIdAndRuleCode(
                    OperationalEventId eventId,
                    ValidationRuleCode ruleCode);

    List<EventValidationResult>
            findAllCurrentByEventIdOrderByRuleCode(
                    OperationalEventId eventId);

    void saveInvalidation(
            EventValidationResult validationResult);

}