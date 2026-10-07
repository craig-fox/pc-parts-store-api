package nz.fox.craig.api;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class DownstreamServiceUnavailableExceptionTest {

    @Test
    void shouldCreateExceptionWithServiceNameAndCause() {
        RuntimeException cause = new RuntimeException("Connection refused");

        DownstreamServiceUnavailableException exception =
                new DownstreamServiceUnavailableException("Customer", cause);

        assertThat(exception)
                .hasMessage("Customer service is unavailable")
                .hasCause(cause);
    }
}
