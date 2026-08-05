package nz.fox.craig.order.mapper;

import org.springframework.stereotype.Component;

import nz.fox.craig.order.dto.response.OrderItemResponse;
import nz.fox.craig.order.dto.response.OrderResponse;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderItem;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .orderDate(order.getOrderDate())
                .status(order.getStatus().name())
                .subtotal(order.getSubtotal())
                .shipping(order.getShipping())
                .total(order.getTotal())
                .items(order.getItems().stream()
                        .map(this::toResponse)
                        .toList())
                .build();
    }

    public OrderItemResponse toResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
