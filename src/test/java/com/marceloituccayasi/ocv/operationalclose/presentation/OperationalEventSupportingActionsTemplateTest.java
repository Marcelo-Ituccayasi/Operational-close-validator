package com.marceloituccayasi.ocv.operationalclose.presentation;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalEventSupportingActionsTemplateTest {

    private static final Path TEMPLATE =
            Path.of(
                    "src/main/resources/templates/events/detail.html");

    @Test
    void exposesEvidenceDeactivationOnlyForEditableActiveEvidence()
            throws IOException {

        String content =
                Files.readString(
                        TEMPLATE,
                        UTF_8);

        assertThat(content)
                .contains(
                        "th:if=\"${closeEditable and evidence.active}\"")
                .contains(
                        "method=\"post\"")
                .contains(
                        "th:action=\"@{/closes/{closeId}/events/{eventId}/supporting-evidence/{evidenceId}/deactivate(closeId=${closeId},eventId=${event.id},evidenceId=${evidence.id})}\"")
                .contains(
                        "Desactivar evidencia");
    }

    @Test
    void exposesAuthorizationDeactivationOnlyForEditableActiveAuthorization()
            throws IOException {

        String content =
                Files.readString(
                        TEMPLATE,
                        UTF_8);

        assertThat(content)
                .contains(
                        "th:if=\"${closeEditable and authorization.active}\"")
                .contains(
                        "th:action=\"@{/closes/{closeId}/events/{eventId}/authorizations/{authorizationId}/deactivate(closeId=${closeId},eventId=${event.id},authorizationId=${authorization.id})}\"")
                .contains(
                        "Desactivar autorización");
    }

}