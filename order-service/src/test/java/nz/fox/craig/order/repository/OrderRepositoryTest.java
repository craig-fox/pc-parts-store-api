package nz.fox.craig.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import nz.fox.craig.order.fixture.OrderFixture;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderItem;
import nz.fox.craig.order.model.OrderStatus;
import nz.fox.craig.test.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;


@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class OrderRepositoryTest extends AbstractPostgresTest {

    @Autowired private OrderRepository orderRepository;

    @PersistenceContext private EntityManager entityManager;

    @Test
    void shouldSaveOrder() {
        final Order order = OrderFixture.anOrder();
        orderRepository.saveAndFlush(order);
        final Order found = orderRepository.findById(order.getId()).orElseThrow();

        assertThat(found.getCustomerId()).isEqualTo(order.getCustomerId());
        assertThat(found.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(found.getSubtotal()).isEqualByComparingTo("1000.00");
        assertThat(found.getShipping()).isEqualByComparingTo("25.00");
        assertThat(found.getTotal()).isEqualByComparingTo("1025.00");
        assertThat(found.getShippingAddress()).isNotNull();
        assertThat(found.getShippingAddress().addressLine1()).isEqualTo("1 Main St");
        assertThat(found.getShippingAddress().city()).isEqualTo("Auckland");
        assertThat(found.getShippingAddress().postcode()).isEqualTo("1010");
        assertThat(found.getShippingAddress().country()).isEqualTo("NZ");
    }

    @Test
    void shouldSaveOrderWithItems() {
        final Order order = OrderFixture.anOrder();

        addItemsToOrder(order);
        orderRepository.saveAndFlush(order);
        entityManager.flush();
        entityManager.clear();

        final Order found = orderRepository.findById(order.getId()).orElseThrow();

        assertThat(found.getItems()).hasSize(2);
        final OrderItem item = found.getItems().getFirst();

        assertThat(item.getProductName()).isEqualTo("RTX 5070");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("899.00");
        assertThat(item.getQuantity()).isEqualTo(1);
    }


    @Test
    void shouldRemoveItemFromOrder() {
        final Order order = OrderFixture.anOrder();
        addItemsToOrder(order);
        orderRepository.save(order);
        final Order found = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(found.getItems()).hasSize(3);
        found.getItems().removeFirst();
        orderRepository.save(found);
        entityManager.flush();
        entityManager.clear();
        final Order reloaded = orderRepository.findById(found.getId()).orElseThrow();
        final OrderItem item = reloaded.getItems().getFirst();
        assertThat(reloaded.getItems()).hasSize(2);
        assertThat(item.getOrder()).isNotNull();
        assertThat(item.getOrder().getId()).isEqualTo(found.getId());
    }

    @Test
    void shouldFindOrderForCustomer() {
        UUID customerId = UUID.randomUUID();

        Order order = OrderFixture.anOrder();
        order.setCustomerId(customerId);
        orderRepository.saveAndFlush(order);

        Optional<Order> found =
                orderRepository.findByIdAndCustomerId(order.getId(), customerId);

        assertThat(found)
                .isPresent()
                .get()
                .extracting(Order::getId)
                .isEqualTo(order.getId());
    }

    @Test
    void shouldNotFindOrderForDifferentCustomer() {
        UUID customerId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();

        Order order = OrderFixture.anOrder();
        order.setCustomerId(customerId);
        orderRepository.saveAndFlush(order);

        Optional<Order> found =
                orderRepository.findByIdAndCustomerId(order.getId(), otherCustomerId);

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindCustomerOrdersNewestFirst() {
        UUID customerId = UUID.randomUUID();

        Order olderOrder = OrderFixture.anOrder();
        olderOrder.setCustomerId(customerId);
        olderOrder.setOrderDate(LocalDateTime.of(2026, 1, 1, 10, 0));

        Order newerOrder = OrderFixture.anOrder();
        newerOrder.setCustomerId(customerId);
        newerOrder.setOrderDate(LocalDateTime.of(2026, 1, 2, 10, 0));

        orderRepository.save(olderOrder);
        orderRepository.save(newerOrder);

        List<Order> orders =
                orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);

        assertThat(orders)
                .extracting(Order::getId)
                .containsExactly(newerOrder.getId(), olderOrder.getId());
    }

    private OrderItem createOrderItem(
            UUID productId, String productName, int quantity, BigDecimal price) {
        return OrderItem.builder()
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .unitPrice(price)
                .build();
    }

    private void addItemsToOrder(Order order) {
        order.addItem(createOrderItem(UUID.randomUUID(), "RTX 5070", 1, new BigDecimal("899.00")));

        order.addItem(
                createOrderItem(UUID.randomUUID(), "Ryzen 9800X3D", 2, new BigDecimal("449.00")));
    }
}
