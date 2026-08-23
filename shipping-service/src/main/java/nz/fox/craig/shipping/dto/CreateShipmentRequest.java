package nz.fox.craig.shipping.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateShipmentRequest(

    @NotNull
    UUID quoteId) {
}
