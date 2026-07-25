package com.marceloituccayasi.ocv.operationalclose.presentation;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import com.marceloituccayasi.ocv.operationalclose.application.CreateEventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.application.CreateEventAuthorizationCommand;
import com.marceloituccayasi.ocv.operationalclose.application.CreateEventAuthorizationResult;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalCloseDetail;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalCloseResult;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalEventDetail;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalEventResult;
import com.marceloituccayasi.ocv.operationalclose.presentation.form.EventAuthorizationForm;

/**
 * MVC entry point for Event Authorization registration.
 *
 * <p>Formal references remain opaque business text and are never interpreted
 * as URLs, filesystem paths or external resources.
 */
@Controller
public class EventAuthorizationPageController {

    private static final String SENT_TO_ACCOUNTING =
            "SENT_TO_ACCOUNTING";

    private final GetOperationalCloseDetail
            getOperationalCloseDetail;

    private final GetOperationalEventDetail
            getOperationalEventDetail;

    private final CreateEventAuthorization
            createEventAuthorization;

    public EventAuthorizationPageController(
            GetOperationalCloseDetail getOperationalCloseDetail,
            GetOperationalEventDetail getOperationalEventDetail,
            CreateEventAuthorization createEventAuthorization) {

        this.getOperationalCloseDetail =
                Objects.requireNonNull(
                        getOperationalCloseDetail);

        this.getOperationalEventDetail =
                Objects.requireNonNull(
                        getOperationalEventDetail);

        this.createEventAuthorization =
                Objects.requireNonNull(
                        createEventAuthorization);
    }

    @GetMapping(
            "/closes/{closeId}/events/{eventId}/authorizations/new")
    ModelAndView newForm(
            @PathVariable String closeId,
            @PathVariable String eventId) {

        ParsedIdentifiers identifiers =
                parseIdentifiers(
                        closeId,
                        eventId);

        if (identifiers.error() != null) {
            return identifiers.error();
        }

        GetOperationalCloseResult closeResult =
                getOperationalCloseDetail.execute(
                        identifiers.closeId());

        if (closeResult.status()
                == GetOperationalCloseResult.Status.NOT_FOUND) {

            return closeNotFound();
        }

        String closeState =
                closeResult.operationalClose()
                        .state();

        if (SENT_TO_ACCOUNTING.equals(
                closeState)) {

            return closeNotEditable();
        }

        GetOperationalEventResult eventResult =
                getOperationalEventDetail.execute(
                        identifiers.closeId(),
                        identifiers.eventId());

        if (eventResult.status()
                == GetOperationalEventResult.Status.NOT_FOUND) {

            return eventNotFound();
        }

        return formView(
                identifiers.closeId(),
                identifiers.eventId(),
                closeState,
                new EventAuthorizationForm());
    }

    @PostMapping(
            "/closes/{closeId}/events/{eventId}/authorizations")
    ModelAndView create(
            @PathVariable String closeId,
            @PathVariable String eventId,
            @ModelAttribute("authorizationForm")
            EventAuthorizationForm authorizationForm) {

        ParsedIdentifiers identifiers =
                parseIdentifiers(
                        closeId,
                        eventId);

        if (identifiers.error() != null) {
            return identifiers.error();
        }

        CreateEventAuthorizationCommand command;

        try {
            command =
                    authorizationForm.toCreateCommand(
                            identifiers.closeId(),
                            identifiers.eventId());
        }
        catch (IllegalArgumentException exception) {
            return formError(
                    identifiers.closeId(),
                    identifiers.eventId(),
                    authorizationForm,
                    HttpStatus.BAD_REQUEST,
                    "Los datos ingresados no son válidos.");
        }

        CreateEventAuthorizationResult result =
                createEventAuthorization.execute(
                        command);

        return switch (result.status()) {
            case CREATED ->
                    eventDetailRedirect(
                            identifiers.closeId(),
                            identifiers.eventId());

            case INVALID_INPUT ->
                    formError(
                            identifiers.closeId(),
                            identifiers.eventId(),
                            authorizationForm,
                            HttpStatus.BAD_REQUEST,
                            "Los datos de la autorización no son válidos.");

            case ACTOR_REJECTED ->
                    statusError(
                            HttpStatus.FORBIDDEN,
                            "Operación no autorizada",
                            "No tienes autorización para registrar esta autorización.");

            case CLOSE_NOT_FOUND ->
                    closeNotFound();

            case CLOSE_NOT_EDITABLE ->
                    closeNotEditable();

            case EVENT_NOT_FOUND ->
                    eventNotFound();
        };
    }

    private static ModelAndView formView(
            UUID closeId,
            UUID eventId,
            String closeState,
            EventAuthorizationForm authorizationForm) {

        ModelAndView modelAndView =
                new ModelAndView(
                        "event-authorization/form");

        modelAndView.addObject(
                "closeId",
                closeId);

        modelAndView.addObject(
                "eventId",
                eventId);

        modelAndView.addObject(
                "closeState",
                closeState);

        modelAndView.addObject(
                "authorizationForm",
                authorizationForm);

        return modelAndView;
    }

    private static ModelAndView formError(
            UUID closeId,
            UUID eventId,
            EventAuthorizationForm authorizationForm,
            HttpStatus status,
            String errorMessage) {

        ModelAndView modelAndView =
                formView(
                        closeId,
                        eventId,
                        null,
                        authorizationForm);

        modelAndView.setStatus(
                status);

        modelAndView.addObject(
                "errorMessage",
                errorMessage);

        return modelAndView;
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

    private static ModelAndView eventNotFound() {
        return statusError(
                HttpStatus.NOT_FOUND,
                "Evento no encontrado",
                "El evento solicitado no existe dentro de este cierre.");
    }

    private static ModelAndView closeNotEditable() {
        return statusError(
                HttpStatus.CONFLICT,
                "Cierre no modificable",
                "El cierre fue enviado a contabilidad y ya no admite autorizaciones.");
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