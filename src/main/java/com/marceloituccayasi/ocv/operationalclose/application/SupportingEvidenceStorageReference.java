package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;

/**
 * Validated internal reference for Supporting Evidence content managed by the
 * application.
 *
 * <p>The persisted representation is:
 *
 * <pre>
 * stored:evidence/{evidenceId}/{sha256}.{extension}
 * </pre>
 */
public record SupportingEvidenceStorageReference(
        SupportingEvidenceId evidenceId,
        String sha256,
        String extension) {

    private static final String PREFIX =
            "stored:evidence/";

    private static final Pattern SHA_256_PATTERN =
            Pattern.compile(
                    "[0-9a-f]{64}");

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(
                    "pdf",
                    "png",
                    "jpg",
                    "jpeg");

    public SupportingEvidenceStorageReference {
        if (evidenceId == null) {
            throw new IllegalArgumentException(
                    "evidenceId must not be null");
        }

        sha256 =
                requireUnmodifiedText(
                        sha256,
                        "sha256");

        extension =
                requireUnmodifiedText(
                        extension,
                        "extension");

        if (!SHA_256_PATTERN.matcher(
                sha256)
                .matches()) {

            throw new IllegalArgumentException(
                    "sha256 must contain exactly 64 lowercase hexadecimal characters");
        }

        if (!ALLOWED_EXTENSIONS.contains(
                extension)) {

            throw new IllegalArgumentException(
                    "stored evidence extension is not allowed");
        }
    }

    public static SupportingEvidenceStorageReference create(
            SupportingEvidenceId evidenceId,
            String sha256,
            String extension) {

        return new SupportingEvidenceStorageReference(
                evidenceId,
                sha256,
                extension);
    }

    public static SupportingEvidenceStorageReference parse(
            String contentReference) {

        String value =
                requireUnmodifiedText(
                        contentReference,
                        "contentReference");

        if (!value.startsWith(
                PREFIX)) {

            throw invalidReference();
        }

        String relativeReference =
                value.substring(
                        PREFIX.length());

        int separatorIndex =
                relativeReference.indexOf('/');

        if (separatorIndex < 1
                || separatorIndex
                        != relativeReference.lastIndexOf('/')) {

            throw invalidReference();
        }

        String evidenceIdText =
                relativeReference.substring(
                        0,
                        separatorIndex);

        String storedFileName =
                relativeReference.substring(
                        separatorIndex + 1);

        int extensionSeparatorIndex =
                storedFileName.lastIndexOf('.');

        if (extensionSeparatorIndex < 1
                || extensionSeparatorIndex
                        == storedFileName.length() - 1) {

            throw invalidReference();
        }

        String digest =
                storedFileName.substring(
                        0,
                        extensionSeparatorIndex);

        String storedExtension =
                storedFileName.substring(
                        extensionSeparatorIndex + 1);

        UUID evidenceUuid;

        try {
            evidenceUuid =
                    UUID.fromString(
                            evidenceIdText);
        }
        catch (IllegalArgumentException exception) {
            throw invalidReference();
        }

        if (!evidenceUuid.toString()
                .equals(
                        evidenceIdText)) {

            throw invalidReference();
        }

        SupportingEvidenceStorageReference parsedReference;

        try {
            parsedReference =
                    new SupportingEvidenceStorageReference(
                            new SupportingEvidenceId(
                                    evidenceUuid),
                            digest,
                            storedExtension);
        }
        catch (IllegalArgumentException exception) {
            throw invalidReference();
        }

        if (!parsedReference.value()
                .equals(
                        value)) {

            throw invalidReference();
        }

        return parsedReference;
    }

    public String value() {
        return PREFIX
                + evidenceId.value()
                + "/"
                + sha256
                + "."
                + extension;
    }

    public String mediaType() {
        return switch (extension) {
            case "pdf" ->
                "application/pdf";

            case "png" ->
                "image/png";

            case "jpg", "jpeg" ->
                "image/jpeg";

            default ->
                throw new IllegalStateException(
                        "stored evidence extension is not supported");
        };
    }

    private static String requireUnmodifiedText(
            String value,
            String fieldName) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    fieldName + " must not be blank");
        }

        if (!value.equals(
                value.trim())) {

            throw new IllegalArgumentException(
                    fieldName + " must not contain surrounding whitespace");
        }

        return value;
    }

    private static IllegalArgumentException invalidReference() {
        return new IllegalArgumentException(
                "stored evidence content reference is invalid");
    }

}