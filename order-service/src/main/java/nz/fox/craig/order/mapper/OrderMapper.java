package nz.fox.craig.order.mapper;

import org.mapstruct.Mapper;

import nz.fox.craig.order.dto.response.OrderItemResponse;
import nz.fox.craig.order.dto.response.OrderResponse;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    OrderItemResponse toResponse(OrderItem item);
}
