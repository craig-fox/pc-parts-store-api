package nz.fox.craig.order.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nz.fox.craig.order.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

}
