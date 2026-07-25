package com.marceloituccayasi.ocv.operationalclose.presentation;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import com.marceloituccayasi.ocv.operationalclose.application.DeactivateEventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.application.DeactivateEventAuthorizationCommand;
import com.marceloituccayasi.ocv.operationalclose.application.DeactivateEventAuthorizationResult;

/**
 * MVC entry point for Event Authorization deactivation.
 */
@Controller
public class EventAuthorizationDeactivationPageController {

    private final DeactivateEventAuthorization
            deactivateEventAuthorization;

    public EventAuthorizationDeactivationPageController(
            DeactivateEventAuthorization deactivateEventAuthorization) {

        this.deactivateEventAuthorization =
                Objects.requireNonNull(
                        deactivateEventAuthorization);
    }

    @PostMapping(
            "/closes/{closeId}/events/{eventId}"
                    + "/authorizations/{authorizationId}/deactivate")
    ModelAndView deactivate(
            @PathVariable String closeId,
            @PathVariable String eventId,
            @PathVariable String authorizationId) {

        ParsedIdentifiers identifiers =
                parseIdentifiers(
                        closeId,
                        eventId,
                        authorizationId);

        if (identifiers.error() != null) {
            return identifiers.error();
        }

        DeactivateEventAuthorizationResult result =
                deactivateEventAuthorization.execute(
                        new DeactivateEventAuthorizationCommand(
                                identifiers.closeId(),
                                identifiers.eventId(),
                                identifiers.authorizationId()));

        return switch (result.status()) {
            case DEACTIVATED ->
                    eventDetailRedirect(
                            identifiers.closeId(),
                            identifiers.eventId());

            case INVALID_INPUT ->
                    statusError(
                            HttpStatus.BAD_REQUEST,
                            "Solicitud inválida",
                            "Los datos de la autorización no son válidos.");

            case ACTOR_REJECTED ->
                    statusError(
                            HttpStatus.FORBIDDEN,
                            "Operación no autorizada",
                            "No tienes autorización para desactivar este registro.");

            case CLOSE_NOT_FOUND ->
                    closeNotFound();

            case CLOSE_NOT_EDITABLE ->
                    closeNotEditable();

            case EVENT_NOT_FOUND ->
                    eventNotFound();

            case AUTHORIZATION_NOT_FOUND ->
                    authorizationNotFound();

            case AUTHORIZATION_ALREADY_INACTIVE ->
                    authorizationAlreadyInactive();
        };
    }

    private static ParsedIdentifiers parseIdentifiers(
            String closeId,
            String eventId,
            String authorizationId) {

        UUID parsedCloseId =
                parseUuid(
                        closeId);

        if (parsedCloseId == null) {
            return new ParsedIdentifiers(
                    null,
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
                    null,
                    invalidIdentifier(
                            "El identificador del evento no es válido."));
        }

        UUID parsedAuthorizationId =
                parseUuid(
                        authorizationId);

        if (parsedAuthorizationId == null) {
            return new ParsedIdentifiers(
                    null,
                    null,
                    null,
                    invalidIdentifier(
                            "El identificador de la autorización no es válido."));
        }

        return new ParsedIdentifiers(
                parsedCloseId,
                parsedEventId,
                parsedAuthorizationId,
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
                "El cierre ya no permite desactivar autorizaciones.");
    }

    private static ModelAndView eventNotFound() {
        return statusError(
                HttpStatus.NOT_FOUND,
                "Evento no encontrado",
                "El evento solicitado no existe dentro de este cierre.");
    }

    private static ModelAndView authorizationNotFound() {
        return statusError(
                HttpStatus.NOT_FOUND,
                "Autorización no encontrada",
                "La autorización solicitada no existe dentro de este evento.");
    }

    private static ModelAndView authorizationAlreadyInactive() {
        return statusError(
                HttpStatus.CONFLICT,
                "Autorización inactiva",
                "La autorización solicitada ya se encuentra inactiva.");
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
            UUID authorizationId,
            ModelAndView error) {
    }

}