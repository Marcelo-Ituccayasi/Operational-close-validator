package com.marceloituccayasi.ocv.operationalclose.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marceloituccayasi.ocv.operationalclose.application.CloseConsolidationReadinessEvaluator;
import com.marceloituccayasi.ocv.operationalclose.application.SubmitOperationalCloseToAccounting;
import com.marceloituccayasi.ocv.operationalclose.application.Vr008Evaluator;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.AccountingSubmissionAttemptRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.CloseValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.ConsolidationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationAlertRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;

/**
 * Assembles internal accounting submission application components.
 */
@Configuration(proxyBeanMethods = false)
public class AccountingSubmissionApplicationConfiguration {

    @Bean
    Vr008Evaluator vr008Evaluator() {
        return new Vr008Evaluator();
    }

    @Bean
    SubmitOperationalCloseToAccounting
            submitOperationalCloseToAccounting(
                    OperationalCloseLockRepository closeLockRepository,
                    OperationalCloseRevisionRepository
                            closeRevisionRepository,
                    OperationalEventRepository eventRepository,
                    EventValidationResultRepository
                            eventValidationResultRepository,
                    EventValidationAlertRepository
                            eventValidationAlertRepository,
                    ConsolidationRepository consolidationRepository,
                    CloseValidationResultRepository
                            closeValidationResultRepository,
                    AccountingSubmissionAttemptRepository
                            submissionAttemptRepository,
                    CloseConsolidationReadinessEvaluator
                            readinessEvaluator,
                    Vr008Evaluator vr008Evaluator,
                    CurrentActorProvider currentActorProvider,
                    ApplicationClock applicationClock,
                    UuidGenerator uuidGenerator,
                    TransactionRunner transactionRunner) {

        return new SubmitOperationalCloseToAccounting(
                closeLockRepository,
                closeRevisionRepository,
                eventRepository,
                eventValidationResultRepository,
                eventValidationAlertRepository,
                consolidationRepository,
                closeValidationResultRepository,
                submissionAttemptRepository,
                readinessEvaluator,
                vr008Evaluator,
                currentActorProvider,
                applicationClock,
                uuidGenerator,
                transactionRunner);
    }

}