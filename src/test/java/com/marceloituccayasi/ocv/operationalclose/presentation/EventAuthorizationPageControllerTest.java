package com.marceloituccayasi.ocv.operationalclose.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

import com.marceloituccayasi.ocv.operationalclose.application.CreateEventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.application.CreateEventAuthorizationResult;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalCloseDetail;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalCloseResult;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalEventDetail;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalEventResult;
import com.marceloituccayasi.ocv.operationalclose.application.OperationalCloseView;
import com.marceloituccayasi.ocv.operationalclose.presentation.form.EventAuthorizationForm;

class EventAuthorizationPageControllerTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "b16b6ed5-d1aa-43bd-a7e8-72f549a00001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "b16b6ed5-d1aa-43bd-a7e8-72f549a00002");

    private final GetOperationalCloseDetail
            getOperationalCloseDetail =
                    mock(
                            GetOperationalCloseDetail.class);

    private final GetOperationalEventDetail
            getOperationalEventDetail =
                    mock(
                            GetOperationalEventDetail.class);

    private final CreateEventAuthorization
            createEventAuthorization =
                    mock(
                            CreateEventAuthorization.class);

    private final EventAuthorizationPageController controller =
            new EventAuthorizationPageController(
                    getOperationalCloseDetail,
                    getOperationalEventDetail,
                    createEventAuthorization);

    @Test
    void displaysAuthorizationFormForEditableCloseAndOwnedEvent() {
        prepareClose(
                "PREPARATION");

        GetOperationalEventResult eventResult =
                mock(
                        GetOperationalEventResult.class);

        when(eventResult.status())
                .thenReturn(
                        GetOperationalEventResult.Status.FOUND);

        when(
                getOperationalEventDetail.execute(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        eventResult);

        ModelAndView response =
                controller.newForm(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString());

        assertThat(response.getStatus())
                .isNull();

        assertThat(response.getViewName())
                .isEqualTo(
                        "event-authorization/form");

        assertThat(response.getModel().get("closeId"))
                .isEqualTo(
                        CLOSE_ID);

        assertThat(response.getModel().get("eventId"))
                .isEqualTo(
                        EVENT_ID);

        assertThat(response.getModel().get("closeState"))
                .isEqualTo(
                        "PREPARATION");

        assertThat(response.getModel().get("authorizationForm"))
                .isInstanceOf(
                        EventAuthorizationForm.class);
    }

    @Test
    void rejectsDirectFormWhenCloseWasSentToAccounting() {
        prepareClose(
                "SENT_TO_ACCOUNTING");

        ModelAndView response =
                controller.newForm(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString());

        assertThat(response.getStatus())
                .isEqualTo(
                        HttpStatus.CONFLICT);

        assertThat(response.getViewName())
                .isEqualTo(
                        "errors/status");

        verifyNoInteractions(
                getOperationalEventDetail,
                createEventAuthorization);
    }

    @Test
    void returnsNotFoundWhenEventDoesNotBelongToClose() {
        prepareClose(
                "PREPARATION");

        GetOperationalEventResult eventResult =
                mock(
                        GetOperationalEventResult.class);

        when(eventResult.status())
                .thenReturn(
                        GetOperationalEventResult.Status.NOT_FOUND);

        when(
                getOperationalEventDetail.execute(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        eventResult);

        ModelAndView response =
                controller.newForm(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString());

        assertThat(response.getStatus())
                .isEqualTo(
                        HttpStatus.NOT_FOUND);

        assertThat(response.getViewName())
                .isEqualTo(
                        "errors/status");
    }

    @Test
    void createsAuthorizationAndRedirectsToEventDetail() {
        EventAuthorizationForm form =
                validForm();

        CreateEventAuthorizationResult result =
                mock(
                        CreateEventAuthorizationResult.class);

        when(result.status())
                .thenReturn(
                        CreateEventAuthorizationResult.Status.CREATED);

        when(
                createEventAuthorization.execute(
                        any()))
                .thenReturn(
                        result);

        ModelAndView response =
                controller.create(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString(),
                        form);

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
                createEventAuthorization)
                .execute(
                        argThat(
                                command ->
                                    command.closeId().equals(
                                            CLOSE_ID)
                                    && command.eventId().equals(
                                            EVENT_ID)
                                    && command.authorizedByName().equals(
                                            "Supervisor de operaciones")
                                    && command.formalReference().equals(
                                            "AUTH-2026-0042")));
    }

    @Test
    void returnsBadRequestBeforeUseCaseForMalformedForm() {
        EventAuthorizationForm form =
                validForm();

        form.setAuthorizedAt(
                "24/07/2026 09:15");

        ModelAndView response =
                controller.create(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString(),
                        form);

        assertThat(response.getStatus())
                .isEqualTo(
                        HttpStatus.BAD_REQUEST);

        assertThat(response.getViewName())
                .isEqualTo(
                        "event-authorization/form");

        assertThat(response.getModel().get("errorMessage"))
                .isEqualTo(
                        "Los datos ingresados no son válidos.");

        verifyNoInteractions(
                createEventAuthorization);
    }

    @Test
    void rejectsMalformedPathIdentifierBeforeQueries() {
        ModelAndView response =
                controller.newForm(
                        "not-a-uuid",
                        EVENT_ID.toString());

        assertThat(response.getStatus())
                .isEqualTo(
                        HttpStatus.BAD_REQUEST);

        verifyNoInteractions(
                getOperationalCloseDetail,
                getOperationalEventDetail,
                createEventAuthorization);
    }

    private void prepareClose(
            String state) {

        GetOperationalCloseResult closeResult =
                mock(
                        GetOperationalCloseResult.class);

        OperationalCloseView closeView =
                mock(
                        OperationalCloseView.class);

        when(closeResult.status())
                .thenReturn(
                        GetOperationalCloseResult.Status.FOUND);

        when(closeResult.operationalClose())
                .thenReturn(
                        closeView);

        when(closeView.state())
                .thenReturn(
                        state);

        when(
                getOperationalCloseDetail.execute(
                        CLOSE_ID))
                .thenReturn(
                        closeResult);
    }

    private static EventAuthorizationForm validForm() {
        EventAuthorizationForm form =
                new EventAuthorizationForm();

        form.setAuthorizedByName(
                "Supervisor de operaciones");

        form.setReason(
                "Excepción aprobada para el cierre");

        form.setAuthorizedAt(
                "2026-07-24T09:15:00Z");

        form.setFormalReference(
                "AUTH-2026-0042");

        return form;
    }

}