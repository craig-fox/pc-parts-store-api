package nz.fox.craig.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;
import nz.fox.craig.api.ShippingMethod;


@Builder
public record OrderRequest(
        @Valid @NotEmpty(message = "Items must not be empty") List<OrderItemRequest> items,
       
        @Valid @NotNull(message = "Shipping address is required") ShippingAddressRequest shippingAddress,
        @Valid @NotNull(message = "Must choose a shipping method") ShippingMethod shippingMethod) {

        public OrderRequest {
                items = items == null ? null : List.copyOf(items);
        }
}
