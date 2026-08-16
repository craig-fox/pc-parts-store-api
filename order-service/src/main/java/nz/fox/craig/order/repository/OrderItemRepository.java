package nz.fox.craig.order.repository;

import java.util.UUID;
import nz.fox.craig.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> { }
