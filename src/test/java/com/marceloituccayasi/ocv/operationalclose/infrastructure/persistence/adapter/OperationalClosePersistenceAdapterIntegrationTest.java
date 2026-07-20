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
import com.marceloituccayasi.ocv.operationalclose.application.DuplicateOperationalClosePeriodException;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OperationalClosePersistenceAdapterIntegrationTest {

    private static final UUID FIRST_CLOSE_ID =
            UUID.fromString(
                    "44998d31-1f05-426e-b4e8-2c36e02b946f");

    private static final UUID SECOND_CLOSE_ID =
            UUID.fromString(
                    "8b289280-69cf-4794-8101-54fc12a26aca");

    private static final UUID UNKNOWN_CLOSE_ID =
            UUID.fromString(
                    "837975df-bcc5-488c-a5e9-a8eff10e216f");

    private static final UUID FIRST_TRANSITION_ID =
            UUID.fromString(
                    "cd0b538b-a106-4675-8350-79f60ec3fd90");

    private static final UUID SECOND_TRANSITION_ID =
            UUID.fromString(
                    "75364656-33af-446a-945b-86e81e418f0c");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-20T08:00:00Z");

    @Autowired
    private OperationalCloseRepository repository;

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
    void persistsCloseAndInitialTransitionInOneTransaction() {
        OperationalClose operationalClose =
                operationalClose(
                        FIRST_CLOSE_ID,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31));

        CloseStateTransition initialTransition =
                CloseStateTransition.initial(
                        new CloseStateTransitionId(
                                FIRST_TRANSITION_ID),
                        operationalClose.id(),
                        NOW,
                        actor());

        transactionRunner.execute(
                (Runnable) () -> repository.saveNew(
                        operationalClose,
                        initialTransition));

        Long persistedCloses =
                countRows("ocv.operational_close");

        Long persistedTransitions =
                countRows("ocv.close_state_transition");

        String persistedCurrency =
                jdbcTemplate.queryForObject(
                        """
                        SELECT currency_code
                        FROM ocv.operational_close
                        WHERE id = ?
                        """,
                        String.class,
                        FIRST_CLOSE_ID);

        String persistedState =
                jdbcTemplate.queryForObject(
                        """
                        SELECT state
                        FROM ocv.operational_close
                        WHERE id = ?
                        """,
                        String.class,
                        FIRST_CLOSE_ID);

        String transitionToState =
                jdbcTemplate.queryForObject(
                        """
                        SELECT to_state
                        FROM ocv.close_state_transition
                        WHERE id = ?
                        """,
                        String.class,
                        FIRST_TRANSITION_ID);

        Long initialTransitions =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.close_state_transition
                        WHERE id = ?
                          AND close_id = ?
                          AND from_state IS NULL
                          AND cause_code = 'CLOSE_CREATED'
                        """,
                        Long.class,
                        FIRST_TRANSITION_ID,
                        FIRST_CLOSE_ID);

        assertThat(persistedCloses)
                .isEqualTo(1L);

        assertThat(persistedTransitions)
                .isEqualTo(1L);

        assertThat(persistedCurrency)
                .isEqualTo("PEN");

        assertThat(persistedState)
                .isEqualTo("PREPARATION");

        assertThat(transitionToState)
                .isEqualTo("PREPARATION");

        assertThat(initialTransitions)
                .isEqualTo(1L);
    }

    @Test
    void rollsBackCloseWhenInitialTransitionCannotBePersisted() {
        OperationalClose operationalClose =
                operationalClose(
                        FIRST_CLOSE_ID,
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31));

        CloseStateTransition invalidTransition =
                CloseStateTransition.initial(
                        new CloseStateTransitionId(
                                FIRST_TRANSITION_ID),
                        new OperationalCloseId(
                                UNKNOWN_CLOSE_ID),
                        NOW,
                        actor());

        assertThatThrownBy(
                () -> transactionRunner.execute(
                        (Runnable) () -> repository.saveNew(
                                operationalClose,
                                invalidTransition)))
                .isInstanceOf(
                        DataIntegrityViolationException.class);

        assertThat(
                countRows("ocv.operational_close"))
                .isZero();

        assertThat(
                countRows("ocv.close_state_transition"))
                .isZero();
    }

    @Test
    void translatesDuplicatePeriodAndPreservesOriginalAggregate() {
        OperationalClose originalClose =
                operationalClose(
                        FIRST_CLOSE_ID,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30));

        CloseStateTransition originalTransition =
                CloseStateTransition.initial(
                        new CloseStateTransitionId(
                                FIRST_TRANSITION_ID),
                        originalClose.id(),
                        NOW,
                        actor());

        transactionRunner.execute(
                (Runnable) () -> repository.saveNew(
                        originalClose,
                        originalTransition));

        OperationalClose duplicatePeriodClose =
                operationalClose(
                        SECOND_CLOSE_ID,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30));

        CloseStateTransition duplicateTransition =
                CloseStateTransition.initial(
                        new CloseStateTransitionId(
                                SECOND_TRANSITION_ID),
                        duplicatePeriodClose.id(),
                        NOW.plusSeconds(1),
                        actor());

        assertThatThrownBy(
                () -> transactionRunner.execute(
                        (Runnable) () -> repository.saveNew(
                                duplicatePeriodClose,
                                duplicateTransition)))
                .isInstanceOf(
                        DuplicateOperationalClosePeriodException.class);

        assertThat(
                countRows("ocv.operational_close"))
                .isEqualTo(1L);

        assertThat(
                countRows("ocv.close_state_transition"))
                .isEqualTo(1L);

        Long originalCloseRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.operational_close
                        WHERE id = ?
                        """,
                        Long.class,
                        FIRST_CLOSE_ID);

        Long duplicateCloseRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.operational_close
                        WHERE id = ?
                        """,
                        Long.class,
                        SECOND_CLOSE_ID);

        assertThat(originalCloseRows)
                .isEqualTo(1L);

        assertThat(duplicateCloseRows)
                .isZero();
    }

    private void cleanOperationalCloseTables() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    ocv.close_state_transition,
                    ocv.operational_close
                """);
    }

    private Long countRows(
            String qualifiedTableName) {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM "
                        + qualifiedTableName,
                Long.class);
    }

    private static OperationalClose operationalClose(
            UUID closeId,
            LocalDate periodStart,
            LocalDate periodEnd) {

        return OperationalClose.create(
                new OperationalCloseId(closeId),
                new OperationalPeriod(
                        periodStart,
                        periodEnd),
                new CurrencyCode("PEN"),
                new InitialBalance(
                        new BigDecimal("1250.5000")),
                NOW,
                actor());
    }

    private static AuditActor actor() {
        return new AuditActor(
                "responsible-user",
                "responsible");
    }

}
