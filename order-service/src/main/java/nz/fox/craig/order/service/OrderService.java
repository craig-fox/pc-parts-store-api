package nz.fox.craig.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import nz.fox.craig.order.client.CustomerClient;
import nz.fox.craig.order.client.InventoryClient;
import nz.fox.craig.order.client.ProductClient;
import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.dto.request.OrderItemRequest;
import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.dto.response.OrderItemResponse;
import nz.fox.craig.order.dto.response.OrderResponse;
import nz.fox.craig.order.exception.OrderAlreadyCancelledException;
import nz.fox.craig.order.exception.OrderNotFoundException;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderItem;
import nz.fox.craig.order.model.OrderStatus;
import nz.fox.craig.order.repository.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final CustomerClient customerClient;
	private final ProductClient productClient;
	private final InventoryClient inventoryClient;

	@Transactional
	public OrderResponse createOrder(OrderRequest request) {

		validateCustomer(request.customerId());
		for (OrderItemRequest item : request.items()) {
			inventoryClient.reserveStock(item.productId(), item.quantity());
		}
		Order order = assembleOrder(request);
		Order savedOrder = orderRepository.save(order);
		return mapToResponse(savedOrder);
	}

	private Order assembleOrder(OrderRequest request) {

		List<OrderItem> items = buildOrderItems(request);
	
		BigDecimal subtotal = calculateSubtotal(items);
		BigDecimal shipping = calculateShipping(subtotal);
		BigDecimal total = calculateTotal(subtotal, shipping);
	
		Order order = Order.builder()
				.customerId(request.customerId())
				.orderDate(LocalDateTime.now())
				.status(OrderStatus.PLACED)
				.subtotal(subtotal)
				.shipping(shipping)
				.total(total)
				.build();
	
		items.forEach(order::addItem);
	
		return order;
	}

	private void validateCustomer(UUID customerId) {
		customerClient.validateCustomerExists(customerId);
	}
	
	private List<OrderItem> buildOrderItems(OrderRequest request) {
		return request.items().stream()
				.map(item -> {
					ProductSnapshot product =
							productClient.getProduct(item.productId());
	
					BigDecimal unitPrice = product.price();
	
					return OrderItem.builder()
							.productId(product.id())
							.productName(product.name())
							.unitPrice(unitPrice)
							.quantity(item.quantity())
							.build();
				})
				.toList();
	}

	private BigDecimal calculateSubtotal(List<OrderItem> items) {

		return items.stream()
				.map(OrderItem::getLineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal calculateShipping(BigDecimal subtotal) {
		return BigDecimal.ZERO;
	}

	private BigDecimal calculateTotal(
        BigDecimal subtotal,
        BigDecimal shipping) {

		return subtotal.add(shipping);
	}

	
	private OrderResponse mapToResponse(Order order) {

		return OrderResponse.builder()
				.id(order.getId())
				.customerId(order.getCustomerId())
				.orderDate(order.getOrderDate())
				.status(order.getStatus().name())
				.subtotal(order.getSubtotal())
				.shipping(order.getShipping())
				.total(order.getTotal())
				.items(
						order.getItems().stream()
								.map(this::mapToResponse)
								.toList()
				)
				.build();
	}

	private OrderItemResponse mapToResponse(OrderItem item) {

		return OrderItemResponse.builder()
				.productId(item.getProductId())
				.productName(item.getProductName())
				.quantity(item.getQuantity())
				.unitPrice(item.getUnitPrice())
				.lineTotal(item.getLineTotal())
				.build();
	}

	@Transactional(readOnly = true)
	public OrderResponse getOrder(UUID id) {

		Order order = findOrderById(id);
		return mapToResponse(order);
	}

	private Order findOrderById(UUID id) {
		return orderRepository.findById(id)
				.orElseThrow(() -> new OrderNotFoundException(id));
	}

	@Transactional
	public OrderResponse cancelOrder(UUID id) {
		Order order = findOrderById(id);
		validateOrderCanBeCancelled(order);
		order.setStatus(OrderStatus.CANCELLED);
		Order savedOrder = orderRepository.save(order);
		return mapToResponse(savedOrder);
	}

	private void validateOrderCanBeCancelled(Order order) {

		if (order.getStatus() == OrderStatus.CANCELLED) {
			throw new OrderAlreadyCancelledException(order.getId());
		}
	}

}
