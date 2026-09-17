package com.srm.creditengine.currency;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exchange-rates")
public class ExchangeRateController {

    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateController(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @PostMapping
    public ResponseEntity<ExchangeRateResponse> create(@Valid @RequestBody CreateExchangeRateRequest request) {
        ExchangeRateEntity exchangeRate = new ExchangeRateEntity(
                request.sourceCurrency(),
                request.targetCurrency(),
                request.rate(),
                request.effectiveAt());

        ExchangeRateEntity savedExchangeRate = exchangeRateRepository.save(exchangeRate);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExchangeRateResponse.from(savedExchangeRate));
    }
}
