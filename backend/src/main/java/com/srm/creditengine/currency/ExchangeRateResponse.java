package com.srm.creditengine.currency;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExchangeRateResponse(
        UUID id,
        Currency sourceCurrency,
        Currency targetCurrency,
        BigDecimal rate,
        OffsetDateTime effectiveAt,
        OffsetDateTime createdAt) {

    public static ExchangeRateResponse from(ExchangeRateEntity exchangeRate) {
        return new ExchangeRateResponse(
                exchangeRate.getId(),
                exchangeRate.getSourceCurrency(),
                exchangeRate.getTargetCurrency(),
                exchangeRate.getRate(),
                exchangeRate.getEffectiveAt(),
                exchangeRate.getCreatedAt());
    }
}
