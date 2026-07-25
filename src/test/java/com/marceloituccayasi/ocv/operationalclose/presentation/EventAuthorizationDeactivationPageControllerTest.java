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

import com.marceloituccayasi.ocv.operationalclose.application.DeactivateEventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.application.DeactivateEventAuthorizationResult;

class EventAuthorizationDeactivationPageControllerTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "b887134e-d229-4e84-9052-195c7a100001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "b887134e-d229-4e84-9052-195c7a100002");

    private static final UUID AUTHORIZATION_ID =
            UUID.fromString(
                    "b887134e-d229-4e84-9052-195c7a100003");

    private final DeactivateEventAuthorization
            deactivateEventAuthorization =
                    mock(
                            DeactivateEventAuthorization.class);

    private final EventAuthorizationDeactivationPageController
            controller =
                    new EventAuthorizationDeactivationPageController(
                            deactivateEventAuthorization);

    @Test
    void deactivatesAuthorizationAndRedirectsToEventDetail() {
        when(
                deactivateEventAuthorization.execute(
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        DeactivateEventAuthorizationResult.deactivated(
                                AUTHORIZATION_ID));

        ModelAndView response =
                controller.deactivate(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString(),
                        AUTHORIZATION_ID.toString());

        assertThat(response.getStatus())
                .isEqualTo(
                        HttpStatus.SEE_OTHER);

        assertThat(response.getViewName())
                .isEqualTo(
                        "redirect:/closes/"
                                + CLOSE_ID
                                + "/events/"
                                + EVENT_ID);

        verify(
                deactivateEventAuthorization)
                .execute(
                        argThat(
                                command ->
                                    command.closeId().equals(
                                            CLOSE_ID)
                                    && command.eventId().equals(
                                            EVENT_ID)
                                    && command.authorizationId().equals(
                                            AUTHORIZATION_ID)));
    }

    @Test
    void rejectsMalformedAuthorizationIdentifierBeforeUseCase() {
        ModelAndView response =
                controller.deactivate(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString(),
                        "not-a-uuid");

        assertThat(response.getStatus())
                .isEqualTo(
                        HttpStatus.BAD_REQUEST);

        assertThat(response.getViewName())
                .isEqualTo(
                        "errors/status");

        verifyNoInteractions(
                deactivateEventAuthorization);
    }

    @Test
    void reportsAlreadyInactiveAuthorizationAsConflict() {
        when(
                deactivateEventAuthorization.execute(
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        DeactivateEventAuthorizationResult
                                .authorizationAlreadyInactive());

        ModelAndView response =
                controller.deactivate(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString(),
                        AUTHORIZATION_ID.toString());

        assertThat(response.getStatus())
                .isEqualTo(
                        HttpStatus.CONFLICT);

        assertThat(response.getViewName())
                .isEqualTo(
                        "errors/status");

        assertThat(response.getModel().get("title"))
                .isEqualTo(
                        "Autorización inactiva");
    }

}