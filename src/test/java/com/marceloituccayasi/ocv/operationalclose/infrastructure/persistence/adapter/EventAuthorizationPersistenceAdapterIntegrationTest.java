package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventAuthorizationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorizationId;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EventAuthorizationPersistenceAdapterIntegrationTest {

    private static final UUID FIRST_CLOSE_ID =
            UUID.fromString(
                    "04a9293e-9d45-4028-87b0-469414100001");

    private static final UUID FIRST_CLOSE_TRANSITION_ID =
            UUID.fromString(
                    "04a9293e-9d45-4028-87b0-469414100002");

    private static final UUID SECOND_CLOSE_ID =
            UUID.fromString(
                    "04a9293e-9d45-4028-87b0-469414100003");

    private static final UUID SECOND_CLOSE_TRANSITION_ID =
            UUID.fromString(
                    "04a9293e-9d45-4028-87b0-469414100004");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "04a9293e-9d45-4028-87b0-469414100005");

    private static final UUID EVENT_TRANSITION_ID =
            UUID.fromString(
                    "04a9293e-9d45-4028-87b0-469414100006");

    private static final UUID FIRST_AUTHORIZATION_ID =
            UUID.fromString(
                    "04a9293e-9d45-4028-87b0-469414100007");

    private static final UUID SECOND_AUTHORIZATION_ID =
            UUID.fromString(
                    "04a9293e-9d45-4028-87b0-469414100008");

    private static final UUID OTHER_EVENT_ID =
            UUID.fromString(
                    "04a9293e-9d45-4028-87b0-469414100009");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-23T18:00:00Z");

    private static final Instant UPDATED_AT =
            Instant.parse(
                    "2026-07-23T19:00:00Z");

    private static final Instant OCCURRED_AT =
            Instant.parse(
                    "2026-07-23T17:30:00Z");

    @Autowired
    private OperationalCloseRepository closeRepository;

    @Autowired
    private OperationalEventRepository eventRepository;

    @Autowired
    private EventAuthorizationRepository authorizationRepository;

    @Autowired
    private TransactionRunner transactionRunner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanBeforeTest() {
        cleanOperationalCloseTables();
    }

    @AfterEach
    void cleanAfterTest() {
        cleanOperationalCloseTables();
    }

    @Test
    void persistsAndReconstructsEventAuthorization() {
        persistParentGraph();

        EventAuthorization authorization =
                activeAuthorization(
                        FIRST_AUTHORIZATION_ID,
                        CREATED_AT.minusSeconds(
                                300),
                        "AUTH-2026-0001");

        transactionRunner.execute(
                (Runnable) () ->
                        authorizationRepository.saveNew(
                                authorization));

        Optional<EventAuthorization> loaded =
                authorizationRepository.findById(
                        new EventAuthorizationId(
                                FIRST_AUTHORIZATION_ID));

        Long persistedRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.event_authorization
                        WHERE id = ?
                          AND event_id = ?
                          AND authorized_by_name =
                              'Jefatura de Operaciones'
                          AND reason =
                              'Excepción aprobada por contingencia'
                          AND formal_reference =
                              'AUTH-2026-0001'
                          AND is_active = TRUE
                          AND revision = 1
                          AND deactivated_at IS NULL
                        """,
                        Long.class,
                        FIRST_AUTHORIZATION_ID,
                        EVENT_ID);

        assertThat(persistedRows)
                .isEqualTo(1L);

        assertThat(loaded)
                .isPresent();

        EventAuthorization reconstructed =
                loaded.orElseThrow();

        assertThat(reconstructed.id().value())
                .isEqualTo(
                        FIRST_AUTHORIZATION_ID);

        assertThat(reconstructed.eventId().value())
                .isEqualTo(
                        EVENT_ID);

        assertThat(reconstructed.authorizedByName())
                .isEqualTo(
                        "Jefatura de Operaciones");

        assertThat(reconstructed.reason())
                .isEqualTo(
                        "Excepción aprobada por contingencia");

        assertThat(reconstructed.authorizedAt())
                .isEqualTo(
                        CREATED_AT.minusSeconds(
                                300));

        assertThat(reconstructed.formalReference())
                .isEqualTo(
                        "AUTH-2026-0001");

        assertThat(reconstructed.active())
                .isTrue();

        assertThat(reconstructed.revision())
                .isEqualTo(1L);

        assertThat(reconstructed.createdBy())
                .isEqualTo(
                        actor());
    }

    @Test
    void preservesDescendingAuthorizationTimeOrder() {
        persistParentGraph();

        EventAuthorization olderAuthorization =
                activeAuthorization(
                        FIRST_AUTHORIZATION_ID,
                        CREATED_AT.minusSeconds(
                                600),
                        "AUTH-2026-0001");

        EventAuthorization newerAuthorization =
                activeAuthorization(
                        SECOND_AUTHORIZATION_ID,
                        CREATED_AT.minusSeconds(
                                300),
                        "AUTH-2026-0002");

        transactionRunner.execute(
                (Runnable) () -> {
                    authorizationRepository.saveNew(
                            olderAuthorization);

                    authorizationRepository.saveNew(
                            newerAuthorization);
                });

        List<EventAuthorization> result =
                authorizationRepository
                        .findAllByEventIdOrderByAuthorizedAtDescending(
                                new OperationalEventId(
                                        EVENT_ID));

        assertThat(result)
                .hasSize(2);

        assertThat(result.get(0).id().value())
                .isEqualTo(
                        SECOND_AUTHORIZATION_ID);

        assertThat(result.get(1).id().value())
                .isEqualTo(
                        FIRST_AUTHORIZATION_ID);
    }

    @Test
    void persistsInactiveAuthorizationRevisionWithoutDeletingHistory() {
        persistParentGraph();

        EventAuthorization activeAuthorization =
                activeAuthorization(
                        FIRST_AUTHORIZATION_ID,
                        CREATED_AT.minusSeconds(
                                300),
                        "AUTH-2026-0001");

        transactionRunner.execute(
                (Runnable) () ->
                        authorizationRepository.saveNew(
                                activeAuthorization));

        EventAuthorization inactiveRevision =
                activeAuthorization.deactivate(
                        UPDATED_AT,
                        actor());

        transactionRunner.execute(
                (Runnable) () ->
                        authorizationRepository.saveRevision(
                                inactiveRevision));

        Long persistedRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.event_authorization
                        WHERE id = ?
                        """,
                        Long.class,
                        FIRST_AUTHORIZATION_ID);

        Long inactiveRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.event_authorization
                        WHERE id = ?
                          AND is_active = FALSE
                          AND revision = 2
                          AND deactivated_at IS NOT NULL
                        """,
                        Long.class,
                        FIRST_AUTHORIZATION_ID);

        assertThat(persistedRows)
                .isEqualTo(1L);

        assertThat(inactiveRows)
                .isEqualTo(1L);

        EventAuthorization loaded =
                authorizationRepository.findById(
                                activeAuthorization.id())
                        .orElseThrow();

        assertThat(loaded.active())
                .isFalse();

        assertThat(loaded.revision())
                .isEqualTo(2L);

        assertThat(loaded.deactivatedAt())
                .isEqualTo(
                        UPDATED_AT);

        assertThat(loaded.authorizedByName())
                .isEqualTo(
                        activeAuthorization.authorizedByName());

        assertThat(loaded.reason())
                .isEqualTo(
                        activeAuthorization.reason());

        assertThat(loaded.formalReference())
                .isEqualTo(
                        activeAuthorization.formalReference());
    }

    @Test
    void scopesPessimisticLookupToOwningCloseAndEvent() {
        persistParentGraph();

        EventAuthorization authorization =
                activeAuthorization(
                        FIRST_AUTHORIZATION_ID,
                        CREATED_AT.minusSeconds(
                                300),
                        "AUTH-2026-0001");

        transactionRunner.execute(
                (Runnable) () ->
                        authorizationRepository.saveNew(
                                authorization));

        Optional<EventAuthorization> owningResult =
                transactionRunner.execute(
                        () -> authorizationRepository
                                .findByIdForUpdate(
                                        new OperationalCloseId(
                                                FIRST_CLOSE_ID),
                                        new OperationalEventId(
                                                EVENT_ID),
                                        authorization.id()));

        Optional<EventAuthorization> otherCloseResult =
                transactionRunner.execute(
                        () -> authorizationRepository
                                .findByIdForUpdate(
                                        new OperationalCloseId(
                                                SECOND_CLOSE_ID),
                                        new OperationalEventId(
                                                EVENT_ID),
                                        authorization.id()));

        Optional<EventAuthorization> otherEventResult =
                transactionRunner.execute(
                        () -> authorizationRepository
                                .findByIdForUpdate(
                                        new OperationalCloseId(
                                                FIRST_CLOSE_ID),
                                        new OperationalEventId(
                                                OTHER_EVENT_ID),
                                        authorization.id()));

        assertThat(owningResult)
                .isPresent();

        assertThat(otherCloseResult)
                .isEmpty();

        assertThat(otherEventResult)
                .isEmpty();
    }

    private void persistParentGraph() {
        persistClose(
                FIRST_CLOSE_ID,
                FIRST_CLOSE_TRANSITION_ID,
                LocalDate.of(
                        2026,
                        7,
                        1),
                LocalDate.of(
                        2026,
                        7,
                        31));

        persistClose(
                SECOND_CLOSE_ID,
                SECOND_CLOSE_TRANSITION_ID,
                LocalDate.of(
                        2026,
                        8,
                        1),
                LocalDate.of(
                        2026,
                        8,
                        31));

        OperationalEvent event =
                OperationalEvent.create(
                        new OperationalEventId(
                                EVENT_ID),
                        new OperationalCloseId(
                                FIRST_CLOSE_ID),
                        OperationalEventType.EXPENSE,
                        new OperationalEventAmount(
                                new BigDecimal(
                                        "125.5000")),
                        OCCURRED_AT,
                        "Caja principal",
                        "Evento padre para autorización",
                        false,
                        true,
                        CREATED_AT,
                        actor());

        EventStateTransition transition =
                EventStateTransition.initial(
                        new EventStateTransitionId(
                                EVENT_TRANSITION_ID),
                        event.id(),
                        CREATED_AT,
                        actor());

        transactionRunner.execute(
                (Runnable) () ->
                        eventRepository.saveNew(
                                event,
                                transition));
    }

    private void persistClose(
            UUID closeId,
            UUID transitionId,
            LocalDate periodStart,
            LocalDate periodEnd) {

        OperationalClose operationalClose =
                OperationalClose.create(
                        new OperationalCloseId(
                                closeId),
                        new OperationalPeriod(
                                periodStart,
                                periodEnd),
                        new CurrencyCode(
                                "PEN"),
                        new InitialBalance(
                                new BigDecimal(
                                        "1000.0000")),
                        CREATED_AT,
                        actor());

        CloseStateTransition transition =
                CloseStateTransition.initial(
                        new CloseStateTransitionId(
                                transitionId),
                        operationalClose.id(),
                        CREATED_AT,
                        actor());

        transactionRunner.execute(
                (Runnable) () ->
                        closeRepository.saveNew(
                                operationalClose,
                                transition));
    }

    private static EventAuthorization activeAuthorization(
            UUID authorizationId,
            Instant authorizedAt,
            String formalReference) {

        return EventAuthorization.create(
                new EventAuthorizationId(
                        authorizationId),
                new OperationalEventId(
                        EVENT_ID),
                "Jefatura de Operaciones",
                "Excepción aprobada por contingencia",
                authorizedAt,
                formalReference,
                CREATED_AT,
                actor());
    }

    private static AuditActor actor() {
        return new AuditActor(
                "responsible-user",
                "responsible");
    }

    private void cleanOperationalCloseTables() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    ocv.validation_result,
                    ocv.supporting_evidence,
                    ocv.event_authorization,
                    ocv.event_state_transition,
                    ocv.operational_event,
                    ocv.close_state_transition,
                    ocv.operational_close
                """);
    }

}