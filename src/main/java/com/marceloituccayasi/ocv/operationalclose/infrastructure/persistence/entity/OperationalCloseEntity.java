package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of an Operational Close.
 *
 * <p>This class is a persistence model and must not depend on domain objects.
 */
@Entity
@Table(name = "operational_close", schema = "ocv")
public class OperationalCloseEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false)
    private UUID id;

    @Column(
            name = "period_start",
            nullable = false,
            updatable = false)
    private LocalDate periodStart;

    @Column(
            name = "period_end",
            nullable = false,
            updatable = false)
    private LocalDate periodEnd;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "currency_code",
            nullable = false,
            updatable = false,
            length = 3)
    private String currencyCode;

    @Column(
            name = "initial_balance",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 4)
    private BigDecimal initialBalance;

    @Column(
            name = "state",
            nullable = false,
            length = 30)
    private String state;

    @Column(
            name = "state_changed_at",
            nullable = false)
    private Instant stateChangedAt;

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

    protected OperationalCloseEntity() {
        // Required by JPA.
    }

    private OperationalCloseEntity(
            UUID id,
            LocalDate periodStart,
            LocalDate periodEnd,
            String currencyCode,
            BigDecimal initialBalance,
            String state,
            Instant stateChangedAt,
            Instant createdAt,
            String createdByUserId,
            String createdByUsername,
            Instant updatedAt,
            String updatedByUserId,
            String updatedByUsername) {

        this.id = Objects.requireNonNull(id);
        this.periodStart = Objects.requireNonNull(periodStart);
        this.periodEnd = Objects.requireNonNull(periodEnd);
        this.currencyCode = Objects.requireNonNull(currencyCode);
        this.initialBalance = Objects.requireNonNull(initialBalance);
        this.state = Objects.requireNonNull(state);
        this.stateChangedAt = Objects.requireNonNull(stateChangedAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.createdByUserId = Objects.requireNonNull(createdByUserId);
        this.createdByUsername = Objects.requireNonNull(createdByUsername);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.updatedByUserId = Objects.requireNonNull(updatedByUserId);
        this.updatedByUsername = Objects.requireNonNull(updatedByUsername);
    }

    public static OperationalCloseEntity create(
            UUID id,
            LocalDate periodStart,
            LocalDate periodEnd,
            String currencyCode,
            BigDecimal initialBalance,
            String state,
            Instant stateChangedAt,
            Instant createdAt,
            String createdByUserId,
            String createdByUsername,
            Instant updatedAt,
            String updatedByUserId,
            String updatedByUsername) {

        return new OperationalCloseEntity(
                id,
                periodStart,
                periodEnd,
                currencyCode,
                initialBalance,
                state,
                stateChangedAt,
                createdAt,
                createdByUserId,
                createdByUsername,
                updatedAt,
                updatedByUserId,
                updatedByUsername);
    }

    public UUID id() {
        return id;
    }

    public LocalDate periodStart() {
        return periodStart;
    }

    public LocalDate periodEnd() {
        return periodEnd;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public BigDecimal initialBalance() {
        return initialBalance;
    }

    public String state() {
        return state;
    }

    public Instant stateChangedAt() {
        return stateChangedAt;
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

}
