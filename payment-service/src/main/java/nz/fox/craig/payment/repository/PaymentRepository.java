package nz.fox.craig.payment.repository;

import java.util.List;
import java.util.UUID;
import nz.fox.craig.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByCustomerId(UUID customerId);
}
