package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import java.util.List;
import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
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

    List<EventValidationResult>
            findAllCurrentForInvalidation(
                    OperationalCloseId closeId,
                    List<OperationalEventId> eventIds);

    void saveInvalidation(
            EventValidationResult validationResult);

}