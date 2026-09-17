package com.srm.creditengine.receivable;

import com.srm.creditengine.pricing.ReceivableType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateReceivableRequest(
        @NotBlank String assignor,
        @NotNull ReceivableType type,
        @NotNull @Positive BigDecimal faceValue,
        @NotNull LocalDate dueDate) {
}
