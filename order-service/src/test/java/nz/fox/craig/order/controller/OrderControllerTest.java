package nz.fox.craig.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import nz.fox.craig.order.dto.request.OrderItemRequest;
import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.dto.request.ShippingAddressRequest;
import nz.fox.craig.order.dto.response.OrderResponse;
import nz.fox.craig.order.exception.CustomerNotFoundException;
import nz.fox.craig.order.exception.InsufficientStockException;
import nz.fox.craig.order.exception.OrderAlreadyCancelledException;
import nz.fox.craig.order.exception.OrderExceptionHandler;
import nz.fox.craig.order.exception.OrderNotFoundException;
import nz.fox.craig.order.exception.ProductNotFoundException;
import nz.fox.craig.order.fixture.OrderResponseFixtures;
import nz.fox.craig.order.service.OrderService;
import nz.fox.craig.security.JwtAuthenticationFilter;
import nz.fox.craig.security.TokenService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(OrderExceptionHandler.class)
@WithMockUser
class OrderControllerTest {
    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OrderService orderService;

    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean private TokenService tokenService;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Nested
    class CreateOrder {
        @Test
        void returnsCreatedOrder() throws Exception {
                OrderRequest request = orderRequest(orderItems());
                OrderResponse response = OrderResponseFixtures.anOrderResponse();
            
                when(orderService.createOrder(any(OrderRequest.class))).thenReturn(response);
            
                mockMvc.perform(
                                post("/api/orders")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").value(response.id().toString()))
                        .andExpect(jsonPath("$.items").isArray());
            
                verify(orderService).createOrder(any(OrderRequest.class));
            }

        @Test
        void emptyOrderItemsReturnsBadRequest() throws Exception {
            OrderRequest request = orderRequest(List.of());

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("items: Items must not be empty"));
        }

        @Test
        void customerNotFoundReturns404() throws Exception {
            var missingCustomerID = UUID.randomUUID();
            OrderRequest request = orderRequest(orderItems());

            when(orderService.createOrder(any(OrderRequest.class)))
                    .thenThrow(new CustomerNotFoundException(missingCustomerID));

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(
                            jsonPath("$.message")
                                    .value("Customer not found with id: " + missingCustomerID));
        }

        @Test
        void insufficientStockReturns409() throws Exception {
            OrderRequest request = orderRequest(orderItems());

            when(orderService.createOrder(any(OrderRequest.class)))
                    .thenThrow(new InsufficientStockException(PRODUCT_ID));

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(
                            jsonPath("$.message")
                                    .value("Insufficient stock for product " + PRODUCT_ID));

            verify(orderService).createOrder(any(OrderRequest.class));
        }

        @Test
        void productNotFoundReturns404() throws Exception {
            OrderRequest request = orderRequest(orderItems());

            when(orderService.createOrder(any(OrderRequest.class)))
                    .thenThrow(new ProductNotFoundException(PRODUCT_ID));

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Product " + PRODUCT_ID + " not found"));

            verify(orderService).createOrder(any(OrderRequest.class));
        }

        @Test
        void nullProductIdReturnsBadRequest() throws Exception {
            OrderItemRequest item = OrderItemRequest.builder().productId(null).quantity(1).build();

            OrderRequest request = orderRequest(List.of(item));

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("items[0].productId: must not be null"));

            verifyNoInteractions(orderService);
        }

        @Test
        void nullQuantityReturnsBadRequest() throws Exception {
            OrderItemRequest item =
                    OrderItemRequest.builder().productId(PRODUCT_ID).quantity(null).build();

            OrderRequest request = orderRequest(List.of(item));

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("items[0].quantity: must not be null"));

            verifyNoInteractions(orderService);
        }

        @Test
        void quantityLessThanOneReturnsBadRequest() throws Exception {
            OrderItemRequest item =
                    OrderItemRequest.builder().productId(PRODUCT_ID).quantity(0).build();

            OrderRequest request = orderRequest(List.of(item));

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.message")
                                    .value(
                                            "items[0].quantity: must be greater than or equal to 1"));

            verifyNoInteractions(orderService);
        }

        @Test
        void blankAddressLine1ReturnsBadRequest() throws Exception {
            ShippingAddressRequest shippingAddress =
                    ShippingAddressRequest.builder()
                            .addressLine1("")
                            .city("Auckland")
                            .postcode("1010")
                            .country("NZ")
                            .build();

            OrderRequest request =
                    OrderRequest.builder()
                            .items(orderItems())
                            .shippingAddress(shippingAddress)
                            .build();

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.message")
                                    .value("shippingAddress.addressLine1: must not be blank"));

            verifyNoInteractions(orderService);
        }
    }

    @Nested
    class GetOrder {
        @Test
        void returnsOrder() throws Exception {
            when(orderService.getOrder(ORDER_ID)).thenReturn(OrderResponseFixtures.anOrderResponse(ORDER_ID));

            mockMvc.perform(get("/api/orders/{id}", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                    .andExpect(jsonPath("$.status").value("PLACED"));
            verify(orderService).getOrder(ORDER_ID);
        }

        @Test
        void orderNotFoundReturns404() throws Exception {
            var MISSING_ORDER = UUID.randomUUID();

            when(orderService.getOrder(MISSING_ORDER))
                    .thenThrow(new OrderNotFoundException(MISSING_ORDER));

            mockMvc.perform(get("/api/orders/{id}", MISSING_ORDER))
                    .andExpect(status().isNotFound())
                    .andExpect(
                            jsonPath("$.message").value("Order " + MISSING_ORDER + " not found"));
            verify(orderService).getOrder(MISSING_ORDER);
        }

        @Test
        void returnsOrders() throws Exception {
            OrderResponse first =  OrderResponseFixtures.anOrderResponse();
            OrderResponse second = OrderResponseFixtures.anOrderResponse("CANCELLED");

            when(orderService.getOrdersForAuthenticatedCustomer())
                    .thenReturn(List.of(first, second));

            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].status").value("PLACED"))
                    .andExpect(jsonPath("$[1].status").value("CANCELLED"));

            verify(orderService).getOrdersForAuthenticatedCustomer();
        }

        @Test
        void returnsEmptyListWhenCustomerHasNoOrders() throws Exception {
            when(orderService.getOrdersForAuthenticatedCustomer()).thenReturn(List.of());

            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(orderService).getOrdersForAuthenticatedCustomer();
        }
    }

    @Nested
    class CancelOrder {
        @Test
        void returnsCancelledOrder() throws Exception {
            OrderResponse cancelled = OrderResponseFixtures.anOrderResponse("CANCELLED");

            when(orderService.cancelOrder(ORDER_ID)).thenReturn(cancelled);

            mockMvc.perform(post("/api/orders/{id}/cancel", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
            verify(orderService).cancelOrder(ORDER_ID);
        }

        @Test
        void orderNotFoundReturns404() throws Exception {
            var missingOrderID = UUID.randomUUID();
            when(orderService.cancelOrder(missingOrderID))
                    .thenThrow(new OrderNotFoundException(missingOrderID));

            mockMvc.perform(post("/api/orders/{id}/cancel", missingOrderID))
                    .andExpect(status().isNotFound())
                    .andExpect(
                            jsonPath("$.message").value("Order " + missingOrderID + " not found"));
            verify(orderService).cancelOrder(missingOrderID);
        }

        @Test
        void returnsConflictIfOrderCancelled() throws Exception {
            when(orderService.cancelOrder(ORDER_ID))
                    .thenThrow(new OrderAlreadyCancelledException(ORDER_ID));

            mockMvc.perform(post("/api/orders/{id}/cancel", ORDER_ID))
                    .andExpect(status().isConflict())
                    .andExpect(
                            jsonPath("$.message")
                                    .value("Order " + ORDER_ID + " is already cancelled"));
            verify(orderService).cancelOrder(ORDER_ID);
        }
    }


    private OrderRequest orderRequest(List<OrderItemRequest> items) {
        return OrderRequest.builder().items(items).shippingAddress(shippingAddress()).build();
    }

    private List<OrderItemRequest> orderItems() {
        return List.of(OrderItemRequest.builder().productId(PRODUCT_ID).quantity(1).build());
    }

    private ShippingAddressRequest shippingAddress() {
        return ShippingAddressRequest.builder()
                .addressLine1("123 Test Street")
                .city("Auckland")
                .postcode("1010")
                .country("NZ")
                .build();
    }
}
