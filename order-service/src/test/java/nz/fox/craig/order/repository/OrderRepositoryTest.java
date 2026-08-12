package nz.fox.craig.order.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderItem;
import nz.fox.craig.order.model.OrderStatus;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class OrderRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private OrderRepository orderRepository;

    @PersistenceContext
    private EntityManager entityManager;


    @Test
    void shouldSaveOrder() {
        final Order order = createOrder(UUID.randomUUID());
        orderRepository.saveAndFlush(order);
        final Order found = orderRepository.findById(order.getId()).orElseThrow();

        assertThat(found.getCustomerId()).isEqualTo(order.getCustomerId());
        assertThat(found.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(found.getSubtotal()).isEqualByComparingTo("1000.00");
        assertThat(found.getShipping()).isEqualByComparingTo("25.00");
        assertThat(found.getTotal()).isEqualByComparingTo("1025.00");
    }

    @Test
    void shouldSaveOrderWithItems() {
        final Order order = createOrder(UUID.randomUUID());

        addItemsToOrder(order);
        orderRepository.saveAndFlush(order);
        entityManager.flush();
        entityManager.clear();

        final Order found = orderRepository.findById(order.getId()).orElseThrow();

        assertThat(found.getItems()).hasSize(2);
        final OrderItem item = found.getItems().getFirst();

        assertThat(item.getProductName())
        .isEqualTo("RTX 5070");

        assertThat(item.getUnitPrice())
        .isEqualByComparingTo("899.00");

        assertThat(item.getQuantity())
        .isEqualTo(1);
    }

    @Test
    void shouldFindOrdersByCustomerId() {
        final UUID customerId = UUID.randomUUID();

        final Order order1 = createOrder(customerId);
        final Order order2 = createOrder(customerId);

        orderRepository.save(order1);
        orderRepository.save(order2);

        final List<Order> orders =
                orderRepository.findByCustomerId(customerId);

        assertThat(orders)
                .hasSize(2)
                .extracting(Order::getId)
                .containsExactlyInAnyOrder(
                        order1.getId(),
                        order2.getId());
    }

    @Test
    void shouldRemoveItemFromOrder() {
        final Order order = createOrder(UUID.randomUUID());
        addItemsToOrder(order);
        orderRepository.save(order);
        final Order found = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(found.getItems()).hasSize(2);
        found.getItems().removeFirst();
        orderRepository.save(found);
        entityManager.flush();
        entityManager.clear();
        final Order reloaded = orderRepository.findById(found.getId()).orElseThrow();
        final OrderItem item = reloaded.getItems().getFirst();
        assertThat(reloaded.getItems()).hasSize(1);
        assertThat(item.getOrder()).isNotNull();
        assertThat(item.getOrder().getId())
                .isEqualTo(found.getId());
    }

    @Test
    void shouldReturnEmptyListWhenCustomerHasNoOrders() {
        final List<Order> orders =
                orderRepository.findByCustomerId(UUID.randomUUID());
        assertThat(orders).isEmpty();
    }

    @Test
    void shouldOnlyFindOrdersForSpecifiedCustomer() {
        final UUID customerId = UUID.randomUUID();
        final UUID otherCustomerId = UUID.randomUUID();

        final Order customerOrder = createOrder(customerId);
        final Order otherCustomerOrder = createOrder(otherCustomerId);

        orderRepository.save(customerOrder);
        orderRepository.save(otherCustomerOrder);

        final List<Order> orders =
                orderRepository.findByCustomerId(customerId);

        assertThat(orders)
                .hasSize(1)
                .first()
                .extracting(Order::getCustomerId)
                .isEqualTo(customerId);
    }
    

    private Order createOrder(UUID customerId) {
        return Order.builder()
                .customerId(customerId)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PLACED)
                .subtotal(new BigDecimal("1000.00"))
                .shipping(new BigDecimal("25.00"))
                .total(new BigDecimal("1025.00"))
                .build();
    }

    private OrderItem createOrderItem(UUID productId,
            String productName,
            int quantity,
            BigDecimal price) {
        return OrderItem.builder()
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .unitPrice(price)
                .build();
    }

    private void addItemsToOrder(Order order) {
        order.addItem(createOrderItem(
            UUID.randomUUID(),
            "RTX 5070",
            1,
            new BigDecimal("899.00")));

        order.addItem(createOrderItem(
            UUID.randomUUID(),
            "Ryzen 9800X3D",
            2,
            new BigDecimal("449.00")));
    }

}
