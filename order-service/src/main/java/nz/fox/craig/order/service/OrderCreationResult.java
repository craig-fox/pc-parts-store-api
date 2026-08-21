package nz.fox.craig.order.service;

import nz.fox.craig.order.dto.response.OrderResponse;

public record OrderCreationResult(OrderResponse order, boolean created) {}
