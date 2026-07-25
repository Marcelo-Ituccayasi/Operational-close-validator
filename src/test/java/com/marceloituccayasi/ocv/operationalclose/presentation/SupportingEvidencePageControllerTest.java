package com.marceloituccayasi.ocv.operationalclose.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

import com.marceloituccayasi.ocv.operationalclose.application.CreateSupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.application.CreateSupportingEvidenceResult;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalCloseDetail;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalCloseResult;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalEventDetail;
import com.marceloituccayasi.ocv.operationalclose.application.GetOperationalEventResult;
import com.marceloituccayasi.ocv.operationalclose.application.OperationalCloseView;
import com.marceloituccayasi.ocv.operationalclose.presentation.form.SupportingEvidenceReferenceForm;

class SupportingEvidencePageControllerTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "84bbdf80-81a1-4a4e-ab17-e11491100001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "84bbdf80-81a1-4a4e-ab17-e11491100002");

    private final GetOperationalCloseDetail
            getOperationalCloseDetail =
                    mock(
                            GetOperationalCloseDetail.class);

    private final GetOperationalEventDetail
            getOperationalEventDetail =
                    mock(
                            GetOperationalEventDetail.class);

    private final CreateSupportingEvidence
            createSupportingEvidence =
                    mock(
                            CreateSupportingEvidence.class);

    private final SupportingEvidencePageController controller =
            new SupportingEvidencePageController(
                    getOperationalCloseDetail,
                    getOperationalEventDetail,
                    createSupportingEvidence);

    @Test
    void displaysReferenceFormForEditableOwningCloseAndEvent() {
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
                controller.newReferenceForm(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString());

        assertThat(response.getStatus())
                .isNull();

        assertThat(response.getViewName())
                .isEqualTo(
                        "supporting-evidence/reference-form");

        assertThat(response.getModel().get("closeId"))
                .isEqualTo(
                        CLOSE_ID);

        assertThat(response.getModel().get("eventId"))
                .isEqualTo(
                        EVENT_ID);

        assertThat(response.getModel().get("closeState"))
                .isEqualTo(
                        "PREPARATION");

        assertThat(response.getModel().get("evidenceForm"))
                .isInstanceOf(
                        SupportingEvidenceReferenceForm.class);
    }

    @Test
    void rejectsDirectFormWhenCloseWasSentToAccounting() {
        prepareClose(
                "SENT_TO_ACCOUNTING");

        ModelAndView response =
                controller.newReferenceForm(
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
                createSupportingEvidence);
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
                controller.newReferenceForm(
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
    void createsReferenceEvidenceAndRedirectsToEventDetail() {
        SupportingEvidenceReferenceForm form =
                validForm();

        CreateSupportingEvidenceResult result =
                mock(
                        CreateSupportingEvidenceResult.class);

        when(result.status())
                .thenReturn(
                        CreateSupportingEvidenceResult.Status.CREATED);

        when(
                createSupportingEvidence.execute(
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        result);

        ModelAndView response =
                controller.createReference(
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
                createSupportingEvidence)
                .execute(
                        org.mockito.ArgumentMatchers.argThat(
                                command ->
                                    command.closeId().equals(
                                            CLOSE_ID)
                                    && command.eventId().equals(
                                            EVENT_ID)
                                    && command.contentReference().equals(
                                            "reference:receipt-register-2026-07")));
    }

    @Test
    void returnsBadRequestBeforeUseCaseForMalformedForm() {
        SupportingEvidenceReferenceForm form =
                validForm();

        form.setEvidenceDate(
                "24/07/2026");

        ModelAndView response =
                controller.createReference(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString(),
                        form);

        assertThat(response.getStatus())
                .isEqualTo(
                        HttpStatus.BAD_REQUEST);

        assertThat(response.getViewName())
                .isEqualTo(
                        "supporting-evidence/reference-form");

        assertThat(response.getModel().get("errorMessage"))
                .isEqualTo(
                        "Los datos ingresados no son válidos.");

        verifyNoInteractions(
                createSupportingEvidence);
    }

    @Test
    void rejectsMalformedPathIdentifierBeforeQueries() {
        ModelAndView response =
                controller.newReferenceForm(
                        "not-a-uuid",
                        EVENT_ID.toString());

        assertThat(response.getStatus())
                .isEqualTo(
                        HttpStatus.BAD_REQUEST);

        verifyNoInteractions(
                getOperationalCloseDetail,
                getOperationalEventDetail,
                createSupportingEvidence);
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

    private static SupportingEvidenceReferenceForm
            validForm() {

        SupportingEvidenceReferenceForm form =
                new SupportingEvidenceReferenceForm();

        form.setEvidenceType(
                "RECEIPT");

        form.setContentReference(
                "receipt-register-2026-07");

        form.setSupportedAmount(
                "125.5000");

        form.setEvidenceDate(
                "2026-07-24");

        form.setLegibilityStatus(
                "LEGIBLE");

        return form;
    }

}