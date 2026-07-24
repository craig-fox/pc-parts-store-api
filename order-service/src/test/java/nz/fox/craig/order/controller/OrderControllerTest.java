package nz.fox.craig.order.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import nz.fox.craig.order.dto.CreateOrderItemRequest;
import nz.fox.craig.order.dto.CreateOrderRequest;
import nz.fox.craig.order.dto.OrderResponse;
import nz.fox.craig.order.exception.CustomerNotFoundException;
import nz.fox.craig.order.exception.OrderAlreadyCancelledException;
import nz.fox.craig.order.exception.OrderExceptionHandler;
import nz.fox.craig.order.exception.OrderNotFoundException;
import nz.fox.craig.order.model.OrderStatus;
import nz.fox.craig.order.service.OrderService;

import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(OrderController.class)
@Import(OrderExceptionHandler.class)
class OrderControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private OrderService orderService;

	private static final UUID ORDER_ID = UUID.randomUUID();
	private static final UUID CUSTOMER_ID = UUID.randomUUID();
	private static final UUID PRODUCT_ID = UUID.randomUUID();

	@Nested
	class CreateOrder {
		@Test
		void returnsCreatedOrder() throws Exception {
			CreateOrderRequest request = orderRequest(CUSTOMER_ID, orderItems());
			OrderResponse response = sampleResponse(OrderStatus.PLACED);

			when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

			mockMvc.perform(post("/api/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
					.andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
					.andExpect(jsonPath("$.status").value("PLACED"))
					.andExpect(jsonPath("$.items").isArray())
					.andExpect(jsonPath("$.total").value(0));
			verify(orderService).createOrder(any(CreateOrderRequest.class));
		}

		@Test
		void missingCustomerIdReturnsBadRequest() throws Exception {
			CreateOrderRequest request = orderRequest(null, orderItems());

			mockMvc.perform(post("/api/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message")
							.value("customerId: Customer id is required"));
		}

		@Test
		void emptyOrderItemsReturnsBadRequest() throws Exception {
			CreateOrderRequest request = orderRequest(CUSTOMER_ID, List.of());

			mockMvc.perform(post("/api/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message")
							.value("items: Items must not be empty"));
		}

		@Test
		void customerNotFoundReturns404() throws Exception {
			var missingCustomerID = UUID.randomUUID();
			CreateOrderRequest request = orderRequest(CUSTOMER_ID, orderItems());

			when(orderService.createOrder(any(CreateOrderRequest.class)))
					.thenThrow(new CustomerNotFoundException(missingCustomerID));

			mockMvc.perform(post("/api/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value("Customer not found with id: " + missingCustomerID));
		}

	}

	@Nested
	class GetOrder {
		@Test
		void returnsOrder() throws Exception {
			when(orderService.getOrder(ORDER_ID))
					.thenReturn(sampleResponse(OrderStatus.PLACED));

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
					.andExpect(jsonPath("$.message").value("Order " + MISSING_ORDER + " not found"));
			verify(orderService).getOrder(MISSING_ORDER);
		}

	}

	@Nested
	class CancelOrder {
		@Test
		void returnsCancelledOrder() throws Exception {
			OrderResponse cancelled = sampleResponse(OrderStatus.CANCELLED);

			when(orderService.cancelOrder(CUSTOMER_ID)).thenReturn(cancelled);

			mockMvc.perform(post("/api/orders/{id}/cancel", CUSTOMER_ID))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("CANCELLED"));
			verify(orderService).cancelOrder(CUSTOMER_ID);
		}

		@Test
		void orderNotFoundReturns404() throws Exception {
			var missingCustomerID = UUID.randomUUID();
			when(orderService.cancelOrder(missingCustomerID)).thenThrow(new OrderNotFoundException(missingCustomerID));

			mockMvc.perform(post("/api/orders/{id}/cancel", missingCustomerID))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value("Order " + missingCustomerID + " not found"));
			verify(orderService).cancelOrder(missingCustomerID);
		}

		@Test
		void returnsConflictIfOrderCancelled() throws Exception {
			when(orderService.cancelOrder(CUSTOMER_ID))
					.thenThrow(new OrderAlreadyCancelledException(CUSTOMER_ID));

			mockMvc.perform(post("/api/orders/{id}/cancel", CUSTOMER_ID))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.message").value("Order " + CUSTOMER_ID + " is already cancelled"));
			verify(orderService).cancelOrder(CUSTOMER_ID);
		}
	}

	private OrderResponse sampleResponse(OrderStatus status) {
		return OrderResponse.builder()
				.id(ORDER_ID)
				.customerId(CUSTOMER_ID)
				.orderDate(LocalDateTime.of(2026, 7, 14, 10, 0))
				.status(status.name())
				.subtotal(BigDecimal.ZERO)
				.shipping(BigDecimal.ZERO)
				.total(BigDecimal.ZERO)
				.items(List.of())
				.build();
	}

	private CreateOrderRequest orderRequest(UUID customerId, List<CreateOrderItemRequest> items) {
		return CreateOrderRequest.builder()
				.customerId(customerId)
				.items(items)
				.build();
	}

	private List<CreateOrderItemRequest> orderItems() {
		return List.of(
				CreateOrderItemRequest.builder()
						.productId(PRODUCT_ID)
						.quantity(1)
						.build());
	}

}
