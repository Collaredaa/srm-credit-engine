package com.srm.creditengine.settlement;

import com.srm.creditengine.currency.Currency;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SettlementResult(
        UUID id,
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
}
