package nz.fox.craig.order.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nz.fox.craig.api.DownstreamServiceUnavailableException;
import nz.fox.craig.order.client.CustomerClient;
import nz.fox.craig.order.client.InventoryClient;
import nz.fox.craig.order.client.PaymentClient;
import nz.fox.craig.order.client.ProductClient;
import nz.fox.craig.order.client.ShippingClient;
import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.dto.request.OrderItemRequest;
import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.dto.request.ShippingAddressRequest;
import nz.fox.craig.order.dto.request.ShippingQuoteRequest;
import nz.fox.craig.order.dto.response.OrderResponse;
import nz.fox.craig.order.dto.response.ShippingQuoteResponse;
import nz.fox.craig.order.event.OrderPlacedEvent;
import nz.fox.craig.order.exception.IdempotencyKeyReuseException;
import nz.fox.craig.order.exception.OrderAlreadyCancelledException;
import nz.fox.craig.order.exception.OrderNotFoundException;
import nz.fox.craig.order.mapper.OrderMapper;
import nz.fox.craig.order.metrics.OrderMetrics;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderItem;
import nz.fox.craig.order.model.OrderStatus;
import nz.fox.craig.order.model.ShippingAddress;
import nz.fox.craig.order.repository.OrderRepository;
import nz.fox.craig.security.service.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final ShippingClient shippingClient;
    private final OrderMapper orderMapper;
    private final OrderPersistenceService orderPersistenceService;
    private final OrderMetrics orderMetrics;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public OrderCreationResult createOrder(String idempotencyKey, OrderRequest request) {
        UUID customerId = currentUser.customerId();
        String requestHash = hashOrderRequest(request);

        log.info("Creating order for customer {}", customerId);

        Optional<Order> existingOrder =
                orderRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey);

        if (existingOrder.isPresent()) {
            return handleIdempotentRequest(existingOrder.get(), requestHash);
        }

        validateCustomer(customerId);

        List<OrderItemRequest> reservedItems = new ArrayList<>();

        try {
            reserveInventory(request.items(), reservedItems);

            Order order = createAndSaveOrder(request, customerId, idempotencyKey, requestHash);

            paymentClient.processPayment(order.getId(), customerId, order.getTotal(), "NZD");

            confirmInventory(order);

            Order paidOrder = markOrderAsPaid(order);
            publishOrderPlacedEvent(paidOrder, customerId);

            orderMetrics.orderCreated();

            return new OrderCreationResult(orderMapper.toResponse(paidOrder), true);

        } catch (DataIntegrityViolationException ex) {
            releaseReservedStock(reservedItems);
            return handleConcurrentOrder(customerId, idempotencyKey, requestHash, ex);
        } catch (RuntimeException ex) {
            releaseReservedStock(reservedItems);
            throw ex;
        }
    }

    private Order assembleOrder(
            UUID orderId,
            OrderRequest request,
            UUID customerId,
            String idempotencyKey,
            String idempotencyHash) {

        List<OrderItem> items = buildOrderItems(request);

        BigDecimal subtotal = calculateSubtotal(items);
        BigDecimal totalWeight = calculateTotalWeight(items);

        ShippingQuoteResponse shippingQuote =
                shippingClient.calculateQuote(
                        new ShippingQuoteRequest(
                                orderId,
                                request.shippingAddress(),
                                totalWeight,
                                request.shippingMethod()));

        BigDecimal shipping = shippingQuote.price();

        Order order =
                Order.builder()
                        .id(orderId)
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
                orderRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey);

        if (existingOrder.isEmpty()) {
            throw ex;
        }

        return new OrderCreationResult(
                handleExistingOrder(existingOrder.get(), requestHash), false);
    }

    private OrderCreationResult handleIdempotentRequest(Order existingOrder, String requestHash) {

        log.info("Returning existing order {} for duplicate request", existingOrder.getId());

        return new OrderCreationResult(handleExistingOrder(existingOrder, requestHash), false);
    }

    private void reserveInventory(
            List<OrderItemRequest> items, List<OrderItemRequest> reservedItems) {

        for (OrderItemRequest item : items) {
            inventoryClient.reserveStock(item.productId(), item.quantity());
            reservedItems.add(item);
        }
    }

    private Order createAndSaveOrder(
            OrderRequest request, UUID customerId, String idempotencyKey, String requestHash) {

        UUID orderId = UUID.randomUUID();

        Order order = assembleOrder(orderId, request, customerId, idempotencyKey, requestHash);

        return orderPersistenceService.save(order);
    }

    private void confirmInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            inventoryClient.confirmReservation(item.getProductId(), item.getQuantity());
        }
    }

    private Order markOrderAsPaid(Order order) {
        order.setStatus(OrderStatus.PAID);
        return orderPersistenceService.save(order);
    }

    private void publishOrderPlacedEvent(Order order, UUID customerId) {
        log.info(
                "Order {} created successfully with total {} {}",
                order.getId(),
                order.getTotal(),
                "NZD");

        applicationEventPublisher.publishEvent(
                new OrderPlacedEvent(order.getId(), customerId, order.getTotal(), "NZD"));
    }

    private void releaseReservedStock(List<OrderItemRequest> reservedItems) {

        for (OrderItemRequest item : reservedItems) {

            try {
                inventoryClient.releaseStock(item.productId(), item.quantity());

            } catch (DownstreamServiceUnavailableException ex) {

                log.warn(
                        "Failed to release reserved inventory for product {}",
                        item.productId(),
                        ex);
            }
        }
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
                request.items().stream().map(this::canonicalItem).collect(Collectors.joining("|"))
                        + "|"
                        + canonicalAddress(request.shippingAddress());

        return sha256(canonicalRequest);
    }

    private String canonicalItem(OrderItemRequest item) {
        return item.productId() + ":" + item.quantity();
    }

    private String canonicalAddress(ShippingAddressRequest address) {
        return String.join(
                ":", address.addressLine1(), address.city(), address.postcode(), address.country());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
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
        UUID customerId = currentUser.customerId();

        Order order =
                orderRepository
                        .findByIdAndCustomerId(id, customerId)
                        .orElseThrow(() -> new OrderNotFoundException(id));

        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID id) {
        UUID customerId = currentUser.customerId();

        Order order =
                orderRepository
                        .findByIdAndCustomerId(id, customerId)
                        .orElseThrow(() -> new OrderNotFoundException(id));

        validateOrderCanBeCancelled(order);

        for (OrderItem item : order.getItems()) {
            inventoryClient.releaseStock(item.getProductId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderPersistenceService.save(order);

        log.info("Order {} cancelled", cancelledOrder.getId());
        orderMetrics.orderCancelled();

        return orderMapper.toResponse(cancelledOrder);
    }

    private void validateOrderCanBeCancelled(Order order) {

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException(order.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForAuthenticatedCustomer() {
        UUID customerId = currentUser.customerId();

        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }
}
