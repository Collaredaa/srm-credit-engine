package com.srm.creditengine.settlement;

import com.srm.creditengine.currency.Currency;
import com.srm.creditengine.currency.CurrencyConversionService;
import com.srm.creditengine.currency.ExchangeRate;
import com.srm.creditengine.currency.ExchangeRateEntity;
import com.srm.creditengine.currency.ExchangeRateRepository;
import com.srm.creditengine.pricing.PricingResult;
import com.srm.creditengine.pricing.PricingService;
import com.srm.creditengine.receivable.Receivable;
import com.srm.creditengine.receivable.ReceivableRepository;
import com.srm.creditengine.receivable.ReceivableStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {

    private final ReceivableRepository receivableRepository;
    private final SettlementRepository settlementRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final PricingService pricingService;
    private final CurrencyConversionService currencyConversionService;

    public SettlementService(
            ReceivableRepository receivableRepository,
            SettlementRepository settlementRepository,
            ExchangeRateRepository exchangeRateRepository,
            PricingService pricingService,
            CurrencyConversionService currencyConversionService) {
        this.receivableRepository = Objects.requireNonNull(receivableRepository, "receivableRepository must not be null");
        this.settlementRepository = Objects.requireNonNull(settlementRepository, "settlementRepository must not be null");
        this.exchangeRateRepository = Objects.requireNonNull(exchangeRateRepository, "exchangeRateRepository must not be null");
        this.pricingService = Objects.requireNonNull(pricingService, "pricingService must not be null");
        this.currencyConversionService = Objects.requireNonNull(currencyConversionService, "currencyConversionService must not be null");
    }

    @Transactional
    public SettlementResult settle(SettlementRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        return settlementRepository.findByIdempotencyKey(request.idempotencyKey())
                .map(existingSettlement -> validateIdempotentRequest(existingSettlement, request))
                .orElseGet(() -> createSettlement(request));
    }

    @Transactional(readOnly = true)
    public Page<SettlementResult> findAll(Pageable pageable) {
        return settlementRepository.findAll(pageable).map(this::toResult);
    }

    private SettlementResult validateIdempotentRequest(Settlement existingSettlement, SettlementRequest request) {
        if (!existingSettlement.getReceivableId().equals(request.receivableId())) {
            throw new IllegalArgumentException("Idempotency key was already used for a different receivable");
        }
        if (existingSettlement.getPaymentCurrency() != request.paymentCurrency()) {
            throw new IllegalArgumentException("Idempotency key was already used for a different payment currency");
        }
        return toResult(existingSettlement);
    }

    private SettlementResult createSettlement(SettlementRequest request) {
        OffsetDateTime settledAt = OffsetDateTime.now();
        Receivable receivable = receivableRepository.findById(request.receivableId())
                .orElseThrow(() -> new IllegalArgumentException("Receivable not found: " + request.receivableId()));

        if (receivable.getStatus() != ReceivableStatus.OPEN) {
            throw new IllegalStateException("Receivable is not open for settlement");
        }

        int termInMonths = calculateTermInMonths(settledAt.toLocalDate(), receivable.getDueDate());
        PricingResult pricingResult = pricingService.price(
                receivable.getFaceValue(),
                termInMonths,
                receivable.getType());
        PaymentSnapshot paymentSnapshot = calculatePayment(request.paymentCurrency(), pricingResult, settledAt);

        Settlement settlement = new Settlement(
                request.receivableId(),
                request.idempotencyKey(),
                pricingResult.faceValue(),
                pricingResult.presentValue(),
                paymentSnapshot.paymentAmount(),
                request.paymentCurrency(),
                pricingResult.baseRate(),
                pricingResult.spread(),
                paymentSnapshot.exchangeRate(),
                paymentSnapshot.exchangeRateEffectiveAt(),
                settledAt);

        Settlement savedSettlement = settlementRepository.save(settlement);
        receivable.settle();
        receivableRepository.save(receivable);

        return toResult(savedSettlement);
    }

    private PaymentSnapshot calculatePayment(
            Currency paymentCurrency,
            PricingResult pricingResult,
            OffsetDateTime settledAt) {
        if (paymentCurrency == Currency.BRL) {
            return new PaymentSnapshot(pricingResult.presentValue(), null, null);
        }
        if (paymentCurrency == Currency.USD) {
            ExchangeRateEntity exchangeRateEntity = exchangeRateRepository
                    .findFirstBySourceCurrencyAndTargetCurrencyAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
                            Currency.USD,
                            Currency.BRL,
                            settledAt)
                    .orElseThrow(() -> new IllegalStateException("No valid USD to BRL exchange rate found"));
            BigDecimal paymentAmount = currencyConversionService.convert(
                    pricingResult.presentValue(),
                    new ExchangeRate(Currency.USD, Currency.BRL, exchangeRateEntity.getRate()));
            return new PaymentSnapshot(
                    paymentAmount,
                    exchangeRateEntity.getRate(),
                    exchangeRateEntity.getEffectiveAt());
        }

        throw new IllegalArgumentException("Unsupported payment currency: " + paymentCurrency);
    }

    private int calculateTermInMonths(LocalDate currentDate, LocalDate dueDate) {
        long termInMonths = ChronoUnit.MONTHS.between(currentDate, dueDate);
        if (termInMonths < 0) {
            throw new IllegalArgumentException("dueDate must not be before the settlement date");
        }
        return Math.toIntExact(termInMonths);
    }

    private SettlementResult toResult(Settlement settlement) {
        return new SettlementResult(
                settlement.getId(),
                settlement.getReceivableId(),
                settlement.getIdempotencyKey(),
                settlement.getFaceValue(),
                settlement.getPresentValueBrl(),
                settlement.getPaymentAmount(),
                settlement.getPaymentCurrency(),
                settlement.getBaseRate(),
                settlement.getSpread(),
                settlement.getExchangeRate(),
                settlement.getExchangeRateEffectiveAt(),
                settlement.getSettledAt());
    }

    private record PaymentSnapshot(
            BigDecimal paymentAmount,
            BigDecimal exchangeRate,
            OffsetDateTime exchangeRateEffectiveAt) {
    }
}
