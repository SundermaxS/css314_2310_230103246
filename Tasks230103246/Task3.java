package Tasks230103246;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;

public class Task3 {

    private static final long ITERATIONS = 50_000_000L;
    private static final long STUDENT_ID = 230103246L;

    public static void main(String[] args) throws InterruptedException {
        int workers = getPhysicalCoreCount();

        System.out.println("Student ID: " + STUDENT_ID);
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Architecture: " + System.getProperty("os.arch"));
        System.out.println("Physical cores / workers: " + workers);
        System.out.println("Increments per thread: " + ITERATIONS);
        System.out.println();

        long[] sharedCounts = new long[workers];

        long startUnpadded = System.nanoTime();

        runUnpadded(sharedCounts, workers);

        long endUnpadded = System.nanoTime();

        double unpaddedMs =
                (endUnpadded - startUnpadded) / 1_000_000.0;

        long unpaddedTotal = 0;
        for (long count : sharedCounts) {
            unpaddedTotal += count;
        }

        long[] localCounts = new long[workers];

        long startLocal = System.nanoTime();

        runLocal(localCounts, workers);

        long endLocal = System.nanoTime();

        double localMs = (endLocal - startLocal) / 1_000_000.0;

        long localTotal = 0;
        for (long count : localCounts) {
            localTotal += count;
        }

        System.out.printf(
                "Unpadded Shared: %.3f ms%n",
                unpaddedMs
        );

        System.out.printf(
                "Padded/Local:    %.3f ms%n",
                localMs
        );

        System.out.printf(
                "Execution ratio (Unpadded / Local): %.3fx%n",
                unpaddedMs / localMs
        );

        System.out.println("Unpadded total: " + unpaddedTotal);
        System.out.println("Local total: " + localTotal);
        System.out.println("Expected total: "
                + (ITERATIONS * workers));
    }

    private static void runUnpadded(
            long[] sharedCounts,
            int workers
    ) throws InterruptedException {

        Thread[] threads = new Thread[workers];
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < workers; i++) {
            final int index = i;

            threads[i] = new Thread(() -> {
                ready.countDown();

                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                for (long j = 0; j < ITERATIONS; j++) {
                    sharedCounts[index]++;
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        ready.await();
        start.countDown();

        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static void runLocal(
            long[] localCounts,
            int workers
    ) throws InterruptedException {

        Thread[] threads = new Thread[workers];
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < workers; i++) {
            final int index = i;

            threads[i] = new Thread(() -> {
                ready.countDown();

                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                long localCount = 0;

                for (long j = 0; j < ITERATIONS; j++) {
                    localCount++;
                }

                localCounts[index] = localCount;
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        ready.await();
        start.countDown();

        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static int getPhysicalCoreCount() {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            try {
                Process process = new ProcessBuilder(
                        "sysctl", "-n", "hw.physicalcpu"
                ).start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String result = reader.readLine();
                    process.waitFor();

                    if (result != null) {
                        int cores = Integer.parseInt(result.trim());
                        if (cores > 0) {
                            return cores;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return Runtime.getRuntime().availableProcessors();
    }
}

