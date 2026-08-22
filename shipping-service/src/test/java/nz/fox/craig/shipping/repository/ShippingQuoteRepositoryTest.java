package nz.fox.craig.shipping.repository;

import nz.fox.craig.shipping.fixture.ShippingFixture;
import nz.fox.craig.shipping.model.ShippingMethod;
import nz.fox.craig.shipping.model.ShippingQuote;
import nz.fox.craig.test.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ShippingQuoteRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private ShippingQuoteRepository shippingQuoteRepository;

    @Test
    void shouldSaveAndRetrieveShippingQuote() {
        UUID orderId = UUID.randomUUID();

        ShippingQuote quote = ShippingFixture.shippingQuote(orderId);

        ShippingQuote savedQuote = shippingQuoteRepository.save(quote);

        ShippingQuote retrievedQuote = shippingQuoteRepository
                .findById(savedQuote.getId())
                .orElseThrow();

        assertThat(retrievedQuote.getId()).isEqualTo(savedQuote.getId());
        assertThat(retrievedQuote.getOrderId()).isEqualTo(orderId);
        assertThat(retrievedQuote.getDestination().getAddressLine1())
                .isEqualTo("123 Test Street");
        assertThat(retrievedQuote.getDestination().getCity())
                .isEqualTo("Auckland");
        assertThat(retrievedQuote.getDestination().getPostcode())
                .isEqualTo("1010");
        assertThat(retrievedQuote.getDestination().getCountry())
                .isEqualTo("NZ");
        assertThat(retrievedQuote.getWeightKg()).isEqualByComparingTo("2.500");
        assertThat(retrievedQuote.getShippingMethod()).isEqualTo(ShippingMethod.STANDARD);
        assertThat(retrievedQuote.getPrice()).isEqualByComparingTo("15.00");
        assertThat(retrievedQuote.getCurrency()).isEqualTo("NZD");
        assertThat(retrievedQuote.getExpiresAt()).isEqualTo(quote.getExpiresAt());
        assertThat(retrievedQuote.getCreatedAt()).isEqualTo(quote.getCreatedAt());
    }

    @Test
    void shouldFindQuotesByOrderId() {
        UUID orderId = UUID.randomUUID();
        UUID anotherOrderId = UUID.randomUUID();

        ShippingQuote firstQuote = ShippingFixture.shippingQuote(orderId, ShippingMethod.STANDARD);
        ShippingQuote secondQuote = ShippingFixture.shippingQuote(orderId, ShippingMethod.EXPRESS);
        ShippingQuote otherOrderQuote = ShippingFixture.shippingQuote(anotherOrderId, ShippingMethod.STANDARD);

        shippingQuoteRepository.saveAll(
                List.of(firstQuote, secondQuote, otherOrderQuote)
        );

        List<ShippingQuote> quotes = shippingQuoteRepository.findByOrderId(orderId);

        assertThat(quotes)
                .hasSize(2)
                .extracting(ShippingQuote::getOrderId)
                .containsOnly(orderId);
    }

}
