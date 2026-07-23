package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.UUID;

/**
 * Explicit application result for Supporting Evidence deactivation.
 *
 * @param status operation status
 * @param evidenceId deactivated identifier when successful
 * @param message safe result description when applicable
 */
public record DeactivateSupportingEvidenceResult(
        Status status,
        UUID evidenceId,
        String message) {

    public enum Status {
        DEACTIVATED,
        INVALID_INPUT,
        ACTOR_REJECTED,
        CLOSE_NOT_FOUND,
        CLOSE_NOT_EDITABLE,
        EVENT_NOT_FOUND,
        EVIDENCE_NOT_FOUND,
        EVIDENCE_ALREADY_INACTIVE
    }

    public DeactivateSupportingEvidenceResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        if (status == Status.DEACTIVATED
                && evidenceId == null) {

            throw new IllegalArgumentException(
                    "deactivated result must contain evidenceId");
        }

        if (status != Status.DEACTIVATED
                && evidenceId != null) {

            throw new IllegalArgumentException(
                    "non-deactivated result must not contain evidenceId");
        }

        if (message != null
                && message.isBlank()) {

            throw new IllegalArgumentException(
                    "message must not be blank");
        }
    }

    public static DeactivateSupportingEvidenceResult deactivated(
            UUID evidenceId) {

        return new DeactivateSupportingEvidenceResult(
                Status.DEACTIVATED,
                Objects.requireNonNull(
                        evidenceId),
                null);
    }

    public static DeactivateSupportingEvidenceResult invalidInput(
            String message) {

        return new DeactivateSupportingEvidenceResult(
                Status.INVALID_INPUT,
                null,
                Objects.requireNonNull(
                        message));
    }

    public static DeactivateSupportingEvidenceResult actorRejected() {
        return new DeactivateSupportingEvidenceResult(
                Status.ACTOR_REJECTED,
                null,
                "The authenticated actor cannot perform this operation.");
    }

    public static DeactivateSupportingEvidenceResult closeNotFound() {
        return new DeactivateSupportingEvidenceResult(
                Status.CLOSE_NOT_FOUND,
                null,
                "The requested Operational Close does not exist.");
    }

    public static DeactivateSupportingEvidenceResult closeNotEditable() {
        return new DeactivateSupportingEvidenceResult(
                Status.CLOSE_NOT_EDITABLE,
                null,
                "The Operational Close does not allow evidence deactivation.");
    }

    public static DeactivateSupportingEvidenceResult eventNotFound() {
        return new DeactivateSupportingEvidenceResult(
                Status.EVENT_NOT_FOUND,
                null,
                "The requested Operational Event does not exist.");
    }

    public static DeactivateSupportingEvidenceResult evidenceNotFound() {
        return new DeactivateSupportingEvidenceResult(
                Status.EVIDENCE_NOT_FOUND,
                null,
                "The requested Supporting Evidence does not exist.");
    }

    public static DeactivateSupportingEvidenceResult
            evidenceAlreadyInactive() {

        return new DeactivateSupportingEvidenceResult(
                Status.EVIDENCE_ALREADY_INACTIVE,
                null,
                "The requested Supporting Evidence is already inactive.");
    }

}