package nz.fox.craig.customer;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractPostgresTest {

        @Container
        @ServiceConnection
        static final PostgreSQLContainer<?> postgres =
                new PostgreSQLContainer<>("postgres:17");

}
