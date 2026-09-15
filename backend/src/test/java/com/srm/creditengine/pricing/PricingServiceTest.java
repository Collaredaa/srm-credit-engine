package com.srm.creditengine.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService(List.of(
                new DuplicataPricingStrategy(),
                new ChequePricingStrategy()));
    }

    @Test
    void shouldPriceDuplicataMercantil() {
        PricingResult result = pricingService.price(
                new BigDecimal("100000.00"),
                3,
                ReceivableType.DUPLICATA_MERCANTIL);

        assertEquals(new BigDecimal("100000.00"), result.faceValue());
        assertEquals(new BigDecimal("92859.94"), result.presentValue());
        assertEquals(new BigDecimal("7140.06"), result.discountAmount());
        assertEquals(new BigDecimal("0.01"), result.baseRate());
        assertEquals(new BigDecimal("0.015"), result.spread());
        assertEquals(3, result.termInMonths());
    }

    @Test
    void shouldPriceChequePreDatado() {
        PricingResult result = pricingService.price(
                new BigDecimal("25000.00"),
                2,
                ReceivableType.CHEQUE_PRE_DATADO);

        assertEquals(new BigDecimal("25000.00"), result.faceValue());
        assertEquals(new BigDecimal("23337.77"), result.presentValue());
        assertEquals(new BigDecimal("1662.23"), result.discountAmount());
        assertEquals(new BigDecimal("0.01"), result.baseRate());
        assertEquals(new BigDecimal("0.025"), result.spread());
        assertEquals(2, result.termInMonths());
    }
}
