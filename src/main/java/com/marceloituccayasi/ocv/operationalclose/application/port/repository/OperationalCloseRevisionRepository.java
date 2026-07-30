package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttemptId;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;

/**
 * Persistence contract for revisions of an already locked Operational Close.
 */
public interface OperationalCloseRevisionRepository {

    void saveRevision(
            OperationalClose operationalClose);

    void appendStateTransition(
            CloseStateTransition stateTransition);

    void appendConsolidationStateTransition(
            CloseStateTransition stateTransition,
            ConsolidationId consolidationId);

    void appendSubmissionStateTransition(
            CloseStateTransition stateTransition,
            ValidationResultId validationResultId,
            AccountingSubmissionAttemptId submissionAttemptId);

    void appendSubmissionStateTransition(
            CloseStateTransition stateTransition,
            ValidationResultId validationResultId,
            ConsolidationId consolidationId,
            AccountingSubmissionAttemptId submissionAttemptId);

}