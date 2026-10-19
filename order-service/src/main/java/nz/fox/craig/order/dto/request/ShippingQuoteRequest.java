package nz.fox.craig.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import nz.fox.craig.api.ShippingMethod;

@Builder
public record ShippingQuoteRequest(
        @NotNull UUID orderId,
        @Valid @NotNull ShippingAddressRequest destinationRequest,
        @NotNull @Positive BigDecimal weightKg,
        @NotNull ShippingMethod shippingMethod) {}
