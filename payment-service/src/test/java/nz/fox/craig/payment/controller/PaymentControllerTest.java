package nz.fox.craig.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nz.fox.craig.payment.dto.CreatePaymentRequest;
import nz.fox.craig.payment.dto.PaymentResponse;
import nz.fox.craig.payment.model.PaymentStatus;
import nz.fox.craig.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void shouldCreatePayment() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        orderId,
                        customerId,
                        new BigDecimal("149.99"),
                        "NZD");

        PaymentResponse response =
                new PaymentResponse(
                        paymentId,
                        orderId,
                        customerId,
                        new BigDecimal("149.99"),
                        "NZD",
                        PaymentStatus.COMPLETED,
                        Instant.now(),
                        Instant.now());

        when(paymentService.createPayment(any(CreatePaymentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.amount").value(149.99))
                .andExpect(jsonPath("$.currency").value("NZD"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(paymentService).createPayment(any(CreatePaymentRequest.class));
    }

    @Test
    void shouldGetPaymentsByCustomerId() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        PaymentResponse response =
                new PaymentResponse(
                        paymentId,
                        orderId,
                        customerId,
                        new BigDecimal("149.99"),
                        "NZD",
                        PaymentStatus.COMPLETED,
                        Instant.now(),
                        Instant.now());

        when(paymentService.getPaymentsByCustomer(customerId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/payments/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(paymentId.toString()))
                .andExpect(jsonPath("$[0].orderId").value(orderId.toString()))
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$[0].amount").value(149.99))
                .andExpect(jsonPath("$[0].currency").value("NZD"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        verify(paymentService).getPaymentsByCustomer(customerId);
    }

    @Test
    void shouldReturnEmptyListWhenCustomerHasNoPayments() throws Exception {
        UUID customerId = UUID.randomUUID();

        when(paymentService.getPaymentsByCustomer(customerId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/payments/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(paymentService).getPaymentsByCustomer(customerId);
    }

    @Test
    void shouldRejectPaymentWhenOrderIdIsMissing() throws Exception {
        String request =
                """
                {
                    "customerId": "%s",
                    "amount": 149.99,
                    "currency": "NZD"
                }
                """
                        .formatted(UUID.randomUUID());

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectPaymentWhenCustomerIdIsMissing() throws Exception {
        String request =
                """
                {
                    "orderId": "%s",
                    "amount": 149.99,
                    "currency": "NZD"
                }
                """
                        .formatted(UUID.randomUUID());

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectPaymentWhenAmountIsInvalid() throws Exception {
        String request =
                """
                {
                    "orderId": "%s",
                    "customerId": "%s",
                    "amount": 0,
                    "currency": "NZD"
                }
                """
                        .formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectPaymentWhenCurrencyIsMissing() throws Exception {
        String request =
                """
                {
                    "orderId": "%s",
                    "customerId": "%s",
                    "amount": 149.99
                }
                """
                        .formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isBadRequest());
    }
}