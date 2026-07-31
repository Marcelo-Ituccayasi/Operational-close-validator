package com.marceloituccayasi.ocv.infrastructure.configuration;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/**
 * External business time-zone configuration.
 *
 * <p>The application preserves instants in UTC and applies this zone only at
 * presentation boundaries.
 *
 * @param timeZone configured business time zone
 */
@Validated
@ConfigurationProperties(prefix = "ocv.business")
public record BusinessTimeZoneProperties(

        @NotNull(
                message = "OCV_BUSINESS_TIME_ZONE must be a valid time-zone identifier"
        )
        ZoneId timeZone) {

}