package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ValidateOperationalEventResultTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "a95e4a29-8f53-47ee-9f75-15170f100001");

    @Test
    void createsValidatedResult() {
        ValidateOperationalEventResult result =
                ValidateOperationalEventResult.validated(
                        EVENT_ID);

        assertThat(
                result.status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .VALIDATED);

        assertThat(
                result.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(
                result.message())
                .isNull();
    }

    @Test
    void createsPersistedValidationFailureResult() {
        ValidateOperationalEventResult result =
                ValidateOperationalEventResult
                        .validationFailed(
                                EVENT_ID,
                                "The event contains failed validation rules.");

        assertThat(
                result.status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .VALIDATION_FAILED);

        assertThat(
                result.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(
                result.message())
                .isEqualTo(
                        "The event contains failed validation rules.");
    }

    @Test
    void rejectsMissingEventIdWhenValidationWasExecuted() {
        assertThatThrownBy(
                () -> new ValidateOperationalEventResult(
                        ValidateOperationalEventResult.Status
                                .VALIDATED,
                        null,
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "executed validation result must contain eventId");

        assertThatThrownBy(
                () -> new ValidateOperationalEventResult(
                        ValidateOperationalEventResult.Status
                                .VALIDATION_FAILED,
                        null,
                        "Validation failed."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "executed validation result must contain eventId");
    }

    @Test
    void rejectsEventIdWhenValidationWasNotExecuted() {
        assertThatThrownBy(
                () -> new ValidateOperationalEventResult(
                        ValidateOperationalEventResult.Status
                                .EVENT_NOT_FOUND,
                        EVENT_ID,
                        "The event does not exist."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "non-executed validation result must not contain eventId");
    }

    @Test
    void rejectsMessageForValidatedResult() {
        assertThatThrownBy(
                () -> new ValidateOperationalEventResult(
                        ValidateOperationalEventResult.Status
                                .VALIDATED,
                        EVENT_ID,
                        "Unexpected message."))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "validated result must not contain message");
    }

    @Test
    void rejectsMissingOrBlankMessageForNonValidatedResult() {
        assertThatThrownBy(
                () -> new ValidateOperationalEventResult(
                        ValidateOperationalEventResult.Status
                                .VALIDATION_FAILED,
                        EVENT_ID,
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "non-validated result must contain message");

        assertThatThrownBy(
                () -> new ValidateOperationalEventResult(
                        ValidateOperationalEventResult.Status
                                .INVALID_INPUT,
                        null,
                        "   "))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "non-validated result must contain message");
    }

    @Test
    void createsExplicitNonExecutedResults() {
        assertThat(
                ValidateOperationalEventResult
                        .invalidInput(
                                "Invalid identifiers.")
                        .status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .INVALID_INPUT);

        assertThat(
                ValidateOperationalEventResult
                        .actorRejected()
                        .status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .ACTOR_REJECTED);

        assertThat(
                ValidateOperationalEventResult
                        .closeNotFound()
                        .status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .CLOSE_NOT_FOUND);

        assertThat(
                ValidateOperationalEventResult
                        .closeNotEditable()
                        .status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .CLOSE_NOT_EDITABLE);

        assertThat(
                ValidateOperationalEventResult
                        .eventNotFound()
                        .status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .EVENT_NOT_FOUND);
    }

}