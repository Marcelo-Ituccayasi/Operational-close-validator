package com.marceloituccayasi.ocv.operationalclose.presentation.form;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.CreateOperationalEventCommand;
import com.marceloituccayasi.ocv.operationalclose.application.OperationalEventView;
import com.marceloituccayasi.ocv.operationalclose.application.UpdateOperationalEventCommand;

/**
 * Mutable web form used to bind Operational Event creation and revision fields.
 */
public final class OperationalEventForm {

    private String eventType;
    private String amount;
    private String reversedEventId;
    private String occurredAt;
    private String responsibleName;
    private String description;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(
            String eventType) {

        this.eventType =
                eventType;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(
            String amount) {

        this.amount =
                amount;
    }

    public String getReversedEventId() {
        return reversedEventId;
    }

    public void setReversedEventId(
            String reversedEventId) {

        this.reversedEventId =
                reversedEventId;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(
            String occurredAt) {

        this.occurredAt =
                occurredAt;
    }

    public String getResponsibleName() {
        return responsibleName;
    }

    public void setResponsibleName(
            String responsibleName) {

        this.responsibleName =
                responsibleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(
            String description) {

        this.description =
                description;
    }

    public CreateOperationalEventCommand toCreateCommand(
            UUID closeId) {

        ParsedFields fields =
                parseFields();

        return new CreateOperationalEventCommand(
                requireIdentifier(
                        closeId,
                        "El identificador del cierre es obligatorio."),
                fields.eventType(),
                fields.amount(),
                fields.reversedEventId(),
                fields.occurredAt(),
                fields.responsibleName(),
                fields.description());
    }

    public UpdateOperationalEventCommand toUpdateCommand(
            UUID closeId,
            UUID eventId) {

        ParsedFields fields =
                parseFields();

        return new UpdateOperationalEventCommand(
                requireIdentifier(
                        closeId,
                        "El identificador del cierre es obligatorio."),
                requireIdentifier(
                        eventId,
                        "El identificador del evento es obligatorio."),
                fields.eventType(),
                fields.amount(),
                fields.reversedEventId(),
                fields.occurredAt(),
                fields.responsibleName(),
                fields.description());
    }

    public static OperationalEventForm fromView(
            OperationalEventView operationalEvent) {

        Objects.requireNonNull(
                operationalEvent,
                "operationalEvent must not be null");

        OperationalEventForm form =
                new OperationalEventForm();

        form.setEventType(
                operationalEvent.eventType());

        form.setAmount(
                operationalEvent.amount()
                        .toPlainString());

        form.setReversedEventId(
                operationalEvent.reversedEventId() == null
                        ? ""
                        : operationalEvent
                                .reversedEventId()
                                .toString());

        form.setOccurredAt(
                operationalEvent.occurredAt()
                        .toString());

        form.setResponsibleName(
                operationalEvent.responsibleName());

        form.setDescription(
                operationalEvent.description());

        return form;
    }

    private ParsedFields parseFields() {
        try {
            return new ParsedFields(
                    requiredValue(
                            eventType),
                    new BigDecimal(
                            requiredValue(
                                    amount)),
                    optionalUuid(
                            reversedEventId),
                    Instant.parse(
                            requiredValue(
                                    occurredAt)),
                    requiredValue(
                            responsibleName),
                    requiredValue(
                            description));
        }
        catch (DateTimeParseException
                | NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "Los datos ingresados no tienen el formato esperado.",
                    exception);
        }
    }

    private static UUID optionalUuid(
            String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return UUID.fromString(
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

    private record ParsedFields(
            String eventType,
            BigDecimal amount,
            UUID reversedEventId,
            Instant occurredAt,
            String responsibleName,
            String description) {
    }

}