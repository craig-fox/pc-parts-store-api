package nz.fox.craig.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nz.fox.craig.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerId(UUID customerId);

    List<Order> findByCustomerIdOrderByOrderDateDesc(UUID customerId);

    Optional<Order> findByCustomerIdAndIdempotencyKey(
        UUID customerId, String idempotencyKey);
}
