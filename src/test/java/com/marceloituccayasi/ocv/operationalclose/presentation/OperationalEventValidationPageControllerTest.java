package com.marceloituccayasi.ocv.operationalclose.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.marceloituccayasi.ocv.operationalclose.application.ValidateOperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.application.ValidateOperationalEventResult;

class OperationalEventValidationPageControllerTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "a571126c-f42d-4c5e-a4ce-230000000001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "a571126c-f42d-4c5e-a4ce-230000000002");

    private final ValidateOperationalEvent
            validateOperationalEvent =
                    mock(
                            ValidateOperationalEvent.class);

    private final OperationalEventValidationPageController
            controller =
                    new OperationalEventValidationPageController(
                            validateOperationalEvent);

    @Test
    void validatesEventAndRedirectsWithSuccessMessage() {
        when(
                validateOperationalEvent.execute(
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        ValidateOperationalEventResult.validated(
                                EVENT_ID));

        RedirectAttributesModelMap redirectAttributes =
                new RedirectAttributesModelMap();

        ModelAndView response =
                controller.validate(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString(),
                        redirectAttributes);

        assertThat(
                response.getStatus())
                .isEqualTo(
                        HttpStatus.SEE_OTHER);

        assertThat(
                response.getViewName())
                .isEqualTo(
                        "redirect:/closes/"
                                + CLOSE_ID
                                + "/events/"
                                + EVENT_ID);

        assertThat(
                redirectAttributes.getFlashAttributes()
                        .get(
                                "validationMessage"))
                .isEqualTo(
                        "El evento fue validado correctamente.");

        assertThat(
                redirectAttributes.getFlashAttributes()
                        .get(
                                "validationSuccessful"))
                .isEqualTo(
                        true);

        verify(
                validateOperationalEvent)
                .execute(
                        argThat(
                                command ->
                                    command.closeId()
                                            .equals(
                                                    CLOSE_ID)
                                    && command.eventId()
                                            .equals(
                                                    EVENT_ID)));
    }

    @Test
    void redirectsFailedValidationWithVisibleWarning() {
        when(
                validateOperationalEvent.execute(
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        ValidateOperationalEventResult
                                .validationFailed(
                                        EVENT_ID,
                                        "Failed rules."));

        RedirectAttributesModelMap redirectAttributes =
                new RedirectAttributesModelMap();

        ModelAndView response =
                controller.validate(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString(),
                        redirectAttributes);

        assertThat(
                response.getStatus())
                .isEqualTo(
                        HttpStatus.SEE_OTHER);

        assertThat(
                response.getViewName())
                .isEqualTo(
                        "redirect:/closes/"
                                + CLOSE_ID
                                + "/events/"
                                + EVENT_ID);

        assertThat(
                redirectAttributes.getFlashAttributes()
                        .get(
                                "validationMessage"))
                .isEqualTo(
                        "La validación finalizó con reglas fallidas. "
                                + "Revisa el estado del evento y sus alertas.");

        assertThat(
                redirectAttributes.getFlashAttributes()
                        .get(
                                "validationSuccessful"))
                .isEqualTo(
                        false);
    }

    @Test
    void rejectsMalformedIdentifierBeforeExecutingUseCase() {
        RedirectAttributesModelMap redirectAttributes =
                new RedirectAttributesModelMap();

        ModelAndView response =
                controller.validate(
                        CLOSE_ID.toString(),
                        "not-a-uuid",
                        redirectAttributes);

        assertThat(
                response.getStatus())
                .isEqualTo(
                        HttpStatus.BAD_REQUEST);

        assertThat(
                response.getViewName())
                .isEqualTo(
                        "errors/status");

        assertThat(
                response.getModel()
                        .get(
                                "message"))
                .isEqualTo(
                        "El identificador del evento no es válido.");

        assertThat(
                redirectAttributes.getFlashAttributes())
                .isEmpty();

        verifyNoInteractions(
                validateOperationalEvent);
    }

    @Test
    void reportsSentCloseAsConflict() {
        when(
                validateOperationalEvent.execute(
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        ValidateOperationalEventResult
                                .closeNotEditable());

        ModelAndView response =
                controller.validate(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString(),
                        new RedirectAttributesModelMap());

        assertThat(
                response.getStatus())
                .isEqualTo(
                        HttpStatus.CONFLICT);

        assertThat(
                response.getViewName())
                .isEqualTo(
                        "errors/status");

        assertThat(
                response.getModel()
                        .get(
                                "title"))
                .isEqualTo(
                        "Cierre no modificable");
    }

    @Test
    void reportsMissingEventAsNotFound() {
        when(
                validateOperationalEvent.execute(
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        ValidateOperationalEventResult
                                .eventNotFound());

        ModelAndView response =
                controller.validate(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString(),
                        new RedirectAttributesModelMap());

        assertThat(
                response.getStatus())
                .isEqualTo(
                        HttpStatus.NOT_FOUND);

        assertThat(
                response.getViewName())
                .isEqualTo(
                        "errors/status");

        assertThat(
                response.getModel()
                        .get(
                                "title"))
                .isEqualTo(
                        "Evento no encontrado");
    }

}