package nz.fox.craig.order.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import nz.fox.craig.api.ShippingMethod;

@Builder
public record ShippingQuoteRequest(

    @NotNull
    UUID orderId,

    @Valid
    @NotNull
    ShippingAddressRequest destinationRequest,

    @NotNull
    @Positive
    BigDecimal weightKg,

    @NotNull
    ShippingMethod shippingMethod) {
}
