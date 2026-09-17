package com.srm.creditengine.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.srm.creditengine.currency.Currency;
import com.srm.creditengine.currency.CurrencyConversionService;
import com.srm.creditengine.currency.ExchangeRateEntity;
import com.srm.creditengine.currency.ExchangeRateRepository;
import com.srm.creditengine.receivable.ReceivableRepository;
import com.srm.creditengine.settlement.SettlementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PricingSimulationServiceTest {

    private ExchangeRateRepository exchangeRateRepository;
    private PricingSimulationService pricingSimulationService;

    @BeforeEach
    void setUp() {
        exchangeRateRepository = mock(ExchangeRateRepository.class);
        PricingService pricingService = new PricingService(List.of(
                new DuplicataPricingStrategy(),
                new ChequePricingStrategy()));
        pricingSimulationService = new PricingSimulationService(
                pricingService,
                new CurrencyConversionService(),
                exchangeRateRepository,
                new TermCalculator());
    }

    @Test
    void shouldSimulateBrlPricing() {
        PricingSimulationRequest request = new PricingSimulationRequest(
                ReceivableType.DUPLICATA_MERCANTIL,
                new BigDecimal("100000.00"),
                LocalDate.now().plusMonths(3),
                Currency.BRL);

        PricingSimulationResponse response = pricingSimulationService.simulate(request);

        assertEquals(new BigDecimal("100000.00"), response.faceValue());
        assertEquals(new BigDecimal("92859.94"), response.presentValueBrl());
        assertEquals(new BigDecimal("7140.06"), response.discountAmount());
        assertEquals(Currency.BRL, response.paymentCurrency());
        assertEquals(new BigDecimal("92859.94"), response.paymentAmount());
        assertEquals(new BigDecimal("0.01"), response.baseRate());
        assertEquals(new BigDecimal("0.015"), response.spread());
        assertEquals(3, response.termInMonths());
        assertNull(response.exchangeRate());
        assertNull(response.exchangeRateEffectiveAt());
        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    void shouldSimulateUsdPricingUsingExchangeRate() {
        OffsetDateTime effectiveAt = OffsetDateTime.now().minusHours(1);
        ExchangeRateEntity exchangeRate = new ExchangeRateEntity(
                Currency.USD,
                Currency.BRL,
                new BigDecimal("5.4321"),
                effectiveAt);
        when(exchangeRateRepository.findFirstBySourceCurrencyAndTargetCurrencyAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
                any(),
                any(),
                any()))
                .thenReturn(Optional.of(exchangeRate));
        PricingSimulationRequest request = new PricingSimulationRequest(
                ReceivableType.DUPLICATA_MERCANTIL,
                new BigDecimal("100000.00"),
                LocalDate.now().plusMonths(3),
                Currency.USD);

        PricingSimulationResponse response = pricingSimulationService.simulate(request);

        assertEquals(new BigDecimal("92859.94"), response.presentValueBrl());
        assertEquals(new BigDecimal("7140.06"), response.discountAmount());
        assertEquals(Currency.USD, response.paymentCurrency());
        assertEquals(new BigDecimal("17094.67"), response.paymentAmount());
        assertEquals(new BigDecimal("5.4321"), response.exchangeRate());
        assertEquals(effectiveAt, response.exchangeRateEffectiveAt());
    }

    @Test
    void shouldFailWhenUsdRateDoesNotExist() {
        when(exchangeRateRepository.findFirstBySourceCurrencyAndTargetCurrencyAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
                any(),
                any(),
                any()))
                .thenReturn(Optional.empty());
        PricingSimulationRequest request = new PricingSimulationRequest(
                ReceivableType.DUPLICATA_MERCANTIL,
                new BigDecimal("100000.00"),
                LocalDate.now().plusMonths(3),
                Currency.USD);

        assertThrows(IllegalStateException.class, () -> pricingSimulationService.simulate(request));
    }

    @Test
    void shouldNotDependOnReceivableOrSettlementRepositories() {
        List<? extends Class<?>> fieldTypes = Arrays.stream(PricingSimulationService.class.getDeclaredFields())
                .map(field -> field.getType())
                .toList();

        assertFalse(fieldTypes.contains(ReceivableRepository.class));
        assertFalse(fieldTypes.contains(SettlementRepository.class));
    }
}
