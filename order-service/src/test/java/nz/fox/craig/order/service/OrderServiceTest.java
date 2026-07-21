package nz.fox.craig.order.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nz.fox.craig.order.client.CustomerClient;
import nz.fox.craig.order.dto.OrderRequest;
import nz.fox.craig.order.dto.OrderResponse;
import nz.fox.craig.order.exception.CustomerNotFoundException;
import nz.fox.craig.order.exception.OrderAlreadyCancelledException;
import nz.fox.craig.order.exception.OrderNotFoundException;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderStatus;
import nz.fox.craig.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderServiceTest {

    private static final Long CUSTOMER_ID = 1L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long ORDER_ID = 123L;
    private static final int QUANTITY = 1;
    private static final Long NEW_CUSTOMER_ID = 2L;
    private static final Long NEW_PRODUCT_ID = 11L;
    private static final int NEW_QUANTITY = 2;

    @Mock
    private OrderRepository repository;

    @Mock
    private CustomerClient client;

    @InjectMocks
    private OrderService service;

   // @Nested
    class CreateOrder {
        @Test
        public void shouldCreateOrder() {
            when(repository.save(any(Order.class))).thenReturn(savedOrder());
    
            OrderResponse response = service.createOrder(orderRequest());
    
            verify(client).validateCustomerExists(CUSTOMER_ID);
            verify(repository).save(any(Order.class));
            assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);
            assertThat(response.productId()).isEqualTo(PRODUCT_ID);
            assertThat(response.quantity()).isEqualTo(QUANTITY);
            assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
        }

        @Test
        public void shouldThrowWhenCustomerDoesNotExist() {
            OrderRequest request = orderRequest();
            doThrow(new CustomerNotFoundException(CUSTOMER_ID))
                    .when(client)
                    .validateCustomerExists(CUSTOMER_ID);
    
            assertThrows(CustomerNotFoundException.class, () -> service.createOrder(request));
    
            verify(repository, never()).save(any());
        }
    }


    @Test
    public void getOrder_shouldReturnOrder() {
        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder()));

        OrderResponse response = service.getOrder(ORDER_ID);

        assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.quantity()).isEqualTo(QUANTITY);
        assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    public void getOrder_shouldThrowWhenOrderNotFound() {
        when(repository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> service.getOrder(ORDER_ID));
    }

    @Test
    public void updateOrder_shouldUpdateOrder() {
        OrderRequest newRequest = new OrderRequest(NEW_CUSTOMER_ID, NEW_PRODUCT_ID, NEW_QUANTITY);
    
        Order existingOrder = savedOrder();
        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(repository.save(existingOrder)).thenReturn(updatedOrder());
    
        OrderResponse response = service.updateOrder(ORDER_ID, newRequest);
    
        verify(client).validateCustomerExists(NEW_CUSTOMER_ID);
        verify(repository).save(existingOrder);
        assertThat(response.customerId()).isEqualTo(NEW_CUSTOMER_ID);
        assertThat(response.productId()).isEqualTo(NEW_PRODUCT_ID);
        assertThat(response.quantity()).isEqualTo(NEW_QUANTITY);
    }

    @Test
    public void updateOrder_shouldThrowWhenOrderNotFound() {
        when(repository.findById(ORDER_ID)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> service.updateOrder(ORDER_ID, orderRequest()));
        verify(repository, never()).save(any());
    }

    @Test
    public void updateOrder_shouldThrowWhenOrderAlreadyCancelled() {
        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(cancelledOrder()));
        verify(client, never()).validateCustomerExists(anyLong());
        assertThrows(OrderAlreadyCancelledException.class, () -> service.updateOrder(ORDER_ID, orderRequest()));
    }

    @Test
    public void updateOrder_shouldThrowWhenCustomerMissing() {
        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder()));
        doThrow(new CustomerNotFoundException(orderRequest().customerId()))
                .when(client)
                .validateCustomerExists(anyLong());

        assertThrows(CustomerNotFoundException.class, () -> service.updateOrder(ORDER_ID, orderRequest()));

        verify(repository, never()).save(any());
    }

    @Test
    public void cancelOrder_shouldCancelOrder() {
        Order existingOrder = savedOrder();
        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
        when(repository.save(existingOrder)).thenReturn(existingOrder);
    
        OrderResponse response = service.cancelOrder(ORDER_ID);
    
        verify(repository).save(existingOrder);
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    public void cancelOrder_shouldThrowWhenOrderNotFound() {
        when(repository.findById(ORDER_ID)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> service.cancelOrder(ORDER_ID));
        verify(client, never()).validateCustomerExists(anyLong());
        verify(repository, never()).save(any());
    }

    @Test
    public void cancelOrder_shouldThrowWhenOrderAlreadyCancelled() {
        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(cancelledOrder()));
        assertThrows(OrderAlreadyCancelledException.class, () -> service.cancelOrder(ORDER_ID));
        verify(client, never()).validateCustomerExists(anyLong());
        verify(repository, never()).save(any());
    }

    private OrderRequest orderRequest() {
        return new OrderRequest(CUSTOMER_ID, PRODUCT_ID, QUANTITY);
    }

    private Order savedOrder() {
        return Order.builder()
                .id(ORDER_ID)
                .customerId(CUSTOMER_ID)
                .productId(PRODUCT_ID)
                .quantity(QUANTITY)
                .orderDateTime(LocalDateTime.now())
                .status(OrderStatus.PLACED)
                .build();
    }

    private Order updatedOrder() {
        return Order.builder()
                .id(ORDER_ID)
                .customerId(2L)
                .productId(11L)
                .quantity(2)
                .orderDateTime(LocalDateTime.now())
                .status(OrderStatus.PLACED)
                .build();
    }

    private Order cancelledOrder() {
        Order order = savedOrder();
        order.setStatus(OrderStatus.CANCELLED);
        return order;
    }

}