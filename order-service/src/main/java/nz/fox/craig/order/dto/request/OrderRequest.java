package nz.fox.craig.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Builder;

@Builder
public record OrderRequest(
        @Valid @NotEmpty(message = "Items must not be empty") List<OrderItemRequest> items,
        @Valid ShippingAddressRequest shippingAddress) {}
