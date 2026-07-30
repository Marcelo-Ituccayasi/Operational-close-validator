package com.marceloituccayasi.ocv.presentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Translates unexpected technical failures without exposing internal details.
 */
@ControllerAdvice
public final class TechnicalErrorHandler {

    public static final String LOG_MESSAGE =
            "Unexpected technical failure handled";

    public static final String USER_TITLE =
            "Error técnico";

    public static final String USER_MESSAGE =
            "No fue posible completar la solicitud. "
                    + "Intenta nuevamente más tarde.";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    TechnicalErrorHandler.class);

    @ExceptionHandler(IllegalStateException.class)
    ModelAndView handleUnexpectedTechnicalFailure(
            IllegalStateException exception) {

        LOGGER.atError()
                .addKeyValue(
                        "eventType",
                        "TECHNICAL_ERROR")
                .addKeyValue(
                        "result",
                        "unexpected_failure")
                .addKeyValue(
                        "cause",
                        exception.getClass()
                                .getSimpleName())
                .log(
                        LOG_MESSAGE);

        ModelAndView modelAndView =
                new ModelAndView(
                        "errors/status");

        modelAndView.setStatus(
                HttpStatus.INTERNAL_SERVER_ERROR);

        modelAndView.addObject(
                "statusCode",
                HttpStatus.INTERNAL_SERVER_ERROR.value());

        modelAndView.addObject(
                "title",
                USER_TITLE);

        modelAndView.addObject(
                "message",
                USER_MESSAGE);

        return modelAndView;
    }

}