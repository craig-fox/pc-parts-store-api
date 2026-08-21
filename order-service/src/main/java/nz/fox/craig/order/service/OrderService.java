package nz.fox.craig.order.service;

import static nz.fox.craig.order.shipping.ShippingPolicy.FREE_SHIPPING_THRESHOLD;
import static nz.fox.craig.order.shipping.ShippingPolicy.HEAVY_SHIPPING;
import static nz.fox.craig.order.shipping.ShippingPolicy.LIGHT_SHIPPING;
import static nz.fox.craig.order.shipping.ShippingPolicy.LIGHT_WEIGHT_LIMIT;
import static nz.fox.craig.order.shipping.ShippingPolicy.STANDARD_SHIPPING;
import static nz.fox.craig.order.shipping.ShippingPolicy.STANDARD_WEIGHT_LIMIT;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;
import nz.fox.craig.order.dto.request.ShippingAddressRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nz.fox.craig.dto.AuthenticatedUser;
import nz.fox.craig.order.client.CustomerClient;
import nz.fox.craig.order.client.InventoryClient;
import nz.fox.craig.order.client.ProductClient;
import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.dto.request.OrderItemRequest;
import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.dto.response.OrderResponse;
import nz.fox.craig.order.exception.IdempotencyKeyReuseException;
import nz.fox.craig.order.exception.OrderAlreadyCancelledException;
import nz.fox.craig.order.exception.OrderNotFoundException;
import nz.fox.craig.order.mapper.OrderMapper;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderItem;
import nz.fox.craig.order.model.OrderStatus;
import nz.fox.craig.order.model.ShippingAddress;
import nz.fox.craig.order.repository.OrderRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final OrderMapper orderMapper;
    private final OrderPersistenceService orderPersistenceService;

    @Transactional
    public OrderCreationResult createOrder(String idempotencyKey, OrderRequest request) {
        UUID customerId = getAuthenticatedCustomerId();
        String requestHash = hashOrderRequest(request);
    
        Optional<Order> existingOrder =
                orderRepository.findByCustomerIdAndIdempotencyKey(
                        customerId, idempotencyKey);
    
        if (existingOrder.isPresent()) {
            return new OrderCreationResult(
                    handleExistingOrder(existingOrder.get(), requestHash),
                    false);
        }
    
        validateCustomer(customerId);
    
        List<OrderItemRequest> reservedItems = new ArrayList<>();
    
        try {
            for (OrderItemRequest item : request.items()) {
                inventoryClient.reserveStock(item.productId(), item.quantity());
                reservedItems.add(item);
            }
    
            final Order order = assembleOrder(request, customerId, idempotencyKey, requestHash);
            final Order savedOrder = orderPersistenceService.save(order);
    
            return new OrderCreationResult(
                    orderMapper.toResponse(savedOrder),
                    true);
        } catch (DataIntegrityViolationException ex) {
            releaseReservedStock(reservedItems);
            return handleConcurrentOrder(
                    customerId,
                    idempotencyKey,
                    requestHash,
                    ex);
        } catch (RuntimeException ex) {
            releaseReservedStock(reservedItems);
            throw ex;
                }
    }

    private Order assembleOrder(OrderRequest request, 
                                UUID customerId, 
                                String idempotencyKey, 
                                String idempotencyHash) {

        final List<OrderItem> items = buildOrderItems(request);

        final BigDecimal subtotal = calculateSubtotal(items);
        final BigDecimal totalWeight = calculateTotalWeight(items);
        final BigDecimal shipping = calculateShipping(subtotal, totalWeight);

        final Order order =
                Order.builder()
                        .customerId(customerId)
                        .idempotencyKey(idempotencyKey)
                        .idempotencyRequestHash(idempotencyHash)
                        .orderDate(LocalDateTime.now())
                        .status(OrderStatus.PLACED)
                        .subtotal(subtotal)
                        .shipping(shipping)
                        .total(calculateTotal(subtotal, shipping))
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

    private OrderCreationResult handleConcurrentOrder(
        UUID customerId,
        String idempotencyKey,
        String requestHash,
        DataIntegrityViolationException ex) {

        Optional<Order> existingOrder =
                orderRepository.findByCustomerIdAndIdempotencyKey(
                        customerId, idempotencyKey);

        if (existingOrder.isEmpty()) {
            throw ex;
        }

        return new OrderCreationResult(
                handleExistingOrder(existingOrder.get(), requestHash),
                false);
    }

    private void releaseReservedStock(List<OrderItemRequest> reservedItems) {
        for (OrderItemRequest item : reservedItems) {
            try {
                inventoryClient.releaseStock(item.productId(), item.quantity());
            } catch (RestClientException ex) {
                log.warn(
                        "Failed to release item {}. {}",
                        item.productId(),
                        ex.getMessage(),
                        ex);
            }
        }
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
                                    .unitWeightKg(product.weightKg())
                                    .quantity(item.quantity())
                                    .build();
                        })
                .toList();
    }

    private BigDecimal calculateSubtotal(List<OrderItem> items) {

        return items.stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateShipping(BigDecimal subtotal, BigDecimal totalWeight) {

        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }

        if (totalWeight.compareTo(LIGHT_WEIGHT_LIMIT) <= 0) {
            return LIGHT_SHIPPING;
        }

        if (totalWeight.compareTo(STANDARD_WEIGHT_LIMIT) <= 0) {
            return STANDARD_SHIPPING;
        }

        return HEAVY_SHIPPING;
    }

    private BigDecimal calculateTotalWeight(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getLineWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal shipping) {

        return subtotal.add(shipping);
    }

    private String hashOrderRequest(OrderRequest request) {

        String canonicalRequest =
                request.items().stream()
                        .map(this::canonicalItem)
                        .collect(Collectors.joining("|"))
                        + "|"
                        + canonicalAddress(request.shippingAddress());
    
        return sha256(canonicalRequest);
    }
    
    private String canonicalItem(OrderItemRequest item) {
        return item.productId() + ":" + item.quantity();
    }
    
    private String canonicalAddress(ShippingAddressRequest address) {
        return String.join(
                ":",
                address.addressLine1(),
                address.city(),
                address.postcode(),
                address.country());
    }
    
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
    
            return HexFormat.of()
                    .formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private OrderResponse handleExistingOrder(Order order, String requestHash) {

        if (!MessageDigest.isEqual(
                order.getIdempotencyRequestHash().getBytes(StandardCharsets.UTF_8),
                requestHash.getBytes(StandardCharsets.UTF_8))) {
            throw new IdempotencyKeyReuseException(order.getIdempotencyKey());
        }
    
        return orderMapper.toResponse(order);
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

    private Optional<Order> findExistingOrder(
        UUID customerId, String idempotencyKey) {

    return orderRepository.findByCustomerIdAndIdempotencyKey(
            customerId, idempotencyKey);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForAuthenticatedCustomer() {
        UUID customerId = getAuthenticatedCustomerId();

        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    
}
