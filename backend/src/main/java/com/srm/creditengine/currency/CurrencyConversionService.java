package com.srm.creditengine.currency;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CurrencyConversionService {

    private static final MathContext CALCULATION_CONTEXT = new MathContext(34, RoundingMode.HALF_EVEN);
    private static final int MONEY_SCALE = 2;

    public BigDecimal convert(BigDecimal amount, ExchangeRate exchangeRate) {
        validateInput(amount, exchangeRate);
        validateSupportedConversion(exchangeRate);

        return amount
                .divide(exchangeRate.rate(), CALCULATION_CONTEXT)
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    private void validateInput(BigDecimal amount, ExchangeRate exchangeRate) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(exchangeRate, "exchangeRate must not be null");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
    }

    private void validateSupportedConversion(ExchangeRate exchangeRate) {
    if (exchangeRate.sourceCurrency() != Currency.USD
            || exchangeRate.targetCurrency() != Currency.BRL) {
        throw new IllegalArgumentException(
                "A USD to BRL exchange rate is required for BRL to USD conversion");
        }
    }
}
