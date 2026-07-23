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

import com.marceloituccayasi.ocv.operationalclose.application.CreateOperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.application.CreateOperationalEventCommand;
import com.marceloituccayasi.ocv.operationalclose.application.CreateOperationalEventResult;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalEventDetail;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalEventResult;
import com.marceloituccayasi.ocv.operationalclose.application.ListOperationalEvents;
import com.marceloituccayasi.ocv.operationalclose.application.ListOperationalEventsResult;
import com.marceloituccayasi.ocv.operationalclose.application.UpdateOperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.application.UpdateOperationalEventCommand;
import com.marceloituccayasi.ocv.operationalclose.application.UpdateOperationalEventResult;
import com.marceloituccayasi.ocv.operationalclose.presentation.form.OperationalEventForm;

/**
 * MVC entry point for Operational Event creation, revision and queries.
 */
@Controller
public class OperationalEventPageController {

    private final CreateOperationalEvent createOperationalEvent;

    private final UpdateOperationalEvent updateOperationalEvent;

    private final ListOperationalEvents listOperationalEvents;

    private final GetOperationalEventDetail
            getOperationalEventDetail;

    public OperationalEventPageController(
            CreateOperationalEvent createOperationalEvent,
            UpdateOperationalEvent updateOperationalEvent,
            ListOperationalEvents listOperationalEvents,
            GetOperationalEventDetail getOperationalEventDetail) {

        this.createOperationalEvent =
                Objects.requireNonNull(
                        createOperationalEvent);

        this.updateOperationalEvent =
                Objects.requireNonNull(
                        updateOperationalEvent);

        this.listOperationalEvents =
                Objects.requireNonNull(
                        listOperationalEvents);

        this.getOperationalEventDetail =
                Objects.requireNonNull(
                        getOperationalEventDetail);
    }

    @GetMapping("/closes/{closeId}/events")
    ModelAndView list(
            @PathVariable String closeId) {

        UUID parsedCloseId =
                parseUuid(closeId);

        if (parsedCloseId == null) {
            return invalidIdentifier(
                    "El identificador del cierre no es válido.");
        }

        ListOperationalEventsResult result =
                listOperationalEvents.execute(
                        parsedCloseId);

        if (result.status()
                == ListOperationalEventsResult.Status
                        .CLOSE_NOT_FOUND) {

            return closeNotFound();
        }

        ModelAndView modelAndView =
                new ModelAndView(
                        "events/list");

        modelAndView.addObject(
                "closeId",
                parsedCloseId);

        modelAndView.addObject(
                "events",
                result.operationalEvents());

        return modelAndView;
    }

    @GetMapping("/closes/{closeId}/events/new")
    ModelAndView newForm(
            @PathVariable String closeId) {

        UUID parsedCloseId =
                parseUuid(closeId);

        if (parsedCloseId == null) {
            return invalidIdentifier(
                    "El identificador del cierre no es válido.");
        }

        ListOperationalEventsResult closeResult =
                listOperationalEvents.execute(
                        parsedCloseId);

        if (closeResult.status()
                == ListOperationalEventsResult.Status
                        .CLOSE_NOT_FOUND) {

            return closeNotFound();
        }

        ModelAndView modelAndView =
                new ModelAndView(
                        "events/form");

        modelAndView.addObject(
                "closeId",
                parsedCloseId);

        modelAndView.addObject(
                "eventForm",
                new OperationalEventForm());

        return modelAndView;
    }

    @PostMapping("/closes/{closeId}/events")
    ModelAndView create(
            @PathVariable String closeId,
            @ModelAttribute("eventForm")
            OperationalEventForm eventForm) {

        UUID parsedCloseId =
                parseUuid(closeId);

        if (parsedCloseId == null) {
            return invalidIdentifier(
                    "El identificador del cierre no es válido.");
        }

        CreateOperationalEventCommand command;

        try {
            command =
                    eventForm.toCreateCommand(
                            parsedCloseId);
        }
        catch (IllegalArgumentException exception) {
            return createFormError(
                    parsedCloseId,
                    eventForm,
                    HttpStatus.BAD_REQUEST,
                    "Los datos ingresados no son válidos.");
        }

        CreateOperationalEventResult result =
                createOperationalEvent.execute(
                        command);

        return switch (result.status()) {
            case CREATED ->
                    detailRedirect(
                            parsedCloseId,
                            result.eventId());

            case INVALID_INPUT ->
                    createFormError(
                            parsedCloseId,
                            eventForm,
                            HttpStatus.BAD_REQUEST,
                            "Los datos del evento no son válidos.");

            case ACTOR_REJECTED ->
                    unauthorizedOperation(
                            "No tienes autorización para registrar este evento.");

            case CLOSE_NOT_FOUND ->
                    closeNotFound();

            case CLOSE_NOT_EDITABLE ->
                    createFormError(
                            parsedCloseId,
                            eventForm,
                            HttpStatus.CONFLICT,
                            "El cierre ya no permite registrar eventos.");

            case REVERSED_EVENT_NOT_FOUND ->
                    createFormError(
                            parsedCloseId,
                            eventForm,
                            HttpStatus.NOT_FOUND,
                            "El evento que se desea anular no existe.");

            case CANCELLATION_CONFLICT ->
                    createFormError(
                            parsedCloseId,
                            eventForm,
                            HttpStatus.CONFLICT,
                            "El evento seleccionado ya tiene una anulación.");
        };
    }

    @GetMapping(
            "/closes/{closeId}/events/{eventId}")
    ModelAndView detail(
            @PathVariable String closeId,
            @PathVariable String eventId) {

        ParsedEventIdentifiers identifiers =
                parseEventIdentifiers(
                        closeId,
                        eventId);

        if (identifiers.error() != null) {
            return identifiers.error();
        }

        GetOperationalEventResult result =
                getOperationalEventDetail.execute(
                        identifiers.closeId(),
                        identifiers.eventId());

        if (result.status()
                == GetOperationalEventResult.Status.NOT_FOUND) {

            return eventNotFound();
        }

        ModelAndView modelAndView =
                new ModelAndView(
                        "events/detail");

        modelAndView.addObject(
                "closeId",
                identifiers.closeId());

        modelAndView.addObject(
                "event",
                result.operationalEvent());

        return modelAndView;
    }

    @GetMapping(
            "/closes/{closeId}/events/{eventId}/edit")
    ModelAndView editForm(
            @PathVariable String closeId,
            @PathVariable String eventId) {

        ParsedEventIdentifiers identifiers =
                parseEventIdentifiers(
                        closeId,
                        eventId);

        if (identifiers.error() != null) {
            return identifiers.error();
        }

        ListOperationalEventsResult closeResult =
                listOperationalEvents.execute(
                        identifiers.closeId());

        if (closeResult.status()
                == ListOperationalEventsResult.Status
                        .CLOSE_NOT_FOUND) {

            return closeNotFound();
        }

        GetOperationalEventResult eventResult =
                getOperationalEventDetail.execute(
                        identifiers.closeId(),
                        identifiers.eventId());

        if (eventResult.status()
                == GetOperationalEventResult.Status.NOT_FOUND) {

            return eventNotFound();
        }

        return editFormView(
                identifiers.closeId(),
                identifiers.eventId(),
                OperationalEventForm.fromView(
                        eventResult.operationalEvent()));
    }

    @PostMapping(
            "/closes/{closeId}/events/{eventId}/edit")
    ModelAndView update(
            @PathVariable String closeId,
            @PathVariable String eventId,
            @ModelAttribute("eventForm")
            OperationalEventForm eventForm) {

        ParsedEventIdentifiers identifiers =
                parseEventIdentifiers(
                        closeId,
                        eventId);

        if (identifiers.error() != null) {
            return identifiers.error();
        }

        UpdateOperationalEventCommand command;

        try {
            command =
                    eventForm.toUpdateCommand(
                            identifiers.closeId(),
                            identifiers.eventId());
        }
        catch (IllegalArgumentException exception) {
            return updateFormError(
                    identifiers.closeId(),
                    identifiers.eventId(),
                    eventForm,
                    HttpStatus.BAD_REQUEST,
                    "Los datos ingresados no son válidos.");
        }

        UpdateOperationalEventResult result =
                updateOperationalEvent.execute(
                        command);

        return switch (result.status()) {
            case UPDATED ->
                    detailRedirect(
                            identifiers.closeId(),
                            result.eventId());

            case INVALID_INPUT ->
                    updateFormError(
                            identifiers.closeId(),
                            identifiers.eventId(),
                            eventForm,
                            HttpStatus.BAD_REQUEST,
                            "Los datos del evento no son válidos.");

            case ACTOR_REJECTED ->
                    unauthorizedOperation(
                            "No tienes autorización para modificar este evento.");

            case CLOSE_NOT_FOUND ->
                    closeNotFound();

            case CLOSE_NOT_EDITABLE ->
                    updateFormError(
                            identifiers.closeId(),
                            identifiers.eventId(),
                            eventForm,
                            HttpStatus.CONFLICT,
                            "El cierre ya no permite modificar eventos.");

            case EVENT_NOT_FOUND ->
                    eventNotFound();

            case REVERSED_EVENT_NOT_FOUND ->
                    updateFormError(
                            identifiers.closeId(),
                            identifiers.eventId(),
                            eventForm,
                            HttpStatus.NOT_FOUND,
                            "El evento que se desea anular no existe.");

            case CANCELLATION_CONFLICT ->
                    updateFormError(
                            identifiers.closeId(),
                            identifiers.eventId(),
                            eventForm,
                            HttpStatus.CONFLICT,
                            "La modificación entra en conflicto con una anulación existente.");
        };
    }

    private static ParsedEventIdentifiers
            parseEventIdentifiers(
                    String closeId,
                    String eventId) {

        UUID parsedCloseId =
                parseUuid(closeId);

        if (parsedCloseId == null) {
            return new ParsedEventIdentifiers(
                    null,
                    null,
                    invalidIdentifier(
                            "El identificador del cierre no es válido."));
        }

        UUID parsedEventId =
                parseUuid(eventId);

        if (parsedEventId == null) {
            return new ParsedEventIdentifiers(
                    null,
                    null,
                    invalidIdentifier(
                            "El identificador del evento no es válido."));
        }

        return new ParsedEventIdentifiers(
                parsedCloseId,
                parsedEventId,
                null);
    }

    private static UUID parseUuid(
            String value) {

        try {
            return UUID.fromString(value);
        }
        catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static ModelAndView editFormView(
            UUID closeId,
            UUID eventId,
            OperationalEventForm eventForm) {

        ModelAndView modelAndView =
                new ModelAndView(
                        "events/edit");

        modelAndView.addObject(
                "closeId",
                closeId);

        modelAndView.addObject(
                "eventId",
                eventId);

        modelAndView.addObject(
                "eventForm",
                eventForm);

        return modelAndView;
    }

    private static ModelAndView detailRedirect(
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

    private static ModelAndView createFormError(
            UUID closeId,
            OperationalEventForm eventForm,
            HttpStatus status,
            String errorMessage) {

        ModelAndView modelAndView =
                new ModelAndView(
                        "events/form");

        modelAndView.setStatus(
                status);

        modelAndView.addObject(
                "closeId",
                closeId);

        modelAndView.addObject(
                "eventForm",
                eventForm);

        modelAndView.addObject(
                "errorMessage",
                errorMessage);

        return modelAndView;
    }

    private static ModelAndView updateFormError(
            UUID closeId,
            UUID eventId,
            OperationalEventForm eventForm,
            HttpStatus status,
            String errorMessage) {

        ModelAndView modelAndView =
                editFormView(
                        closeId,
                        eventId,
                        eventForm);

        modelAndView.setStatus(
                status);

        modelAndView.addObject(
                "errorMessage",
                errorMessage);

        return modelAndView;
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

    private static ModelAndView unauthorizedOperation(
            String message) {

        return statusError(
                HttpStatus.FORBIDDEN,
                "Operación no autorizada",
                message);
    }

    private static ModelAndView invalidIdentifier(
            String message) {

        return statusError(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                message);
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

    private record ParsedEventIdentifiers(
            UUID closeId,
            UUID eventId,
            ModelAndView error) {
    }

}