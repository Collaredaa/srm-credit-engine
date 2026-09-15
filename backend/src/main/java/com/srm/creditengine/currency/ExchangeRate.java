package com.srm.creditengine.currency;

import java.math.BigDecimal;
import java.util.Objects;

public record ExchangeRate(
        Currency sourceCurrency,
        Currency targetCurrency,
        BigDecimal rate) {

    public ExchangeRate {
        Objects.requireNonNull(sourceCurrency, "sourceCurrency must not be null");
        Objects.requireNonNull(targetCurrency, "targetCurrency must not be null");
        Objects.requireNonNull(rate, "rate must not be null");

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("rate must be greater than zero");
        }
    }
}
