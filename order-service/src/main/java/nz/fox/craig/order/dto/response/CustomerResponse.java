package nz.fox.craig.order.dto.response;

public record CustomerResponse(
    Long id,
    String name,
    String email,
    String address
) {
}
