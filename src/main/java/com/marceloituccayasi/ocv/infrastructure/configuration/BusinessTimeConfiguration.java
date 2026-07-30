package com.marceloituccayasi.ocv.infrastructure.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marceloituccayasi.ocv.presentation.BusinessDateTimeFormatter;

/**
 * Registers business time-zone presentation services.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        BusinessTimeZoneProperties.class)
public class BusinessTimeConfiguration {

    @Bean
    BusinessDateTimeFormatter businessDateTimeFormatter(
            BusinessTimeZoneProperties properties) {

        return new BusinessDateTimeFormatter(
                properties.timeZone());
    }

}