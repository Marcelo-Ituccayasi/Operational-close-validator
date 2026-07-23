package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceLegibilityStatus;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.SupportingEvidenceEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.SupportingEvidencePersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.SupportingEvidenceJpaRepository;

class SupportingEvidencePersistenceAdapterTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "13533cba-386a-4f88-b174-50e629100001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "13533cba-386a-4f88-b174-50e629100002");

    private static final UUID FIRST_EVIDENCE_ID =
            UUID.fromString(
                    "13533cba-386a-4f88-b174-50e629100003");

    private static final UUID SECOND_EVIDENCE_ID =
            UUID.fromString(
                    "13533cba-386a-4f88-b174-50e629100004");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-23T18:00:00Z");

    private final SupportingEvidenceJpaRepository
            supportingEvidenceJpaRepository =
                    mock(
                            SupportingEvidenceJpaRepository.class);

    private final SupportingEvidencePersistenceMapper mapper =
            new SupportingEvidencePersistenceMapper();

    private final SupportingEvidencePersistenceAdapter adapter =
            new SupportingEvidencePersistenceAdapter(
                    supportingEvidenceJpaRepository,
                    mapper);

    @Test
    void savesNewEvidenceAndSubsequentRevision() {
        SupportingEvidence evidence =
                evidence(
                        FIRST_EVIDENCE_ID,
                        LocalDate.of(
                                2026,
                                7,
                                23));

        adapter.saveNew(
                evidence);

        adapter.saveRevision(
                evidence);

        verify(
                supportingEvidenceJpaRepository,
                times(2))
                .saveAndFlush(
                        any(
                                SupportingEvidenceEntity.class));
    }

    @Test
    void returnsMappedEvidenceById() {
        SupportingEvidence expected =
                evidence(
                        FIRST_EVIDENCE_ID,
                        LocalDate.of(
                                2026,
                                7,
                                23));

        when(
                supportingEvidenceJpaRepository.findById(
                        FIRST_EVIDENCE_ID))
                .thenReturn(
                        Optional.of(
                                mapper.toEntity(
                                        expected)));

        Optional<SupportingEvidence> result =
                adapter.findById(
                        new SupportingEvidenceId(
                                FIRST_EVIDENCE_ID));

        assertThat(result)
                .isPresent();

        assertThat(
                result.orElseThrow()
                        .id()
                        .value())
                .isEqualTo(
                        FIRST_EVIDENCE_ID);

        assertThat(
                result.orElseThrow()
                        .eventId()
                        .value())
                .isEqualTo(
                        EVENT_ID);

        assertThat(
                result.orElseThrow()
                        .contentReference())
                .isEqualTo(
                        "reference:evidence-"
                                + FIRST_EVIDENCE_ID);
    }

    @Test
    void preservesRepositoryEvidenceOrder() {
        SupportingEvidence first =
                evidence(
                        FIRST_EVIDENCE_ID,
                        LocalDate.of(
                                2026,
                                7,
                                23));

        SupportingEvidence second =
                evidence(
                        SECOND_EVIDENCE_ID,
                        LocalDate.of(
                                2026,
                                7,
                                22));

        when(
                supportingEvidenceJpaRepository
                        .findAllByEventIdOrderByEvidenceDateDescCreatedAtDescIdDesc(
                                EVENT_ID))
                .thenReturn(
                        List.of(
                                mapper.toEntity(
                                        first),
                                mapper.toEntity(
                                        second)));

        List<SupportingEvidence> result =
                adapter
                        .findAllByEventIdOrderByEvidenceDateDescending(
                                new OperationalEventId(
                                        EVENT_ID));

        assertThat(result)
                .hasSize(2);

        assertThat(
                result.get(0)
                        .id()
                        .value())
                .isEqualTo(
                        FIRST_EVIDENCE_ID);

        assertThat(
                result.get(1)
                        .id()
                        .value())
                .isEqualTo(
                        SECOND_EVIDENCE_ID);
    }

    @Test
    void scopesEvidenceLockByCloseAndEvidenceId() {
        SupportingEvidence expected =
                evidence(
                        FIRST_EVIDENCE_ID,
                        LocalDate.of(
                                2026,
                                7,
                                23));

        when(
                supportingEvidenceJpaRepository.findByIdForUpdate(
                        CLOSE_ID,
                        FIRST_EVIDENCE_ID))
                .thenReturn(
                        Optional.of(
                                mapper.toEntity(
                                        expected)));

        Optional<SupportingEvidence> result =
                adapter.findByIdForUpdate(
                        new OperationalCloseId(
                                CLOSE_ID),
                        new SupportingEvidenceId(
                                FIRST_EVIDENCE_ID));

        assertThat(result)
                .isPresent();

        verify(
                supportingEvidenceJpaRepository)
                .findByIdForUpdate(
                        CLOSE_ID,
                        FIRST_EVIDENCE_ID);
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
                        .findAllByEventIdOrderByEvidenceDateDescending(
                                null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.findByIdForUpdate(
                        null,
                        new SupportingEvidenceId(
                                FIRST_EVIDENCE_ID)))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.findByIdForUpdate(
                        new OperationalCloseId(
                                CLOSE_ID),
                        null))
                .isInstanceOf(
                        NullPointerException.class);

        assertThatThrownBy(
                () -> adapter.saveRevision(
                        null))
                .isInstanceOf(
                        NullPointerException.class);
    }

    private static SupportingEvidence evidence(
            UUID evidenceId,
            LocalDate evidenceDate) {

        return SupportingEvidence.create(
                new SupportingEvidenceId(
                        evidenceId),
                new OperationalEventId(
                        EVENT_ID),
                "RECEIPT",
                "reference:evidence-"
                        + evidenceId,
                new BigDecimal(
                        "150.0000"),
                evidenceDate,
                SupportingEvidenceLegibilityStatus.UNVERIFIED,
                NOW,
                actor());
    }

    private static AuditActor actor() {
        return new AuditActor(
                "responsible-user",
                "responsible");
    }

}