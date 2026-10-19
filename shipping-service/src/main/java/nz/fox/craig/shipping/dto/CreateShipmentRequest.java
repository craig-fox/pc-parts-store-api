package nz.fox.craig.shipping.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateShipmentRequest(@NotNull UUID quoteId) {}
