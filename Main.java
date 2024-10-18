import java.util.Random;
import java.util.concurrent.Semaphore;

public class Main {
    // Maximum number of riders the bus can accommodate
    static final int BUS_CAPACITY = 50;
    // shared variable for counting riders waiting
    static int riders = 0;
    // Semaphore to restrict bus capacity to 50 riders
    static Semaphore multiplex = new Semaphore(BUS_CAPACITY);
    // blocks riders until the bus arrives
    static Semaphore bus = new Semaphore(0);
    // signals when all riders have boarded
    static Semaphore allAboard = new Semaphore(0);
    //mutex to safe access the riders count
    static Semaphore mutex = new Semaphore(1);
    // Set rider arrival mean time to 30 seconds
    static float riderArrivalMean = 30f * 1000;
    // Set bus arrival mean time to 20 minutes
    static float busArrivalMean = 20 * 60f * 1000;
    // Random number generator to produce exponential inter-arrival times
    private static final Random random = new Random();
    // Calculates the inter-arrival time based on an exponential distribution
    public static long calculateInterArrivalTime(float meanTime) {
        float lambda = 1 / meanTime;
        return Math.round(-Math.log(1 - random.nextFloat()) / lambda);
    }

    public static void main(String[] args) {
        // Create and start the rider and bus generator threads
        RiderGenerator riderGenerator = new RiderGenerator();
        BusGenerator busGenerator = new BusGenerator();
        // Start the generators
        riderGenerator.start();
        busGenerator.start();

    }

}

class Bus extends Thread {
    int busIndex;

    Bus(int busIndex) {
        this.busIndex = busIndex;
    }

    @Override
    public void run() {
        try {
            // Only those passengers present when the bus arrived are permitted to board.
            Main.mutex.acquire();
            System.out.println("Bus #" + this.busIndex + " has arrived at the station. " + Main.riders + " riders waiting to board.");
            if (Main.riders > 0) {
                // Awake a rider who was waiting to board the bus
                Main.bus.release();
                // Wait until all riders have boarded
                Main.allAboard.acquire();
            } else {
                System.out.println("Bus arrived. No riders, Departing immediately.");
            }
            //Allow the new riders to wait for the next bus
            Main.mutex.release();
            depart();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Simulate the process of bus departing
    private void depart() {
        System.out.println("Bus #" + this.busIndex + " departed .");
    }

}

class Rider extends Thread {
    int riderIndex;

    Rider(int riderIndex) {
        this.riderIndex = riderIndex;
    }

    @Override
    public void run() {
        try {
            // The rider acquires the multiplex, which controls how many riders can board the bus.
            Main.multiplex.acquire();
            System.out.println("Rider #" + this.riderIndex + " has arrived at the bus stop.");
            // If the bus is not present, increase the count of riders waiting to board.
            // When a bus arrives at the stop, the thread will be unable to acquire this mutex since it is held by the bus.
            Main.mutex.acquire();
            Main.riders++;
            Main.mutex.release();

            // The rider sleeps till the bus to signal that boarding can begin.
            Main.bus.acquire();

            //The rider releases the multiplex after being allowed to board the bus
            Main.multiplex.release();
            boardBus();

            // No need to lock this section as only one rider at a time to proceed with the boarding process .
            Main.riders--;

            if (Main.riders == 0) {
                System.out.println("All riders have boarded. The bus is now preparing to depart.");
                //The last rider signals the bus that all riders have boarded.
                Main.allAboard.release();
            } else {
                // A rider (who is not the last) allow one more rider to board the bus by releasing this semaphore
                Main.bus.release();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    void boardBus() {
        System.out.println("Rider # " + this.riderIndex + " boarded");
    }

}


class RiderGenerator extends Thread {
    private int riderCount = 0;
    RiderGenerator() {}

    @Override
    public void run() {
        while (true) {
            riderCount++;
            Rider rider = new Rider(riderCount);
            rider.start();

            try {
                Thread.sleep(Main.calculateInterArrivalTime(Main.riderArrivalMean));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

class BusGenerator extends Thread {
    private int busCount = 0;
    BusGenerator() {}

    @Override
    public void run() {
        while (true) {
            busCount++;
            Bus bus = new Bus(busCount);
            bus.start();

            try {
                Thread.sleep(Main.calculateInterArrivalTime(Main.busArrivalMean));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}