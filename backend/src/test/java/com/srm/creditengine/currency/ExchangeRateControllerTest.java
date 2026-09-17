package com.srm.creditengine.currency;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.creditengine.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class ExchangeRateControllerTest {

    private ExchangeRateRepository exchangeRateRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        exchangeRateRepository = mock(ExchangeRateRepository.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExchangeRateController(exchangeRateRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldCreateExchangeRate() throws Exception {
        when(exchangeRateRepository.save(any(ExchangeRateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceCurrency": "USD",
                                  "targetCurrency": "BRL",
                                  "rate": 5.4321,
                                  "effectiveAt": "2026-09-17T10:00:00-03:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceCurrency").value("USD"))
                .andExpect(jsonPath("$.targetCurrency").value("BRL"))
                .andExpect(jsonPath("$.rate").value(5.4321));
    }
}
