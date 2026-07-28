package com.marceloituccayasi.ocv.operationalclose.presentation;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.marceloituccayasi.ocv.operationalclose.application.ValidateOperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.application.ValidateOperationalEventCommand;
import com.marceloituccayasi.ocv.operationalclose.application.ValidateOperationalEventResult;

/**
 * MVC entry point for Operational Event validation and revalidation.
 */
@Controller
public class OperationalEventValidationPageController {

    private static final String VALIDATED_MESSAGE =
            "El evento fue validado correctamente.";

    private static final String VALIDATION_FAILED_MESSAGE =
            "La validación finalizó con reglas fallidas. "
                    + "Revisa el estado del evento y sus alertas.";

    private final ValidateOperationalEvent
            validateOperationalEvent;

    public OperationalEventValidationPageController(
            ValidateOperationalEvent validateOperationalEvent) {

        this.validateOperationalEvent =
                Objects.requireNonNull(
                        validateOperationalEvent);
    }

    @PostMapping(
            "/closes/{closeId}/events/{eventId}/validate")
    ModelAndView validate(
            @PathVariable String closeId,
            @PathVariable String eventId,
            RedirectAttributes redirectAttributes) {

        ParsedIdentifiers identifiers =
                parseIdentifiers(
                        closeId,
                        eventId);

        if (identifiers.error() != null) {
            return identifiers.error();
        }

        ValidateOperationalEventResult result =
                validateOperationalEvent.execute(
                        new ValidateOperationalEventCommand(
                                identifiers.closeId(),
                                identifiers.eventId()));

        return switch (result.status()) {
            case VALIDATED ->
                    validationRedirect(
                            identifiers.closeId(),
                            identifiers.eventId(),
                            redirectAttributes,
                            VALIDATED_MESSAGE,
                            true);

            case VALIDATION_FAILED ->
                    validationRedirect(
                            identifiers.closeId(),
                            identifiers.eventId(),
                            redirectAttributes,
                            VALIDATION_FAILED_MESSAGE,
                            false);

            case INVALID_INPUT ->
                    statusError(
                            HttpStatus.BAD_REQUEST,
                            "Solicitud inválida",
                            "Los identificadores de validación no son válidos.");

            case ACTOR_REJECTED ->
                    statusError(
                            HttpStatus.FORBIDDEN,
                            "Operación no autorizada",
                            "No tienes autorización para validar este evento.");

            case CLOSE_NOT_FOUND ->
                    closeNotFound();

            case CLOSE_NOT_EDITABLE ->
                    closeNotEditable();

            case EVENT_NOT_FOUND ->
                    eventNotFound();
        };
    }

    private static ParsedIdentifiers parseIdentifiers(
            String closeId,
            String eventId) {

        UUID parsedCloseId =
                parseUuid(
                        closeId);

        if (parsedCloseId == null) {
            return new ParsedIdentifiers(
                    null,
                    null,
                    invalidIdentifier(
                            "El identificador del cierre no es válido."));
        }

        UUID parsedEventId =
                parseUuid(
                        eventId);

        if (parsedEventId == null) {
            return new ParsedIdentifiers(
                    null,
                    null,
                    invalidIdentifier(
                            "El identificador del evento no es válido."));
        }

        return new ParsedIdentifiers(
                parsedCloseId,
                parsedEventId,
                null);
    }

    private static UUID parseUuid(
            String value) {

        try {
            return UUID.fromString(
                    value);
        }
        catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static ModelAndView validationRedirect(
            UUID closeId,
            UUID eventId,
            RedirectAttributes redirectAttributes,
            String message,
            boolean successful) {

        redirectAttributes.addFlashAttribute(
                "validationMessage",
                message);

        redirectAttributes.addFlashAttribute(
                "validationSuccessful",
                successful);

        return eventDetailRedirect(
                closeId,
                eventId);
    }

    private static ModelAndView eventDetailRedirect(
            UUID closeId,
            UUID eventId) {

        ModelAndView modelAndView =
                new ModelAndView(
                        "redirect:/closes/"
                                + closeId
                                + "/events/"
                                + eventId);

        modelAndView.setStatus(
                HttpStatus.SEE_OTHER);

        return modelAndView;
    }

    private static ModelAndView invalidIdentifier(
            String message) {

        return statusError(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                message);
    }

    private static ModelAndView closeNotFound() {
        return statusError(
                HttpStatus.NOT_FOUND,
                "Cierre no encontrado",
                "El cierre solicitado no existe.");
    }

    private static ModelAndView closeNotEditable() {
        return statusError(
                HttpStatus.CONFLICT,
                "Cierre no modificable",
                "El cierre ya no permite validar eventos.");
    }

    private static ModelAndView eventNotFound() {
        return statusError(
                HttpStatus.NOT_FOUND,
                "Evento no encontrado",
                "El evento solicitado no existe dentro de este cierre.");
    }

    private static ModelAndView statusError(
            HttpStatus status,
            String title,
            String message) {

        ModelAndView modelAndView =
                new ModelAndView(
                        "errors/status");

        modelAndView.setStatus(
                status);

        modelAndView.addObject(
                "statusCode",
                status.value());

        modelAndView.addObject(
                "title",
                title);

        modelAndView.addObject(
                "message",
                message);

        return modelAndView;
    }

    private record ParsedIdentifiers(
            UUID closeId,
            UUID eventId,
            ModelAndView error) {
    }

}