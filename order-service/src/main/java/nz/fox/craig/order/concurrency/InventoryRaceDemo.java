
package nz.fox.craig.order.concurrency;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class InventoryRaceDemo {

    public static void main(String[] args) throws Exception {

        var barrier = new CyclicBarrier(2);
        var inventory = new UnsafeInventory(barrier);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> requestA =
                    executor.submit(() -> inventory.reserve(7));

            Future<Boolean> requestB =
                    executor.submit(() -> inventory.reserve(7));

            System.out.println(
                    "Request A succeeded: " + requestA.get());

            System.out.println(
                    "Request B succeeded: " + requestB.get());

            System.out.println(
                    "Remaining stock: "
                            + inventory.getAvailableStock());

        } finally {
            executor.shutdown();
        }
    }
}

