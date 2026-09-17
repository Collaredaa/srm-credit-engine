package com.srm.creditengine.receivable;

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

class ReceivableControllerTest {

    private ReceivableRepository receivableRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        receivableRepository = mock(ReceivableRepository.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReceivableController(receivableRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldCreateReceivable() throws Exception {
        when(receivableRepository.save(any(Receivable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/receivables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignor": "Fornecedor XPTO",
                                  "type": "DUPLICATA_MERCANTIL",
                                  "faceValue": 100000.00,
                                  "dueDate": "2026-12-15"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignor").value("Fornecedor XPTO"))
                .andExpect(jsonPath("$.type").value("DUPLICATA_MERCANTIL"))
                .andExpect(jsonPath("$.faceValue").value(100000.00))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void shouldReturnBadRequestForInvalidReceivableRequest() throws Exception {
        mockMvc.perform(post("/api/v1/receivables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignor": "",
                                  "type": "DUPLICATA_MERCANTIL",
                                  "faceValue": -10.00,
                                  "dueDate": "2026-12-15"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
