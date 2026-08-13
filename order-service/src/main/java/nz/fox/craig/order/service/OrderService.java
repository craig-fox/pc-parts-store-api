package nz.fox.craig.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.dto.AuthenticatedUser;
import nz.fox.craig.order.client.CustomerClient;
import nz.fox.craig.order.client.InventoryClient;
import nz.fox.craig.order.client.ProductClient;
import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.dto.request.OrderItemRequest;
import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.dto.response.OrderResponse;
import nz.fox.craig.order.exception.OrderAlreadyCancelledException;
import nz.fox.craig.order.exception.OrderNotFoundException;
import nz.fox.craig.order.mapper.OrderMapper;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderItem;
import nz.fox.craig.order.model.OrderStatus;
import nz.fox.craig.order.model.ShippingAddress;
import nz.fox.craig.order.repository.OrderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        UUID customerId = getAuthenticatedCustomerId();
        validateCustomer(customerId);
        for (OrderItemRequest item : request.items()) {
            inventoryClient.reserveStock(item.productId(), item.quantity());
        }
        final Order order = assembleOrder(request, customerId);
        final Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }

    private Order assembleOrder(OrderRequest request, UUID customerId) {

        final List<OrderItem> items = buildOrderItems(request);

        final BigDecimal subtotal = calculateSubtotal(items);
        final BigDecimal shipping = calculateShipping(subtotal);
        final BigDecimal total = calculateTotal(subtotal, shipping);

        final Order order =
                Order.builder()
                        .customerId(customerId)
                        .orderDate(LocalDateTime.now())
                        .status(OrderStatus.PLACED)
                        .subtotal(subtotal)
                        .shipping(shipping)
                        .total(total)
                        .shippingAddress(
                                new ShippingAddress(
                                        request.shippingAddress().addressLine1(),
                                        request.shippingAddress().city(),
                                        request.shippingAddress().postcode(),
                                        request.shippingAddress().country()))
                        .build();

        items.forEach(order::addItem);

        return order;
    }

    private UUID getAuthenticatedCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();

        return user.id();
    }

    private void validateCustomer(UUID customerId) {
        customerClient.validateCustomerExists(customerId);
    }

    private List<OrderItem> buildOrderItems(OrderRequest request) {
        return request.items().stream()
                .map(
                        item -> {
                            final ProductSnapshot product =
                                    productClient.getProduct(item.productId());

                            final BigDecimal unitPrice = product.price();

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

        return items.stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateShipping(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal shipping) {

        return subtotal.add(shipping);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {

        final Order order = findOrderById(id);
        return orderMapper.toResponse(order);
    }

    private Order findOrderById(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public OrderResponse cancelOrder(UUID id) {
        final Order order = findOrderById(id);
        validateOrderCanBeCancelled(order);
        order.setStatus(OrderStatus.CANCELLED);
        final Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }

    private void validateOrderCanBeCancelled(Order order) {

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException(order.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForAuthenticatedCustomer() {
        UUID customerId = getAuthenticatedCustomerId();

        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }
}
