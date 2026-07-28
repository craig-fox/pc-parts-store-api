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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Nested;

import nz.fox.craig.order.client.CustomerClient;
import nz.fox.craig.order.client.ProductClient;
import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.dto.request.CreateOrderItemRequest;
import nz.fox.craig.order.dto.request.CreateOrderRequest;
import nz.fox.craig.order.dto.response.OrderItemResponse;
import nz.fox.craig.order.dto.response.OrderResponse;
import nz.fox.craig.order.exception.CustomerNotFoundException;
import nz.fox.craig.order.exception.OrderAlreadyCancelledException;
import nz.fox.craig.order.exception.OrderNotFoundException;
import nz.fox.craig.order.exception.ProductNotFoundException;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderItem;
import nz.fox.craig.order.model.OrderStatus;
import nz.fox.craig.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private OrderRepository repository;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private ProductClient productClient;

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
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(productClient.getProduct(PRODUCT_ID))
                    .thenReturn(productSnapshot());

            // Act
            OrderResponse response = service.createOrder(orderRequest());

            // Assert - interactions
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);

            verify(customerClient).validateCustomerExists(CUSTOMER_ID);
            verify(productClient).getProduct(PRODUCT_ID);
            verify(repository).save(captor.capture());
            verifyNoMoreInteractions(
                    customerClient,
                    productClient,
                    repository);

            // Assert - order persisted
            Order savedOrder = captor.getValue();
            OrderItem item = savedOrder.getItems().getFirst();

            assertThat(item.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(item.getProductName()).isEqualTo("Gaming Mouse");
            assertThat(item.getUnitPrice())
                    .isEqualByComparingTo(new BigDecimal("89.99"));
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getLineTotal())
                    .isEqualByComparingTo(new BigDecimal("179.98"));

            assertThat(savedOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
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
            assertThat(response.id()).isEqualTo(savedOrder.getId());
            assertThat(response.customerId()).isEqualTo(savedOrder.getCustomerId());
            assertThat(response.status()).isEqualTo(savedOrder.getStatus().name());
            assertThat(response.total()).isEqualByComparingTo(savedOrder.getTotal());
            assertThat(response.items())
                    .hasSize(1);

            OrderItemResponse itemResponse = response.items().getFirst();

            assertThat(itemResponse.productId()).isEqualTo(PRODUCT_ID);
            assertThat(itemResponse.productName()).isEqualTo("Gaming Mouse");
            assertThat(itemResponse.quantity()).isEqualTo(2);
            assertThat(itemResponse.unitPrice())
                    .isEqualByComparingTo(new BigDecimal("89.99"));
            assertThat(itemResponse.lineTotal())
                    .isEqualByComparingTo(new BigDecimal("179.98"));

        }

        @Test
        void shouldThrowWhenCustomerDoesNotExist() {
            CreateOrderRequest request = orderRequest();
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
            when(repository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder()));

            OrderResponse response = service.getOrder(ORDER_ID);

            assertThat(response.status())
                    .isEqualTo(OrderStatus.PLACED.name());

            assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);

            assertThat(response.items())
                    .hasSize(1);

            OrderItemResponse itemResponse = response.items().getFirst();

            assertThat(itemResponse.productId()).isEqualTo(PRODUCT_ID);
            assertThat(itemResponse.productName()).isEqualTo("Gaming Mouse");
            assertThat(itemResponse.quantity()).isEqualTo(2);
            assertThat(itemResponse.unitPrice())
                    .isEqualByComparingTo("89.99");
            assertThat(itemResponse.lineTotal())
                    .isEqualByComparingTo("179.98");

            assertThat(response.total()).isEqualByComparingTo("179.98");
            verify(repository).findById(ORDER_ID);
            verifyNoMoreInteractions(repository);
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
            Order existingOrder = existingOrder();
            when(repository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
            when(repository.save(existingOrder)).thenReturn(existingOrder);

            OrderResponse response = service.cancelOrder(ORDER_ID);

            verify(repository).save(existingOrder);
            assertThat(response.status())
                    .isEqualTo(OrderStatus.CANCELLED.name());
            assertThat(existingOrder.getStatus())
                    .isEqualTo(OrderStatus.CANCELLED);
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

    private CreateOrderRequest orderRequest() {
        return CreateOrderRequest.builder()
                .customerId(CUSTOMER_ID)
                .items(List.of(
                        CreateOrderItemRequest.builder()
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
                        .quantity(2)
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