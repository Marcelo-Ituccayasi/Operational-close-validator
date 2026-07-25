package com.marceloituccayasi.ocv.operationalclose.presentation;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import com.marceloituccayasi.ocv.operationalclose.application.DeactivateSupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.application.DeactivateSupportingEvidenceCommand;
import com.marceloituccayasi.ocv.operationalclose.application.DeactivateSupportingEvidenceResult;

/**
 * MVC entry point for Supporting Evidence deactivation.
 */
@Controller
public class SupportingEvidenceDeactivationPageController {

    private final DeactivateSupportingEvidence
            deactivateSupportingEvidence;

    public SupportingEvidenceDeactivationPageController(
            DeactivateSupportingEvidence deactivateSupportingEvidence) {

        this.deactivateSupportingEvidence =
                Objects.requireNonNull(
                        deactivateSupportingEvidence);
    }

    @PostMapping(
            "/closes/{closeId}/events/{eventId}"
                    + "/supporting-evidence/{evidenceId}/deactivate")
    ModelAndView deactivate(
            @PathVariable String closeId,
            @PathVariable String eventId,
            @PathVariable String evidenceId) {

        ParsedIdentifiers identifiers =
                parseIdentifiers(
                        closeId,
                        eventId,
                        evidenceId);

        if (identifiers.error() != null) {
            return identifiers.error();
        }

        DeactivateSupportingEvidenceResult result =
                deactivateSupportingEvidence.execute(
                        new DeactivateSupportingEvidenceCommand(
                                identifiers.closeId(),
                                identifiers.eventId(),
                                identifiers.evidenceId()));

        return switch (result.status()) {
            case DEACTIVATED ->
                    eventDetailRedirect(
                            identifiers.closeId(),
                            identifiers.eventId());

            case INVALID_INPUT ->
                    statusError(
                            HttpStatus.BAD_REQUEST,
                            "Solicitud inválida",
                            "Los datos de la evidencia no son válidos.");

            case ACTOR_REJECTED ->
                    statusError(
                            HttpStatus.FORBIDDEN,
                            "Operación no autorizada",
                            "No tienes autorización para desactivar esta evidencia.");

            case CLOSE_NOT_FOUND ->
                    closeNotFound();

            case CLOSE_NOT_EDITABLE ->
                    closeNotEditable();

            case EVENT_NOT_FOUND ->
                    eventNotFound();

            case EVIDENCE_NOT_FOUND ->
                    evidenceNotFound();

            case EVIDENCE_ALREADY_INACTIVE ->
                    evidenceAlreadyInactive();
        };
    }

    private static ParsedIdentifiers parseIdentifiers(
            String closeId,
            String eventId,
            String evidenceId) {

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

        UUID parsedEvidenceId =
                parseUuid(
                        evidenceId);

        if (parsedEvidenceId == null) {
            return new ParsedIdentifiers(
                    null,
                    null,
                    null,
                    invalidIdentifier(
                            "El identificador de la evidencia no es válido."));
        }

        return new ParsedIdentifiers(
                parsedCloseId,
                parsedEventId,
                parsedEvidenceId,
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
                "El cierre ya no permite desactivar evidencias.");
    }

    private static ModelAndView eventNotFound() {
        return statusError(
                HttpStatus.NOT_FOUND,
                "Evento no encontrado",
                "El evento solicitado no existe dentro de este cierre.");
    }

    private static ModelAndView evidenceNotFound() {
        return statusError(
                HttpStatus.NOT_FOUND,
                "Evidencia no encontrada",
                "La evidencia solicitada no existe dentro de este evento.");
    }

    private static ModelAndView evidenceAlreadyInactive() {
        return statusError(
                HttpStatus.CONFLICT,
                "Evidencia inactiva",
                "La evidencia solicitada ya se encuentra inactiva.");
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
            UUID evidenceId,
            ModelAndView error) {
    }

}