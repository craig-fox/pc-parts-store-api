package nz.fox.craig.auth.dto;

import java.util.UUID;


public record AuthenticatedCustomer(
    UUID id,
    String email,
    String password,
    boolean active,
    String firstName,
    String preferredName
) {}