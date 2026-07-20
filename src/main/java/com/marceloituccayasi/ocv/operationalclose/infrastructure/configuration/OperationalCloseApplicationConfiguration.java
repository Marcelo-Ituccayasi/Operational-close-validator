package com.marceloituccayasi.ocv.operationalclose.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marceloituccayasi.ocv.operationalclose.application.CreateOperationalClose;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalCloseDetail;
import com.marceloituccayasi.ocv.operationalclose.application.ListOperationalCloses;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;

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

}
