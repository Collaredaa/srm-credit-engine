package com.srm.creditengine.pricing;

import com.srm.creditengine.currency.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PricingSimulationRequest(
        @NotNull ReceivableType type,
        @NotNull @Positive BigDecimal faceValue,
        @NotNull LocalDate dueDate,
        @NotNull Currency paymentCurrency) {
}
