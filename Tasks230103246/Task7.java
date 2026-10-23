package Tasks230103246;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Task7 {

    private static final long STUDENT_ID = 230103246L;
    private static final int SIZE = 30_000_000;
    private static final int REPEATS = 3;

    private static volatile double[] outputReference;

    public static void main(String[] args) throws Exception {
        int maxCores = Runtime.getRuntime().availableProcessors();

        int[] requested = {1, 2, maxCores};
        int[] workers = Arrays.stream(requested).distinct().toArray();

        System.out.println("Student ID: " + STUDENT_ID);
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Architecture: " + System.getProperty("os.arch"));
        System.out.println("Logical cores: " + maxCores);
        System.out.println("Array size: " + SIZE);
        System.out.println("Data type: double (float64)");
        System.out.println();

        double[] a = new double[SIZE];
        double[] b = new double[SIZE];

        for (int i = 0; i < SIZE; i++) {
            a[i] = (i % 1000) * 0.001;
        }

        ExecutorService executor =
                Executors.newFixedThreadPool(maxCores);

        try {
            System.out.println("=== Kernel A: Memory-Bound (Copy) ===");
            System.out.printf(
                    "%-10s | %-15s | %-12s | %-12s%n",
                    "Cores", "Time (ms)", "Speedup", "Efficiency"
            );

            double copyBaseline = 0.0;

            for (int coreCount : workers) {
                copyKernel(a, b, coreCount, executor);

                double time = medianTime(() ->
                        copyKernel(a, b, coreCount, executor));

                if (coreCount == 1) {
                    copyBaseline = time;
                }

                double speedup = copyBaseline / time;
                double efficiency = speedup / coreCount;

                System.out.printf(
                        "%-10d | %-15.3f | %-12.3f | %-12.3f%n",
                        coreCount, time, speedup, efficiency
                );
            }

            System.out.println();
            System.out.println("=== Kernel B: Compute-Bound (Trigonometry) ===");
            System.out.printf(
                    "%-10s | %-15s | %-12s | %-12s%n",
                    "Cores", "Time (ms)", "Speedup", "Efficiency"
            );

            double computeBaseline = 0.0;

            for (int coreCount : workers) {
                computeKernel(a, b, coreCount, executor);

                double time = medianTime(() ->
                        computeKernel(a, b, coreCount, executor));

                if (coreCount == 1) {
                    computeBaseline = time;
                }

                double speedup = computeBaseline / time;
                double efficiency = speedup / coreCount;

                System.out.printf(
                        "%-10d | %-15.3f | %-12.3f | %-12.3f%n",
                        coreCount, time, speedup, efficiency
                );
            }

            System.out.println();
            System.out.println("=== Scaling Analysis ===");

            double copyMulti = medianTime(() ->
                    copyKernel(a, b, maxCores, executor));
            double copySpeedup = copyBaseline / copyMulti;

            double computeMulti = medianTime(() ->
                    computeKernel(a, b, maxCores, executor));
            double computeSpeedup = computeBaseline / computeMulti;

            System.out.printf(
                    "Copy speedup at %d cores: %.3fx%n",
                    maxCores, copySpeedup
            );
            System.out.printf(
                    "Compute speedup at %d cores: %.3fx%n",
                    maxCores, computeSpeedup
            );

            if (copySpeedup < computeSpeedup) {
                System.out.println(
                        "Conclusion: Kernel A (copy) scaled worse than "
                                + "Kernel B. This is consistent with a possible "
                                + "memory-bandwidth bottleneck, but does not "
                                + "prove RAM channel saturation."
                );
            } else {
                System.out.println(
                        "Conclusion: Kernel A did not scale worse than "
                                + "Kernel B in this run. RAM channel saturation "
                                + "was not established by this benchmark."
                );
            }

            System.out.printf(
                    "Output checksum: %.6f%n",
                    b[SIZE - 1]
            );

        } finally {
            executor.shutdown();
        }
    }

    private static void copyKernel(
            double[] a,
            double[] b,
            int workers,
            ExecutorService executor
    ) throws Exception {
        runParallel(a.length, workers, executor, (start, end) -> {
            for (int i = start; i < end; i++) {
                b[i] = a[i];
            }
        });

        outputReference = b;
    }

    private static void computeKernel(
            double[] a,
            double[] b,
            int workers,
            ExecutorService executor
    ) throws Exception {
        runParallel(a.length, workers, executor, (start, end) -> {
            for (int i = start; i < end; i++) {
                b[i] = Math.sin(a[i]) * Math.cos(a[i])
                        + Math.tan(a[i]);
            }
        });

        outputReference = b;
    }

    private static void runParallel(
            int size,
            int workers,
            ExecutorService executor,
            RangeOperation operation
    ) throws Exception {
        int chunkSize = (size + workers - 1) / workers;
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int worker = 0; worker < workers; worker++) {
            int start = worker * chunkSize;
            int end = Math.min(start + chunkSize, size);

            if (start >= size) {
                break;
            }

            tasks.add(() -> {
                operation.run(start, end);
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);

        for (Future<Void> future : futures) {
            future.get();
        }
    }

    private static double medianTime(
            ThrowingRunnable operation
    ) throws Exception {
        double[] times = new double[REPEATS];

        for (int i = 0; i < REPEATS; i++) {
            long start = System.nanoTime();
            operation.run();
            long end = System.nanoTime();

            times[i] = (end - start) / 1_000_000.0;
        }

        Arrays.sort(times);
        return times[REPEATS / 2];
    }

    private interface RangeOperation {
        void run(int start, int end);
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

