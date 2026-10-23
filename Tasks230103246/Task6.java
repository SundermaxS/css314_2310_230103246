package Tasks230103246;


import java.util.concurrent.atomic.AtomicInteger;

public class Task6 {

    private static final long STUDENT_ID = 230103246L;
    private static final int THREADS = 4;
    private static final int INCREMENTS_PER_THREAD = 2_000_000;
    private static final int EXPECTED = THREADS * INCREMENTS_PER_THREAD;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Student ID: " + STUDENT_ID);
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Logical cores: "
                + Runtime.getRuntime().availableProcessors());
        System.out.println("Threads: " + THREADS);
        System.out.println("Increments per thread: "
                + INCREMENTS_PER_THREAD);
        System.out.println("Expected final value: " + EXPECTED);
        System.out.println();

        // Version 1: Without synchronization
        int[] sharedCounter = {0};

        long startUnsafe = System.nanoTime();

        Thread[] unsafeThreads = new Thread[THREADS];

        for (int i = 0; i < THREADS; i++) {
            unsafeThreads[i] = new Thread(() -> {
                for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                    sharedCounter[0]++;
                }
            });
            unsafeThreads[i].start();
        }

        for (Thread thread : unsafeThreads) {
            thread.join();
        }

        long endUnsafe = System.nanoTime();

        int actualUnsafe = sharedCounter[0];
        double unsafeMs = (endUnsafe - startUnsafe) / 1_000_000.0;

        long lostUpdates = EXPECTED - (long) actualUnsafe;
        double lostPercentage =
                lostUpdates * 100.0 / EXPECTED;

        System.out.println("=== Without Synchronization ===");
        System.out.println("Expected value: " + EXPECTED);
        System.out.println("Actual value: " + actualUnsafe);
        System.out.println("Lost updates: " + lostUpdates);
        System.out.printf("Lost updates percentage: %.2f%%%n",
                lostPercentage);
        System.out.printf("Execution time: %.3f ms%n", unsafeMs);
        System.out.println();

        // Version 2: With AtomicInteger
        AtomicInteger atomicCounter = new AtomicInteger(0);

        long startAtomic = System.nanoTime();

        Thread[] atomicThreads = new Thread[THREADS];

        for (int i = 0; i < THREADS; i++) {
            atomicThreads[i] = new Thread(() -> {
                for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                    atomicCounter.incrementAndGet();
                }
            });
            atomicThreads[i].start();
        }

        for (Thread thread : atomicThreads) {
            thread.join();
        }

        long endAtomic = System.nanoTime();

        int actualAtomic = atomicCounter.get();
        double atomicMs = (endAtomic - startAtomic) / 1_000_000.0;

        double penaltyMs = atomicMs - unsafeMs;
        double penaltyPercentage = unsafeMs > 0
                ? penaltyMs * 100.0 / unsafeMs
                : 0.0;

        System.out.println("=== With AtomicInteger ===");
        System.out.println("Expected value: " + EXPECTED);
        System.out.println("Actual value: " + actualAtomic);
        System.out.println("Lost updates: "
                + (EXPECTED - (long) actualAtomic));
        System.out.printf("Execution time: %.3f ms%n", atomicMs);
        System.out.println();

        System.out.println("=== Synchronization Cost ===");
        System.out.printf("Time penalty: %.3f ms%n", penaltyMs);
        System.out.printf("Time penalty percentage: %.2f%%%n",
                penaltyPercentage);

        System.out.println();
        System.out.println("Benchmark completed.");
    }
}

