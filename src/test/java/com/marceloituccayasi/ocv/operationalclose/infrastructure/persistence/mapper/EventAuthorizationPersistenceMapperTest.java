package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorizationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.EventAuthorizationEntity;

class EventAuthorizationPersistenceMapperTest {

    private static final UUID AUTHORIZATION_ID =
            UUID.fromString(
                    "0de4804d-963d-43a0-940e-0cc622940001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "0de4804d-963d-43a0-940e-0cc622940002");

    private static final Instant AUTHORIZED_AT =
            Instant.parse(
                    "2026-07-23T15:00:00Z");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-23T16:00:00Z");

    private static final Instant UPDATED_AT =
            Instant.parse(
                    "2026-07-23T17:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    "responsible-user",
                    "responsible");

    private final EventAuthorizationPersistenceMapper mapper =
            new EventAuthorizationPersistenceMapper();

    @Test
    void mapsDomainAuthorizationToEntity() {
        EventAuthorization authorization =
                EventAuthorization.create(
                        new EventAuthorizationId(
                                AUTHORIZATION_ID),
                        new OperationalEventId(
                                EVENT_ID),
                        "Jefatura de Operaciones",
                        "Excepción aprobada por contingencia",
                        AUTHORIZED_AT,
                        "AUT-2026-0007",
                        CREATED_AT,
                        ACTOR);

        EventAuthorizationEntity entity =
                mapper.toEntity(
                        authorization);

        assertThat(entity.id())
                .isEqualTo(
                        AUTHORIZATION_ID);

        assertThat(entity.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(entity.authorizedByName())
                .isEqualTo(
                        "Jefatura de Operaciones");

        assertThat(entity.reason())
                .isEqualTo(
                        "Excepción aprobada por contingencia");

        assertThat(entity.authorizedAt())
                .isEqualTo(
                        AUTHORIZED_AT);

        assertThat(entity.formalReference())
                .isEqualTo(
                        "AUT-2026-0007");

        assertThat(entity.active())
                .isTrue();

        assertThat(entity.revision())
                .isEqualTo(1L);

        assertThat(entity.createdByUserId())
                .isEqualTo(
                        "responsible-user");

        assertThat(entity.createdByUsername())
                .isEqualTo(
                        "responsible");

        assertThat(entity.deactivatedAt())
                .isNull();
    }

    @Test
    void mapsInactiveEntityToDomainAuthorization() {
        EventAuthorizationEntity entity =
                EventAuthorizationEntity.create(
                        AUTHORIZATION_ID,
                        EVENT_ID,
                        "Gerencia de Operaciones",
                        "Autorización formal por contingencia",
                        AUTHORIZED_AT,
                        "AUT-2026-0008",
                        false,
                        2L,
                        CREATED_AT,
                        "responsible-user",
                        "responsible",
                        UPDATED_AT,
                        "responsible-user",
                        "responsible",
                        UPDATED_AT);

        EventAuthorization authorization =
                mapper.toDomain(
                        entity);

        assertThat(authorization.id().value())
                .isEqualTo(
                        AUTHORIZATION_ID);

        assertThat(authorization.eventId().value())
                .isEqualTo(
                        EVENT_ID);

        assertThat(authorization.authorizedByName())
                .isEqualTo(
                        "Gerencia de Operaciones");

        assertThat(authorization.reason())
                .isEqualTo(
                        "Autorización formal por contingencia");

        assertThat(authorization.authorizedAt())
                .isEqualTo(
                        AUTHORIZED_AT);

        assertThat(authorization.formalReference())
                .isEqualTo(
                        "AUT-2026-0008");

        assertThat(authorization.active())
                .isFalse();

        assertThat(authorization.revision())
                .isEqualTo(2L);

        assertThat(authorization.createdAt())
                .isEqualTo(
                        CREATED_AT);

        assertThat(authorization.updatedAt())
                .isEqualTo(
                        UPDATED_AT);

        assertThat(authorization.createdBy())
                .isEqualTo(
                        ACTOR);

        assertThat(authorization.updatedBy())
                .isEqualTo(
                        ACTOR);

        assertThat(authorization.deactivatedAt())
                .isEqualTo(
                        UPDATED_AT);
    }

    @Test
    void rejectsNullMappingArguments() {
        assertThatThrownBy(
                () -> mapper.toEntity(
                        null))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessageContaining(
                        "eventAuthorization");

        assertThatThrownBy(
                () -> mapper.toDomain(
                        null))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessageContaining(
                        "entity");
    }

}