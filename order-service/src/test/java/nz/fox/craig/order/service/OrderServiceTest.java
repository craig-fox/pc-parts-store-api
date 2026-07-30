package nz.fox.craig.order.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Nested;

import nz.fox.craig.order.client.CustomerClient;
import nz.fox.craig.order.client.InventoryClient;
import nz.fox.craig.order.client.ProductClient;
import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.dto.request.OrderItemRequest;
import nz.fox.craig.order.dto.request.OrderRequest;
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

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

        private static final UUID CUSTOMER_ID = UUID.randomUUID();
        private static final UUID ORDER_ID = UUID.randomUUID();
        private static final UUID PRODUCT_ID = UUID.randomUUID();
        private static final int QUANTITY = 2;

        @Mock
        private OrderRepository repository;

        @Mock
        private CustomerClient customerClient;

        @Mock
        private ProductClient productClient;

        @Mock
        private InventoryClient inventoryClient;

        @Mock
        private OrderMapper orderMapper;

        @InjectMocks
        private OrderService service;

        @Nested
        class CreateOrder {
                @Test
                void shouldCreateOrder() {
                        // Arrange
                        doNothing().when(customerClient)
                                        .validateCustomerExists(CUSTOMER_ID);

                        when(repository.save(any(Order.class)))
                                        .thenAnswer(invocation -> {
                                                Order order = invocation.getArgument(0);
                                                if (order.getId() == null) {
                                                        order.setId(UUID.randomUUID());
                                                }
                                                order.getItems().forEach(item -> {
                                                        if (item.getId() == null) {
                                                                item.setId(UUID.randomUUID());
                                                        }
                                                });
                                                return order;
                                        });
                        when(productClient.getProduct(PRODUCT_ID))
                                        .thenReturn(productSnapshot());
                        OrderResponse expectedResponse = OrderResponse.builder()
                                        .id(ORDER_ID)
                                        .status("PLACED")
                                        .build();

                        when(orderMapper.toResponse(any(Order.class)))
                                        .thenReturn(expectedResponse);

                        // Act
                        OrderResponse response = service.createOrder(orderRequest());

                        // Assert - interactions
                        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
                     

                        InOrder inOrder = inOrder(
                                        customerClient,
                                        inventoryClient,
                                        productClient,
                                        repository,
                                        orderMapper);
                        inOrder.verify(customerClient)
                                        .validateCustomerExists(CUSTOMER_ID);

                        inOrder.verify(inventoryClient)
                                        .reserveStock(PRODUCT_ID, QUANTITY);

                        inOrder.verify(productClient)
                                        .getProduct(PRODUCT_ID);

                        inOrder.verify(repository)
                                        .save(captor.capture());
                        

                           // Assert - order persisted
                        Order savedOrder = captor.getValue();
                        OrderItem item = savedOrder.getItems().getFirst();  
                        inOrder.verify(orderMapper)
                                        .toResponse(savedOrder);              

                        verifyNoMoreInteractions(
                                        customerClient,
                                        inventoryClient,
                                        productClient,
                                        repository);

                        assertThat(item.getProductId()).isEqualTo(PRODUCT_ID);
                        assertThat(item.getProductName()).isEqualTo("Gaming Mouse");
                        assertThat(item.getUnitPrice())
                                        .isEqualByComparingTo(new BigDecimal("89.99"));
                        assertThat(item.getQuantity()).isEqualTo(2);
                        assertThat(item.getLineTotal())
                                        .isEqualByComparingTo(new BigDecimal("179.98"));
                        assertThat(savedOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
                        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
                        assertThat(savedOrder.getSubtotal())
                                        .isEqualByComparingTo(new BigDecimal("179.98"));

                        assertThat(savedOrder.getShipping())
                                        .isEqualByComparingTo(new BigDecimal("0"));

                        assertThat(savedOrder.getTotal())
                                        .isEqualByComparingTo(new BigDecimal("179.98"));
                        assertThat(savedOrder.getItems())
                                        .hasSize(1);
                        assertThat(savedOrder.getId()).isNotNull();
                        assertThat(savedOrder.getOrderDate()).isNotNull();

                        // Assert - returned response
                        assertThat(response).isSameAs(expectedResponse);
                }

                @Test
                void shouldNotCreateOrderWhenInventoryReservationFails() {
                        doThrow(new InsufficientStockException(PRODUCT_ID))
                                        .when(inventoryClient)
                                        .reserveStock(PRODUCT_ID, QUANTITY);

                        assertThatThrownBy(() -> service.createOrder(orderRequest()))
                                        .isInstanceOf(InsufficientStockException.class);

                        verify(repository, never()).save(any());
                        verify(inventoryClient)
                                        .reserveStock(PRODUCT_ID, QUANTITY);

                        verify(repository, never()).save(any());
                        verify(productClient, never()).getProduct(any());
                }

                @Test
                void shouldThrowWhenCustomerDoesNotExist() {
                        OrderRequest request = orderRequest();
                        doThrow(new CustomerNotFoundException(CUSTOMER_ID))
                                        .when(customerClient)
                                        .validateCustomerExists(CUSTOMER_ID);

                        assertThrows(CustomerNotFoundException.class, () -> service.createOrder(request));

                        verify(repository, never()).save(any());
                }

                @Test
                void shouldThrowWhenProductDoesNotExist() {
                        when(productClient.getProduct(PRODUCT_ID))
                                        .thenThrow(new ProductNotFoundException(PRODUCT_ID));
                        doNothing().when(customerClient)
                                        .validateCustomerExists(CUSTOMER_ID);

                        assertThatThrownBy(() -> service.createOrder(orderRequest()))
                                        .isInstanceOf(ProductNotFoundException.class);

                        verify(repository, never()).save(any());
                }
        }

        @Nested
        class GetOrder {
                @Test
                void shouldReturnOrder() {
                        // Arrange
                        Order order = existingOrder();

                        OrderResponse expectedResponse = OrderResponse.builder()
                                        .id(ORDER_ID)
                                        .customerId(CUSTOMER_ID)
                                        .status(OrderStatus.PLACED.name())
                                        .subtotal(new BigDecimal("179.98"))
                                        .shipping(BigDecimal.ZERO)
                                        .total(new BigDecimal("179.98"))
                                        .items(List.of(
                                                        OrderItemResponse.builder()
                                                                        .productId(PRODUCT_ID)
                                                                        .productName("Gaming Mouse")
                                                                        .quantity(QUANTITY)
                                                                        .unitPrice(new BigDecimal("89.99"))
                                                                        .lineTotal(new BigDecimal("179.98"))
                                                                        .build()))
                                        .build();

                        when(repository.findById(ORDER_ID))
                                        .thenReturn(Optional.of(order));

                        when(orderMapper.toResponse(order))
                                        .thenReturn(expectedResponse);

                        // Act
                        OrderResponse response = service.getOrder(ORDER_ID);

                        // Assert
                        assertThat(response).isSameAs(expectedResponse);

                        verify(repository).findById(ORDER_ID);
                        verify(orderMapper).toResponse(order);
                        verifyNoMoreInteractions(repository, orderMapper);
                }

                @Test
                void shouldThrowWhenOrderNotFound() {
                        when(repository.findById(ORDER_ID)).thenReturn(Optional.empty());

                        assertThrows(OrderNotFoundException.class, () -> service.getOrder(ORDER_ID));
                }
        }

        @Nested
        class CancelOrder {
                @Test
                void shouldCancelOrder() {
                        // Arrange
                        Order existingOrder = existingOrder();

                        OrderResponse expectedResponse = OrderResponse.builder()
                                        .id(ORDER_ID)
                                        .customerId(CUSTOMER_ID)
                                        .status(OrderStatus.CANCELLED.name())
                                        .subtotal(new BigDecimal("179.98"))
                                        .shipping(BigDecimal.ZERO)
                                        .total(new BigDecimal("179.98"))
                                        .items(List.of(
                                                        OrderItemResponse.builder()
                                                                        .productId(PRODUCT_ID)
                                                                        .productName("Gaming Mouse")
                                                                        .quantity(QUANTITY)
                                                                        .unitPrice(new BigDecimal("89.99"))
                                                                        .lineTotal(new BigDecimal("179.98"))
                                                                        .build()))
                                        .build();

                        when(repository.findById(ORDER_ID))
                                        .thenReturn(Optional.of(existingOrder));
                        when(repository.save(existingOrder))
                                        .thenReturn(existingOrder);
                        when(orderMapper.toResponse(existingOrder))
                                        .thenReturn(expectedResponse);

                        // Act
                        OrderResponse response = service.cancelOrder(ORDER_ID);

                        // Assert
                        assertThat(existingOrder.getStatus())
                                        .isEqualTo(OrderStatus.CANCELLED);

                        assertThat(response).isSameAs(expectedResponse);

                        verify(repository).findById(ORDER_ID);
                        verify(repository).save(existingOrder);
                        verify(orderMapper).toResponse(existingOrder);

                        verifyNoMoreInteractions(repository, orderMapper);
                }

                @Test
                void shouldThrowWhenOrderNotFound() {
                        when(repository.findById(ORDER_ID)).thenReturn(Optional.empty());
                        assertThrows(OrderNotFoundException.class, () -> service.cancelOrder(ORDER_ID));
                        verify(repository, never()).save(any());
                }

                @Test
                void shouldThrowWhenOrderAlreadyCancelled() {
                        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(cancelledOrder()));
                        assertThrows(OrderAlreadyCancelledException.class, () -> service.cancelOrder(ORDER_ID));
                        verify(repository, never()).save(any());
                }
        }

        private OrderRequest orderRequest() {
                return OrderRequest.builder()
                                .customerId(CUSTOMER_ID)
                                .items(List.of(
                                                OrderItemRequest.builder()
                                                                .productId(PRODUCT_ID)
                                                                .quantity(2)
                                                                .build()))
                                .build();
        }

        private Order existingOrder() {
                Order order = Order.builder()
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
                                                .quantity(QUANTITY)
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
}
