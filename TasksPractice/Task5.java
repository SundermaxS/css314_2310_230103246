package TasksPractice;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Task5 {

    private static final int TASKS = 10_000;
    private static final int HASH_ITERATIONS = 500;
    private static final int STUDENT_ID = 230103246;
    private static volatile long resultSink;

    public static void main(String[] args) throws Exception {
        int cores = Runtime.getRuntime().availableProcessors();

        Set<Integer> poolSizes = new LinkedHashSet<>();
        poolSizes.add(1);
        poolSizes.add(Math.max(1, cores / 2));
        poolSizes.add(cores);
        poolSizes.add(cores * 2);
        poolSizes.add(cores * 4);
        poolSizes.add(cores * 16);
        poolSizes.add(256);

        int[] sizes = poolSizes.stream()
                .mapToInt(Integer::intValue)
                .toArray();

        long[] times = new long[sizes.length];

        System.out.println("TASK 5: Thread Oversubscription Threshold");
        System.out.println("Student ID: " + STUDENT_ID);
        System.out.println("Available logical processors: " + cores);
        System.out.println("CPU-bound tasks: " + TASKS);
        System.out.println("Hashes per task: " + HASH_ITERATIONS);
        System.out.println();

        // Warm up the JVM before measuring.
        runBatch(1);

        for (int i = 0; i < sizes.length; i++) {
            // Each measurement executes the same batch.
            times[i] = measure(sizes[i]);
        }

        double baselineMs = times[0] / 1_000_000.0;
        int firstWorseningIndex = -1;

        System.out.printf(
                "%-12s %-22s %-30s %s%n",
                "Pool Size", "Execution Time (ms)",
                "Speedup relative to 1 thread", "Status"
        );

        for (int i = 0; i < sizes.length; i++) {
            double timeMs = times[i] / 1_000_000.0;
            double speedup = (double) times[0] / times[i];

            String status = "OK";

            if (i > 0 && times[i] > times[i - 1]
                    && firstWorseningIndex == -1) {
                firstWorseningIndex = i;
                status = "FIRST WORSENING";
            } else if (i > 0 && times[i] > times[i - 1]) {
                status = "SLOWER";
            }

            System.out.printf(
                    "%-12d %-22.3f %-30.3f %s%n",
                    sizes[i], timeMs, speedup, status
            );
        }

        System.out.println();

        if (firstWorseningIndex >= 0) {
            System.out.printf(
                    "First observed worsening: pool size %d threads "
                            + "(%.3f ms -> %.3f ms).%n",
                    sizes[firstWorseningIndex],
                    times[firstWorseningIndex - 1] / 1_000_000.0,
                    times[firstWorseningIndex] / 1_000_000.0
            );
            System.out.println(
                    "This is an observed timing increase, not proof "
                            + "of context switching as the sole cause."
            );
        } else {
            System.out.println(
                    "No worsening was observed between adjacent "
                            + "tested pool sizes."
            );
        }
    }

    private static long measure(int poolSize) throws Exception {
        long start = System.nanoTime();
        long checksum = runBatch(poolSize);
        long elapsed = System.nanoTime() - start;

        resultSink = checksum;
        return elapsed;
    }

    private static long runBatch(int poolSize) throws Exception {
        ExecutorService executor =
                Executors.newFixedThreadPool(poolSize);

        try {
            List<Callable<Long>> tasks = new ArrayList<>(TASKS);

            for (int i = 0; i < TASKS; i++) {
                final int taskId = i;

                tasks.add(() -> {
                    MessageDigest digest =
                            MessageDigest.getInstance("SHA-256");

                    byte[] data = new byte[1024];
                    Arrays.fill(data, (byte) (taskId + STUDENT_ID));

                    byte[] hash = data;

                    for (int j = 0; j < HASH_ITERATIONS; j++) {
                        hash = digest.digest(hash);
                    }

                    long value = 0;
                    for (byte b : hash) {
                        value = value * 31 + (b & 0xff);
                    }

                    return value;
                });
            }

            List<Future<Long>> futures = executor.invokeAll(tasks);
            long checksum = 0;

            for (Future<Long> future : futures) {
                checksum += future.get();
            }

            return checksum;
        } finally {
            executor.shutdown();
        }
    }
}

