package com.srm.creditengine.pricing;

import com.srm.creditengine.currency.Currency;
import com.srm.creditengine.currency.CurrencyConversionService;
import com.srm.creditengine.currency.ExchangeRate;
import com.srm.creditengine.currency.ExchangeRateEntity;
import com.srm.creditengine.currency.ExchangeRateRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingSimulationService {

    private final PricingService pricingService;
    private final CurrencyConversionService currencyConversionService;
    private final ExchangeRateRepository exchangeRateRepository;
    private final TermCalculator termCalculator;

    public PricingSimulationService(
            PricingService pricingService,
            CurrencyConversionService currencyConversionService,
            ExchangeRateRepository exchangeRateRepository,
            TermCalculator termCalculator) {
        this.pricingService = Objects.requireNonNull(pricingService, "pricingService must not be null");
        this.currencyConversionService = Objects.requireNonNull(currencyConversionService, "currencyConversionService must not be null");
        this.exchangeRateRepository = Objects.requireNonNull(exchangeRateRepository, "exchangeRateRepository must not be null");
        this.termCalculator = Objects.requireNonNull(termCalculator, "termCalculator must not be null");
    }

    @Transactional(readOnly = true)
    public PricingSimulationResponse simulate(PricingSimulationRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        OffsetDateTime simulatedAt = OffsetDateTime.now();
        int termInMonths = termCalculator.calculate(simulatedAt.toLocalDate(), request.dueDate());
        PricingResult pricingResult = pricingService.price(
                request.faceValue(),
                termInMonths,
                request.type());
        PaymentSimulation paymentSimulation = calculatePayment(
                request.paymentCurrency(),
                pricingResult,
                simulatedAt);

        return new PricingSimulationResponse(
                pricingResult.faceValue(),
                pricingResult.presentValue(),
                pricingResult.discountAmount(),
                request.paymentCurrency(),
                paymentSimulation.paymentAmount(),
                pricingResult.baseRate(),
                pricingResult.spread(),
                pricingResult.termInMonths(),
                paymentSimulation.exchangeRate(),
                paymentSimulation.exchangeRateEffectiveAt());
    }

    private PaymentSimulation calculatePayment(
            Currency paymentCurrency,
            PricingResult pricingResult,
            OffsetDateTime simulatedAt) {
        if (paymentCurrency == Currency.BRL) {
            return new PaymentSimulation(pricingResult.presentValue(), null, null);
        }
        if (paymentCurrency == Currency.USD) {
            ExchangeRateEntity exchangeRateEntity = exchangeRateRepository
                    .findFirstBySourceCurrencyAndTargetCurrencyAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
                            Currency.USD,
                            Currency.BRL,
                            simulatedAt)
                    .orElseThrow(() -> new IllegalStateException("No valid USD to BRL exchange rate found"));
            BigDecimal paymentAmount = currencyConversionService.convert(
                    pricingResult.presentValue(),
                    new ExchangeRate(Currency.USD, Currency.BRL, exchangeRateEntity.getRate()));
            return new PaymentSimulation(
                    paymentAmount,
                    exchangeRateEntity.getRate(),
                    exchangeRateEntity.getEffectiveAt());
        }

        throw new IllegalArgumentException("Unsupported payment currency: " + paymentCurrency);
    }

    private record PaymentSimulation(
            BigDecimal paymentAmount,
            BigDecimal exchangeRate,
            OffsetDateTime exchangeRateEffectiveAt) {
    }
}
