package com.srm.creditengine.currency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "exchange_rates")
public class ExchangeRateEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency sourceCurrency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency targetCurrency;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    @Column(nullable = false)
    private OffsetDateTime effectiveAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ExchangeRateEntity() {
    }

    public ExchangeRateEntity(
            Currency sourceCurrency,
            Currency targetCurrency,
            BigDecimal rate,
            OffsetDateTime effectiveAt) {
        this.sourceCurrency = Objects.requireNonNull(sourceCurrency, "sourceCurrency must not be null");
        this.targetCurrency = Objects.requireNonNull(targetCurrency, "targetCurrency must not be null");
        this.rate = Objects.requireNonNull(rate, "rate must not be null");
        this.effectiveAt = Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
        this.createdAt = OffsetDateTime.now();

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("rate must be greater than zero");
        }
    }

    public UUID getId() {
        return id;
    }

    public Currency getSourceCurrency() {
        return sourceCurrency;
    }

    public Currency getTargetCurrency() {
        return targetCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public OffsetDateTime getEffectiveAt() {
        return effectiveAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
