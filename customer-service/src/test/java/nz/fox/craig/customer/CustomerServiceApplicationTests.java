package nz.fox.craig.customer;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nz.fox.craig.customer.common.AbstractPostgresTest;



@SpringBootTest
class CustomerServiceApplicationTests extends AbstractPostgresTest {

	@Test
	void contextLoads() {
	}

	@Autowired
    DataSource dataSource;

}
