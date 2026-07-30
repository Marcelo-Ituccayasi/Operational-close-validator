package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import java.util.List;
import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttempt;
import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttemptId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;

/**
 * Persistence contract for immutable accounting submission attempts.
 *
 * <p>Mutation use cases must lock the owning Operational Close before
 * persisting an attempt.</p>
 */
public interface AccountingSubmissionAttemptRepository {

    void saveNew(
            AccountingSubmissionAttempt attempt);

    Optional<AccountingSubmissionAttempt> findById(
            AccountingSubmissionAttemptId attemptId);

    Optional<AccountingSubmissionAttempt> findLatestByCloseId(
            OperationalCloseId closeId);

    List<AccountingSubmissionAttempt>
            findAllByCloseIdOrderByAttemptedAt(
                    OperationalCloseId closeId);

}