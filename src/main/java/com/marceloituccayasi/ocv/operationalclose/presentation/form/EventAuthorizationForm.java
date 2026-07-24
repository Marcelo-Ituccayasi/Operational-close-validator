package com.marceloituccayasi.ocv.operationalclose.presentation.form;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.CreateEventAuthorizationCommand;

/**
 * Mutable web form used to register an Event Authorization.
 *
 * <p>The formal reference is preserved as opaque business text. Presentation
 * does not interpret it as a URL, filesystem path or external resource.
 */
public final class EventAuthorizationForm {

    private String authorizedByName;

    private String reason;

    private String authorizedAt;

    private String formalReference;

    public String getAuthorizedByName() {
        return authorizedByName;
    }

    public void setAuthorizedByName(
            String authorizedByName) {

        this.authorizedByName =
                authorizedByName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(
            String reason) {

        this.reason =
                reason;
    }

    public String getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(
            String authorizedAt) {

        this.authorizedAt =
                authorizedAt;
    }

    public String getFormalReference() {
        return formalReference;
    }

    public void setFormalReference(
            String formalReference) {

        this.formalReference =
                formalReference;
    }

    public CreateEventAuthorizationCommand toCreateCommand(
            UUID closeId,
            UUID eventId) {

        try {
            return new CreateEventAuthorizationCommand(
                    requireIdentifier(
                            closeId,
                            "El identificador del cierre es obligatorio."),
                    requireIdentifier(
                            eventId,
                            "El identificador del evento es obligatorio."),
                    requiredValue(
                            authorizedByName),
                    requiredValue(
                            reason),
                    Instant.parse(
                            requiredValue(
                                    authorizedAt)),
                    requiredValue(
                            formalReference));
        }
        catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Los datos ingresados no tienen el formato esperado.",
                    exception);
        }
    }

    private static UUID requireIdentifier(
            UUID value,
            String message) {

        if (value == null) {
            throw new IllegalArgumentException(
                    message);
        }

        return value;
    }

    private static String requiredValue(
            String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Todos los campos obligatorios deben completarse.");
        }

        return value.trim();
    }

}