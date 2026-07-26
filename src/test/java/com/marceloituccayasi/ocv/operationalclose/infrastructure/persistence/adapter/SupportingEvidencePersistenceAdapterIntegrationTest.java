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
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.SupportingEvidenceRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
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
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceLegibilityStatus;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SupportingEvidencePersistenceAdapterIntegrationTest {

    private static final UUID FIRST_CLOSE_ID =
            UUID.fromString(
                    "e8b93e46-ea44-4ef7-8d34-7cc975100001");

    private static final UUID FIRST_CLOSE_TRANSITION_ID =
            UUID.fromString(
                    "e8b93e46-ea44-4ef7-8d34-7cc975100002");

    private static final UUID SECOND_CLOSE_ID =
            UUID.fromString(
                    "e8b93e46-ea44-4ef7-8d34-7cc975100003");

    private static final UUID SECOND_CLOSE_TRANSITION_ID =
            UUID.fromString(
                    "e8b93e46-ea44-4ef7-8d34-7cc975100004");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "e8b93e46-ea44-4ef7-8d34-7cc975100005");

    private static final UUID EVENT_TRANSITION_ID =
            UUID.fromString(
                    "e8b93e46-ea44-4ef7-8d34-7cc975100006");

    private static final UUID FIRST_EVIDENCE_ID =
            UUID.fromString(
                    "e8b93e46-ea44-4ef7-8d34-7cc975100007");

    private static final UUID SECOND_EVIDENCE_ID =
            UUID.fromString(
                    "e8b93e46-ea44-4ef7-8d34-7cc975100008");

    private static final UUID OTHER_EVENT_ID =
            UUID.fromString(
                    "e8b93e46-ea44-4ef7-8d34-7cc975100009");

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
    private SupportingEvidenceRepository evidenceRepository;

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
    void persistsAndReconstructsSupportingEvidence() {
        persistParentGraph();

        SupportingEvidence evidence =
                activeEvidence(
                        FIRST_EVIDENCE_ID,
                        LocalDate.of(
                                2026,
                                7,
                                23),
                        "stored:evidence/receipt.pdf",
                        SupportingEvidenceLegibilityStatus.LEGIBLE);

        transactionRunner.execute(
                (Runnable) () ->
                        evidenceRepository.saveNew(
                                evidence));

        Optional<SupportingEvidence> loaded =
                evidenceRepository.findById(
                        new SupportingEvidenceId(
                                FIRST_EVIDENCE_ID));

        Long persistedRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.supporting_evidence
                        WHERE id = ?
                          AND event_id = ?
                          AND evidence_type = 'RECEIPT'
                          AND content_reference =
                              'stored:evidence/receipt.pdf'
                          AND supported_amount = 125.5000
                          AND legibility_status = 'LEGIBLE'
                          AND is_active = TRUE
                          AND revision = 1
                          AND deactivated_at IS NULL
                        """,
                        Long.class,
                        FIRST_EVIDENCE_ID,
                        EVENT_ID);

        assertThat(persistedRows)
                .isEqualTo(1L);

        assertThat(loaded)
                .isPresent();

        SupportingEvidence reconstructed =
                loaded.orElseThrow();

        assertThat(reconstructed.id().value())
                .isEqualTo(
                        FIRST_EVIDENCE_ID);

        assertThat(reconstructed.eventId().value())
                .isEqualTo(
                        EVENT_ID);

        assertThat(reconstructed.evidenceType())
                .isEqualTo(
                        "RECEIPT");

        assertThat(reconstructed.contentReference())
                .isEqualTo(
                        "stored:evidence/receipt.pdf");

        assertThat(reconstructed.supportedAmount())
                .isEqualByComparingTo(
                        "125.5000");

        assertThat(reconstructed.evidenceDate())
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                7,
                                23));

        assertThat(reconstructed.legibilityStatus())
                .isEqualTo(
                        SupportingEvidenceLegibilityStatus.LEGIBLE);

        assertThat(reconstructed.active())
                .isTrue();

        assertThat(reconstructed.revision())
                .isEqualTo(1L);

        assertThat(reconstructed.createdBy())
                .isEqualTo(
                        actor());
    }

    @Test
    void preservesDescendingEvidenceDateOrder() {
        persistParentGraph();

        SupportingEvidence olderEvidence =
                activeEvidence(
                        FIRST_EVIDENCE_ID,
                        LocalDate.of(
                                2026,
                                7,
                                21),
                        "reference:older-evidence",
                        SupportingEvidenceLegibilityStatus.UNVERIFIED);

        SupportingEvidence newerEvidence =
                activeEvidence(
                        SECOND_EVIDENCE_ID,
                        LocalDate.of(
                                2026,
                                7,
                                23),
                        "reference:newer-evidence",
                        SupportingEvidenceLegibilityStatus.UNVERIFIED);

        transactionRunner.execute(
                (Runnable) () -> {
                    evidenceRepository.saveNew(
                            olderEvidence);

                    evidenceRepository.saveNew(
                            newerEvidence);
                });

        List<SupportingEvidence> result =
                evidenceRepository
                        .findAllByEventIdOrderByEvidenceDateDescending(
                                new OperationalEventId(
                                        EVENT_ID));

        assertThat(result)
                .hasSize(2);

        assertThat(result.get(0).id().value())
                .isEqualTo(
                        SECOND_EVIDENCE_ID);

        assertThat(result.get(1).id().value())
                .isEqualTo(
                        FIRST_EVIDENCE_ID);
    }

    @Test
    void persistsInactiveEvidenceRevisionWithoutDeletingHistory() {
        persistParentGraph();

        SupportingEvidence activeEvidence =
                activeEvidence(
                        FIRST_EVIDENCE_ID,
                        LocalDate.of(
                                2026,
                                7,
                                23),
                        "reference:evidence-to-deactivate",
                        SupportingEvidenceLegibilityStatus.UNVERIFIED);

        transactionRunner.execute(
                (Runnable) () ->
                        evidenceRepository.saveNew(
                                activeEvidence));

        SupportingEvidence inactiveRevision =
                new SupportingEvidence(
                        activeEvidence.id(),
                        activeEvidence.eventId(),
                        activeEvidence.evidenceType(),
                        activeEvidence.contentReference(),
                        activeEvidence.supportedAmount(),
                        activeEvidence.evidenceDate(),
                        activeEvidence.legibilityStatus(),
                        false,
                        2L,
                        activeEvidence.createdAt(),
                        activeEvidence.createdBy(),
                        UPDATED_AT,
                        actor(),
                        UPDATED_AT);

        transactionRunner.execute(
                (Runnable) () ->
                        evidenceRepository.saveRevision(
                                inactiveRevision));

        Long persistedRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.supporting_evidence
                        WHERE id = ?
                        """,
                        Long.class,
                        FIRST_EVIDENCE_ID);

        Long inactiveRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.supporting_evidence
                        WHERE id = ?
                          AND is_active = FALSE
                          AND revision = 2
                          AND deactivated_at IS NOT NULL
                        """,
                        Long.class,
                        FIRST_EVIDENCE_ID);

        assertThat(persistedRows)
                .isEqualTo(1L);

        assertThat(inactiveRows)
                .isEqualTo(1L);

        SupportingEvidence loaded =
                evidenceRepository.findById(
                                activeEvidence.id())
                        .orElseThrow();

        assertThat(loaded.active())
                .isFalse();

        assertThat(loaded.revision())
                .isEqualTo(2L);

        assertThat(loaded.deactivatedAt())
                .isEqualTo(
                        UPDATED_AT);
    }

    @Test
    void scopesReadLookupToOwningCloseAndEvent() {
        persistParentGraph();

        SupportingEvidence evidence =
                activeEvidence(
                        FIRST_EVIDENCE_ID,
                        LocalDate.of(
                                2026,
                                7,
                                23),
                        "reference:read-scope",
                        SupportingEvidenceLegibilityStatus.UNVERIFIED);

        transactionRunner.execute(
                (Runnable) () ->
                        evidenceRepository.saveNew(
                                evidence));

        Optional<SupportingEvidence> owningResult =
                transactionRunner.execute(
                        () -> evidenceRepository.findById(
                                new OperationalCloseId(
                                        FIRST_CLOSE_ID),
                                new OperationalEventId(
                                        EVENT_ID),
                                evidence.id()));

        Optional<SupportingEvidence> otherCloseResult =
                transactionRunner.execute(
                        () -> evidenceRepository.findById(
                                new OperationalCloseId(
                                        SECOND_CLOSE_ID),
                                new OperationalEventId(
                                        EVENT_ID),
                                evidence.id()));

        Optional<SupportingEvidence> otherEventResult =
                transactionRunner.execute(
                        () -> evidenceRepository.findById(
                                new OperationalCloseId(
                                        FIRST_CLOSE_ID),
                                new OperationalEventId(
                                        OTHER_EVENT_ID),
                                evidence.id()));

        assertThat(owningResult)
                .isPresent();

        assertThat(otherCloseResult)
                .isEmpty();

        assertThat(otherEventResult)
                .isEmpty();
    }

    @Test
    void scopesPessimisticLookupToOwningCloseAndEvent() {
        persistParentGraph();

        SupportingEvidence evidence =
                activeEvidence(
                        FIRST_EVIDENCE_ID,
                        LocalDate.of(
                                2026,
                                7,
                                23),
                        "reference:lock-scope",
                        SupportingEvidenceLegibilityStatus.UNVERIFIED);

        transactionRunner.execute(
                (Runnable) () ->
                        evidenceRepository.saveNew(
                                evidence));

        Optional<SupportingEvidence> owningResult =
                transactionRunner.execute(
                        () -> evidenceRepository.findByIdForUpdate(
                                new OperationalCloseId(
                                        FIRST_CLOSE_ID),
                                new OperationalEventId(
                                        EVENT_ID),
                                evidence.id()));

        Optional<SupportingEvidence> otherCloseResult =
                transactionRunner.execute(
                        () -> evidenceRepository.findByIdForUpdate(
                                new OperationalCloseId(
                                        SECOND_CLOSE_ID),
                                new OperationalEventId(
                                        EVENT_ID),
                                evidence.id()));

        Optional<SupportingEvidence> otherEventResult =
                transactionRunner.execute(
                        () -> evidenceRepository.findByIdForUpdate(
                                new OperationalCloseId(
                                        FIRST_CLOSE_ID),
                                new OperationalEventId(
                                        OTHER_EVENT_ID),
                                evidence.id()));

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
                        OperationalEventType.INCOME,
                        new OperationalEventAmount(
                                new BigDecimal(
                                        "125.5000")),
                        OCCURRED_AT,
                        "Caja principal",
                        "Evento padre para evidencia",
                        true,
                        false,
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

    private static SupportingEvidence activeEvidence(
            UUID evidenceId,
            LocalDate evidenceDate,
            String contentReference,
            SupportingEvidenceLegibilityStatus legibilityStatus) {

        return SupportingEvidence.create(
                new SupportingEvidenceId(
                        evidenceId),
                new OperationalEventId(
                        EVENT_ID),
                "RECEIPT",
                contentReference,
                new BigDecimal(
                        "125.5000"),
                evidenceDate,
                legibilityStatus,
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
