package nz.fox.craig.order.dto.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductSnapshotTest {

    @Test
    void shouldReturnFalseWhenActiveIsNull() {
        ProductSnapshot snapshot = ProductSnapshot.builder()
                .active(null)
                .build();

        assertThat(snapshot.active()).isFalse();
    }

    @Test
    void shouldReturnTrueWhenActiveIsTrue() {
        ProductSnapshot snapshot = ProductSnapshot.builder()
                .active(true)
                .build();

        assertThat(snapshot.active()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenActiveIsFalse() {
        ProductSnapshot snapshot = ProductSnapshot.builder()
                .active(false)
                .build();

        assertThat(snapshot.active()).isFalse();
    }
}
