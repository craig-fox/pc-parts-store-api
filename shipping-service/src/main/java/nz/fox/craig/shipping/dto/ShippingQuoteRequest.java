package nz.fox.craig.shipping.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import nz.fox.craig.api.ShippingMethod;

import java.math.BigDecimal;
import java.util.UUID;

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
