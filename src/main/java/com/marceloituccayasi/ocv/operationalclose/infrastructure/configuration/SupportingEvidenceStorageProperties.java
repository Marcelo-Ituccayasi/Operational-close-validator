package com.marceloituccayasi.ocv.operationalclose.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * External configuration for application-managed Supporting Evidence content.
 *
 * <p>The storage path is supplied through
 * {@code OCV_EVIDENCE_STORAGE_PATH}. No production path default is provided.
 *
 * <p>The maximum content size may be reduced through
 * {@code OCV_EVIDENCE_MAX_FILE_SIZE_BYTES}, but it cannot exceed
 * the approved 10 MiB limit.
 *
 * @param path absolute existing storage directory
 * @param maximumContentBytes configured maximum content size
 */
@Validated
@ConfigurationProperties(prefix = "ocv.evidence.storage")
public record SupportingEvidenceStorageProperties(

        @NotBlank(
                message = "OCV_EVIDENCE_STORAGE_PATH must not be blank")
        String path,

        @Min(
                value = 1L,
                message = "OCV_EVIDENCE_MAX_FILE_SIZE_BYTES must be at least 1")
        @Max(
                value = 10_485_760L,
                message = "OCV_EVIDENCE_MAX_FILE_SIZE_BYTES must not exceed 10485760")
        long maximumContentBytes) {

    public SupportingEvidenceStorageProperties {
        if (path != null
                && !path.equals(
                        path.trim())) {

            throw new IllegalArgumentException(
                    "OCV_EVIDENCE_STORAGE_PATH must not contain surrounding whitespace");
        }
    }

}