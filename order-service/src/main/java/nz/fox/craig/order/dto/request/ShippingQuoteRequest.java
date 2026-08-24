package nz.fox.craig.order.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import nz.fox.craig.api.ShippingMethod;


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
