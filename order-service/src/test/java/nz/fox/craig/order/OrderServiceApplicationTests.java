package nz.fox.craig.order;

import java.util.Arrays;

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
    void dumpMapperBeans() {

        context.getBeansOfType(OrderMapper.class)
                .forEach((k, v) -> System.out.println(k + " -> " + v));

        Arrays.stream(context.getBeanDefinitionNames())
                .filter(n -> n.contains("Mapper"))
                .sorted()
                .forEach(System.out::println);
    }
}
