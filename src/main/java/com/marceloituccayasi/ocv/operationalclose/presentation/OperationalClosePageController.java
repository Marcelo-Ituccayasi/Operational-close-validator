package com.marceloituccayasi.ocv.operationalclose.presentation;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import com.marceloituccayasi.ocv.operationalclose.application.CreateOperationalClose;
import com.marceloituccayasi.ocv.operationalclose.application.CreateOperationalCloseCommand;
import com.marceloituccayasi.ocv.operationalclose.application.CreateOperationalCloseResult;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalCloseDetail;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalCloseResult;
import com.marceloituccayasi.ocv.operationalclose.application.ListOperationalCloses;
import com.marceloituccayasi.ocv.operationalclose.presentation.form.OperationalCloseForm;

/**
 * MVC entry point for the Operational Close lifecycle.
 */
@Controller
public class OperationalClosePageController {

    private final CreateOperationalClose createOperationalClose;
    private final GetOperationalCloseDetail getOperationalCloseDetail;
    private final ListOperationalCloses listOperationalCloses;

    public OperationalClosePageController(
            CreateOperationalClose createOperationalClose,
            GetOperationalCloseDetail getOperationalCloseDetail,
            ListOperationalCloses listOperationalCloses) {

        this.createOperationalClose =
                Objects.requireNonNull(
                        createOperationalClose);

        this.getOperationalCloseDetail =
                Objects.requireNonNull(
                        getOperationalCloseDetail);

        this.listOperationalCloses =
                Objects.requireNonNull(
                        listOperationalCloses);
    }

    @GetMapping("/dashboard")
    String dashboard(
            Authentication authentication,
            Model model) {

        model.addAttribute(
                "username",
                authentication.getName());

        return "dashboard";
    }

    @GetMapping("/closes")
    ModelAndView list() {
        ModelAndView modelAndView =
                new ModelAndView("closes/list");

        modelAndView.addObject(
                "closes",
                listOperationalCloses.execute());

        return modelAndView;
    }

    @GetMapping("/closes/new")
    ModelAndView newForm() {
        ModelAndView modelAndView =
                new ModelAndView("closes/form");

        modelAndView.addObject(
                "closeForm",
                new OperationalCloseForm());

        return modelAndView;
    }

    @PostMapping("/closes")
    ModelAndView create(
            @ModelAttribute("closeForm")
            OperationalCloseForm closeForm) {

        CreateOperationalCloseCommand command;

        try {
            command = closeForm.toCommand();
        }
        catch (IllegalArgumentException exception) {
            return formError(
                    closeForm,
                    HttpStatus.BAD_REQUEST,
                    "Los datos ingresados no son válidos.");
        }

        CreateOperationalCloseResult result =
                createOperationalClose.execute(command);

        return switch (result.status()) {
            case CREATED ->
                    createdRedirect(result.closeId());

            case INVALID_INPUT ->
                    formError(
                            closeForm,
                            HttpStatus.BAD_REQUEST,
                            "Los datos del cierre no son válidos.");

            case PERIOD_CONFLICT ->
                    formError(
                            closeForm,
                            HttpStatus.CONFLICT,
                            "Ya existe un cierre para el período indicado.");

            case ACTOR_REJECTED ->
                    statusError(
                            HttpStatus.FORBIDDEN,
                            "Operación no autorizada",
                            "No tienes autorización para crear este cierre.");
        };
    }

    @GetMapping("/closes/{closeId}")
    ModelAndView detail(
            @PathVariable String closeId) {

        UUID parsedCloseId;

        try {
            parsedCloseId =
                    UUID.fromString(closeId);
        }
        catch (IllegalArgumentException exception) {
            return statusError(
                    HttpStatus.BAD_REQUEST,
                    "Solicitud inválida",
                    "El identificador del cierre no es válido.");
        }

        GetOperationalCloseResult result =
                getOperationalCloseDetail.execute(
                        parsedCloseId);

        if (result.status()
                == GetOperationalCloseResult.Status.NOT_FOUND) {

            return statusError(
                    HttpStatus.NOT_FOUND,
                    "Cierre no encontrado",
                    "El cierre solicitado no existe.");
        }

        ModelAndView modelAndView =
                new ModelAndView("closes/detail");

        modelAndView.addObject(
                "close",
                result.operationalClose());

        return modelAndView;
    }

    private static ModelAndView createdRedirect(
            UUID closeId) {

        ModelAndView modelAndView =
                new ModelAndView(
                        "redirect:/closes/" + closeId);

        modelAndView.setStatus(
                HttpStatus.SEE_OTHER);

        return modelAndView;
    }

    private static ModelAndView formError(
            OperationalCloseForm closeForm,
            HttpStatus status,
            String errorMessage) {

        ModelAndView modelAndView =
                new ModelAndView("closes/form");

        modelAndView.setStatus(status);
        modelAndView.addObject(
                "closeForm",
                closeForm);

        modelAndView.addObject(
                "errorMessage",
                errorMessage);

        return modelAndView;
    }

    private static ModelAndView statusError(
            HttpStatus status,
            String title,
            String message) {

        ModelAndView modelAndView =
                new ModelAndView("errors/status");

        modelAndView.setStatus(status);
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
