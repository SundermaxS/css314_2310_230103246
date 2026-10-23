package TasksPractice;


import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Task3 {

    private static final int STUDENT_ID = 230103246;
    private static final int SIZE = 2_000_000;

    public static void main(String[] args) throws Exception {
        int[] input = new int[SIZE];
        Random random = new Random(STUDENT_ID);

        for (int i = 0; i < SIZE; i++) {
            input[i] = random.nextInt(10) + 1;
        }

        int[] serialOutput = new int[SIZE];
        int[] parallelOutput = new int[SIZE];

        int workers = Runtime.getRuntime().availableProcessors();
        workers = Math.max(1, Math.min(workers, SIZE));

        System.out.println("TASK 3: Parallel Prefix Scan");
        System.out.println("Student ID: " + STUDENT_ID);
        System.out.println("Array size: " + SIZE);
        System.out.println("Workers: " + workers);
        System.out.println();

        // Serial inclusive prefix sum.
        long serialStart = System.nanoTime();

        serialOutput[0] = input[0];
        for (int i = 1; i < SIZE; i++) {
            serialOutput[i] = serialOutput[i - 1] + input[i];
        }

        long serialTime = System.nanoTime() - serialStart;

        ExecutorService executor =
                Executors.newFixedThreadPool(workers);

        long parallelTime;

        try {
            // Warm-up parallel implementation.
            parallelScan(input, parallelOutput, executor, workers);

            long parallelStart = System.nanoTime();

            parallelScan(input, parallelOutput, executor, workers);

            parallelTime = System.nanoTime() - parallelStart;
        } finally {
            executor.shutdown();
        }

        double serialMs = serialTime / 1_000_000.0;
        double parallelMs = parallelTime / 1_000_000.0;
        double speedup = (double) serialTime / parallelTime;

        boolean verified =
                parallelOutput[SIZE - 1] == serialOutput[SIZE - 1];

        System.out.printf("Serial Time (ms): %.3f%n", serialMs);
        System.out.printf("Parallel Time (ms): %.3f%n", parallelMs);
        System.out.printf("Speedup: %.3fx%n", speedup);
        System.out.println(
                "Verification Status: " + (verified ? "PASS" : "FAIL")
        );

        System.out.println(
                "Serial final sum: " + serialOutput[SIZE - 1]
        );
        System.out.println(
                "Parallel final sum: " + parallelOutput[SIZE - 1]
        );

        if (!verified) {
            System.exit(1);
        }
    }

    private static void parallelScan(
            int[] input,
            int[] output,
            ExecutorService executor,
            int workers
    ) throws Exception {

        int chunkSize = (input.length + workers - 1) / workers;
        int[] chunkSums = new int[workers];
        int[] offsets = new int[workers];

        // Pass 1: calculate local inclusive scans and chunk sums.
        Future<?>[] futures = new Future<?>[workers];

        for (int w = 0; w < workers; w++) {
            final int worker = w;
            final int start = worker * chunkSize;
            final int end = Math.min(start + chunkSize, input.length);

            futures[w] = executor.submit(() -> {
                if (start >= end) {
                    chunkSums[worker] = 0;
                    return;
                }

                int sum = 0;

                for (int i = start; i < end; i++) {
                    sum += input[i];
                    output[i] = sum;
                }

                chunkSums[worker] = sum;
            });
        }

        for (Future<?> future : futures) {
            future.get();
        }

        // Pass 2a: calculate each chunk's starting offset.
        int runningTotal = 0;

        for (int w = 0; w < workers; w++) {
            offsets[w] = runningTotal;
            runningTotal += chunkSums[w];
        }

        // Pass 2b: apply offsets to each chunk in parallel.
        for (int w = 0; w < workers; w++) {
            final int worker = w;
            final int start = worker * chunkSize;
            final int end = Math.min(start + chunkSize, input.length);
            final int offset = offsets[worker];

            futures[w] = executor.submit(() -> {
                if (offset == 0) {
                    return;
                }

                for (int i = start; i < end; i++) {
                    output[i] += offset;
                }
            });
        }

        for (Future<?> future : futures) {
            future.get();
        }
    }
}

