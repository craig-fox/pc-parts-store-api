package nz.fox.craig.customer;

import javax.sql.DataSource;
import nz.fox.craig.customer.common.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CustomerServiceApplicationTests extends AbstractPostgresTest {

    @Test
    void contextLoads() { }

    @Autowired DataSource dataSource;
}
