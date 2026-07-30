package com.marceloituccayasi.ocv.operationalclose.presentation;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.marceloituccayasi.ocv.operationalclose.application.SubmitOperationalCloseToAccounting;
import com.marceloituccayasi.ocv.operationalclose.application.SubmitOperationalCloseToAccountingCommand;
import com.marceloituccayasi.ocv.operationalclose.application.SubmitOperationalCloseToAccountingResult;

/**
 * MVC entry point for internal accounting submission.
 */
@Controller
public class OperationalCloseAccountingSubmissionPageController {

    private static final String SUBMITTED_MESSAGE =
            "Cierre enviado a contabilidad.";

    private static final String REJECTED_MESSAGE =
            "El envÃ­o fue rechazado. Revisa las causas registradas.";

    private final SubmitOperationalCloseToAccounting
            submitOperationalCloseToAccounting;

    public OperationalCloseAccountingSubmissionPageController(
            SubmitOperationalCloseToAccounting
                    submitOperationalCloseToAccounting) {

        this.submitOperationalCloseToAccounting =
                Objects.requireNonNull(
                        submitOperationalCloseToAccounting);
    }

    @PostMapping(
            "/closes/{closeId}/submit-to-accounting")
    ModelAndView submit(
            @PathVariable String closeId,
            RedirectAttributes redirectAttributes) {

        UUID parsedCloseId =
                parseUuid(
                        closeId);

        if (parsedCloseId == null) {
            return invalidCloseIdentifier();
        }

        SubmitOperationalCloseToAccountingResult result =
                submitOperationalCloseToAccounting.execute(
                        new SubmitOperationalCloseToAccountingCommand(
                                parsedCloseId));

        return switch (result.status()) {
            case SUBMITTED ->
                    submissionRedirect(
                            parsedCloseId,
                            redirectAttributes,
                            SUBMITTED_MESSAGE,
                            true,
                            result.submissionAttemptId(),
                            result.validationResultId(),
                            List.of());

            case SUBMISSION_REJECTED ->
                    submissionRedirect(
                            parsedCloseId,
                            redirectAttributes,
                            REJECTED_MESSAGE,
                            false,
                            result.submissionAttemptId(),
                            result.validationResultId(),
                            result.issueTypeNames());

            case INVALID_INPUT ->
                    statusError(
                            HttpStatus.BAD_REQUEST,
                            "Solicitud invÃ¡lida",
                            "La solicitud de envÃ­o no es vÃ¡lida.");

            case ACTOR_REJECTED ->
                    statusError(
                            HttpStatus.FORBIDDEN,
                            "OperaciÃ³n no autorizada",
                            "No tienes autorizaciÃ³n para enviar "
                                    + "este cierre a contabilidad.");

            case CLOSE_NOT_FOUND ->
                    closeNotFound();

            case CLOSE_NOT_SUBMITTABLE ->
                    statusError(
                            HttpStatus.CONFLICT,
                            "Cierre no disponible",
                            "El cierre debe estar validado antes "
                                    + "de enviarlo a contabilidad.");

            case CLOSE_ALREADY_SUBMITTED ->
                    statusError(
                            HttpStatus.CONFLICT,
                            "Cierre ya enviado",
                            "El cierre ya fue enviado a contabilidad.");
        };
    }

    private static ModelAndView submissionRedirect(
            UUID closeId,
            RedirectAttributes redirectAttributes,
            String message,
            boolean successful,
            UUID submissionAttemptId,
            UUID validationResultId,
            List<String> issueTypes) {

        redirectAttributes.addFlashAttribute(
                "accountingSubmissionMessage",
                message);

        redirectAttributes.addFlashAttribute(
                "accountingSubmissionSuccessful",
                successful);

        redirectAttributes.addFlashAttribute(
                "accountingSubmissionAttemptId",
                submissionAttemptId);

        redirectAttributes.addFlashAttribute(
                "accountingSubmissionValidationResultId",
                validationResultId);

        if (!issueTypes.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "accountingSubmissionIssueTypes",
                    issueTypes);
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
                "Solicitud invÃ¡lida",
                "El identificador del cierre no es vÃ¡lido.");
    }

    private static ModelAndView closeNotFound() {
        return statusError(
                HttpStatus.NOT_FOUND,
                "Cierre no encontrado",
                "El cierre solicitado no existe.");
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