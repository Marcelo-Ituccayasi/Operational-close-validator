package com.marceloituccayasi.ocv.operationalclose.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

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

class EventValidationApplicationConfigurationTest {

    @Test
    void assemblesCompleteEventValidationGraph() {
        try (
                AnnotationConfigApplicationContext context =
                        new AnnotationConfigApplicationContext()
        ) {
            registerMock(
                    context,
                    "operationalEventRepository",
                    OperationalEventRepository.class);

            registerMock(
                    context,
                    "supportingEvidenceRepository",
                    SupportingEvidenceRepository.class);

            registerMock(
                    context,
                    "eventAuthorizationRepository",
                    EventAuthorizationRepository.class);

            registerMock(
                    context,
                    "eventValidationAlertRepository",
                    EventValidationAlertRepository.class);

            registerMock(
                    context,
                    "eventValidationResultRepository",
                    EventValidationResultRepository.class);

            registerMock(
                    context,
                    "operationalCloseLockRepository",
                    OperationalCloseLockRepository.class);

            registerMock(
                    context,
                    "operationalEventRevisionRepository",
                    OperationalEventRevisionRepository.class);

            registerMock(
                    context,
                    "currentActorProvider",
                    CurrentActorProvider.class);

            registerMock(
                    context,
                    "applicationClock",
                    ApplicationClock.class);

            registerMock(
                    context,
                    "uuidGenerator",
                    UuidGenerator.class);

            registerMock(
                    context,
                    "transactionRunner",
                    TransactionRunner.class);

            context.register(
                    EventValidationApplicationConfiguration.class);

            context.refresh();

            assertThat(
                    context.getBean(
                            EventValidationContextLoader.class))
                    .isNotNull();

            assertThat(
                    context.getBean(
                            EventValidationEngine.class))
                    .isNotNull();

            assertThat(
                    context.getBean(
                            EventValidationResultFactory.class))
                    .isNotNull();

            assertThat(
                    context.getBean(
                            EventValidationAlertSynchronizer.class))
                    .isNotNull();

            assertThat(
                    context.getBean(
                            EventValidationStateResolver.class))
                    .isNotNull();

            assertThat(
                    context.getBean(
                            ValidateOperationalEvent.class))
                    .isNotNull();
        }
    }

    private static <T> void registerMock(
            AnnotationConfigApplicationContext context,
            String beanName,
            Class<T> type) {

        context.getBeanFactory()
                .registerSingleton(
                        beanName,
                        mock(
                                type));
    }

}