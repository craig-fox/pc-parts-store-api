package nz.fox.craig.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record OrderItemRequest(@NotNull UUID productId, @NotNull @Min(1) Integer quantity) { }
