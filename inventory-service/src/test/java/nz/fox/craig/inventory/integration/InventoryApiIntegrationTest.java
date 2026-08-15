package nz.fox.craig.inventory.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;

import nz.fox.craig.inventory.model.Inventory;
import nz.fox.craig.inventory.repository.AbstractPostgresTest;
import nz.fox.craig.inventory.repository.InventoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryApiIntegrationTest extends AbstractPostgresTest {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    private UUID productId;
    private UUID customerId;
    private String token;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();

        productId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        token =
                JwtTestFactory.createToken(
                        customerId,
                        "test@example.com",
                        jwtSecret,
                        Duration.ofHours(1));

        inventoryRepository.save(
                new Inventory(productId, 20, 5));
    }

    @Test
    void shouldReturnInventory() throws Exception {

        mockMvc.perform(
                get("/api/inventory/{productId}", productId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantityOnHand").value(20))
                .andExpect(jsonPath("$.quantityReserved").value(5))
                .andExpect(jsonPath("$.availableQuantity").value(15))
                .andExpect(jsonPath("$.status").value("IN_STOCK"));
    }

    @Test
    void shouldRequireAuthentication() throws Exception {

        mockMvc.perform(
                get("/api/inventory/{productId}", productId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReserveStock() throws Exception {

        String request = """
                {
                    "quantity": 3
                }
                """;

        mockMvc.perform(
                post("/api/inventory/{productId}/reserve", productId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantityOnHand").value(20))
                .andExpect(jsonPath("$.quantityReserved").value(8))
                .andExpect(jsonPath("$.availableQuantity").value(12))
                .andExpect(jsonPath("$.status").value("IN_STOCK"));

        Inventory inventory = inventoryRepository.findById(productId).orElseThrow();

        assertEquals(20, inventory.getQuantityOnHand());
        assertEquals(8, inventory.getQuantityReserved());
        assertEquals(12, inventory.getAvailableQuantity());
    }

    @Test
    void shouldReturnConflictWhenInsufficientStock() throws Exception {

        String request = """
                {
                    "quantity": 100
                }
                """;

        mockMvc.perform(
                post("/api/inventory/{productId}/reserve", productId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());

        Inventory inventory = inventoryRepository.findById(productId).orElseThrow();

        assertEquals(20, inventory.getQuantityOnHand());
        assertEquals(5, inventory.getQuantityReserved());
        assertEquals(15, inventory.getAvailableQuantity());
    }

    @Test
    void shouldReturnBadRequestWhenReserveQuantityIsInvalid() throws Exception {

        String request = """
                {
                    "quantity": 0
                }
                """;

        mockMvc.perform(
                post("/api/inventory/{productId}/reserve", productId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireAuthenticationWhenReservingStock() throws Exception {

        String request = """
                {
                    "quantity": 3
                }
                """;

        mockMvc.perform(
                post("/api/inventory/{productId}/reserve", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        Inventory inventory = inventoryRepository.findById(productId).orElseThrow();

        assertEquals(5, inventory.getQuantityReserved());
    }

    
}
