package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of an Operational Close consolidation.
 *
 * <p>The calculation content is immutable after insertion. Only validity
 * metadata may change during invalidation.</p>
 */
@Entity
@Table(name = "consolidation", schema = "ocv")
public class ConsolidationEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false)
    private UUID id;

    @Column(
            name = "close_id",
            nullable = false,
            updatable = false)
    private UUID closeId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "currency_code",
            nullable = false,
            updatable = false,
            length = 3)
    private String currencyCode;

    @Column(
            name = "event_count",
            nullable = false,
            updatable = false)
    private int eventCount;

    @Column(
            name = "total_income",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 4)
    private BigDecimal totalIncome;

    @Column(
            name = "total_expense",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 4)
    private BigDecimal totalExpense;

    @Column(
            name = "total_discount",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 4)
    private BigDecimal totalDiscount;

    @Column(
            name = "total_cancellation",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 4)
    private BigDecimal totalCancellation;

    @Column(
            name = "initial_balance",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 4)
    private BigDecimal initialBalance;

    @Column(
            name = "expected_balance",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 4)
    private BigDecimal expectedBalance;

    @Column(
            name = "actual_balance",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 4)
    private BigDecimal actualBalance;

    @Column(
            name = "difference",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 4)
    private BigDecimal difference;

    @Column(
            name = "is_current",
            nullable = false)
    private boolean current;

    @Column(
            name = "completed_at",
            nullable = false,
            updatable = false)
    private Instant completedAt;

    @Column(
            name = "completed_by_user_id",
            nullable = false,
            updatable = false,
            length = 64)
    private String completedByUserId;

    @Column(
            name = "completed_by_username",
            nullable = false,
            updatable = false,
            length = 100)
    private String completedByUsername;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(
            name = "invalidation_reason",
            columnDefinition = "TEXT")
    private String invalidationReason;

    protected ConsolidationEntity() {
        // Required by JPA.
    }

    private ConsolidationEntity(
            UUID id,
            UUID closeId,
            String currencyCode,
            int eventCount,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal totalDiscount,
            BigDecimal totalCancellation,
            BigDecimal initialBalance,
            BigDecimal expectedBalance,
            BigDecimal actualBalance,
            BigDecimal difference,
            boolean current,
            Instant completedAt,
            String completedByUserId,
            String completedByUsername,
            Instant invalidatedAt,
            String invalidationReason) {

        this.id =
                Objects.requireNonNull(id);

        this.closeId =
                Objects.requireNonNull(closeId);

        this.currencyCode =
                Objects.requireNonNull(currencyCode);

        this.eventCount =
                eventCount;

        this.totalIncome =
                Objects.requireNonNull(totalIncome);

        this.totalExpense =
                Objects.requireNonNull(totalExpense);

        this.totalDiscount =
                Objects.requireNonNull(totalDiscount);

        this.totalCancellation =
                Objects.requireNonNull(totalCancellation);

        this.initialBalance =
                Objects.requireNonNull(initialBalance);

        this.expectedBalance =
                Objects.requireNonNull(expectedBalance);

        this.actualBalance =
                Objects.requireNonNull(actualBalance);

        this.difference =
                Objects.requireNonNull(difference);

        this.current =
                current;

        this.completedAt =
                Objects.requireNonNull(completedAt);

        this.completedByUserId =
                Objects.requireNonNull(completedByUserId);

        this.completedByUsername =
                Objects.requireNonNull(completedByUsername);

        this.invalidatedAt =
                invalidatedAt;

        this.invalidationReason =
                invalidationReason;
    }

    public static ConsolidationEntity create(
            UUID id,
            UUID closeId,
            String currencyCode,
            int eventCount,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal totalDiscount,
            BigDecimal totalCancellation,
            BigDecimal initialBalance,
            BigDecimal expectedBalance,
            BigDecimal actualBalance,
            BigDecimal difference,
            boolean current,
            Instant completedAt,
            String completedByUserId,
            String completedByUsername,
            Instant invalidatedAt,
            String invalidationReason) {

        return new ConsolidationEntity(
                id,
                closeId,
                currencyCode,
                eventCount,
                totalIncome,
                totalExpense,
                totalDiscount,
                totalCancellation,
                initialBalance,
                expectedBalance,
                actualBalance,
                difference,
                current,
                completedAt,
                completedByUserId,
                completedByUsername,
                invalidatedAt,
                invalidationReason);
    }

    public UUID id() {
        return id;
    }

    public UUID closeId() {
        return closeId;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public int eventCount() {
        return eventCount;
    }

    public BigDecimal totalIncome() {
        return totalIncome;
    }

    public BigDecimal totalExpense() {
        return totalExpense;
    }

    public BigDecimal totalDiscount() {
        return totalDiscount;
    }

    public BigDecimal totalCancellation() {
        return totalCancellation;
    }

    public BigDecimal initialBalance() {
        return initialBalance;
    }

    public BigDecimal expectedBalance() {
        return expectedBalance;
    }

    public BigDecimal actualBalance() {
        return actualBalance;
    }

    public BigDecimal difference() {
        return difference;
    }

    public boolean current() {
        return current;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public String completedByUserId() {
        return completedByUserId;
    }

    public String completedByUsername() {
        return completedByUsername;
    }

    public Instant invalidatedAt() {
        return invalidatedAt;
    }

    public String invalidationReason() {
        return invalidationReason;
    }

}