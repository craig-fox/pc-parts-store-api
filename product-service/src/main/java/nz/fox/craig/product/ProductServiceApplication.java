package nz.fox.craig.product;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import nz.fox.craig.product.mapper.ProductMapper;

@SpringBootApplication
public class ProductServiceApplication {

	@Bean
    CommandLineRunner runner(ApplicationContext context) {
        return args -> {
            System.out.println(context.getBeansOfType(ProductMapper.class));
        };
    }

	public static void main(String[] args) {
		SpringApplication.run(ProductServiceApplication.class, args);
	}

}
