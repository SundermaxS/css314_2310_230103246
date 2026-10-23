package Tasks230103246;


import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Task2 {

    private static final int SIZE = 20_000_000;
    private static final long STUDENT_ID = 230103246L;
    private static final double P = 0.95;

    public static void main(String[] args) throws Exception {
        int maxCores = Runtime.getRuntime().availableProcessors();

        System.out.println("Student ID: " + STUDENT_ID);
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Architecture: " + System.getProperty("os.arch"));
        System.out.println("Logical cores: " + maxCores);
        System.out.println("Array size: " + SIZE);
        System.out.println();

        double[] data = new double[SIZE];
        Random random = new Random(STUDENT_ID);

        for (int i = 0; i < SIZE; i++) {
            data[i] = random.nextDouble();
        }

        Set<Integer> workerCounts = new LinkedHashSet<>();
        workerCounts.add(1);
        workerCounts.add(2);
        workerCounts.add(4);
        workerCounts.add(maxCores);
        workerCounts.add(maxCores * 2);

        List<Result> results = new ArrayList<>();

        for (int workers : workerCounts) {
            ExecutorService executor =
                    Executors.newFixedThreadPool(workers);

            try {
                calculateSum(data, workers, executor);

                long start = System.nanoTime();
                double sum = calculateSum(data, workers, executor);
                long end = System.nanoTime();

                double wallTimeMs = (end - start) / 1_000_000.0;

                results.add(new Result(
                        workers,
                        wallTimeMs,
                        sum
                ));
            } finally {
                executor.shutdown();
            }
        }

        double baselineTime = results.stream()
                .filter(result -> result.workers == 1)
                .findFirst()
                .orElseThrow()
                .wallTimeMs;

        System.out.printf(
                "%-12s | %-15s | %-12s | %-20s%n",
                "Cores",
                "Wall Time (ms)",
                "Speedup",
                "Amdahl Upper Bound"
        );

        System.out.println(
                "--------------------------------------------------------------------------"
        );

        for (Result result : results) {
            double speedup = baselineTime / result.wallTimeMs;
            double amdahlBound =
                    1.0 / ((1.0 - P) + P / result.workers);

            System.out.printf(
                    "%-12d | %-15.3f | %-12.3f | %-20.3f%n",
                    result.workers,
                    result.wallTimeMs,
                    speedup,
                    amdahlBound
            );
        }

        System.out.println();
        System.out.println("Parallelizable fraction P: " + P);
        System.out.println("Serial fraction: " + (1.0 - P));
        System.out.println("Benchmark completed.");
    }

    private static double calculateSum(
            double[] data,
            int workers,
            ExecutorService executor
    ) throws Exception {

        List<Callable<Double>> tasks = new ArrayList<>();

        int chunkSize = (data.length + workers - 1) / workers;

        for (int worker = 0; worker < workers; worker++) {
            int start = worker * chunkSize;
            int end = Math.min(start + chunkSize, data.length);

            if (start >= data.length) {
                break;
            }

            tasks.add(() -> {
                double partialSum = 0.0;

                for (int i = start; i < end; i++) {
                    partialSum += data[i];
                }

                return partialSum;
            });
        }

        List<Future<Double>> futures = executor.invokeAll(tasks);

        double totalSum = 0.0;

        for (Future<Double> future : futures) {
            totalSum += future.get();
        }

        return totalSum;
    }

    private static class Result {
        final int workers;
        final double wallTimeMs;
        final double sum;

        Result(int workers, double wallTimeMs, double sum) {
            this.workers = workers;
            this.wallTimeMs = wallTimeMs;
            this.sum = sum;
        }
    }
}

