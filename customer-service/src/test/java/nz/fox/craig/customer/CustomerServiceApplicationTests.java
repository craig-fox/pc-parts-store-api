package nz.fox.craig.customer;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;



@SpringBootTest
class CustomerServiceApplicationTests {

	// @Test
	// void contextLoads() {
	// }

	@Autowired
    DataSource dataSource;

    @Test
    void contextLoads() throws Exception {
        System.out.println(dataSource.getConnection().getMetaData().getURL());
    }

}
