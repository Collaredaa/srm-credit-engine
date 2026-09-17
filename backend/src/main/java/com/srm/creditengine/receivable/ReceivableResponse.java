package com.srm.creditengine.receivable;

import com.srm.creditengine.pricing.ReceivableType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReceivableResponse(
        UUID id,
        String assignor,
        ReceivableType type,
        BigDecimal faceValue,
        LocalDate dueDate,
        ReceivableStatus status,
        OffsetDateTime createdAt) {

    public static ReceivableResponse from(Receivable receivable) {
        return new ReceivableResponse(
                receivable.getId(),
                receivable.getAssignor(),
                receivable.getType(),
                receivable.getFaceValue(),
                receivable.getDueDate(),
                receivable.getStatus(),
                receivable.getCreatedAt());
    }
}
