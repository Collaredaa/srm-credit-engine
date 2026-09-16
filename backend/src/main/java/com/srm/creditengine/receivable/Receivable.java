package com.srm.creditengine.receivable;

import com.srm.creditengine.pricing.ReceivableType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "receivables")
public class Receivable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String assignor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceivableType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal faceValue;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceivableStatus status;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Receivable() {
    }

    public Receivable(String assignor, ReceivableType type, BigDecimal faceValue, LocalDate dueDate) {
        this.assignor = Objects.requireNonNull(assignor, "assignor must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.faceValue = Objects.requireNonNull(faceValue, "faceValue must not be null");
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate must not be null");
        this.status = ReceivableStatus.OPEN;
        this.createdAt = OffsetDateTime.now();

        if (assignor.isBlank()) {
            throw new IllegalArgumentException("assignor must not be blank");
        }
        if (faceValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("faceValue must be greater than zero");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getAssignor() {
        return assignor;
    }

    public ReceivableType getType() {
        return type;
    }

    public BigDecimal getFaceValue() {
        return faceValue;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public ReceivableStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void settle() {
        if (status == ReceivableStatus.SETTLED) {
            throw new IllegalStateException("Receivable is already settled");
        }
        this.status = ReceivableStatus.SETTLED;
    }
}
