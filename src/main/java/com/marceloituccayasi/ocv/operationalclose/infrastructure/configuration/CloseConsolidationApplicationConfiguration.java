package com.marceloituccayasi.ocv.operationalclose.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marceloituccayasi.ocv.operationalclose.application.CloseConsolidationReadinessEvaluator;
import com.marceloituccayasi.ocv.operationalclose.application.CompleteOperationalCloseConsolidation;
import com.marceloituccayasi.ocv.operationalclose.application.EventValidationContextLoader;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.ConsolidationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationAlertRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationEngine;

/**
 * Assembles Operational Close consolidation application components.
 */
@Configuration(proxyBeanMethods = false)
public class CloseConsolidationApplicationConfiguration {

    @Bean
    CloseConsolidationReadinessEvaluator
            closeConsolidationReadinessEvaluator(
                    EventValidationContextLoader contextLoader,
                    EventValidationEngine validationEngine,
                    EventValidationResultRepository
                            validationResultRepository,
                    EventValidationAlertRepository alertRepository) {

        return new CloseConsolidationReadinessEvaluator(
                contextLoader,
                validationEngine,
                validationResultRepository,
                alertRepository);
    }

    @Bean
    CompleteOperationalCloseConsolidation
            completeOperationalCloseConsolidation(
                    OperationalCloseLockRepository closeLockRepository,
                    OperationalCloseRevisionRepository
                            closeRevisionRepository,
                    OperationalEventRepository eventRepository,
                    ConsolidationRepository consolidationRepository,
                    CloseConsolidationReadinessEvaluator
                            readinessEvaluator,
                    CurrentActorProvider currentActorProvider,
                    ApplicationClock applicationClock,
                    UuidGenerator uuidGenerator,
                    TransactionRunner transactionRunner) {

        return new CompleteOperationalCloseConsolidation(
                closeLockRepository,
                closeRevisionRepository,
                eventRepository,
                consolidationRepository,
                readinessEvaluator,
                currentActorProvider,
                applicationClock,
                uuidGenerator,
                transactionRunner);
    }

}