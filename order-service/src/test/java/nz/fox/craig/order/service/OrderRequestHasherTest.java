package nz.fox.craig.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.fixture.OrderFixtures;

public class OrderRequestHasherTest {
    
    
    private OrderRequestHasher hasher = new OrderRequestHasher();

    @Test
    void shouldProduceSameHashForSameRequest() {
        OrderRequest request = OrderFixtures.anOrderRequest();
    
        assertThat(hasher.hash(request))
                .isEqualTo(hasher.hash(request));
    }

    @Test
    void shouldProduceDifferentHashWhenQuantityChanges() {
        OrderRequest first = OrderFixtures.anOrderRequest(OrderFixtures.orderItems());
        OrderRequest second = OrderFixtures.anOrderRequest(OrderFixtures.orderItems(2));
    
        assertThat(hasher.hash(first))
                .isNotEqualTo(hasher.hash(second));
    }


}
