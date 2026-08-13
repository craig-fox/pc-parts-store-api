package nz.fox.craig.customer.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> validationErrors,
        String path) {
    public ApiError {
        validationErrors = validationErrors == null ? null : Map.copyOf(validationErrors);
    }
}
