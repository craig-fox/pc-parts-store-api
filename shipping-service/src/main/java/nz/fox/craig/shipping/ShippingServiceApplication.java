package nz.fox.craig.shipping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication (scanBasePackages = "nz.fox.craig")
@ConfigurationPropertiesScan("nz.fox.craig")
public class ShippingServiceApplication  {

	public static void main(String[] args) {
		SpringApplication.run(ShippingServiceApplication.class, args);
	}

}
