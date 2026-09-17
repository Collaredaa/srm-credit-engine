package com.srm.creditengine.settlement;

import com.srm.creditengine.currency.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    public ResponseEntity<SettlementResult> settle(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody SettlementApiRequest request) {
        SettlementRequest settlementRequest = new SettlementRequest(
                request.receivableId(),
                request.paymentCurrency(),
                idempotencyKey);

        SettlementResult result = settlementService.settle(settlementRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public Page<SettlementResult> list(Pageable pageable) {
        return settlementService.findAll(pageable);
    }

    public record SettlementApiRequest(
            @NotNull UUID receivableId,
            @NotNull Currency paymentCurrency) {
    }
}
