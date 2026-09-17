package com.srm.creditengine.settlement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.creditengine.currency.Currency;
import com.srm.creditengine.shared.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class SettlementControllerTest {

    private SettlementService settlementService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        settlementService = mock(SettlementService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new SettlementController(settlementService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldCreateSettlement() throws Exception {
        UUID receivableId = UUID.randomUUID();
        when(settlementService.settle(any(SettlementRequest.class))).thenReturn(new SettlementResult(
                UUID.randomUUID(),
                receivableId,
                "idem-123",
                new BigDecimal("100000.00"),
                new BigDecimal("92859.94"),
                new BigDecimal("92859.94"),
                Currency.BRL,
                new BigDecimal("0.01"),
                new BigDecimal("0.015"),
                null,
                null,
                OffsetDateTime.now()));

        mockMvc.perform(post("/api/v1/settlements")
                        .header("Idempotency-Key", "idem-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receivableId": "%s",
                                  "paymentCurrency": "BRL"
                                }
                                """.formatted(receivableId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receivableId").value(receivableId.toString()))
                .andExpect(jsonPath("$.paymentCurrency").value("BRL"))
                .andExpect(jsonPath("$.paymentAmount").value(92859.94));
    }

    @Test
    void shouldReturnConflictForBusinessError() throws Exception {
        UUID receivableId = UUID.randomUUID();
        when(settlementService.settle(any(SettlementRequest.class)))
                .thenThrow(new IllegalStateException("Receivable is not open for settlement"));

        mockMvc.perform(post("/api/v1/settlements")
                        .header("Idempotency-Key", "idem-456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receivableId": "%s",
                                  "paymentCurrency": "BRL"
                                }
                                """.formatted(receivableId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Receivable is not open for settlement"));
    }
}
