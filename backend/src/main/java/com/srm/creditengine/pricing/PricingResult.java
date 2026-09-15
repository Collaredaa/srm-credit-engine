package com.srm.creditengine.pricing;

import java.math.BigDecimal;

public record PricingResult(
        BigDecimal faceValue,
        BigDecimal presentValue,
        BigDecimal discountAmount,
        BigDecimal baseRate,
        BigDecimal spread,
        int termInMonths) {
}
