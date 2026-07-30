package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.domain.CloseValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

/**
 * Persistence contract for Close-scoped Validation Results.
 *
 * <p>Mutation use cases must lock the owning Operational Close before
 * changing current Results.</p>
 */
public interface CloseValidationResultRepository {

    void saveNew(
            CloseValidationResult validationResult);

    Optional<CloseValidationResult> findById(
            ValidationResultId validationResultId);

    Optional<CloseValidationResult>
            findCurrentByCloseIdAndRuleCode(
                    OperationalCloseId closeId,
                    ValidationRuleCode ruleCode);

    void saveInvalidation(
            CloseValidationResult validationResult);

}