package com.marceloituccayasi.ocv.identityaccess.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
 * External login rate-limit configuration.
 *
 * @param maxFailures failures allowed inside the configured window
 * @param windowSeconds rolling failure window in seconds
 * @param blockSeconds block duration in seconds
 */
@Validated
@ConfigurationProperties(prefix = "ocv.login")
public record LoginRateLimitProperties(

        @Min(
                value = 1,
                message = "OCV_LOGIN_MAX_FAILURES must be at least 1"
        )
        int maxFailures,

        @Min(
                value = 1,
                message = "OCV_LOGIN_WINDOW_SECONDS must be at least 1"
        )
        long windowSeconds,

        @Min(
                value = 1,
                message = "OCV_LOGIN_BLOCK_SECONDS must be at least 1"
        )
        long blockSeconds) {

}