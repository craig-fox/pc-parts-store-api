package nz.fox.craig.dto;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
    UUID id,
    String email,
    Set<Role> roles
) {
}
