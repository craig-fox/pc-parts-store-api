package nz.fox.craig.auth.dto;

import java.util.Set;
import java.util.UUID;

import nz.fox.craig.dto.Role;

public record AuthenticatedCustomer(
    UUID id,
    String email,
    String passwordHash,
    boolean active,
    String firstName,
    String preferredName,
    Set<Role> roles
) {}