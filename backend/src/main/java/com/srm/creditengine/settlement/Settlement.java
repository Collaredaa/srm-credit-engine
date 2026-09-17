package com.srm.creditengine.settlement;

import com.srm.creditengine.currency.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "settlements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_settlements_idempotency_key",
                        columnNames = "idempotency_key"),
                @UniqueConstraint(
                        name = "uk_settlements_receivable_id",
                        columnNames = "receivable_id")
        })
public class Settlement {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "receivable_id", nullable = false)
    private UUID receivableId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal faceValue;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal presentValueBrl;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency paymentCurrency;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal baseRate;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal spread;

    @Column(precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    private OffsetDateTime exchangeRateEffectiveAt;

    @Column(nullable = false)
    private OffsetDateTime settledAt;

    protected Settlement() {
    }

    public Settlement(
            UUID receivableId,
            String idempotencyKey,
            BigDecimal faceValue,
            BigDecimal presentValueBrl,
            BigDecimal paymentAmount,
            Currency paymentCurrency,
            BigDecimal baseRate,
            BigDecimal spread,
            BigDecimal exchangeRate,
            OffsetDateTime exchangeRateEffectiveAt,
            OffsetDateTime settledAt) {
        this.receivableId = Objects.requireNonNull(receivableId, "receivableId must not be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        this.faceValue = Objects.requireNonNull(faceValue, "faceValue must not be null");
        this.presentValueBrl = Objects.requireNonNull(presentValueBrl, "presentValueBrl must not be null");
        this.paymentAmount = Objects.requireNonNull(paymentAmount, "paymentAmount must not be null");
        this.paymentCurrency = Objects.requireNonNull(paymentCurrency, "paymentCurrency must not be null");
        this.baseRate = Objects.requireNonNull(baseRate, "baseRate must not be null");
        this.spread = Objects.requireNonNull(spread, "spread must not be null");
        this.exchangeRate = exchangeRate;
        this.exchangeRateEffectiveAt = exchangeRateEffectiveAt;
        this.settledAt = Objects.requireNonNull(settledAt, "settledAt must not be null");

        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getReceivableId() {
        return receivableId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public BigDecimal getFaceValue() {
        return faceValue;
    }

    public BigDecimal getPresentValueBrl() {
        return presentValueBrl;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public Currency getPaymentCurrency() {
        return paymentCurrency;
    }

    public BigDecimal getBaseRate() {
        return baseRate;
    }

    public BigDecimal getSpread() {
        return spread;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public OffsetDateTime getExchangeRateEffectiveAt() {
        return exchangeRateEffectiveAt;
    }

    public OffsetDateTime getSettledAt() {
        return settledAt;
    }
}
