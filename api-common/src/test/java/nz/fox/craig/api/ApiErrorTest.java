package nz.fox.craig.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApiErrorTest {

    @Test
    void shouldAllowNullValidationErrors() {
        ApiError error =
                new ApiError(
                        Instant.now(), 400, "Bad Request", "Validation failed", null, "/api/test");

        assertThat(error.validationErrors()).isNull();
    }
}
