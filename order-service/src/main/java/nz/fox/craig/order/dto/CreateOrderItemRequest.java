package nz.fox.craig.order.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;


@Builder
public record CreateOrderItemRequest(

        @NotNull
        UUID productId,

        @NotNull
        @Min(1)
        Integer quantity
) {
}

