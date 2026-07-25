package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.CloseStateTransitionEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.OperationalCloseEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.OperationalClosePersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.CloseStateTransitionJpaRepository;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.OperationalCloseJpaRepository;

class OperationalCloseRevisionPersistenceAdapterTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "4b398909-2f78-43b7-992b-e54167810001");

    private static final UUID TRANSITION_ID =
            UUID.fromString(
                    "4b398909-2f78-43b7-992b-e54167810002");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-23T08:00:00Z");

    private static final Instant REVISED_AT =
            Instant.parse(
                    "2026-07-23T10:00:00Z");

    private final OperationalCloseJpaRepository
            closeJpaRepository =
                    mock(
                            OperationalCloseJpaRepository.class);

    private final CloseStateTransitionJpaRepository
            transitionJpaRepository =
                    mock(
                            CloseStateTransitionJpaRepository.class);

    private final OperationalClosePersistenceAdapter adapter =
            new OperationalClosePersistenceAdapter(
                    closeJpaRepository,
                    transitionJpaRepository,
                    new OperationalClosePersistenceMapper());

    @Test
    void savesRevisedCloseStateAndAuditMetadata() {
        OperationalClose revisedClose =
                revisedClose();

        adapter.saveRevision(
                revisedClose);

        ArgumentCaptor<OperationalCloseEntity> captor =
                ArgumentCaptor.forClass(
                        OperationalCloseEntity.class);

        verify(closeJpaRepository)
                .saveAndFlush(
                        captor.capture());

        OperationalCloseEntity savedEntity =
                captor.getValue();

        assertThat(savedEntity.id())
                .isEqualTo(CLOSE_ID);

        assertThat(savedEntity.state())
                .isEqualTo("BLOCKED");

        assertThat(savedEntity.stateChangedAt())
                .isEqualTo(REVISED_AT);

        assertThat(savedEntity.updatedAt())
                .isEqualTo(REVISED_AT);

        assertThat(savedEntity.updatedByUserId())
                .isEqualTo(
                        AuditActor.RESPONSIBLE_USER_ID);

        assertThat(savedEntity.updatedByUsername())
                .isEqualTo("responsible");

        assertThat(savedEntity.createdAt())
                .isEqualTo(CREATED_AT);
    }

    @Test
    void appendsMappedCloseStateTransition() {
        CloseStateTransition transition =
                new CloseStateTransition(
                        new CloseStateTransitionId(
                                TRANSITION_ID),
                        new OperationalCloseId(
                                CLOSE_ID),
                        OperationalCloseState.VALIDATED,
                        OperationalCloseState.BLOCKED,
                        "TEST_CAUSE",
                        "Prueba de persistencia",
                        REVISED_AT,
                        actor());

        adapter.appendStateTransition(
                transition);

        ArgumentCaptor<CloseStateTransitionEntity> captor =
                ArgumentCaptor.forClass(
                        CloseStateTransitionEntity.class);

        verify(transitionJpaRepository)
                .saveAndFlush(
                        captor.capture());

        CloseStateTransitionEntity savedEntity =
                captor.getValue();

        assertThat(savedEntity.id())
                .isEqualTo(TRANSITION_ID);

        assertThat(savedEntity.closeId())
                .isEqualTo(CLOSE_ID);

        assertThat(savedEntity.fromState())
                .isEqualTo("VALIDATED");

        assertThat(savedEntity.toState())
                .isEqualTo("BLOCKED");

        assertThat(savedEntity.causeCode())
                .isEqualTo("TEST_CAUSE");

        assertThat(savedEntity.detail())
                .isEqualTo(
                        "Prueba de persistencia");

        assertThat(savedEntity.occurredAt())
                .isEqualTo(REVISED_AT);

        assertThat(savedEntity.actorUserId())
                .isEqualTo(
                        AuditActor.RESPONSIBLE_USER_ID);
    }

    private static OperationalClose revisedClose() {
        return new OperationalClose(
                new OperationalCloseId(
                        CLOSE_ID),
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
                OperationalCloseState.BLOCKED,
                REVISED_AT,
                CREATED_AT,
                actor(),
                REVISED_AT,
                actor());
    }

    private static AuditActor actor() {
        return new AuditActor(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
    }

}