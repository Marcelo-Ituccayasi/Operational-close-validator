package com.marceloituccayasi.ocv.operationalclose.presentation;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.marceloituccayasi.ocv.operationalclose.application.CloseConsolidationPreview;
import com.marceloituccayasi.ocv.operationalclose.application.CompleteOperationalCloseConsolidation;
import com.marceloituccayasi.ocv.operationalclose.application.CompleteOperationalCloseConsolidationCommand;
import com.marceloituccayasi.ocv.operationalclose.application.CompleteOperationalCloseConsolidationResult;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalCloseConsolidation;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalCloseConsolidationResult;
import com.marceloituccayasi.ocv.operationalclose.presentation.form.CloseConsolidationForm;

/**
 * MVC entry point for Operational Close consolidation.
 */
@Controller
public class OperationalCloseConsolidationPageController {

    private static final String CONSOLIDATED_MESSAGE =
            "El cierre fue consolidado correctamente.";

    private static final String CONSOLIDATION_REJECTED_MESSAGE =
            "La consolidación fue rechazada. Revisa los eventos, "
                    + "resultados de validación y alertas bloqueantes.";

    private final GetOperationalCloseConsolidation
            getOperationalCloseConsolidation;

    private final CompleteOperationalCloseConsolidation
            completeOperationalCloseConsolidation;

    public OperationalCloseConsolidationPageController(
            GetOperationalCloseConsolidation
                    getOperationalCloseConsolidation,
            CompleteOperationalCloseConsolidation
                    completeOperationalCloseConsolidation) {

        this.getOperationalCloseConsolidation =
                Objects.requireNonNull(
                        getOperationalCloseConsolidation);

        this.completeOperationalCloseConsolidation =
                Objects.requireNonNull(
                        completeOperationalCloseConsolidation);
    }

    @GetMapping("/closes/{closeId}/consolidate")
    ModelAndView form(
            @PathVariable String closeId) {

        UUID parsedCloseId =
                parseUuid(
                        closeId);

        if (parsedCloseId == null) {
            return invalidCloseIdentifier();
        }

        return loadForm(
                parsedCloseId,
                new CloseConsolidationForm(),
                null,
                HttpStatus.OK);
    }

    @PostMapping("/closes/{closeId}/consolidate")
    ModelAndView complete(
            @PathVariable String closeId,
            @ModelAttribute("consolidationForm")
            CloseConsolidationForm consolidationForm,
            RedirectAttributes redirectAttributes) {

        UUID parsedCloseId =
                parseUuid(
                        closeId);

        if (parsedCloseId == null) {
            return invalidCloseIdentifier();
        }

        CompleteOperationalCloseConsolidationCommand command;

        try {
            command =
                    consolidationForm.toCommand(
                            parsedCloseId);
        }
        catch (IllegalArgumentException exception) {
            return loadForm(
                    parsedCloseId,
                    consolidationForm,
                    exception.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }

        CompleteOperationalCloseConsolidationResult result =
                completeOperationalCloseConsolidation.execute(
                        command);

        return switch (result.status()) {
            case CONSOLIDATED ->
                    consolidationRedirect(
                            parsedCloseId,
                            redirectAttributes,
                            CONSOLIDATED_MESSAGE,
                            true,
                            result.consolidationId(),
                            List.of());

            case CONSOLIDATION_REJECTED ->
                    consolidationRedirect(
                            parsedCloseId,
                            redirectAttributes,
                            CONSOLIDATION_REJECTED_MESSAGE,
                            false,
                            null,
                            result.affectedEventIds());

            case INVALID_INPUT ->
                    loadForm(
                            parsedCloseId,
                            consolidationForm,
                            "El saldo real ingresado no es válido.",
                            HttpStatus.BAD_REQUEST);

            case ACTOR_REJECTED ->
                    statusError(
                            HttpStatus.FORBIDDEN,
                            "Operación no autorizada",
                            "No tienes autorización para consolidar "
                                    + "este cierre.");

            case CLOSE_NOT_FOUND ->
                    closeNotFound();

            case CLOSE_NOT_CONSOLIDATABLE ->
                    closeNotAvailable();
        };
    }

    private ModelAndView loadForm(
            UUID closeId,
            CloseConsolidationForm consolidationForm,
            String errorMessage,
            HttpStatus status) {

        GetOperationalCloseConsolidationResult result =
                getOperationalCloseConsolidation.execute(
                        closeId);

        return switch (result.status()) {
            case FOUND ->
                    formView(
                            result.preview(),
                            consolidationForm,
                            errorMessage,
                            status);

            case NOT_FOUND ->
                    closeNotFound();

            case NOT_AVAILABLE ->
                    closeNotAvailable();
        };
    }

    private static ModelAndView formView(
            CloseConsolidationPreview preview,
            CloseConsolidationForm consolidationForm,
            String errorMessage,
            HttpStatus status) {

        ModelAndView modelAndView =
                new ModelAndView(
                        "consolidations/create");

        modelAndView.setStatus(
                status);

        modelAndView.addObject(
                "preview",
                preview);

        modelAndView.addObject(
                "consolidationForm",
                consolidationForm);

        if (errorMessage != null) {
            modelAndView.addObject(
                    "errorMessage",
                    errorMessage);
        }

        return modelAndView;
    }

    private static ModelAndView consolidationRedirect(
            UUID closeId,
            RedirectAttributes redirectAttributes,
            String message,
            boolean successful,
            UUID consolidationId,
            List<UUID> affectedEventIds) {

        redirectAttributes.addFlashAttribute(
                "consolidationMessage",
                message);

        redirectAttributes.addFlashAttribute(
                "consolidationSuccessful",
                successful);

        if (consolidationId != null) {
            redirectAttributes.addFlashAttribute(
                    "consolidationId",
                    consolidationId);
        }

        if (!affectedEventIds.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "consolidationAffectedEventIds",
                    affectedEventIds);
        }

        ModelAndView modelAndView =
                new ModelAndView(
                        "redirect:/closes/"
                                + closeId);

        modelAndView.setStatus(
                HttpStatus.SEE_OTHER);

        return modelAndView;
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

    private static ModelAndView invalidCloseIdentifier() {
        return statusError(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "El identificador del cierre no es válido.");
    }

    private static ModelAndView closeNotFound() {
        return statusError(
                HttpStatus.NOT_FOUND,
                "Cierre no encontrado",
                "El cierre solicitado no existe.");
    }

    private static ModelAndView closeNotAvailable() {
        return statusError(
                HttpStatus.CONFLICT,
                "Cierre no consolidable",
                "El cierre debe estar en preparación o bloqueado "
                        + "para ejecutar la consolidación.");
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

}