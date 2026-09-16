package com.srm.creditengine.settlement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.srm.creditengine.currency.Currency;
import com.srm.creditengine.currency.CurrencyConversionService;
import com.srm.creditengine.currency.ExchangeRateEntity;
import com.srm.creditengine.currency.ExchangeRateRepository;
import com.srm.creditengine.pricing.ChequePricingStrategy;
import com.srm.creditengine.pricing.DuplicataPricingStrategy;
import com.srm.creditengine.pricing.PricingService;
import com.srm.creditengine.pricing.ReceivableType;
import com.srm.creditengine.receivable.Receivable;
import com.srm.creditengine.receivable.ReceivableRepository;
import com.srm.creditengine.receivable.ReceivableStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SettlementServiceTest {

    @Mock
    private ReceivableRepository receivableRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        PricingService pricingService = new PricingService(List.of(
                new DuplicataPricingStrategy(),
                new ChequePricingStrategy()));
        settlementService = new SettlementService(
                receivableRepository,
                settlementRepository,
                exchangeRateRepository,
                pricingService,
                new CurrencyConversionService());
    }

    @Test
    void shouldSettleInBrlSuccessfully() {
        UUID receivableId = UUID.randomUUID();
        Receivable receivable = duplicataReceivable();
        SettlementRequest request = new SettlementRequest(receivableId, Currency.BRL, "idem-brl-1");
        when(settlementRepository.findByIdempotencyKey("idem-brl-1")).thenReturn(Optional.empty());
        when(receivableRepository.findById(receivableId)).thenReturn(Optional.of(receivable));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SettlementResult result = settlementService.settle(request);

        assertEquals(new BigDecimal("100000.00"), result.faceValue());
        assertEquals(new BigDecimal("92859.94"), result.presentValueBrl());
        assertEquals(new BigDecimal("92859.94"), result.paymentAmount());
        assertEquals(Currency.BRL, result.paymentCurrency());
        assertNull(result.exchangeRate());
        assertNull(result.exchangeRateEffectiveAt());
        assertEquals(ReceivableStatus.SETTLED, receivable.getStatus());
        verifyNoInteractions(exchangeRateRepository);
        verify(receivableRepository).save(receivable);
    }

    @Test
    void shouldSettleInUsdUsingExchangeRate() {
        UUID receivableId = UUID.randomUUID();
        Receivable receivable = duplicataReceivable();
        OffsetDateTime effectiveAt = OffsetDateTime.now().minusHours(1);
        ExchangeRateEntity exchangeRate = new ExchangeRateEntity(
                Currency.USD,
                Currency.BRL,
                new BigDecimal("5.4321"),
                effectiveAt);
        SettlementRequest request = new SettlementRequest(receivableId, Currency.USD, "idem-usd-1");
        when(settlementRepository.findByIdempotencyKey("idem-usd-1")).thenReturn(Optional.empty());
        when(receivableRepository.findById(receivableId)).thenReturn(Optional.of(receivable));
        when(exchangeRateRepository.findFirstBySourceCurrencyAndTargetCurrencyAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
                any(),
                any(),
                any()))
                .thenReturn(Optional.of(exchangeRate));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SettlementResult result = settlementService.settle(request);

        assertEquals(new BigDecimal("92859.94"), result.presentValueBrl());
        assertEquals(new BigDecimal("17094.67"), result.paymentAmount());
        assertEquals(Currency.USD, result.paymentCurrency());
        assertEquals(new BigDecimal("5.4321"), result.exchangeRate());
        assertEquals(effectiveAt, result.exchangeRateEffectiveAt());
        assertEquals(ReceivableStatus.SETTLED, receivable.getStatus());
    }

    @Test
    void shouldReturnExistingSettlementForSameIdempotencyKey() {
        UUID receivableId = UUID.randomUUID();
        Settlement existingSettlement = new Settlement(
                receivableId,
                "idem-repeat-1",
                new BigDecimal("100000.00"),
                new BigDecimal("92859.94"),
                new BigDecimal("92859.94"),
                Currency.BRL,
                new BigDecimal("0.01"),
                new BigDecimal("0.015"),
                null,
                null,
                OffsetDateTime.now());
        when(settlementRepository.findByIdempotencyKey("idem-repeat-1")).thenReturn(Optional.of(existingSettlement));
        SettlementRequest request = new SettlementRequest(receivableId, Currency.BRL, "idem-repeat-1");

        SettlementResult repeatedResult = settlementService.settle(request);

        assertEquals(new BigDecimal("92859.94"), repeatedResult.paymentAmount());
        assertEquals(receivableId, repeatedResult.receivableId());
        assertEquals(Currency.BRL, repeatedResult.paymentCurrency());
        verify(settlementRepository, never()).save(any(Settlement.class));
        verifyNoInteractions(receivableRepository);
    }

    @Test
    void shouldFailWhenSameIdempotencyKeyHasDifferentReceivableId() {
        UUID originalReceivableId = UUID.randomUUID();
        UUID newReceivableId = UUID.randomUUID();
        Settlement existingSettlement = existingBrlSettlement(originalReceivableId, "idem-different-receivable-1");
        when(settlementRepository.findByIdempotencyKey("idem-different-receivable-1"))
                .thenReturn(Optional.of(existingSettlement));
        SettlementRequest request = new SettlementRequest(
                newReceivableId,
                Currency.BRL,
                "idem-different-receivable-1");

        assertThrows(IllegalArgumentException.class, () -> settlementService.settle(request));

        verify(settlementRepository, never()).save(any(Settlement.class));
        verifyNoInteractions(receivableRepository);
    }

    @Test
    void shouldFailWhenSameIdempotencyKeyHasDifferentPaymentCurrency() {
        UUID receivableId = UUID.randomUUID();
        Settlement existingSettlement = existingBrlSettlement(receivableId, "idem-different-currency-1");
        when(settlementRepository.findByIdempotencyKey("idem-different-currency-1"))
                .thenReturn(Optional.of(existingSettlement));
        SettlementRequest request = new SettlementRequest(
                receivableId,
                Currency.USD,
                "idem-different-currency-1");

        assertThrows(IllegalArgumentException.class, () -> settlementService.settle(request));

        verify(settlementRepository, never()).save(any(Settlement.class));
        verifyNoInteractions(receivableRepository);
    }

    @Test
    void shouldFailWhenReceivableIsAlreadySettled() {
        UUID receivableId = UUID.randomUUID();
        Receivable receivable = duplicataReceivable();
        receivable.settle();
        when(settlementRepository.findByIdempotencyKey("idem-settled-1")).thenReturn(Optional.empty());
        when(receivableRepository.findById(receivableId)).thenReturn(Optional.of(receivable));
        SettlementRequest request = new SettlementRequest(receivableId, Currency.BRL, "idem-settled-1");

        assertThrows(IllegalStateException.class, () -> settlementService.settle(request));

        verify(settlementRepository, never()).save(any(Settlement.class));
        verify(receivableRepository, never()).save(any(Receivable.class));
    }

    @Test
    void shouldFailAndKeepReceivableOpenWhenUsdRateDoesNotExist() {
        UUID receivableId = UUID.randomUUID();
        Receivable receivable = duplicataReceivable();
        when(settlementRepository.findByIdempotencyKey("idem-missing-rate-1")).thenReturn(Optional.empty());
        when(receivableRepository.findById(receivableId)).thenReturn(Optional.of(receivable));
        when(exchangeRateRepository.findFirstBySourceCurrencyAndTargetCurrencyAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
                any(),
                any(),
                any()))
                .thenReturn(Optional.empty());
        SettlementRequest request = new SettlementRequest(receivableId, Currency.USD, "idem-missing-rate-1");

        assertThrows(IllegalStateException.class, () -> settlementService.settle(request));

        assertEquals(ReceivableStatus.OPEN, receivable.getStatus());
        verify(settlementRepository, never()).save(any(Settlement.class));
        verify(receivableRepository, never()).save(any(Receivable.class));
    }

    private Receivable duplicataReceivable() {
        return new Receivable(
                "Fornecedor SRM",
                ReceivableType.DUPLICATA_MERCANTIL,
                new BigDecimal("100000.00"),
                LocalDate.now().plusMonths(3));
    }

    private Settlement existingBrlSettlement(UUID receivableId, String idempotencyKey) {
        return new Settlement(
                receivableId,
                idempotencyKey,
                new BigDecimal("100000.00"),
                new BigDecimal("92859.94"),
                new BigDecimal("92859.94"),
                Currency.BRL,
                new BigDecimal("0.01"),
                new BigDecimal("0.015"),
                null,
                null,
                OffsetDateTime.now());
    }
}
