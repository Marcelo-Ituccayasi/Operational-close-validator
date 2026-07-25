package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.List;
import java.util.Objects;

/**
 * Explicit result for evidence and authorization queries within an
 * Operational Event.
 *
 * @param status query status
 * @param supportingEvidence all current and historical evidence
 * @param authorizations all current and historical authorizations
 */
public record GetOperationalEventSupportingInformationResult(
        Status status,
        List<SupportingEvidenceView> supportingEvidence,
        List<EventAuthorizationView> authorizations) {

    public enum Status {
        FOUND,
        NOT_FOUND
    }

    public GetOperationalEventSupportingInformationResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        supportingEvidence =
                List.copyOf(
                        Objects.requireNonNull(
                                supportingEvidence,
                                "supportingEvidence must not be null"));

        authorizations =
                List.copyOf(
                        Objects.requireNonNull(
                                authorizations,
                                "authorizations must not be null"));

        if (status == Status.NOT_FOUND
                && (!supportingEvidence.isEmpty()
                        || !authorizations.isEmpty())) {

            throw new IllegalArgumentException(
                    "not-found result must not contain supporting information");
        }
    }

    public static GetOperationalEventSupportingInformationResult found(
            List<SupportingEvidenceView> supportingEvidence,
            List<EventAuthorizationView> authorizations) {

        return new GetOperationalEventSupportingInformationResult(
                Status.FOUND,
                supportingEvidence,
                authorizations);
    }

    public static GetOperationalEventSupportingInformationResult
            notFound() {

        return new GetOperationalEventSupportingInformationResult(
                Status.NOT_FOUND,
                List.of(),
                List.of());
    }

}