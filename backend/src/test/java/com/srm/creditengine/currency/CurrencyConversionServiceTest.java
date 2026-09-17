package com.srm.creditengine.currency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CurrencyConversionServiceTest {

    private final CurrencyConversionService conversionService = new CurrencyConversionService();

    @Test
    void shouldConvertBrlToUsd() {
        ExchangeRate exchangeRate = new ExchangeRate(
                Currency.USD,
                Currency.BRL,
                new BigDecimal("5.4321"));

        BigDecimal convertedAmount = conversionService.convert(
                new BigDecimal("92859.94"),
                exchangeRate);

        assertEquals(new BigDecimal("17094.67"), convertedAmount);
    }
}
