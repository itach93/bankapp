package afric.remit.bankapp.controller;

import afric.remit.bankapp.dto.TransactionRequest;
import afric.remit.bankapp.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(accountController)
            // .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void credit_WhenValidRequest_ShouldReturnSuccess() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("123456789");
        request.setAmount(new BigDecimal("100.00"));

        doNothing().when(accountService).credit(any(TransactionRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/account/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Credit successful"));

        verify(accountService, times(1)).credit(any(TransactionRequest.class));
    }

   @Test
    void credit_WhenInvalidAmount_ShouldReturnBadRequest() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("123456789");
        request.setAmount(new BigDecimal("-100.00"));

        doThrow(new IllegalArgumentException("Invalid amount"))
            .when(accountService).credit(any(TransactionRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/account/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid amount"));

        verify(accountService, times(1)).credit(any(TransactionRequest.class));
    }

    @Test
    void debit_WhenValidRequest_ShouldReturnSuccess() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("123456789");
        request.setAmount(new BigDecimal("50.00"));

        doNothing().when(accountService).debit(any(TransactionRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/account/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Debit successful"));

        verify(accountService, times(1)).debit(any(TransactionRequest.class));
    }

    @Test
    void debit_WhenAccountNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("nonexistent");
        request.setAmount(new BigDecimal("50.00"));

        // Act & Assert
        mockMvc.perform(post("/api/account/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found"));

        verify(accountService, times(1)).debit(any(TransactionRequest.class));
    }

        @Test
    void testWithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("123456789");
        request.setAmount(new BigDecimal("100.00"));

        // Act & Assert
        mockMvc.perform(post("/api/account/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(accountService, never()).credit(any(TransactionRequest.class));
    }

    @Test
    void credit_WhenMissingAccountNumber_ShouldReturnBadRequest() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("100.00"));

        // Act & Assert
        mockMvc.perform(post("/api/account/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account number is required"));

        verify(accountService, never()).credit(any(TransactionRequest.class));
    }

    @Test
    void credit_WhenMissingAmount_ShouldReturnBadRequest() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("123456789");

        // Act & Assert
        mockMvc.perform(post("/api/account/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount is required"));

        verify(accountService, never()).credit(any(TransactionRequest.class));
    }

    @Test
    void debit_WhenMalformedJson_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/account/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).debit(any(TransactionRequest.class));
    }

    @Test
    void credit_WhenServiceUnavailable_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("123456789");
        request.setAmount(new BigDecimal("100.00"));

        doThrow(new RuntimeException("Service unavailable"))
            .when(accountService).credit(any(TransactionRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/account/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An internal server error occurred"));

        verify(accountService, times(1)).credit(any(TransactionRequest.class));
    }

    @Test
    void debit_WhenAmountExceedsMaxLimit_ShouldReturnBadRequest() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("123456789");
        request.setAmount(new BigDecimal("1000000.00"));

        doThrow(new IllegalArgumentException("Amount exceeds maximum transaction limit"))
            .when(accountService).debit(any(TransactionRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/account/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount exceeds maximum transaction limit"));

        verify(accountService, times(1)).debit(any(TransactionRequest.class));
    }


    @Test
    void testWithInvalidToken_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("123456789");
        request.setAmount(new BigDecimal("100.00"));

        // Act & Assert
        mockMvc.perform(post("/api/account/credit")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(accountService, never()).credit(any(TransactionRequest.class));
    }

    @Test
    void testConcurrentTransactions_ShouldHandleRaceCondition() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber("123456789");
        request.setAmount(new BigDecimal("100.00"));

        doThrow(new RuntimeException("Concurrent modification detected"))
            .when(accountService).credit(any(TransactionRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/account/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                    .value("Concurrent modification detected"));

        verify(accountService, times(1)).credit(any(TransactionRequest.class));
    }
}
