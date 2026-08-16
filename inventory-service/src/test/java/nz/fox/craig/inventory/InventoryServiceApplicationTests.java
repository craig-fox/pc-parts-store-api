package nz.fox.craig.inventory;

import nz.fox.craig.inventory.config.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class InventoryServiceApplicationTests {

    @Test
    void contextLoads() { }
}
