package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;

/**
 * Verified Supporting Evidence content recovered from managed storage.
 *
 * @param reference validated stored reference
 * @param content verified binary content
 */
public record StoredSupportingEvidenceContent(
        SupportingEvidenceStorageReference reference,
        byte[] content) {

    public StoredSupportingEvidenceContent {
        Objects.requireNonNull(
                reference,
                "reference must not be null");

        Objects.requireNonNull(
                content,
                "content must not be null");

        if (content.length == 0) {
            throw new IllegalArgumentException(
                    "stored evidence content must not be empty");
        }

        content =
                content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public int size() {
        return content.length;
    }

    public String mediaType() {
        return reference.mediaType();
    }

    public String extension() {
        return reference.extension();
    }

}