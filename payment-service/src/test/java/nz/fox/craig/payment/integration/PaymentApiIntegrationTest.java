package nz.fox.craig.payment.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import nz.fox.craig.payment.model.Payment;
import nz.fox.craig.payment.model.PaymentStatus;
import nz.fox.craig.payment.repository.PaymentRepository;
import nz.fox.craig.test.AbstractPostgresTest;
import nz.fox.craig.test.JwtTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentApiIntegrationTest extends AbstractPostgresTest {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired private MockMvc mockMvc;

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private JsonMapper objectMapper;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void shouldCreatePayment() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        String request =
                """
                {
                    "orderId": "%s",
                    "customerId": "%s",
                    "amount": 149.99,
                    "currency": "NZD"
                }
                """
                        .formatted(orderId, customerId);

        mockMvc.perform(
                        post("/api/payments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.amount").value(149.99))
                .andExpect(jsonPath("$.currency").value("NZD"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        List<Payment> payments = paymentRepository.findByCustomerId(customerId);

        assertThat(payments).hasSize(1);

        Payment payment = payments.getFirst();
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getCustomerId()).isEqualTo(customerId);
        assertThat(payment.getAmount()).isEqualByComparingTo("149.99");
        assertThat(payment.getCurrency()).isEqualTo("NZD");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void shouldReturnExistingPaymentWhenOrderIsSubmittedTwice() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        String request =
                """
                {
                    "orderId": "%s",
                    "customerId": "%s",
                    "amount": 149.99,
                    "currency": "NZD"
                }
                """
                        .formatted(orderId, customerId);

        String firstResponse =
                mockMvc.perform(
                                post("/api/payments")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(request))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                        .andExpect(jsonPath("$.status").value("COMPLETED"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String secondResponse =
                mockMvc.perform(
                                post("/api/payments")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(request))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                        .andExpect(jsonPath("$.status").value("COMPLETED"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UUID firstPaymentId =
                UUID.fromString(objectMapper.readTree(firstResponse).get("id").asText());

        UUID secondPaymentId =
                UUID.fromString(objectMapper.readTree(secondResponse).get("id").asText());

        assertThat(secondPaymentId).isEqualTo(firstPaymentId);

        List<Payment> payments = paymentRepository.findByCustomerId(customerId);

        assertThat(payments).hasSize(1);
        assertThat(payments.getFirst().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void shouldRequireAuthenticationWhenCreatingPayment() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        String request =
                """
                {
                    "orderId": "%s",
                    "customerId": "%s",
                    "amount": 149.99,
                    "currency": "NZD"
                }
                """
                        .formatted(orderId, customerId);

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isUnauthorized());

        assertThat(paymentRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRequireAuthenticationWhenGettingPayments() throws Exception {
        mockMvc.perform(get("/api/payments/customer/{customerId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("invalidCurrencies")
    void shouldRejectInvalidCurrencyFormat(String currency) throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        String request =
                """
                {
                    "orderId": "%s",
                    "customerId": "%s",
                    "amount": 149.99,
                    "currency": "%s"
                }
                """
                        .formatted(orderId, customerId, currency);

        mockMvc.perform(
                        post("/api/payments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isBadRequest());

        assertThat(paymentRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectPaymentWhenAmountIsZero() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        String request =
                """
                {
                    "orderId": "%s",
                    "customerId": "%s",
                    "amount": 0,
                    "currency": "NZD"
                }
                """
                        .formatted(orderId, customerId);

        mockMvc.perform(
                        post("/api/payments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isBadRequest());

        assertThat(paymentRepository.findAll()).isEmpty();
    }

    private static Stream<String> invalidCurrencies() {
        return Stream.of("NZ", "NZDD", "12D", "N D");
    }
}
