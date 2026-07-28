package com.marceloituccayasi.ocv.operationalclose.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marceloituccayasi.ocv.operationalclose.application.EventValidationAlertSynchronizer;
import com.marceloituccayasi.ocv.operationalclose.application.EventValidationContextLoader;
import com.marceloituccayasi.ocv.operationalclose.application.EventValidationResultFactory;
import com.marceloituccayasi.ocv.operationalclose.application.ValidateOperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventAuthorizationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationAlertRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.SupportingEvidenceRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationEngine;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationStateResolver;

/**
 * Assembles Operational Event Validation application components.
 */
@Configuration(proxyBeanMethods = false)
public class EventValidationApplicationConfiguration {

    @Bean
    EventValidationContextLoader eventValidationContextLoader(
            OperationalEventRepository eventRepository,
            SupportingEvidenceRepository evidenceRepository,
            EventAuthorizationRepository authorizationRepository) {

        return new EventValidationContextLoader(
                eventRepository,
                evidenceRepository,
                authorizationRepository);
    }

    @Bean
    EventValidationEngine eventValidationEngine() {
        return new EventValidationEngine();
    }

    @Bean
    EventValidationResultFactory eventValidationResultFactory(
            UuidGenerator uuidGenerator) {

        return new EventValidationResultFactory(
                uuidGenerator);
    }

    @Bean
    EventValidationAlertSynchronizer
            eventValidationAlertSynchronizer(
                    EventValidationAlertRepository
                            alertRepository,
                    UuidGenerator uuidGenerator) {

        return new EventValidationAlertSynchronizer(
                alertRepository,
                uuidGenerator);
    }

    @Bean
    EventValidationStateResolver eventValidationStateResolver() {
        return new EventValidationStateResolver();
    }

    @Bean
    ValidateOperationalEvent validateOperationalEvent(
            OperationalCloseLockRepository closeLockRepository,
            OperationalEventRevisionRepository eventRevisionRepository,
            EventValidationResultRepository validationResultRepository,
            CurrentActorProvider currentActorProvider,
            ApplicationClock applicationClock,
            UuidGenerator uuidGenerator,
            TransactionRunner transactionRunner,
            EventValidationContextLoader contextLoader,
            EventValidationEngine validationEngine,
            EventValidationResultFactory resultFactory,
            EventValidationAlertSynchronizer alertSynchronizer,
            EventValidationStateResolver stateResolver) {

        return new ValidateOperationalEvent(
                closeLockRepository,
                eventRevisionRepository,
                validationResultRepository,
                currentActorProvider,
                applicationClock,
                uuidGenerator,
                transactionRunner,
                contextLoader,
                validationEngine,
                resultFactory,
                alertSynchronizer,
                stateResolver);
    }

}