package nz.fox.craig.order.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OrderRequestTest {

    @Test
    void shouldPreserveNullItems() {
        OrderRequest request = OrderRequest.builder()
                .items(null)
                .build();

        assertThat(request.items()).isNull();
    }

    @Test
    void shouldMakeItemsImmutable() {
        OrderItemRequest item = OrderItemRequest.builder()
                .productId(UUID.randomUUID())
                .quantity(1)
                .build();

        List<OrderItemRequest> items = new ArrayList<>(List.of(item));

        OrderRequest request = OrderRequest.builder()
                .items(items)
                .build();

        items.clear();

        assertThat(request.items()).containsExactly(item);

        assertThatThrownBy(() -> request.items().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
