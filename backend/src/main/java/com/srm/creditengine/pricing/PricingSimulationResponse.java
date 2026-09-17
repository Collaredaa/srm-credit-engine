package com.srm.creditengine.pricing;

import com.srm.creditengine.currency.Currency;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PricingSimulationResponse(
        BigDecimal faceValue,
        BigDecimal presentValueBrl,
        BigDecimal discountAmount,
        Currency paymentCurrency,
        BigDecimal paymentAmount,
        BigDecimal baseRate,
        BigDecimal spread,
        int termInMonths,
        BigDecimal exchangeRate,
        OffsetDateTime exchangeRateEffectiveAt) {
}
