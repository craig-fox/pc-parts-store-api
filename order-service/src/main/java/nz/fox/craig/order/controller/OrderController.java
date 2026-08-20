package nz.fox.craig.order.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.dto.response.OrderResponse;
import nz.fox.craig.order.service.OrderCreationResult;
import nz.fox.craig.order.service.OrderService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderRequest request) {
    
        OrderCreationResult result = orderService.createOrder(idempotencyKey, request);
    
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
    
        return ResponseEntity.status(status).body(result.order());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {

        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders() {
        return ResponseEntity.ok(orderService.getOrdersForAuthenticatedCustomer());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID id) {

        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}
