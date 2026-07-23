package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OperationalCloseRevisionPersistenceIntegrationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "ed04fc74-3dba-48ab-a70e-15f97b990001");

    private static final UUID INITIAL_TRANSITION_ID =
            UUID.fromString(
                    "ed04fc74-3dba-48ab-a70e-15f97b990002");

    private static final UUID REVISION_TRANSITION_ID =
            UUID.fromString(
                    "ed04fc74-3dba-48ab-a70e-15f97b990003");

    private static final UUID INVALID_TRANSITION_ID =
            UUID.fromString(
                    "ed04fc74-3dba-48ab-a70e-15f97b990004");

    private static final UUID UNKNOWN_CLOSE_ID =
            UUID.fromString(
                    "ed04fc74-3dba-48ab-a70e-15f97b990005");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-23T08:00:00Z");

    private static final Instant REVISED_AT =
            Instant.parse(
                    "2026-07-23T10:00:00Z");

    @Autowired
    private OperationalCloseRepository closeRepository;

    @Autowired
    private OperationalCloseLockRepository closeLockRepository;

    @Autowired
    private OperationalCloseRevisionRepository
            closeRevisionRepository;

    @Autowired
    private TransactionRunner transactionRunner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareDatabase() {
        cleanOperationalCloseTables();
        persistInitialClose();
    }

    @AfterEach
    void cleanDatabase() {
        cleanOperationalCloseTables();
    }

    @Test
    void locksAndPersistsCloseRevisionWithTransition() {
        transactionRunner.execute(
                () -> {
                    OperationalClose lockedClose =
                            closeLockRepository
                                    .findByIdForUpdate(
                                            closeId())
                                    .orElseThrow();

                    OperationalClose revisedClose =
                            blockedRevision(
                                    lockedClose);

                    CloseStateTransition transition =
                            blockedTransition(
                                    REVISION_TRANSITION_ID,
                                    lockedClose.id());

                    closeRevisionRepository.saveRevision(
                            revisedClose);

                    closeRevisionRepository.appendStateTransition(
                            transition);
                });

        OperationalClose persistedClose =
                transactionRunner.execute(
                        () -> closeRepository
                                .findById(
                                        closeId())
                                .orElseThrow());

        assertThat(persistedClose.state())
                .isEqualTo(
                        OperationalCloseState.BLOCKED);

        assertThat(persistedClose.stateChangedAt())
                .isEqualTo(REVISED_AT);

        assertThat(persistedClose.updatedAt())
                .isEqualTo(REVISED_AT);

        assertThat(persistedClose.updatedBy())
                .isEqualTo(actor());

        assertThat(
                countRows(
                        "ocv.close_state_transition"))
                .isEqualTo(2L);

        Long revisionTransitions =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.close_state_transition
                        WHERE id = ?
                          AND close_id = ?
                          AND from_state = 'VALIDATED'
                          AND to_state = 'BLOCKED'
                          AND cause_code = 'DEPENDENT_DATA_REVISED'
                        """,
                        Long.class,
                        REVISION_TRANSITION_ID,
                        CLOSE_ID);

        assertThat(revisionTransitions)
                .isEqualTo(1L);
    }

    @Test
    void rollsBackCloseRevisionWhenTransitionPersistenceFails() {
        assertThatThrownBy(
                () -> transactionRunner.execute(
                        () -> {
                            OperationalClose lockedClose =
                                    closeLockRepository
                                            .findByIdForUpdate(
                                                    closeId())
                                            .orElseThrow();

                            OperationalClose revisedClose =
                                    blockedRevision(
                                            lockedClose);

                            CloseStateTransition invalidTransition =
                                    blockedTransition(
                                            INVALID_TRANSITION_ID,
                                            new OperationalCloseId(
                                                    UNKNOWN_CLOSE_ID));

                            closeRevisionRepository.saveRevision(
                                    revisedClose);

                            closeRevisionRepository
                                    .appendStateTransition(
                                            invalidTransition);
                        }))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        OperationalClose persistedClose =
                transactionRunner.execute(
                        () -> closeRepository
                                .findById(
                                        closeId())
                                .orElseThrow());

        assertThat(persistedClose.state())
                .isEqualTo(
                        OperationalCloseState.VALIDATED);

        assertThat(persistedClose.stateChangedAt())
                .isEqualTo(CREATED_AT);

        assertThat(persistedClose.updatedAt())
                .isEqualTo(CREATED_AT);

        assertThat(
                countRows(
                        "ocv.close_state_transition"))
                .isEqualTo(1L);
    }

    private void persistInitialClose() {
        OperationalClose operationalClose =
                new OperationalClose(
                        closeId(),
                        new OperationalPeriod(
                                LocalDate.of(
                                        2026,
                                        7,
                                        1),
                                LocalDate.of(
                                        2026,
                                        7,
                                        31)),
                        new CurrencyCode(
                                "PEN"),
                        new InitialBalance(
                                new BigDecimal(
                                        "1000.0000")),
                        OperationalCloseState.VALIDATED,
                        CREATED_AT,
                        CREATED_AT,
                        actor(),
                        CREATED_AT,
                        actor());

        CloseStateTransition initialTransition =
                new CloseStateTransition(
                        new CloseStateTransitionId(
                                INITIAL_TRANSITION_ID),
                        operationalClose.id(),
                        null,
                        OperationalCloseState.VALIDATED,
                        "TEST_VALIDATED_CLOSE_CREATED",
                        null,
                        CREATED_AT,
                        actor());

        transactionRunner.execute(
                (Runnable) () -> closeRepository.saveNew(
                        operationalClose,
                        initialTransition));
    }

    private static OperationalClose blockedRevision(
            OperationalClose lockedClose) {

        return new OperationalClose(
                lockedClose.id(),
                lockedClose.period(),
                lockedClose.currencyCode(),
                lockedClose.initialBalance(),
                OperationalCloseState.BLOCKED,
                REVISED_AT,
                lockedClose.createdAt(),
                lockedClose.createdBy(),
                REVISED_AT,
                actor());
    }

    private static CloseStateTransition blockedTransition(
            UUID transitionId,
            OperationalCloseId transitionCloseId) {

        return new CloseStateTransition(
                new CloseStateTransitionId(
                        transitionId),
                transitionCloseId,
                OperationalCloseState.VALIDATED,
                OperationalCloseState.BLOCKED,
                "DEPENDENT_DATA_REVISED",
                null,
                REVISED_AT,
                actor());
    }

    private static OperationalCloseId closeId() {
        return new OperationalCloseId(
                CLOSE_ID);
    }

    private Long countRows(
            String qualifiedTableName) {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM "
                        + qualifiedTableName,
                Long.class);
    }

    private void cleanOperationalCloseTables() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    ocv.event_state_transition,
                    ocv.operational_event,
                    ocv.close_state_transition,
                    ocv.operational_close
                """);
    }

    private static AuditActor actor() {
        return new AuditActor(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
    }

}