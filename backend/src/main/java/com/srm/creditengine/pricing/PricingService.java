package com.srm.creditengine.pricing;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

    private static final BigDecimal BASE_RATE = new BigDecimal("0.01");
    private static final MathContext CALCULATION_CONTEXT = new MathContext(34, RoundingMode.HALF_EVEN);
    private static final int MONEY_SCALE = 2;

    private final Map<ReceivableType, PricingStrategy> strategiesByType;

    public PricingService(List<PricingStrategy> strategies) {
        Objects.requireNonNull(strategies, "strategies must not be null");

        this.strategiesByType = new EnumMap<>(ReceivableType.class);
        for (PricingStrategy strategy : strategies) {
            PricingStrategy previous = strategiesByType.put(strategy.supportedType(), strategy);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicated pricing strategy for type: " + strategy.supportedType());
            }
        }
    }

    public PricingResult price(BigDecimal faceValue, int termInMonths, ReceivableType receivableType) {
        validateInput(faceValue, termInMonths, receivableType);

        PricingStrategy strategy = findStrategy(receivableType);
        BigDecimal spread = strategy.spread();
        BigDecimal monthlyRate = BigDecimal.ONE.add(BASE_RATE).add(spread);
        BigDecimal denominator = monthlyRate.pow(termInMonths);

        BigDecimal presentValue = faceValue
                .divide(denominator, CALCULATION_CONTEXT)
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
        BigDecimal discountAmount = faceValue
                .subtract(presentValue)
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);

        return new PricingResult(
                faceValue,
                presentValue,
                discountAmount,
                BASE_RATE,
                spread,
                termInMonths);
    }

    private PricingStrategy findStrategy(ReceivableType receivableType) {
        PricingStrategy strategy = strategiesByType.get(receivableType);
        if (strategy == null) {
            throw new IllegalArgumentException("No pricing strategy found for type: " + receivableType);
        }
        return strategy;
    }

    private void validateInput(BigDecimal faceValue, int termInMonths, ReceivableType receivableType) {
        Objects.requireNonNull(faceValue, "faceValue must not be null");
        Objects.requireNonNull(receivableType, "receivableType must not be null");

        if (faceValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("faceValue must be greater than zero");
        }
        if (termInMonths < 0) {
            throw new IllegalArgumentException("termInMonths must not be negative");
        }
    }
}
