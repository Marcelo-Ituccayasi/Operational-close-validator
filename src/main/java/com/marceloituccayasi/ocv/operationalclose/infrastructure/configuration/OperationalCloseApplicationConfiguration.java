package com.marceloituccayasi.ocv.operationalclose.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marceloituccayasi.ocv.operationalclose.application.CreateOperationalClose;
import com.marceloituccayasi.ocv.operationalclose.application.CreateOperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalCloseDetail;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalEventDetail;
import com.marceloituccayasi.ocv.operationalclose.application.ListOperationalCloses;
import com.marceloituccayasi.ocv.operationalclose.application.ListOperationalEvents;
import com.marceloituccayasi.ocv.operationalclose.application.UpdateOperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventDependentResultInvalidator;
import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventRequirementPolicy;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;

/**
 * Assembles Operational Close application use cases.
 */
@Configuration(proxyBeanMethods = false)
public class OperationalCloseApplicationConfiguration {

    @Bean
    CreateOperationalClose createOperationalClose(
            OperationalCloseRepository repository,
            CurrentActorProvider currentActorProvider,
            ApplicationClock applicationClock,
            UuidGenerator uuidGenerator,
            TransactionRunner transactionRunner) {

        return new CreateOperationalClose(
                repository,
                currentActorProvider,
                applicationClock,
                uuidGenerator,
                transactionRunner);
    }

    @Bean
    CreateOperationalEvent createOperationalEvent(
            OperationalCloseLockRepository closeLockRepository,
            OperationalEventRepository eventRepository,
            CurrentActorProvider currentActorProvider,
            ApplicationClock applicationClock,
            UuidGenerator uuidGenerator,
            TransactionRunner transactionRunner,
            OperationalEventRequirementPolicy requirementPolicy) {

        return new CreateOperationalEvent(
                closeLockRepository,
                eventRepository,
                currentActorProvider,
                applicationClock,
                uuidGenerator,
                transactionRunner,
                requirementPolicy);
    }

    @Bean
    UpdateOperationalEvent updateOperationalEvent(
            OperationalCloseLockRepository closeLockRepository,
            OperationalEventRevisionRepository eventRevisionRepository,
            CurrentActorProvider currentActorProvider,
            ApplicationClock applicationClock,
            UuidGenerator uuidGenerator,
            TransactionRunner transactionRunner,
            OperationalEventRequirementPolicy requirementPolicy,
            OperationalEventDependentResultInvalidator
                    dependentResultInvalidator) {

        return new UpdateOperationalEvent(
                closeLockRepository,
                eventRevisionRepository,
                currentActorProvider,
                applicationClock,
                uuidGenerator,
                transactionRunner,
                requirementPolicy,
                dependentResultInvalidator);
    }

    @Bean
    GetOperationalCloseDetail getOperationalCloseDetail(
            OperationalCloseRepository repository,
            TransactionRunner transactionRunner) {

        return new GetOperationalCloseDetail(
                repository,
                transactionRunner);
    }

    @Bean
    ListOperationalCloses listOperationalCloses(
            OperationalCloseRepository repository,
            TransactionRunner transactionRunner) {

        return new ListOperationalCloses(
                repository,
                transactionRunner);
    }

    @Bean
    ListOperationalEvents listOperationalEvents(
            OperationalCloseRepository closeRepository,
            OperationalEventRepository eventRepository,
            TransactionRunner transactionRunner) {

        return new ListOperationalEvents(
                closeRepository,
                eventRepository,
                transactionRunner);
    }

    @Bean
    GetOperationalEventDetail getOperationalEventDetail(
            OperationalEventRepository repository,
            TransactionRunner transactionRunner) {

        return new GetOperationalEventDetail(
                repository,
                transactionRunner);
    }

}