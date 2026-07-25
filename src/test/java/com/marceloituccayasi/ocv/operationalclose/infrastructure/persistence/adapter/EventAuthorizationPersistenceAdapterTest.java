package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorizationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.EventAuthorizationEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.EventAuthorizationPersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.EventAuthorizationJpaRepository;

class EventAuthorizationPersistenceAdapterTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "91c9f587-5f5e-4f31-a827-d23c0f400001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "91c9f587-5f5e-4f31-a827-d23c0f400002");

    private static final UUID FIRST_AUTHORIZATION_ID =
            UUID.fromString(
                    "91c9f587-5f5e-4f31-a827-d23c0f400003");

    private static final UUID SECOND_AUTHORIZATION_ID =
            UUID.fromString(
                    "91c9f587-5f5e-4f31-a827-d23c0f400004");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-23T18:00:00Z");

    private final EventAuthorizationJpaRepository
            eventAuthorizationJpaRepository =
                    mock(
                            EventAuthorizationJpaRepository.class);

    private final EventAuthorizationPersistenceMapper mapper =
            new EventAuthorizationPersistenceMapper();

    private final EventAuthorizationPersistenceAdapter adapter =
            new EventAuthorizationPersistenceAdapter(
                    eventAuthorizationJpaRepository,
                    mapper);

    @Test
    void savesNewAuthorizationAndSubsequentRevision() {
        EventAuthorization authorization =
                authorization(
                        FIRST_AUTHORIZATION_ID,
                        NOW.minusSeconds(
                                60));

        adapter.saveNew(
                authorization);

        adapter.saveRevision(
                authorization);

        verify(
                eventAuthorizationJpaRepository,
                times(2))
                .saveAndFlush(
                        any(
                                EventAuthorizationEntity.class));
    }

    @Test
    void returnsMappedAuthorizationById() {
        EventAuthorization expected =
                authorization(
                        FIRST_AUTHORIZATION_ID,
                        NOW.minusSeconds(
                                60));

        when(
                eventAuthorizationJpaRepository.findById(
                        FIRST_AUTHORIZATION_ID))
                .thenReturn(
                        Optional.of(
                                mapper.toEntity(
                                        expected)));

        Optional<EventAuthorization> result =
                adapter.findById(
                        new EventAuthorizationId(
                                FIRST_AUTHORIZATION_ID));

        assertThat(result)
                .isPresent();

        assertThat(
                result.orElseThrow()
                        .id()
                        .value())
                .isEqualTo(
                        FIRST_AUTHORIZATION_ID);

        assertThat(
                result.orElseThrow()
                        .eventId()
                        .value())
                .isEqualTo(
                        EVENT_ID);

        assertThat(
                result.orElseThrow()
                        .formalReference())
                .isEqualTo(
                        "AUTH-"
                                + FIRST_AUTHORIZATION_ID);
    }

    @Test
    void preservesRepositoryAuthorizationOrder() {
        EventAuthorization first =
                authorization(
                        FIRST_AUTHORIZATION_ID,
                        NOW.minusSeconds(
                                60));

        EventAuthorization second =
                authorization(
                        SECOND_AUTHORIZATION_ID,
                        NOW.minusSeconds(
                                120));

        when(
                eventAuthorizationJpaRepository
                        .findAllByEventIdOrderByAuthorizedAtDescCreatedAtDescIdDesc(
                                EVENT_ID))
                .thenReturn(
                        List.of(
                                mapper.toEntity(
                                        first),
                                mapper.toEntity(
                                        second)));

        List<EventAuthorization> result =
                adapter
                        .findAllByEventIdOrderByAuthorizedAtDescending(
                                new OperationalEventId(
                                        EVENT_ID));

        assertThat(result)
                .hasSize(2);

        assertThat(
                result.get(0)
                        .id()
                        .value())
                .isEqualTo(
                        FIRST_AUTHORIZATION_ID);

        assertThat(
                result.get(1)
                        .id()
                        .value())
                .isEqualTo(
                        SECOND_AUTHORIZATION_ID);
    }

    @Test
    void scopesAuthorizationLockByCloseEventAndAuthorizationId() {
        EventAuthorization expected =
                authorization(
                        FIRST_AUTHORIZATION_ID,
                        java.time.Instant.parse(
                                "2026-07-23T17:55:00Z"));

        when(
                eventAuthorizationJpaRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID,
                        FIRST_AUTHORIZATION_ID))
                .thenReturn(
                        Optional.of(
                                mapper.toEntity(
                                        expected)));

        Optional<EventAuthorization> result =
                adapter.findByIdForUpdate(
                        new OperationalCloseId(
                                CLOSE_ID),
                        new OperationalEventId(
                                EVENT_ID),
                        new EventAuthorizationId(
                                FIRST_AUTHORIZATION_ID));

        assertThat(result)
                .isPresent();

        verify(
                eventAuthorizationJpaRepository)
                .findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID,
                        FIRST_AUTHORIZATION_ID);
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(
                () -> adapter.saveNew(
                        null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.findById(
                        null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter
                        .findAllByEventIdOrderByAuthorizedAtDescending(
                                null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.findByIdForUpdate(
                        null,
                        new OperationalEventId(
                                EVENT_ID),
                        new EventAuthorizationId(
                                FIRST_AUTHORIZATION_ID)))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.findByIdForUpdate(
                        new OperationalCloseId(
                                CLOSE_ID),
                        null,
                        new EventAuthorizationId(
                                FIRST_AUTHORIZATION_ID)))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.findByIdForUpdate(
                        new OperationalCloseId(
                                CLOSE_ID),
                        new OperationalEventId(
                                EVENT_ID),
                        null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.saveRevision(
                        null))
                .isInstanceOf(
                        NullPointerException.class);
    }

    private static EventAuthorization authorization(
            UUID authorizationId,
            Instant authorizedAt) {

        return EventAuthorization.create(
                new EventAuthorizationId(
                        authorizationId),
                new OperationalEventId(
                        EVENT_ID),
                "Jefatura de Operaciones",
                "Excepción autorizada por contingencia",
                authorizedAt,
                "AUTH-"
                        + authorizationId,
                NOW,
                actor());
    }

    private static AuditActor actor() {
        return new AuditActor(
                "responsible-user",
                "responsible");
    }

}