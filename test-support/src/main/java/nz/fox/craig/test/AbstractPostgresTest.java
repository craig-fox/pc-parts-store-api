package nz.fox.craig.test;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class AbstractPostgresTest {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17");

    static {
        POSTGRES.start();
    }
}
