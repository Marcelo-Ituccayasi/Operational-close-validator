package com.marceloituccayasi.ocv.operationalclose.infrastructure.configuration;

import java.nio.file.Path;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marceloituccayasi.ocv.operationalclose.application.port.SupportingEvidenceContentStorage;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.storage.LocalSupportingEvidenceContentStorage;

/**
 * Registers application-managed Supporting Evidence storage.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        SupportingEvidenceStorageProperties.class)
public class SupportingEvidenceStorageConfiguration {

    @Bean
    SupportingEvidenceContentStorage supportingEvidenceContentStorage(
            SupportingEvidenceStorageProperties properties) {

        return new LocalSupportingEvidenceContentStorage(
                Path.of(
                        properties.path()),
                properties.maximumContentBytes());
    }

}