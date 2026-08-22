package nz.fox.craig.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import nz.fox.craig.payment.model.Payment;
import nz.fox.craig.payment.model.PaymentStatus;
import nz.fox.craig.test.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class PaymentRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void shouldFindPaymentsByCustomerId() {
        UUID customerId = UUID.randomUUID();

        Payment payment = createPayment(customerId);
        paymentRepository.save(payment);

        List<Payment> payments = paymentRepository.findByCustomerId(customerId);

        assertThat(payments).hasSize(1);
        assertThat(payments.getFirst().getCustomerId()).isEqualTo(customerId);
        assertThat(payments.getFirst().getId()).isNotNull();
        assertThat(payments.getFirst().getCreatedAt()).isNotNull();
        assertThat(payments.getFirst().getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldReturnEmptyListWhenCustomerHasNoPayments() {
        List<Payment> payments =
                paymentRepository.findByCustomerId(UUID.randomUUID());

        assertThat(payments).isEmpty();
    }

    private Payment createPayment(UUID customerId) {
        Payment payment = new Payment();
        payment.setOrderId(UUID.randomUUID());
        payment.setCustomerId(customerId);
        payment.setAmount(new BigDecimal("99.99"));
        payment.setCurrency("NZD");
        payment.setStatus(PaymentStatus.COMPLETED);
        return payment;
    }
}
