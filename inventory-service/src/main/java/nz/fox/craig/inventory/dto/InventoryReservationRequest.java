package nz.fox.craig.inventory.dto;

import jakarta.validation.constraints.Positive;

public record InventoryReservationRequest(
        @Positive(message = "Quantity must be greater than zero") int quantity) { }
