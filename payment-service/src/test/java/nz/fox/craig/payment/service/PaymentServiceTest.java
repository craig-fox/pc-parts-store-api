package nz.fox.craig.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nz.fox.craig.payment.dto.CreatePaymentRequest;
import nz.fox.craig.payment.dto.PaymentResponse;
import nz.fox.craig.payment.model.Payment;
import nz.fox.craig.payment.model.PaymentStatus;
import nz.fox.craig.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private UUID paymentId;
    private UUID orderId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    void shouldCreatePayment() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                orderId,
                customerId,
                new BigDecimal("149.99"),
                "NZD");

        Payment savedPayment = createPayment();

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response.id()).isEqualTo(paymentId);
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.amount()).isEqualByComparingTo("149.99");
        assertThat(response.currency()).isEqualTo("NZD");
        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.createdAt()).isEqualTo(savedPayment.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(savedPayment.getUpdatedAt());
       
    }

    @Test
    void shouldCreatePaymentWithCompletedStatus() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                orderId,
                customerId,
                new BigDecimal("149.99"),
                "NZD");

        Payment savedPayment = createPayment();

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        paymentService.createPayment(request);

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository).save(paymentCaptor.capture());

        Payment payment = paymentCaptor.getValue();

        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getCustomerId()).isEqualTo(customerId);
        assertThat(payment.getAmount()).isEqualByComparingTo("149.99");
        assertThat(payment.getCurrency()).isEqualTo("NZD");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void shouldGetPaymentsByCustomerId() {
        Payment firstPayment = createPayment();
        Payment secondPayment = createPayment();
        secondPayment.setAmount(new BigDecimal("299.99"));

        when(paymentRepository.findByCustomerId(customerId))
                .thenReturn(List.of(firstPayment, secondPayment));

        List<PaymentResponse> responses =
                paymentService.getPaymentsByCustomer(customerId);

        assertThat(responses).hasSize(2);

        assertThat(responses.get(0).id()).isEqualTo(paymentId);
        assertThat(responses.get(0).amount()).isEqualByComparingTo("149.99");

        assertThat(responses.get(1).amount()).isEqualByComparingTo("299.99");

        verify(paymentRepository).findByCustomerId(customerId);
    }

    @Test
    void shouldReturnEmptyListWhenCustomerHasNoPayments() {
        when(paymentRepository.findByCustomerId(customerId))
                .thenReturn(List.of());

        List<PaymentResponse> responses =
                paymentService.getPaymentsByCustomer(customerId);

        assertThat(responses).isEmpty();

        verify(paymentRepository).findByCustomerId(customerId);
    }

    @Test
    void shouldReturnExistingPaymentWhenPaymentAlreadyExistsForOrder() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                orderId,
                customerId,
                new BigDecimal("149.99"),
                "NZD");
    
        Payment existingPayment = createPayment();
    
        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(existingPayment));
    
        PaymentResponse response = paymentService.createPayment(request);
    
        assertThat(response.id()).isEqualTo(paymentId);
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.amount()).isEqualByComparingTo("149.99");
        assertThat(response.currency()).isEqualTo("NZD");
        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
    
        verify(paymentRepository).findByOrderId(orderId);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private Payment createPayment() {
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setOrderId(orderId);
        payment.setCustomerId(customerId);
        payment.setAmount(new BigDecimal("149.99"));
        payment.setCurrency("NZD");
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        return payment;
    }
}
