package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class EventAuthorizationTest {

    private static final EventAuthorizationId AUTHORIZATION_ID =
            new EventAuthorizationId(
                    UUID.fromString(
                            "844b8db1-2e79-4524-9c30-e679dc730001"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "844b8db1-2e79-4524-9c30-e679dc730002"));

    private static final Instant AUTHORIZED_AT =
            Instant.parse(
                    "2026-07-23T13:00:00Z");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-23T14:00:00Z");

    private static final Instant DEACTIVATED_AT =
            Instant.parse(
                    "2026-07-23T15:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Test
    void createsActiveAuthorizationWithInitialRevision() {
        EventAuthorization authorization =
                EventAuthorization.create(
                        AUTHORIZATION_ID,
                        EVENT_ID,
                        "  Jefatura de Operaciones  ",
                        "  Excepción aprobada por contingencia  ",
                        AUTHORIZED_AT,
                        "  AUT-2026-0007  ",
                        CREATED_AT,
                        ACTOR);

        assertThat(authorization.id())
                .isEqualTo(
                        AUTHORIZATION_ID);

        assertThat(authorization.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(authorization.authorizedByName())
                .isEqualTo(
                        "Jefatura de Operaciones");

        assertThat(authorization.reason())
                .isEqualTo(
                        "Excepción aprobada por contingencia");

        assertThat(authorization.formalReference())
                .isEqualTo(
                        "AUT-2026-0007");

        assertThat(authorization.authorizedAt())
                .isEqualTo(
                        AUTHORIZED_AT);

        assertThat(authorization.active())
                .isTrue();

        assertThat(authorization.revision())
                .isEqualTo(1L);

        assertThat(authorization.createdAt())
                .isEqualTo(
                        CREATED_AT);

        assertThat(authorization.updatedAt())
                .isEqualTo(
                        CREATED_AT);

        assertThat(authorization.createdBy())
                .isEqualTo(
                        ACTOR);

        assertThat(authorization.updatedBy())
                .isEqualTo(
                        ACTOR);

        assertThat(authorization.deactivatedAt())
                .isNull();
    }

    @Test
    void deactivatesLogicallyAndIncrementsRevision() {
        EventAuthorization activeAuthorization =
                activeAuthorization();

        EventAuthorization deactivatedAuthorization =
                activeAuthorization.deactivate(
                        DEACTIVATED_AT,
                        ACTOR);

        assertThat(deactivatedAuthorization.id())
                .isEqualTo(
                        activeAuthorization.id());

        assertThat(deactivatedAuthorization.eventId())
                .isEqualTo(
                        activeAuthorization.eventId());

        assertThat(deactivatedAuthorization.active())
                .isFalse();

        assertThat(deactivatedAuthorization.revision())
                .isEqualTo(2L);

        assertThat(deactivatedAuthorization.deactivatedAt())
                .isEqualTo(
                        DEACTIVATED_AT);

        assertThat(deactivatedAuthorization.updatedAt())
                .isEqualTo(
                        DEACTIVATED_AT);

        assertThat(deactivatedAuthorization.updatedBy())
                .isEqualTo(
                        ACTOR);

        assertThat(deactivatedAuthorization.createdAt())
                .isEqualTo(
                        CREATED_AT);

        assertThat(deactivatedAuthorization.createdBy())
                .isEqualTo(
                        ACTOR);
    }

    @Test
    void rejectsSecondDeactivation() {
        EventAuthorization deactivatedAuthorization =
                activeAuthorization()
                        .deactivate(
                                DEACTIVATED_AT,
                                ACTOR);

        assertThatThrownBy(
                () -> deactivatedAuthorization.deactivate(
                        DEACTIVATED_AT.plusSeconds(60),
                        ACTOR))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessageContaining(
                        "already inactive");
    }

    @Test
    void rejectsDeactivationBeforePreviousUpdate() {
        EventAuthorization authorization =
                activeAuthorization();

        assertThatThrownBy(
                () -> authorization.deactivate(
                        CREATED_AT.minusSeconds(1),
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "previous update");
    }

    @Test
    void rejectsBlankRequiredText() {
        assertThatThrownBy(
                () -> createAuthorization(
                        "   ",
                        "Reason",
                        "AUT-1"))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "authorizedByName");

        assertThatThrownBy(
                () -> createAuthorization(
                        "Manager",
                        "   ",
                        "AUT-1"))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "reason");

        assertThatThrownBy(
                () -> createAuthorization(
                        "Manager",
                        "Reason",
                        "   "))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "formalReference");
    }

    @Test
    void rejectsTextThatExceedsApprovedLengths() {
        assertThatThrownBy(
                () -> createAuthorization(
                        "X".repeat(201),
                        "Reason",
                        "AUT-1"))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "200 characters");

        assertThatThrownBy(
                () -> createAuthorization(
                        "Manager",
                        "Reason",
                        "X".repeat(501)))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "500 characters");
    }

    @Test
    void rejectsInconsistentActivityMetadata() {
        assertThatThrownBy(
                () -> new EventAuthorization(
                        AUTHORIZATION_ID,
                        EVENT_ID,
                        "Manager",
                        "Reason",
                        AUTHORIZED_AT,
                        "AUT-1",
                        true,
                        1L,
                        CREATED_AT,
                        ACTOR,
                        CREATED_AT,
                        ACTOR,
                        DEACTIVATED_AT))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "active event authorization");

        assertThatThrownBy(
                () -> new EventAuthorization(
                        AUTHORIZATION_ID,
                        EVENT_ID,
                        "Manager",
                        "Reason",
                        AUTHORIZED_AT,
                        "AUT-1",
                        false,
                        2L,
                        CREATED_AT,
                        ACTOR,
                        DEACTIVATED_AT,
                        ACTOR,
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "deactivation instant");
    }

    @Test
    void rejectsInvalidRevisionAndTemporalMetadata() {
        assertThatThrownBy(
                () -> new EventAuthorization(
                        AUTHORIZATION_ID,
                        EVENT_ID,
                        "Manager",
                        "Reason",
                        AUTHORIZED_AT,
                        "AUT-1",
                        true,
                        0L,
                        CREATED_AT,
                        ACTOR,
                        CREATED_AT,
                        ACTOR,
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "revision");

        assertThatThrownBy(
                () -> new EventAuthorization(
                        AUTHORIZATION_ID,
                        EVENT_ID,
                        "Manager",
                        "Reason",
                        AUTHORIZED_AT,
                        "AUT-1",
                        true,
                        1L,
                        CREATED_AT,
                        ACTOR,
                        CREATED_AT.minusSeconds(1),
                        ACTOR,
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "before creation");
    }

    @Test
    void rejectsRevisionOverflowDuringDeactivation() {
        EventAuthorization authorization =
                new EventAuthorization(
                        AUTHORIZATION_ID,
                        EVENT_ID,
                        "Manager",
                        "Reason",
                        AUTHORIZED_AT,
                        "AUT-1",
                        true,
                        Long.MAX_VALUE,
                        CREATED_AT,
                        ACTOR,
                        CREATED_AT,
                        ACTOR,
                        null);

        assertThatThrownBy(
                () -> authorization.deactivate(
                        DEACTIVATED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessageContaining(
                        "cannot be incremented");
    }

    private static EventAuthorization activeAuthorization() {
        return EventAuthorization.create(
                AUTHORIZATION_ID,
                EVENT_ID,
                "Jefatura de Operaciones",
                "Excepción aprobada por contingencia",
                AUTHORIZED_AT,
                "AUT-2026-0007",
                CREATED_AT,
                ACTOR);
    }

    private static EventAuthorization createAuthorization(
            String authorizedByName,
            String reason,
            String formalReference) {

        return EventAuthorization.create(
                AUTHORIZATION_ID,
                EVENT_ID,
                authorizedByName,
                reason,
                AUTHORIZED_AT,
                formalReference,
                CREATED_AT,
                ACTOR);
    }

}