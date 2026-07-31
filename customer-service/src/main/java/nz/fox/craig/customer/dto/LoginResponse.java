package nz.fox.craig.customer.dto;

import java.util.UUID;

public record LoginResponse(
        String token,
        UUID customerId,
        String firstName,
        String preferredName) {
}