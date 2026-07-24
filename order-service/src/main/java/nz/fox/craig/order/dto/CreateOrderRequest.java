package nz.fox.craig.order.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateOrderRequest(

        @NotNull(message = "Customer id is required")
        UUID customerId,

        @Valid
        @NotEmpty(message = "Items must not be empty")
        List<CreateOrderItemRequest> items
) {
}
