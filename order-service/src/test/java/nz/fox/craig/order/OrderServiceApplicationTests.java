package nz.fox.craig.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import nz.fox.craig.order.mapper.OrderMapper;
import nz.fox.craig.order.repository.AbstractPostgresTest;

@SpringBootTest
class OrderServiceApplicationTests extends AbstractPostgresTest {


	@Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        System.out.println(context.getBean(OrderMapper.class));
    }

}
