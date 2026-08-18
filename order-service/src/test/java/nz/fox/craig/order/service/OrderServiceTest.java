package nz.fox.craig.order.service;

import static nz.fox.craig.order.shipping.ShippingPolicy.FREE_SHIPPING_THRESHOLD;
import static nz.fox.craig.order.shipping.ShippingPolicy.HEAVY_SHIPPING;
import static nz.fox.craig.order.shipping.ShippingPolicy.LIGHT_SHIPPING;
import static nz.fox.craig.order.shipping.ShippingPolicy.LIGHT_WEIGHT_LIMIT;
import static nz.fox.craig.order.shipping.ShippingPolicy.STANDARD_SHIPPING;
import static nz.fox.craig.order.shipping.ShippingPolicy.STANDARD_WEIGHT_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import nz.fox.craig.dto.AuthenticatedUser;
import nz.fox.craig.dto.Role;
import nz.fox.craig.order.client.CustomerClient;
import nz.fox.craig.order.client.InventoryClient;
import nz.fox.craig.order.client.ProductClient;
import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.dto.request.OrderItemRequest;
import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.dto.request.ShippingAddressRequest;
import nz.fox.craig.order.dto.response.OrderItemResponse;
import nz.fox.craig.order.dto.response.OrderResponse;
import nz.fox.craig.order.exception.CustomerNotFoundException;
import nz.fox.craig.order.exception.InsufficientStockException;
import nz.fox.craig.order.exception.OrderAlreadyCancelledException;
import nz.fox.craig.order.exception.OrderNotFoundException;
import nz.fox.craig.order.exception.ProductNotFoundException;
import nz.fox.craig.order.mapper.OrderMapper;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderItem;
import nz.fox.craig.order.model.OrderStatus;
import nz.fox.craig.order.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final int DEFAULT_ORDER_QUANTITY = 1;
    private static final int EXISTING_ORDER_QUANTITY = 2;

    @Mock private OrderRepository repository;

    @Mock private CustomerClient customerClient;

    @Mock private ProductClient productClient;

    @Mock private InventoryClient inventoryClient;

    @Mock private OrderMapper orderMapper;

    @InjectMocks private OrderService orderService;

    @BeforeEach
    void setUpSecurityContext() {
        authenticateCustomer();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class CreateOrder {

        @Test
        void shouldCreateOrder() {
            // Arrange
            doNothing().when(customerClient).validateCustomerExists(CUSTOMER_ID);
            configureRepositoryToAssignIds();
            when(productClient.getProduct(PRODUCT_ID)).thenReturn(productSnapshot());

            OrderResponse expectedResponse =
                    OrderResponse.builder().id(ORDER_ID).status("PLACED").build();

            when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);

            // Act
            OrderResponse response = orderService.createOrder(orderRequest());

            // Assert
            Order savedOrder = verifyCreateOrderInteractions();
            assertSavedOrder(savedOrder);

            assertThat(response).isSameAs(expectedResponse);
        }

        @Test
        void shouldNotCreateOrderWhenInventoryReservationFails() {
            doThrow(new InsufficientStockException(PRODUCT_ID))
                    .when(inventoryClient)
                    .reserveStock(PRODUCT_ID, DEFAULT_ORDER_QUANTITY);

            assertThatThrownBy(() -> orderService.createOrder(orderRequest()))
                    .isInstanceOf(InsufficientStockException.class);

            verify(repository, never()).save(any());
            verify(inventoryClient).reserveStock(PRODUCT_ID, DEFAULT_ORDER_QUANTITY);

            verify(repository, never()).save(any());
            verify(productClient, never()).getProduct(any());
        }

        @Test
        void shouldThrowWhenCustomerDoesNotExist() {
            OrderRequest request = orderRequest();
            doThrow(new CustomerNotFoundException(CUSTOMER_ID))
                    .when(customerClient)
                    .validateCustomerExists(CUSTOMER_ID);

            assertThrows(CustomerNotFoundException.class, () -> orderService.createOrder(request));

            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowWhenProductDoesNotExist() {
            when(productClient.getProduct(PRODUCT_ID))
                    .thenThrow(new ProductNotFoundException(PRODUCT_ID));
            doNothing().when(customerClient).validateCustomerExists(CUSTOMER_ID);

            assertThatThrownBy(() -> orderService.createOrder(orderRequest()))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(repository, never()).save(any());
        }

        @Test
        void shouldProvideFreeShippingWhenSubtotalMeetsThreshold() {
            // Arrange
            doNothing().when(customerClient).validateCustomerExists(CUSTOMER_ID);
            configureRepositoryToAssignIds();

            when(productClient.getProduct(PRODUCT_ID))
                    .thenReturn(productSnapshot(FREE_SHIPPING_THRESHOLD, BigDecimal.ONE));

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

            when(orderMapper.toResponse(orderCaptor.capture()))
                    .thenReturn(OrderResponse.builder().id(ORDER_ID).status("PLACED").build());

            // Act
            orderService.createOrder(orderRequest());

            // Assert
            Order order = orderCaptor.getValue();

            assertThat(order.getShipping()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldCalculateLightShipping() {
            // Arrange
            doNothing().when(customerClient).validateCustomerExists(CUSTOMER_ID);
            configureRepositoryToAssignIds();

            when(productClient.getProduct(PRODUCT_ID))
                    .thenReturn(productSnapshot(new BigDecimal("100.00"), LIGHT_WEIGHT_LIMIT));

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

            when(orderMapper.toResponse(orderCaptor.capture()))
                    .thenReturn(OrderResponse.builder().id(ORDER_ID).status("PLACED").build());

            // Act
            orderService.createOrder(orderRequest());

            // Assert
            Order order = orderCaptor.getValue();

            assertThat(order.getShipping()).isEqualByComparingTo(LIGHT_SHIPPING);
        }

        @Test
        void shouldCalculateStandardShipping() {
            // Arrange
            doNothing().when(customerClient).validateCustomerExists(CUSTOMER_ID);
            configureRepositoryToAssignIds();

            when(productClient.getProduct(PRODUCT_ID))
                    .thenReturn(productSnapshot(new BigDecimal("100.00"), STANDARD_WEIGHT_LIMIT));

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

            when(orderMapper.toResponse(orderCaptor.capture()))
                    .thenReturn(OrderResponse.builder().id(ORDER_ID).status("PLACED").build());

            // Act
            orderService.createOrder(orderRequest());

            // Assert
            Order order = orderCaptor.getValue();

            assertThat(order.getShipping()).isEqualByComparingTo(STANDARD_SHIPPING);
        }

        @Test
        void shouldCalculateHeavyShipping() {
            // Arrange
            doNothing().when(customerClient).validateCustomerExists(CUSTOMER_ID);
            configureRepositoryToAssignIds();

            when(productClient.getProduct(PRODUCT_ID))
                    .thenReturn(
                            productSnapshot(
                                    new BigDecimal("100.00"),
                                    STANDARD_WEIGHT_LIMIT.add(new BigDecimal("0.01"))));

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

            when(orderMapper.toResponse(orderCaptor.capture()))
                    .thenReturn(OrderResponse.builder().id(ORDER_ID).status("PLACED").build());

            // Act
            orderService.createOrder(orderRequest());

            // Assert
            Order order = orderCaptor.getValue();

            assertThat(order.getShipping()).isEqualByComparingTo(HEAVY_SHIPPING);
        }
    }

    @Nested
    class GetOrder {
        @Test
        void shouldReturnOrder() {
            // Arrange
            Order order = existingOrder();

            OrderResponse expectedResponse =
                    OrderResponse.builder()
                            .id(ORDER_ID)
                            .customerId(CUSTOMER_ID)
                            .status(OrderStatus.PLACED.name())
                            .subtotal(new BigDecimal("179.98"))
                            .shipping(BigDecimal.ZERO)
                            .total(new BigDecimal("179.98"))
                            .items(
                                    List.of(
                                            OrderItemResponse.builder()
                                                    .productId(PRODUCT_ID)
                                                    .productName("Gaming Mouse")
                                                    .quantity(DEFAULT_ORDER_QUANTITY)
                                                    .unitPrice(new BigDecimal("89.99"))
                                                    .lineTotal(new BigDecimal("179.98"))
                                                    .build()))
                            .build();

            when(repository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            when(orderMapper.toResponse(order)).thenReturn(expectedResponse);

            // Act
            OrderResponse response = orderService.getOrder(ORDER_ID);

            // Assert
            assertThat(response).isSameAs(expectedResponse);

            verify(repository).findById(ORDER_ID);
            verify(orderMapper).toResponse(order);
            verifyNoMoreInteractions(repository, orderMapper);
        }

        @Test
        void shouldThrowWhenOrderNotFound() {
            when(repository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThrows(OrderNotFoundException.class, () -> orderService.getOrder(ORDER_ID));
        }
    }

    @Nested
    class GetOrders {

        @Test
        void shouldReturnOrdersForAuthenticatedCustomer() {
            // Arrange
            Order order1 = existingOrder();
            Order order2 = existingOrder();
            order2.setId(UUID.randomUUID());

            OrderResponse response1 =
                    OrderResponse.builder()
                            .id(order1.getId())
                            .customerId(CUSTOMER_ID)
                            .status(OrderStatus.PLACED.name())
                            .build();

            OrderResponse response2 =
                    OrderResponse.builder()
                            .id(order2.getId())
                            .customerId(CUSTOMER_ID)
                            .status(OrderStatus.PLACED.name())
                            .build();

            when(repository.findByCustomerIdOrderByOrderDateDesc(CUSTOMER_ID))
                    .thenReturn(List.of(order1, order2));

            when(orderMapper.toResponse(order1)).thenReturn(response1);

            when(orderMapper.toResponse(order2)).thenReturn(response2);

            // Act
            List<OrderResponse> responses = orderService.getOrdersForAuthenticatedCustomer();

            // Assert
            assertThat(responses).containsExactly(response1, response2);

            verify(repository).findByCustomerIdOrderByOrderDateDesc(CUSTOMER_ID);
            verify(orderMapper).toResponse(order1);
            verify(orderMapper).toResponse(order2);

            verifyNoMoreInteractions(repository, orderMapper);
        }

        @Test
        void shouldReturnEmptyListWhenAuthenticatedCustomerHasNoOrders() {
            UUID customerId = UUID.randomUUID();

            mockAuthenticatedCustomer(customerId);

            when(repository.findByCustomerIdOrderByOrderDateDesc(customerId)).thenReturn(List.of());

            List<OrderResponse> responses = orderService.getOrdersForAuthenticatedCustomer();

            assertThat(responses).isEmpty();

            verify(repository).findByCustomerIdOrderByOrderDateDesc(customerId);
        }
    }

    @Nested
    class CancelOrder {
        @Test
        void shouldCancelOrder() {
            // Arrange
            Order existingOrder = existingOrder();

            OrderResponse expectedResponse =
                    OrderResponse.builder()
                            .id(ORDER_ID)
                            .customerId(CUSTOMER_ID)
                            .status(OrderStatus.CANCELLED.name())
                            .subtotal(new BigDecimal("179.98"))
                            .shipping(BigDecimal.ZERO)
                            .total(new BigDecimal("179.98"))
                            .items(
                                    List.of(
                                            OrderItemResponse.builder()
                                                    .productId(PRODUCT_ID)
                                                    .productName("Gaming Mouse")
                                                    .quantity(DEFAULT_ORDER_QUANTITY)
                                                    .unitPrice(new BigDecimal("89.99"))
                                                    .lineTotal(new BigDecimal("179.98"))
                                                    .build()))
                            .build();

            when(repository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
            when(repository.save(existingOrder)).thenReturn(existingOrder);
            when(orderMapper.toResponse(existingOrder)).thenReturn(expectedResponse);

            // Act
            OrderResponse response = orderService.cancelOrder(ORDER_ID);

            // Assert
            assertThat(existingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            assertThat(response).isSameAs(expectedResponse);

            verify(repository).findById(ORDER_ID);
            verify(repository).save(existingOrder);
            verify(orderMapper).toResponse(existingOrder);

            verifyNoMoreInteractions(repository, orderMapper);
        }

        @Test
        void shouldThrowWhenOrderNotFound() {
            when(repository.findById(ORDER_ID)).thenReturn(Optional.empty());
            assertThrows(OrderNotFoundException.class, () -> orderService.cancelOrder(ORDER_ID));
            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowWhenOrderAlreadyCancelled() {
            when(repository.findById(ORDER_ID)).thenReturn(Optional.of(cancelledOrder()));
            assertThrows(
                    OrderAlreadyCancelledException.class, () -> orderService.cancelOrder(ORDER_ID));
            verify(repository, never()).save(any());
        }
    }

    private OrderRequest orderRequest() {
        return orderRequest(DEFAULT_ORDER_QUANTITY);
    }

    private OrderRequest orderRequest(int quantity) {
        return OrderRequest.builder()
                .items(
                        List.of(
                                OrderItemRequest.builder()
                                        .productId(PRODUCT_ID)
                                        .quantity(quantity)
                                        .build()))
                .shippingAddress(shippingAddressRequest())
                .build();
    }

    private Order existingOrder() {
        Order order =
                Order.builder()
                        .id(ORDER_ID)
                        .customerId(CUSTOMER_ID)
                        .orderDate(LocalDateTime.now())
                        .status(OrderStatus.PLACED)
                        .subtotal(new BigDecimal("179.98"))
                        .shipping(BigDecimal.ZERO)
                        .total(new BigDecimal("179.98"))
                        .build();

        order.addItem(
                OrderItem.builder()
                        .productId(PRODUCT_ID)
                        .productName("Gaming Mouse")
                        .quantity(DEFAULT_ORDER_QUANTITY)
                        .unitPrice(new BigDecimal("89.99"))
                        .build());
        return order;
    }

    private Order cancelledOrder() {
        Order order = existingOrder();
        order.setStatus(OrderStatus.CANCELLED);
        return order;
    }

    private ProductSnapshot productSnapshot() {
        return ProductSnapshot.builder()
                .id(PRODUCT_ID)
                .name("Gaming Mouse")
                .price(new BigDecimal("89.99"))
                .weightKg(new BigDecimal("0.30"))
                .active(true)
                .build();
    }

    private ProductSnapshot productSnapshot(BigDecimal price, BigDecimal weight) {
        return ProductSnapshot.builder()
                .id(PRODUCT_ID)
                .name("Gaming Mouse")
                .price(price)
                .weightKg(weight)
                .active(true)
                .build();
    }

    private void authenticateCustomer() {
        AuthenticatedUser user =
                new AuthenticatedUser(
                        CUSTOMER_ID, "alice.smith@example.com", Set.of(Role.ROLE_CUSTOMER));

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(user, null, List.of());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void mockAuthenticatedCustomer(UUID customerId) {
        AuthenticatedUser principal =
                new AuthenticatedUser(
                        customerId, "alice.smith@example.com", Set.of(Role.ROLE_CUSTOMER));

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void configureRepositoryToAssignIds() {
        when(repository.save(any(Order.class)))
                .thenAnswer(
                        invocation -> {
                            Order order = invocation.getArgument(0);

                            if (order.getId() == null) {
                                order.setId(UUID.randomUUID());
                            }

                            order.getItems()
                                    .forEach(
                                            item -> {
                                                if (item.getId() == null) {
                                                    item.setId(UUID.randomUUID());
                                                }
                                            });

                            return order;
                        });
    }

    private Order verifyCreateOrderInteractions() {
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);

        verify(customerClient).validateCustomerExists(CUSTOMER_ID);

        verify(inventoryClient).reserveStock(PRODUCT_ID, DEFAULT_ORDER_QUANTITY);

        verify(productClient).getProduct(PRODUCT_ID);

        verify(repository).save(captor.capture());

        Order savedOrder = captor.getValue();

        verify(orderMapper).toResponse(savedOrder);

        return savedOrder;
    }

    private void assertSavedOrder(Order savedOrder) {
        OrderItem item = savedOrder.getItems().getFirst();

        assertThat(item.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(item.getProductName()).isEqualTo("Gaming Mouse");
        assertThat(item.getUnitPrice()).isEqualByComparingTo(new BigDecimal("89.99"));
        assertThat(item.getQuantity()).isEqualTo(1);
        assertThat(item.getLineTotal()).isEqualByComparingTo(new BigDecimal("89.99"));

        assertThat(savedOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(savedOrder.getSubtotal()).isEqualByComparingTo(new BigDecimal("89.99"));
        assertThat(savedOrder.getShipping()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(savedOrder.getTotal()).isEqualByComparingTo(new BigDecimal("97.99"));
        assertThat(savedOrder.getItems()).hasSize(1);
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getOrderDate()).isNotNull();
    }

    private ShippingAddressRequest shippingAddressRequest() {
        return ShippingAddressRequest.builder()
                .addressLine1("1 Main St")
                .city("Auckland")
                .postcode("1010")
                .country("NZ")
                .build();
    }
}
