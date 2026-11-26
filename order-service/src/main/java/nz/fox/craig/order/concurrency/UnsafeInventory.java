package nz.fox.craig.order.concurrency;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class UnsafeInventory {

    private int availableStock = 10;

    private final CyclicBarrier barrier;

    public UnsafeInventory(CyclicBarrier barrier) {
        this.barrier = barrier;
    }

    public boolean reserve(int quantity) throws InterruptedException, BrokenBarrierException {

        // Both threads read the stock before either updates it.
        int observedStock = availableStock;

        if (observedStock < quantity) {
            return false;
        }

        // Teaching aid: force both threads to reach this point.
        barrier.await();

        // This is deliberately not thread-safe.
        availableStock = observedStock - quantity;

        return true;
    }

    public int getAvailableStock() {
        return availableStock;
    }
}
