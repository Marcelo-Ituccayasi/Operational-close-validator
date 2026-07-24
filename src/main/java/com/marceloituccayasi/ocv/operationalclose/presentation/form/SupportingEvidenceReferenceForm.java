package com.marceloituccayasi.ocv.operationalclose.presentation.form;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.CreateSupportingEvidenceCommand;

/**
 * Mutable web form for Supporting Evidence backed by an opaque business
 * reference.
 *
 * <p>The submitted business reference is converted into the canonical
 * {@code reference:} representation expected by Application.
 */
public final class SupportingEvidenceReferenceForm {

    private static final String REFERENCE_PREFIX =
            "reference:";

    private String evidenceType;

    private String contentReference;

    private String supportedAmount;

    private String evidenceDate;

    private String legibilityStatus;

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(
            String evidenceType) {

        this.evidenceType =
                evidenceType;
    }

    public String getContentReference() {
        return contentReference;
    }

    public void setContentReference(
            String contentReference) {

        this.contentReference =
                contentReference;
    }

    public String getSupportedAmount() {
        return supportedAmount;
    }

    public void setSupportedAmount(
            String supportedAmount) {

        this.supportedAmount =
                supportedAmount;
    }

    public String getEvidenceDate() {
        return evidenceDate;
    }

    public void setEvidenceDate(
            String evidenceDate) {

        this.evidenceDate =
                evidenceDate;
    }

    public String getLegibilityStatus() {
        return legibilityStatus;
    }

    public void setLegibilityStatus(
            String legibilityStatus) {

        this.legibilityStatus =
                legibilityStatus;
    }

    public CreateSupportingEvidenceCommand toCreateCommand(
            UUID closeId,
            UUID eventId) {

        try {
            return new CreateSupportingEvidenceCommand(
                    requireIdentifier(
                            closeId,
                            "El identificador del cierre es obligatorio."),
                    requireIdentifier(
                            eventId,
                            "El identificador del evento es obligatorio."),
                    requiredValue(
                            evidenceType),
                    REFERENCE_PREFIX
                            + requiredValue(
                                    contentReference),
                    optionalAmount(
                            supportedAmount),
                    LocalDate.parse(
                            requiredValue(
                                    evidenceDate)),
                    requiredValue(
                            legibilityStatus));
        }
        catch (DateTimeParseException
                | NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "Los datos ingresados no tienen el formato esperado.",
                    exception);
        }
    }

    private static BigDecimal optionalAmount(
            String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return new BigDecimal(
                value.trim());
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