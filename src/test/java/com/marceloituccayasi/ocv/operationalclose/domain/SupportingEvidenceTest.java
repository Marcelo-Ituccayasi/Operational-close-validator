package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SupportingEvidenceTest {

    private static final SupportingEvidenceId EVIDENCE_ID =
            new SupportingEvidenceId(
                    UUID.fromString(
                            "4f034004-4915-4722-b907-c33df18b0001"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "4f034004-4915-4722-b907-c33df18b0002"));

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-23T14:00:00Z");

    private static final Instant DEACTIVATED_AT =
            Instant.parse(
                    "2026-07-23T15:00:00Z");

    private static final LocalDate EVIDENCE_DATE =
            LocalDate.of(
                    2026,
                    7,
                    22);

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Test
    void createsActiveStoredEvidenceWithInitialRevision() {
        SupportingEvidence evidence =
                SupportingEvidence.create(
                        EVIDENCE_ID,
                        EVENT_ID,
                        "  RECEIPT  ",
                        "stored:evidence/"
                                + EVIDENCE_ID
                                + "/digest.pdf",
                        new BigDecimal(
                                "125.5000"),
                        EVIDENCE_DATE,
                        SupportingEvidenceLegibilityStatus.LEGIBLE,
                        CREATED_AT,
                        ACTOR);

        assertThat(evidence.evidenceType())
                .isEqualTo(
                        "RECEIPT");

        assertThat(evidence.active())
                .isTrue();

        assertThat(evidence.revision())
                .isEqualTo(1L);

        assertThat(evidence.createdAt())
                .isEqualTo(CREATED_AT);

        assertThat(evidence.updatedAt())
                .isEqualTo(CREATED_AT);

        assertThat(evidence.createdBy())
                .isEqualTo(ACTOR);

        assertThat(evidence.updatedBy())
                .isEqualTo(ACTOR);

        assertThat(evidence.deactivatedAt())
                .isNull();

        assertThat(evidence.storedContent())
                .isTrue();

        assertThat(evidence.referencedContent())
                .isFalse();
    }

    @Test
    void createsOpaqueReferenceWithoutSupportedAmount() {
        SupportingEvidence evidence =
                SupportingEvidence.create(
                        EVIDENCE_ID,
                        EVENT_ID,
                        "BANK_CONFIRMATION",
                        "  reference:confirmation-2026-007  ",
                        null,
                        EVIDENCE_DATE,
                        SupportingEvidenceLegibilityStatus.UNVERIFIED,
                        CREATED_AT,
                        ACTOR);

        assertThat(evidence.contentReference())
                .isEqualTo(
                        "reference:confirmation-2026-007");

        assertThat(evidence.supportedAmount())
                .isNull();

        assertThat(evidence.storedContent())
                .isFalse();

        assertThat(evidence.referencedContent())
                .isTrue();
    }

    @Test
    void deactivatesLogicallyAndIncrementsRevision() {
        SupportingEvidence activeEvidence =
                activeEvidence();

        SupportingEvidence deactivatedEvidence =
                activeEvidence.deactivate(
                        DEACTIVATED_AT,
                        ACTOR);

        assertThat(deactivatedEvidence.id())
                .isEqualTo(
                        activeEvidence.id());

        assertThat(deactivatedEvidence.eventId())
                .isEqualTo(
                        activeEvidence.eventId());

        assertThat(deactivatedEvidence.active())
                .isFalse();

        assertThat(deactivatedEvidence.revision())
                .isEqualTo(2L);

        assertThat(deactivatedEvidence.deactivatedAt())
                .isEqualTo(
                        DEACTIVATED_AT);

        assertThat(deactivatedEvidence.updatedAt())
                .isEqualTo(
                        DEACTIVATED_AT);

        assertThat(deactivatedEvidence.updatedBy())
                .isEqualTo(ACTOR);

        assertThat(deactivatedEvidence.createdAt())
                .isEqualTo(
                        CREATED_AT);
    }

    @Test
    void rejectsSecondDeactivation() {
        SupportingEvidence deactivatedEvidence =
                activeEvidence()
                        .deactivate(
                                DEACTIVATED_AT,
                                ACTOR);

        assertThatThrownBy(
                () -> deactivatedEvidence.deactivate(
                        DEACTIVATED_AT.plusSeconds(60),
                        ACTOR))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessageContaining(
                        "already inactive");
    }

    @Test
    void rejectsDeactivationBeforePreviousUpdate() {
        SupportingEvidence evidence =
                activeEvidence();

        assertThatThrownBy(
                () -> evidence.deactivate(
                        CREATED_AT.minusSeconds(1),
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "previous update");
    }

    @Test
    void rejectsUnknownOrBlankContentReference() {
        assertThatThrownBy(
                () -> createWithReference(
                        "https://example.invalid/evidence"))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "stored or reference prefix");

        assertThatThrownBy(
                () -> createWithReference(
                        "reference:   "))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "value must not be blank");

        assertThatThrownBy(
                () -> createWithReference(
                        "stored:"))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "value must not be blank");
    }

    @Test
    void rejectsInvalidSupportedAmount() {
        assertThatThrownBy(
                () -> createWithAmount(
                        BigDecimal.ZERO))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "positive");

        assertThatThrownBy(
                () -> createWithAmount(
                        new BigDecimal(
                                "1.00001")))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "four decimal places");

        assertThatThrownBy(
                () -> createWithAmount(
                        new BigDecimal(
                                "1000000000000000.0000")))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "numeric(19,4)");
    }

    @Test
    void rejectsEvidenceTypeLongerThanFortyCharacters() {
        assertThatThrownBy(
                () -> SupportingEvidence.create(
                        EVIDENCE_ID,
                        EVENT_ID,
                        "X".repeat(41),
                        "reference:business-reference",
                        null,
                        EVIDENCE_DATE,
                        SupportingEvidenceLegibilityStatus.UNVERIFIED,
                        CREATED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "40 characters");
    }

    @Test
    void rejectsInconsistentActivityAndDeactivationMetadata() {
        assertThatThrownBy(
                () -> new SupportingEvidence(
                        EVIDENCE_ID,
                        EVENT_ID,
                        "RECEIPT",
                        "reference:business-reference",
                        null,
                        EVIDENCE_DATE,
                        SupportingEvidenceLegibilityStatus.UNVERIFIED,
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
                        "active supporting evidence");

        assertThatThrownBy(
                () -> new SupportingEvidence(
                        EVIDENCE_ID,
                        EVENT_ID,
                        "RECEIPT",
                        "reference:business-reference",
                        null,
                        EVIDENCE_DATE,
                        SupportingEvidenceLegibilityStatus.UNVERIFIED,
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

    private static SupportingEvidence activeEvidence() {
        return SupportingEvidence.create(
                EVIDENCE_ID,
                EVENT_ID,
                "RECEIPT",
                "reference:business-reference",
                new BigDecimal(
                        "125.5000"),
                EVIDENCE_DATE,
                SupportingEvidenceLegibilityStatus.LEGIBLE,
                CREATED_AT,
                ACTOR);
    }

    private static SupportingEvidence createWithReference(
            String contentReference) {

        return SupportingEvidence.create(
                EVIDENCE_ID,
                EVENT_ID,
                "RECEIPT",
                contentReference,
                null,
                EVIDENCE_DATE,
                SupportingEvidenceLegibilityStatus.UNVERIFIED,
                CREATED_AT,
                ACTOR);
    }

    private static SupportingEvidence createWithAmount(
            BigDecimal supportedAmount) {

        return SupportingEvidence.create(
                EVIDENCE_ID,
                EVENT_ID,
                "RECEIPT",
                "reference:business-reference",
                supportedAmount,
                EVIDENCE_DATE,
                SupportingEvidenceLegibilityStatus.UNVERIFIED,
                CREATED_AT,
                ACTOR);
    }

}