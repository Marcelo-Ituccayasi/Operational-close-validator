package com.marceloituccayasi.ocv.operationalclose.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marceloituccayasi.ocv.operationalclose.application.CreateSupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.application.CreateSupportingEvidenceWithStoredContent;
import com.marceloituccayasi.ocv.operationalclose.application.GetSupportingEvidenceContent;
import com.marceloituccayasi.ocv.operationalclose.application.port.SupportingEvidenceContentStorage;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.SupportingEvidenceRepository;

/**
 * Registers application orchestration for managed Supporting Evidence content.
 */
@Configuration(proxyBeanMethods = false)
public class SupportingEvidenceContentApplicationConfiguration {

    @Bean
    CreateSupportingEvidenceWithStoredContent
            createSupportingEvidenceWithStoredContent(
                    UuidGenerator uuidGenerator,
                    SupportingEvidenceContentStorage contentStorage,
                    CreateSupportingEvidence createSupportingEvidence) {

        return new CreateSupportingEvidenceWithStoredContent(
                uuidGenerator,
                contentStorage,
                createSupportingEvidence);
    }

    @Bean
    GetSupportingEvidenceContent getSupportingEvidenceContent(
            SupportingEvidenceRepository evidenceRepository,
            SupportingEvidenceContentStorage contentStorage,
            TransactionRunner transactionRunner) {

        return new GetSupportingEvidenceContent(
                evidenceRepository,
                contentStorage,
                transactionRunner);
    }

}