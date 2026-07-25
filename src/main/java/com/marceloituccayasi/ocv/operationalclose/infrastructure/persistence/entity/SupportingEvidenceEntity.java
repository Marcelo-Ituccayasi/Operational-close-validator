package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of Supporting Evidence.
 *
 * <p>This class is a persistence model and must not depend on domain objects.
 */
@Entity
@Table(name = "supporting_evidence", schema = "ocv")
public class SupportingEvidenceEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false)
    private UUID id;

    @Column(
            name = "event_id",
            nullable = false,
            updatable = false)
    private UUID eventId;

    @Column(
            name = "evidence_type",
            nullable = false,
            length = 40)
    private String evidenceType;

    @Column(
            name = "content_reference",
            nullable = false,
            length = 500)
    private String contentReference;

    @Column(
            name = "supported_amount",
            precision = 19,
            scale = 4)
    private BigDecimal supportedAmount;

    @Column(
            name = "evidence_date",
            nullable = false)
    private LocalDate evidenceDate;

    @Column(
            name = "legibility_status",
            nullable = false,
            length = 20)
    private String legibilityStatus;

    @Column(
            name = "is_active",
            nullable = false)
    private boolean active;

    @Column(
            name = "revision",
            nullable = false)
    private long revision;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false)
    private Instant createdAt;

    @Column(
            name = "created_by_user_id",
            nullable = false,
            updatable = false,
            length = 64)
    private String createdByUserId;

    @Column(
            name = "created_by_username",
            nullable = false,
            updatable = false,
            length = 100)
    private String createdByUsername;

    @Column(
            name = "updated_at",
            nullable = false)
    private Instant updatedAt;

    @Column(
            name = "updated_by_user_id",
            nullable = false,
            length = 64)
    private String updatedByUserId;

    @Column(
            name = "updated_by_username",
            nullable = false,
            length = 100)
    private String updatedByUsername;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    protected SupportingEvidenceEntity() {
        // Required by JPA.
    }

    private SupportingEvidenceEntity(
            UUID id,
            UUID eventId,
            String evidenceType,
            String contentReference,
            BigDecimal supportedAmount,
            LocalDate evidenceDate,
            String legibilityStatus,
            boolean active,
            long revision,
            Instant createdAt,
            String createdByUserId,
            String createdByUsername,
            Instant updatedAt,
            String updatedByUserId,
            String updatedByUsername,
            Instant deactivatedAt) {

        this.id =
                Objects.requireNonNull(
                        id);

        this.eventId =
                Objects.requireNonNull(
                        eventId);

        this.evidenceType =
                Objects.requireNonNull(
                        evidenceType);

        this.contentReference =
                Objects.requireNonNull(
                        contentReference);

        this.supportedAmount =
                supportedAmount;

        this.evidenceDate =
                Objects.requireNonNull(
                        evidenceDate);

        this.legibilityStatus =
                Objects.requireNonNull(
                        legibilityStatus);

        this.active =
                active;

        this.revision =
                revision;

        this.createdAt =
                Objects.requireNonNull(
                        createdAt);

        this.createdByUserId =
                Objects.requireNonNull(
                        createdByUserId);

        this.createdByUsername =
                Objects.requireNonNull(
                        createdByUsername);

        this.updatedAt =
                Objects.requireNonNull(
                        updatedAt);

        this.updatedByUserId =
                Objects.requireNonNull(
                        updatedByUserId);

        this.updatedByUsername =
                Objects.requireNonNull(
                        updatedByUsername);

        this.deactivatedAt =
                deactivatedAt;
    }

    public static SupportingEvidenceEntity create(
            UUID id,
            UUID eventId,
            String evidenceType,
            String contentReference,
            BigDecimal supportedAmount,
            LocalDate evidenceDate,
            String legibilityStatus,
            boolean active,
            long revision,
            Instant createdAt,
            String createdByUserId,
            String createdByUsername,
            Instant updatedAt,
            String updatedByUserId,
            String updatedByUsername,
            Instant deactivatedAt) {

        return new SupportingEvidenceEntity(
                id,
                eventId,
                evidenceType,
                contentReference,
                supportedAmount,
                evidenceDate,
                legibilityStatus,
                active,
                revision,
                createdAt,
                createdByUserId,
                createdByUsername,
                updatedAt,
                updatedByUserId,
                updatedByUsername,
                deactivatedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID eventId() {
        return eventId;
    }

    public String evidenceType() {
        return evidenceType;
    }

    public String contentReference() {
        return contentReference;
    }

    public BigDecimal supportedAmount() {
        return supportedAmount;
    }

    public LocalDate evidenceDate() {
        return evidenceDate;
    }

    public String legibilityStatus() {
        return legibilityStatus;
    }

    public boolean active() {
        return active;
    }

    public long revision() {
        return revision;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String createdByUserId() {
        return createdByUserId;
    }

    public String createdByUsername() {
        return createdByUsername;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public String updatedByUserId() {
        return updatedByUserId;
    }

    public String updatedByUsername() {
        return updatedByUsername;
    }

    public Instant deactivatedAt() {
        return deactivatedAt;
    }

}