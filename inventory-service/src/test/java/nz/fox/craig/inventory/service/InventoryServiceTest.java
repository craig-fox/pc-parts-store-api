package nz.fox.craig.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import nz.fox.craig.inventory.dto.InventoryResponse;
import nz.fox.craig.inventory.exception.InsufficientInventoryException;
import nz.fox.craig.inventory.exception.InventoryNotFoundException;
import nz.fox.craig.inventory.mapper.InventoryMapper;
import nz.fox.craig.inventory.model.Inventory;
import nz.fox.craig.inventory.model.InventoryStatus;
import nz.fox.craig.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private final UUID PRODUCT_ID = UUID.randomUUID();

    private Inventory inventory;
    private InventoryResponse response;

    @Mock private InventoryRepository inventoryRepository;

    @Mock private InventoryMapper inventoryMapper;

    @InjectMocks private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventory = new Inventory(PRODUCT_ID, 20, 5);

        response =
                new InventoryResponse(
                        PRODUCT_ID,
                        20,
                        5,
                        15,
                        InventoryStatus.IN_STOCK,
                        inventory.getLastUpdated());
    }

    @Nested
    class GetInventory {
        @Test
        void shouldReturnInventory() {
            when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(inventory));

            when(inventoryMapper.toResponse(inventory)).thenReturn(response);

            InventoryResponse result = inventoryService.getInventory(PRODUCT_ID);

            assertThat(result).isEqualTo(response);

            verify(inventoryRepository).findById(PRODUCT_ID);
            verify(inventoryMapper).toResponse(inventory);
        }

        @Test
        void shouldThrowWhenInventoryNotFound() {

            when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.getInventory(PRODUCT_ID))
                    .isInstanceOf(InventoryNotFoundException.class);

            verify(inventoryRepository).findById(PRODUCT_ID);
            verifyNoInteractions(inventoryMapper);
        }
    }

    @Nested
    class ReserveStock {
        @Test
        void shouldReserveStock() {

            when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(inventory));

            when(inventoryMapper.toResponse(any())).thenReturn(response);

            InventoryResponse result = inventoryService.reserveStock(PRODUCT_ID, 3);

            assertThat(result).isEqualTo(response);

            verify(inventoryRepository).save(inventory);
            verify(inventoryMapper).toResponse(inventory);
        }

        @Test
        void shouldThrowWhenInventoryNotFound() {

            when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.reserveStock(PRODUCT_ID, 3))
                    .isInstanceOf(InventoryNotFoundException.class);

            verify(inventoryRepository).findById(PRODUCT_ID);
            verifyNoInteractions(inventoryMapper);
        }

        @Test
        void shouldThrowWhenInsufficientStock() {

            when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> inventoryService.reserveStock(PRODUCT_ID, 100))
                    .isInstanceOf(InsufficientInventoryException.class);

            verify(inventoryRepository, never()).save(any());
        }
    }

    @Nested
    class ReleaseReservation {
        @Test
        void shouldReleaseReservation() {

            when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(inventory));

            when(inventoryMapper.toResponse(any())).thenReturn(response);

            InventoryResponse result = inventoryService.releaseReservation(PRODUCT_ID, 2);

            assertThat(result).isEqualTo(response);

            verify(inventoryRepository).save(inventory);
        }

        @Test
        void shouldThrowWhenInventoryNotFound() {

            when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.releaseReservation(PRODUCT_ID, 2))
                    .isInstanceOf(InventoryNotFoundException.class);

            verify(inventoryRepository).findById(PRODUCT_ID);
            verifyNoInteractions(inventoryMapper);
        }
    }

    @Nested
    class ConfirmReservation {
        @Test
        void shouldConfirmReservation() {

            when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(inventory));

            when(inventoryMapper.toResponse(any())).thenReturn(response);

            InventoryResponse result = inventoryService.confirmReservation(PRODUCT_ID, 2);

            assertThat(result).isEqualTo(response);

            verify(inventoryRepository).save(inventory);
        }

        @Test
        void shouldThrowWhenInventoryNotFound() {

            when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.confirmReservation(PRODUCT_ID, 2))
                    .isInstanceOf(InventoryNotFoundException.class);

            verify(inventoryRepository).findById(PRODUCT_ID);
            verifyNoInteractions(inventoryMapper);
        }
    }
}
