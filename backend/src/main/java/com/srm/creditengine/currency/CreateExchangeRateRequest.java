package com.srm.creditengine.currency;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateExchangeRateRequest(
        @NotNull Currency sourceCurrency,
        @NotNull Currency targetCurrency,
        @NotNull @Positive BigDecimal rate,
        @NotNull OffsetDateTime effectiveAt) {
}
