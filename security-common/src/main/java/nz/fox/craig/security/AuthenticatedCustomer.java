package nz.fox.craig.security;

import java.util.UUID;

public record AuthenticatedCustomer(
    UUID customerId,
    String email) {
}
