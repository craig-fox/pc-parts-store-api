package nz.fox.craig.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import nz.fox.craig.api.ShippingMethod;
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
import nz.fox.craig.order.dto.response.OrderItemResponse;
import nz.fox.craig.order.dto.response.OrderResponse;
import nz.fox.craig.order.exception.CustomerNotFoundException;
import nz.fox.craig.order.exception.InsufficientStockException;
import nz.fox.craig.order.exception.OrderAlreadyCancelledException;
import nz.fox.craig.order.exception.OrderNotFoundException;
import nz.fox.craig.order.exception.ProductNotFoundException;
import nz.fox.craig.order.fixture.OrderFixture;
import nz.fox.craig.order.fixture.ShippingFixture;
import nz.fox.craig.order.mapper.OrderMapper;
import nz.fox.craig.order.metrics.OrderMetrics;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderItem;
import nz.fox.craig.order.model.OrderStatus;
import nz.fox.craig.order.repository.OrderRepository;
import nz.fox.craig.security.service.CurrentUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private record SavedOrders(Order placed, Order paid) {}

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final int DEFAULT_ORDER_QUANTITY = 1;
    private static final String IDEMPOTENCY_KEY = UUID.randomUUID().toString();
    

    @Mock 
    private OrderRepository repository;

    @Mock 
    private CustomerClient customerClient;

    @Mock 
    private ProductClient productClient;

    @Mock 
    private InventoryClient inventoryClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private ShippingClient shippingClient;

    @Mock private OrderMapper orderMapper;

    @Mock
    private OrderPersistenceService orderPersistenceService;

    @Mock
    private OrderMetrics orderMetrics;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks 
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        when(currentUser.customerId()).thenReturn(CUSTOMER_ID);
    }

    @Nested
    class CreateOrder {

        @Test
        void shouldCreateOrder() {
            // Arrange
            doNothing().when(customerClient).validateCustomerExists(CUSTOMER_ID);
            configureRepositoryToAssignIds();
            when(productClient.getProduct(PRODUCT_ID)).thenReturn(productSnapshot());
            when(shippingClient.calculateQuote(any(ShippingQuoteRequest.class)))
                .thenReturn(ShippingFixture.shippingQuoteResponse(ShippingMethod.STANDARD, BigDecimal.valueOf(15.00)));

            OrderResponse expectedResponse = OrderFixture.anOrderResponse();

            when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);

            // Act
            OrderCreationResult result =
                    orderService.createOrder(IDEMPOTENCY_KEY, orderRequest());

            OrderResponse response = result.order();

            // Assert
            SavedOrders savedOrders = verifyCreateOrderInteractions();

            assertSavedOrder(savedOrders.paid());


            assertThat(savedOrders.paid().getId())
                    .isEqualTo(savedOrders.placed().getId());

                    

            assertThat(response).isSameAs(expectedResponse);

            verify(paymentClient).processPayment(
                eq(savedOrders.placed().getId()),
                eq(CUSTOMER_ID),
                eq(new BigDecimal("104.99")),
                eq("NZD"));

            verify(shippingClient).calculateQuote(any(ShippingQuoteRequest.class));
            verify(orderMetrics).orderCreated();
        }

        @Test
        void shouldNotCreateOrderWhenInventoryReservationFails() {
            doThrow(new InsufficientStockException(PRODUCT_ID))
                    .when(inventoryClient)
                    .reserveStock(PRODUCT_ID, DEFAULT_ORDER_QUANTITY);

            assertThatThrownBy(() -> orderService.createOrder(IDEMPOTENCY_KEY, orderRequest()))
                    .isInstanceOf(InsufficientStockException.class);

            verify(inventoryClient).reserveStock(PRODUCT_ID, DEFAULT_ORDER_QUANTITY);
            verify(orderPersistenceService, never()).save(any());
            verify(productClient, never()).getProduct(any());
        }

        @Test
        void shouldThrowWhenCustomerDoesNotExist() {
            OrderRequest request = OrderFixture.anOrderRequest();
            doThrow(new CustomerNotFoundException(CUSTOMER_ID))
                    .when(customerClient)
                    .validateCustomerExists(CUSTOMER_ID);

            assertThatThrownBy(() -> orderService.createOrder(IDEMPOTENCY_KEY, request))
                    .isInstanceOf(CustomerNotFoundException.class);

            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowWhenProductDoesNotExist() {
            when(productClient.getProduct(PRODUCT_ID))
                    .thenThrow(new ProductNotFoundException(PRODUCT_ID));
            doNothing().when(customerClient).validateCustomerExists(CUSTOMER_ID);

            assertThatThrownBy(() -> orderService.createOrder(IDEMPOTENCY_KEY, orderRequest()))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(repository, never()).save(any());
        }

        @Test
        void shouldUseShippingQuoteForOrderShippingCost() {

            doNothing().when(customerClient)
                    .validateCustomerExists(CUSTOMER_ID);

            configureRepositoryToAssignIds();

            when(productClient.getProduct(PRODUCT_ID))
                    .thenReturn(
                            productSnapshot(
                                    new BigDecimal("100.00"),
                                    new BigDecimal("0.50")));

            when(shippingClient.calculateQuote(any(ShippingQuoteRequest.class)))
                    .thenReturn(
                            ShippingFixture.shippingQuoteResponse(
                                    ShippingMethod.STANDARD,
                                    new BigDecimal("15.00")));

            ArgumentCaptor<Order> orderCaptor =
                    ArgumentCaptor.forClass(Order.class);

            when(orderMapper.toResponse(orderCaptor.capture()))
                    .thenReturn(OrderFixture.anOrderResponse());

            orderService.createOrder(IDEMPOTENCY_KEY, orderRequest());

            Order order = orderCaptor.getValue();

            assertThat(order.getShipping())
                    .isEqualByComparingTo("15.00");
        }


        @Test
        void shouldSendRequestedShippingMethodToShippingService() {

            doNothing().when(customerClient)
                    .validateCustomerExists(CUSTOMER_ID);
            configureRepositoryToAssignIds();

            when(productClient.getProduct(OrderFixture.FIXTURE_PRODUCT_ID))
                    .thenReturn(productSnapshot());

            when(shippingClient.calculateQuote(any(ShippingQuoteRequest.class)))
                    .thenReturn(
                            ShippingFixture.shippingQuoteResponse(
                                    ShippingMethod.EXPRESS,
                                    new BigDecimal("25.00")));

            when(orderMapper.toResponse(any(Order.class)))
                    .thenReturn(OrderFixture.anOrderResponse());

            OrderRequest request =
                    OrderFixture.anOrderRequest(ShippingMethod.EXPRESS);
            orderService.createOrder(IDEMPOTENCY_KEY, request);

            ArgumentCaptor<ShippingQuoteRequest> captor =
                    ArgumentCaptor.forClass(ShippingQuoteRequest.class);

            verify(shippingClient).calculateQuote(captor.capture());

            assertThat(captor.getValue().shippingMethod())
                    .isEqualTo(ShippingMethod.EXPRESS);
        }

        @Test
        void shouldSendCalculatedWeightToShippingService() {

            doNothing().when(customerClient)
                    .validateCustomerExists(CUSTOMER_ID);

            configureRepositoryToAssignIds();

            when(productClient.getProduct(PRODUCT_ID))
                    .thenReturn(
                            productSnapshot(
                                    new BigDecimal("100.00"),
                                    new BigDecimal("2.50")));

            when(shippingClient.calculateQuote(any(ShippingQuoteRequest.class)))
                    .thenReturn(
                            ShippingFixture.shippingQuoteResponse(
                                    ShippingMethod.STANDARD,
                                    new BigDecimal("25.00")));

            when(orderMapper.toResponse(any(Order.class)))
                    .thenReturn(OrderFixture.anOrderResponse());

            orderService.createOrder(IDEMPOTENCY_KEY, orderRequest(2));

            ArgumentCaptor<ShippingQuoteRequest> captor =
                    ArgumentCaptor.forClass(ShippingQuoteRequest.class);

            verify(shippingClient).calculateQuote(captor.capture());

            assertThat(captor.getValue().weightKg())
                    .isEqualByComparingTo("5.00");
        }

        @Test
        void shouldNotProcessPaymentWhenShippingQuoteFails() {

            doNothing().when(customerClient)
                    .validateCustomerExists(CUSTOMER_ID);

            when(productClient.getProduct(PRODUCT_ID))
                    .thenReturn(productSnapshot());

            RuntimeException exception =
                    new RuntimeException("Shipping service unavailable");

            when(shippingClient.calculateQuote(any(ShippingQuoteRequest.class)))
                    .thenThrow(exception);

            assertThatThrownBy(
                    () -> orderService.createOrder(
                            IDEMPOTENCY_KEY,
                            orderRequest()))
                    .isSameAs(exception);

            verify(paymentClient, never())
                    .processPayment(
                            any(),
                            any(),
                            any(),
                            any());
        }


    }

    @Nested
    class GetOrder {
        @Test
        void shouldReturnOrder() {
            // Arrange
            Order order = OrderFixture.anOrder();
            OrderResponse expectedResponse = OrderFixture.anOrderResponse();

            when(repository.findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(order));

            when(orderMapper.toResponse(order)).thenReturn(expectedResponse);

            // Act
            OrderResponse response = orderService.getOrder(ORDER_ID);

            // Assert
            assertThat(response).isSameAs(expectedResponse);

            verify(repository).findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID);
            verify(orderMapper).toResponse(order);
            verifyNoMoreInteractions(repository, orderMapper);
        }

        @Test
        void shouldThrowWhenOrderNotFound() {
            when(repository.findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> orderService.getOrder(ORDER_ID)).isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    class GetOrders {

        @Test
        void shouldReturnOrdersForAuthenticatedCustomer() {
            // Arrange
            Order order1 = OrderFixture.anOrder();
            Order order2 = OrderFixture.anOrder();
            order2.setId(UUID.randomUUID());

            OrderResponse response1 = OrderFixture.anOrderResponse();

            OrderResponse response2 = OrderFixture.anOrderResponse();

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
            when(repository.findByCustomerIdOrderByOrderDateDesc(CUSTOMER_ID)).thenReturn(List.of());
            List<OrderResponse> responses = orderService.getOrdersForAuthenticatedCustomer();
            assertThat(responses).isEmpty();
            verify(repository).findByCustomerIdOrderByOrderDateDesc(CUSTOMER_ID);
        }
    }

    @Nested
    class CancelOrder {
        @Test
        void shouldCancelOrder() {
            // Arrange
            Order existingOrder = OrderFixture.anOrder();

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

            when(repository.findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID)).thenReturn(Optional.of(existingOrder));
            when(orderMapper.toResponse(existingOrder)).thenReturn(expectedResponse);

            // Act
            OrderResponse response = orderService.cancelOrder(ORDER_ID);

            // Assert
            assertThat(existingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            assertThat(response).isSameAs(expectedResponse);

            verify(repository).findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID);
            verify(orderMapper).toResponse(existingOrder);

            verifyNoMoreInteractions(repository, orderMapper);
            verify(orderMetrics).orderCancelled();
        }

        @Test
        void shouldThrowWhenOrderNotFound() {
            when(repository.findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID)).isInstanceOf(OrderNotFoundException.class);
            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowWhenOrderAlreadyCancelled() {
            when(repository.findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID)).thenReturn(Optional.of(cancelledOrder()));
            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID)).isInstanceOf(OrderAlreadyCancelledException.class);
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

    private Order cancelledOrder() {
        Order order = OrderFixture.anOrder();
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


    private void configureRepositoryToAssignIds() {
        when(orderPersistenceService.save(any(Order.class)))
                .thenAnswer(
                        invocation -> {
                            Order order = invocation.getArgument(0);
    
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

    private SavedOrders verifyCreateOrderInteractions() {
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
    
        verify(customerClient).validateCustomerExists(CUSTOMER_ID);
        verify(inventoryClient).reserveStock(PRODUCT_ID, DEFAULT_ORDER_QUANTITY);
        verify(productClient).getProduct(PRODUCT_ID);
    
        verify(orderPersistenceService, times(2)).save(captor.capture());
    
        List<Order> savedOrders = captor.getAllValues();
    
        assertThat(savedOrders).hasSize(2);
    
        Order placedOrder = savedOrders.get(0);
        Order paidOrder = savedOrders.get(1);
    
        assertThat(paidOrder.getId()).isEqualTo(placedOrder.getId());
    
        verify(orderMapper).toResponse(paidOrder);
        assertThat(placedOrder.getIdempotencyKey())
        .isEqualTo(IDEMPOTENCY_KEY);

        assertThat(placedOrder.getIdempotencyRequestHash())
                .isNotBlank();
    
        return new SavedOrders(placedOrder, paidOrder);
    }

    private void assertSavedOrder(Order savedOrder) {
        OrderItem item = savedOrder.getItems().getFirst();
    
        assertThat(item.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(item.getProductName()).isEqualTo("Gaming Mouse");
        assertThat(item.getUnitPrice()).isEqualByComparingTo(new BigDecimal("89.99"));
        assertThat(item.getQuantity()).isEqualTo(1);
        assertThat(item.getLineTotal()).isEqualByComparingTo(new BigDecimal("89.99"));
    
        assertThat(savedOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(savedOrder.getSubtotal()).isEqualByComparingTo(new BigDecimal("89.99"));
        assertThat(savedOrder.getItems()).hasSize(1);
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getOrderDate()).isNotNull();

        assertThat(savedOrder.getShipping())
        .isEqualByComparingTo("15.00");

        assertThat(savedOrder.getTotal())
        .isEqualByComparingTo("104.99");
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
