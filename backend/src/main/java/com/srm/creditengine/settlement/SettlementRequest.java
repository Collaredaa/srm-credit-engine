package com.srm.creditengine.settlement;

import com.srm.creditengine.currency.Currency;
import java.util.Objects;
import java.util.UUID;

public record SettlementRequest(
        UUID receivableId,
        Currency paymentCurrency,
        String idempotencyKey) {

    public SettlementRequest {
        Objects.requireNonNull(receivableId, "receivableId must not be null");
        Objects.requireNonNull(paymentCurrency, "paymentCurrency must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");

        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }
}
