package com.marceloituccayasi.ocv.operationalclose.presentation.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.application.CreateSupportingEvidenceCommand;

class SupportingEvidenceReferenceFormTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "7779c1f8-d628-49ba-a0fa-3bb216100001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "7779c1f8-d628-49ba-a0fa-3bb216100002");

    @Test
    void mapsOpaqueReferenceFieldsToCreateCommand() {
        SupportingEvidenceReferenceForm form =
                validForm();

        CreateSupportingEvidenceCommand command =
                form.toCreateCommand(
                        CLOSE_ID,
                        EVENT_ID);

        assertThat(command.closeId())
                .isEqualTo(
                        CLOSE_ID);

        assertThat(command.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(command.evidenceType())
                .isEqualTo(
                        "RECEIPT");

        assertThat(command.contentReference())
                .isEqualTo(
                        "reference:receipt-register-2026-07");

        assertThat(command.supportedAmount())
                .isEqualByComparingTo(
                        new BigDecimal(
                                "125.5000"));

        assertThat(command.evidenceDate())
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                7,
                                24));

        assertThat(command.legibilityStatus())
                .isEqualTo(
                        "LEGIBLE");
    }

    @Test
    void allowsSupportedAmountToBeAbsent() {
        SupportingEvidenceReferenceForm form =
                validForm();

        form.setSupportedAmount(
                " ");

        CreateSupportingEvidenceCommand command =
                form.toCreateCommand(
                        CLOSE_ID,
                        EVENT_ID);

        assertThat(command.supportedAmount())
                .isNull();
    }

    @Test
    void preservesAnOpaqueValueThatContainsUrlLikeText() {
        SupportingEvidenceReferenceForm form =
                validForm();

        form.setContentReference(
                "https://records.example/receipt/42");

        CreateSupportingEvidenceCommand command =
                form.toCreateCommand(
                        CLOSE_ID,
                        EVENT_ID);

        assertThat(command.contentReference())
                .isEqualTo(
                        "reference:https://records.example/receipt/42");
    }

    @Test
    void rejectsMissingRequiredField() {
        SupportingEvidenceReferenceForm form =
                validForm();

        form.setEvidenceType(
                " ");

        assertThatThrownBy(
                () -> form.toCreateCommand(
                        CLOSE_ID,
                        EVENT_ID))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "Todos los campos obligatorios deben completarse.");
    }

    @Test
    void rejectsMalformedSupportedAmount() {
        SupportingEvidenceReferenceForm form =
                validForm();

        form.setSupportedAmount(
                "not-a-number");

        assertThatThrownBy(
                () -> form.toCreateCommand(
                        CLOSE_ID,
                        EVENT_ID))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "Los datos ingresados no tienen el formato esperado.");
    }

    @Test
    void rejectsMalformedEvidenceDate() {
        SupportingEvidenceReferenceForm form =
                validForm();

        form.setEvidenceDate(
                "24/07/2026");

        assertThatThrownBy(
                () -> form.toCreateCommand(
                        CLOSE_ID,
                        EVENT_ID))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "Los datos ingresados no tienen el formato esperado.");
    }

    @Test
    void rejectsMissingIdentifiers() {
        SupportingEvidenceReferenceForm form =
                validForm();

        assertThatThrownBy(
                () -> form.toCreateCommand(
                        null,
                        EVENT_ID))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "El identificador del cierre es obligatorio.");

        assertThatThrownBy(
                () -> form.toCreateCommand(
                        CLOSE_ID,
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "El identificador del evento es obligatorio.");
    }

    private static SupportingEvidenceReferenceForm
            validForm() {

        SupportingEvidenceReferenceForm form =
                new SupportingEvidenceReferenceForm();

        form.setEvidenceType(
                " RECEIPT ");

        form.setContentReference(
                " receipt-register-2026-07 ");

        form.setSupportedAmount(
                " 125.5000 ");

        form.setEvidenceDate(
                " 2026-07-24 ");

        form.setLegibilityStatus(
                " LEGIBLE ");

        return form;
    }

}