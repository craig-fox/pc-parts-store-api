package nz.fox.craig.order.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import nz.fox.craig.order.dto.CreateOrderRequest;
import nz.fox.craig.order.dto.OrderResponse;
import nz.fox.craig.order.exception.CustomerNotFoundException;
import nz.fox.craig.order.exception.OrderAlreadyCancelledException;
import nz.fox.craig.order.exception.OrderNotFoundException;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderStatus;
import nz.fox.craig.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    @Mock
    private OrderRepository repository;

    @Mock
    private CustomerClient client;

    @InjectMocks
    private OrderService service;

    @Nested
    class CreateOrder {
        @Test
        void shouldCreateOrder() {
            // Arrange
            when(repository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
        
            // Act
            OrderResponse response = service.createOrder(orderRequest());
        
            // Assert - interactions
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        
            verify(client).validateCustomerExists(CUSTOMER_ID);
            verify(repository).save(captor.capture());
            verifyNoMoreInteractions(client, repository);
        
            // Assert - order persisted
            Order saved = captor.getValue();
        
            assertThat(saved.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(saved.getStatus()).isEqualTo(OrderStatus.PLACED);
            assertThat(saved.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getShipping()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getItems()).isEmpty();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getOrderDate()).isNotNull();
        
            // Assert - returned response
            assertThat(response.id()).isEqualTo(saved.getId());
            assertThat(response.customerId()).isEqualTo(saved.getCustomerId());
            assertThat(response.status()).isEqualTo(saved.getStatus().name());
            assertThat(response.total()).isEqualByComparingTo(saved.getTotal());
            assertThat(response.items()).isEmpty();
        }


        @Test
        void shouldThrowWhenCustomerDoesNotExist() {
            CreateOrderRequest request = orderRequest();
            doThrow(new CustomerNotFoundException(CUSTOMER_ID))
                    .when(client)
                    .validateCustomerExists(CUSTOMER_ID);

            assertThrows(CustomerNotFoundException.class, () -> service.createOrder(request));

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
    
            assertThat(response.items()).isEmpty();
    
            assertThat(response.total())
                    .isEqualByComparingTo("0");
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
                .items(List.of())
                .build();
    }

    private Order existingOrder() {

        return Order.builder()
                .id(ORDER_ID)
                .customerId(CUSTOMER_ID)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PLACED)
                .subtotal(BigDecimal.ZERO)
                .shipping(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .build();
    }

    private Order cancelledOrder() {
        Order order = existingOrder();
        order.setStatus(OrderStatus.CANCELLED);
        return order;
    }

}