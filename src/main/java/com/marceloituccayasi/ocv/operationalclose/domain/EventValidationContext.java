package com.marceloituccayasi.ocv.operationalclose.domain;

import java.util.List;

public record EventValidationContext(
        OperationalEvent event,
        List<SupportingEvidence> supportingEvidence,
        List<EventAuthorization> authorizations) {

    public EventValidationContext {
        if (event == null) {
            throw new IllegalArgumentException(
                    "event must not be null");
        }

        supportingEvidence =
                requireList(
                        supportingEvidence,
                        "supportingEvidence");

        authorizations =
                requireList(
                        authorizations,
                        "authorizations");

        supportingEvidence.forEach(
                evidence -> requireSameEvent(
                        event,
                        evidence));

        authorizations.forEach(
                authorization -> requireSameEvent(
                        event,
                        authorization));
    }

    public List<SupportingEvidence>
            activeSupportingEvidence() {

        return supportingEvidence.stream()
                .filter(
                        SupportingEvidence::active)
                .toList();
    }

    public List<EventAuthorization>
            activeAuthorizations() {

        return authorizations.stream()
                .filter(
                        EventAuthorization::active)
                .toList();
    }

    public boolean hasActiveAuthorization() {
        return authorizations.stream()
                .anyMatch(
                        EventAuthorization::active);
    }

    private static <T> List<T> requireList(
            List<T> values,
            String fieldName) {

        if (values == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null");
        }

        for (T value : values) {
            if (value == null) {
                throw new IllegalArgumentException(
                        fieldName
                                + " must not contain null values");
            }
        }

        return List.copyOf(
                values);
    }

    private static void requireSameEvent(
            OperationalEvent event,
            SupportingEvidence evidence) {

        if (!event.id().equals(
                evidence.eventId())) {

            throw new IllegalArgumentException(
                    "supporting evidence must belong to evaluated event");
        }
    }

    private static void requireSameEvent(
            OperationalEvent event,
            EventAuthorization authorization) {

        if (!event.id().equals(
                authorization.eventId())) {

            throw new IllegalArgumentException(
                    "authorization must belong to evaluated event");
        }
    }

}