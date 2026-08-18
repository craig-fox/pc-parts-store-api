package nz.fox.craig.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import nz.fox.craig.product.fixture.ProductFixtures;
import nz.fox.craig.product.model.Product;
import nz.fox.craig.test.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ProductRepositoryTest extends AbstractPostgresTest {

    @Autowired private ProductRepository productRepository;

    @Test
    void shouldFindActiveProducts() {
        List<Product> products = productRepository.findByActiveTrue();

        assertThat(products).isNotEmpty();
        assertThat(products).allMatch(Product::isActive);
    }

    @Test
    void shouldFindProductBySku() {
        Optional<Product> product = productRepository.findBySku("CPU-AMD-9800X3D");

        assertThat(product).isPresent();
        assertThat(product.get().getName()).isEqualTo("AMD Ryzen 7 9800X3D");
    }

    @Test
    void shouldFindProductsByCategoryIgnoringCase() {
        List<Product> products = productRepository.findByCategoryIgnoreCase("cpu");

        assertThat(products).isNotEmpty();
        assertThat(products).allMatch(product -> product.getCategory().equalsIgnoreCase("cpu"));
    }

    @Test
    void shouldFindProductsByBrandIgnoringCase() {
        List<Product> products = productRepository.findByBrandIgnoreCase("amd");

        assertThat(products).isNotEmpty();
        assertThat(products).allMatch(product -> product.getBrand().equalsIgnoreCase("amd"));
    }

    @Test
    void shouldPopulateFieldsWhenProductIsPersisted() {
        Product product = ProductFixtures.aProduct();

        assertThat(product.getId()).isNull();
        assertThat(product.getCreatedAt()).isNull();
        assertThat(product.getUpdatedAt()).isNull();

        Product saved = productRepository.saveAndFlush(product);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldUpdateUpdatedAtWhenProductIsUpdated() {
        Product product = ProductFixtures.aProduct();
        Product saved = productRepository.saveAndFlush(product);

        Instant originalUpdatedAt = saved.getUpdatedAt();

        saved.setName("Updated Product");
        Product updated = productRepository.saveAndFlush(saved);

        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

}
