package com.marceloituccayasi.ocv.operationalclose.presentation.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.application.CreateEventAuthorizationCommand;

class EventAuthorizationFormTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "9b6cbcb4-bc2f-487d-940e-6fb18e500001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "9b6cbcb4-bc2f-487d-940e-6fb18e500002");

    @Test
    void mapsFieldsToCreateCommand() {
        EventAuthorizationForm form =
                validForm();

        CreateEventAuthorizationCommand command =
                form.toCreateCommand(
                        CLOSE_ID,
                        EVENT_ID);

        assertThat(command.closeId())
                .isEqualTo(
                        CLOSE_ID);

        assertThat(command.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(command.authorizedByName())
                .isEqualTo(
                        "Supervisor de operaciones");

        assertThat(command.reason())
                .isEqualTo(
                        "Excepción aprobada para el cierre");

        assertThat(command.authorizedAt())
                .isEqualTo(
                        Instant.parse(
                                "2026-07-24T09:15:00Z"));

        assertThat(command.formalReference())
                .isEqualTo(
                        "AUTH-2026-0042");
    }

    @Test
    void preservesUrlLikeFormalReferenceAsOpaqueText() {
        EventAuthorizationForm form =
                validForm();

        form.setFormalReference(
                "https://records.example/authorizations/42");

        CreateEventAuthorizationCommand command =
                form.toCreateCommand(
                        CLOSE_ID,
                        EVENT_ID);

        assertThat(command.formalReference())
                .isEqualTo(
                        "https://records.example/authorizations/42");
    }

    @Test
    void trimsSubmittedValues() {
        EventAuthorizationForm form =
                validForm();

        form.setAuthorizedByName(
                "  Supervisor regional  ");

        form.setReason(
                "  Aprobación extraordinaria  ");

        form.setFormalReference(
                "  AUTH-2026-0099  ");

        CreateEventAuthorizationCommand command =
                form.toCreateCommand(
                        CLOSE_ID,
                        EVENT_ID);

        assertThat(command.authorizedByName())
                .isEqualTo(
                        "Supervisor regional");

        assertThat(command.reason())
                .isEqualTo(
                        "Aprobación extraordinaria");

        assertThat(command.formalReference())
                .isEqualTo(
                        "AUTH-2026-0099");
    }

    @Test
    void rejectsMissingRequiredField() {
        EventAuthorizationForm form =
                validForm();

        form.setReason(
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
    void rejectsMalformedAuthorizationInstant() {
        EventAuthorizationForm form =
                validForm();

        form.setAuthorizedAt(
                "24/07/2026 09:15");

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
        EventAuthorizationForm form =
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

    private static EventAuthorizationForm validForm() {
        EventAuthorizationForm form =
                new EventAuthorizationForm();

        form.setAuthorizedByName(
                " Supervisor de operaciones ");

        form.setReason(
                " Excepción aprobada para el cierre ");

        form.setAuthorizedAt(
                " 2026-07-24T09:15:00Z ");

        form.setFormalReference(
                " AUTH-2026-0042 ");

        return form;
    }

}