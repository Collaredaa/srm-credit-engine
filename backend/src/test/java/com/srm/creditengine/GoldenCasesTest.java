package com.srm.creditengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.srm.creditengine.currency.Currency;
import com.srm.creditengine.currency.CurrencyConversionService;
import com.srm.creditengine.currency.ExchangeRate;
import com.srm.creditengine.pricing.ChequePricingStrategy;
import com.srm.creditengine.pricing.DuplicataPricingStrategy;
import com.srm.creditengine.pricing.PricingResult;
import com.srm.creditengine.pricing.PricingService;
import com.srm.creditengine.pricing.ReceivableType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoldenCasesTest {

    private PricingService pricingService;
    private CurrencyConversionService conversionService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService(List.of(
                new DuplicataPricingStrategy(),
                new ChequePricingStrategy()));
        conversionService = new CurrencyConversionService();
    }

    @Test
    void shouldValidateC1DuplicataMercantil() {
        PricingResult result = pricingService.price(
                new BigDecimal("100000.00"),
                3,
                ReceivableType.DUPLICATA_MERCANTIL);

        assertEquals(new BigDecimal("92859.94"), result.presentValue());
        assertEquals(new BigDecimal("7140.06"), result.discountAmount());
    }

    @Test
    void shouldValidateC2ChequePreDatado() {
        PricingResult result = pricingService.price(
                new BigDecimal("25000.00"),
                2,
                ReceivableType.CHEQUE_PRE_DATADO);

        assertEquals(new BigDecimal("23337.77"), result.presentValue());
        assertEquals(new BigDecimal("1662.23"), result.discountAmount());
    }

    @Test
    void shouldValidateC3DuplicataMercantilConvertedToUsd() {
        PricingResult pricingResult = pricingService.price(
                new BigDecimal("100000.00"),
                3,
                ReceivableType.DUPLICATA_MERCANTIL);
        ExchangeRate usdToBrlRate = new ExchangeRate(
                Currency.USD,
                Currency.BRL,
                new BigDecimal("5.4321"));

        BigDecimal presentValueInUsd = conversionService.convert(
                pricingResult.presentValue(),
                usdToBrlRate);

        assertEquals(new BigDecimal("92859.94"), pricingResult.presentValue());
        assertEquals(new BigDecimal("17094.67"), presentValueInUsd);
    }
}
