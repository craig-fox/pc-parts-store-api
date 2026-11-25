
package nz.fox.craig.order.concurrency;

public class SynchronizedInventory {

    private int availableStock = 10;

    public synchronized boolean reserve(int quantity) {

        if (availableStock < quantity) {
            return false;
        }

        availableStock -= quantity;
        return true;
    }

    public synchronized int getAvailableStock() {
        return availableStock;
    }
}

