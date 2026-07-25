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

import com.marceloituccayasi.ocv.operationalclose.application.DeactivateSupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.application.DeactivateSupportingEvidenceResult;

class SupportingEvidenceDeactivationPageControllerTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "ec1a493c-e9c3-4895-ae23-45df9f100001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "ec1a493c-e9c3-4895-ae23-45df9f100002");

    private static final UUID EVIDENCE_ID =
            UUID.fromString(
                    "ec1a493c-e9c3-4895-ae23-45df9f100003");

    private final DeactivateSupportingEvidence
            deactivateSupportingEvidence =
                    mock(
                            DeactivateSupportingEvidence.class);

    private final SupportingEvidenceDeactivationPageController
            controller =
                    new SupportingEvidenceDeactivationPageController(
                            deactivateSupportingEvidence);

    @Test
    void deactivatesEvidenceAndRedirectsToEventDetail() {
        when(
                deactivateSupportingEvidence.execute(
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        DeactivateSupportingEvidenceResult.deactivated(
                                EVIDENCE_ID));

        ModelAndView response =
                controller.deactivate(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString(),
                        EVIDENCE_ID.toString());

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
                deactivateSupportingEvidence)
                .execute(
                        argThat(
                                command ->
                                    command.closeId().equals(
                                            CLOSE_ID)
                                    && command.eventId().equals(
                                            EVENT_ID)
                                    && command.evidenceId().equals(
                                            EVIDENCE_ID)));
    }

    @Test
    void rejectsMalformedEvidenceIdentifierBeforeUseCase() {
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
                deactivateSupportingEvidence);
    }

    @Test
    void reportsAlreadyInactiveEvidenceAsConflict() {
        when(
                deactivateSupportingEvidence.execute(
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        DeactivateSupportingEvidenceResult
                                .evidenceAlreadyInactive());

        ModelAndView response =
                controller.deactivate(
                        CLOSE_ID.toString(),
                        EVENT_ID.toString(),
                        EVIDENCE_ID.toString());

        assertThat(response.getStatus())
                .isEqualTo(
                        HttpStatus.CONFLICT);

        assertThat(response.getViewName())
                .isEqualTo(
                        "errors/status");

        assertThat(response.getModel().get("title"))
                .isEqualTo(
                        "Evidencia inactiva");
    }

}