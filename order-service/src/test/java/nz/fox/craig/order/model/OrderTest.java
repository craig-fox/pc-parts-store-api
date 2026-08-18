package nz.fox.craig.order.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void shouldRemoveItemFromOrder() {
        Order order = Order.builder().customerId(UUID.randomUUID()).build();

        OrderItem item =
                OrderItem.builder()
                        .productId(UUID.randomUUID())
                        .productName("Gaming Mouse")
                        .quantity(1)
                        .build();

        order.addItem(item);

        assertThat(order.getItems()).containsExactly(item);
        assertThat(item.getOrder()).isSameAs(order);

        order.removeItem(item);

        assertThat(order.getItems()).doesNotContain(item);
        assertThat(item.getOrder()).isNull();
    }
}
