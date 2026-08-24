package nz.fox.craig.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ApiErrorTest {

    @Test
    void shouldAllowNullValidationErrors() {
        ApiError error =
                new ApiError(
                        Instant.now(), 400, "Bad Request", "Validation failed", null, "/api/test");

        assertThat(error.validationErrors()).isNull();
    }

    @Test
    void shouldCopyValidationErrors() {
        Map<String, String> validationErrors =
                new HashMap<>();

        validationErrors.put("email", "must be valid");

        ApiError error = new ApiError(
                Instant.now(),
                400,
                "Bad Request",
                "Validation failed",
                validationErrors,
                "/api/test");

        assertThat(error.validationErrors())
                .containsEntry("email", "must be valid");
    }
}
