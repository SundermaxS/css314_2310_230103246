package Tasks230103246;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Task5 {

    private static final long STUDENT_ID = 230103246L;
    private static final int[] SIZES = {
            100, 1_000, 10_000, 100_000,
            1_000_000, 10_000_000
    };

    private static final int REPEATS = 5;
    private static final double A = 2.5;
    private static final double B = 1.5;

    private static volatile double checksum;

    public static void main(String[] args) throws Exception {
        int cores = Runtime.getRuntime().availableProcessors();

        System.out.println("Student ID: " + STUDENT_ID);
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Architecture: " + System.getProperty("os.arch"));
        System.out.println("Available logical cores: " + cores);
        System.out.println("Parallel workers: " + cores);
        System.out.println("Operation: y[i] = a * x[i] + b");
        System.out.println("Timed repetitions: " + REPEATS);
        System.out.println();

        List<Result> results = new ArrayList<>();

        ExecutorService executor =
                Executors.newFixedThreadPool(cores);

        try {
            for (int n : SIZES) {
                double[] x = new double[n];
                double[] y = new double[n];

                for (int i = 0; i < n; i++) {
                    x[i] = (i % 1000) * 0.1;
                }

                // Warm up both implementations.
                serial(x, y);
                parallel(x, y, executor, cores);

                double serialMs = medianTime(
                        () -> serial(x, y)
                );

                double parallelMs = medianTime(
                        () -> parallel(x, y, executor, cores)
                );

                double speedup = serialMs / parallelMs;

                String winner;
                if (parallelMs < serialMs) {
                    winner = "PARALLEL FASTER";
                } else if (parallelMs > serialMs) {
                    winner = "SERIAL FASTER";
                } else {
                    winner = "EQUAL";
                }

                results.add(new Result(
                        n, serialMs, parallelMs, speedup, winner
                ));

                System.out.printf(
                        "%-12d | %-15.4f | %-15.4f | %-10.3f | %s%n",
                        n, serialMs, parallelMs, speedup, winner
                );
            }
        } finally {
            executor.shutdown();
        }

        System.out.println();
        System.out.println("CROSSOVER ANALYSIS");

        boolean found = false;

        for (int i = 1; i < results.size(); i++) {
            Result previous = results.get(i - 1);
            Result current = results.get(i);

            if (previous.parallelMs >= previous.serialMs
                    && current.parallelMs < current.serialMs) {
                System.out.printf(
                        "Crossover range: N > %d and N <= %d%n",
                        previous.n, current.n
                );
                System.out.printf(
                        "At N=%d: serial=%.4f ms, parallel=%.4f ms%n",
                        previous.n, previous.serialMs, previous.parallelMs
                );
                System.out.printf(
                        "At N=%d: serial=%.4f ms, parallel=%.4f ms%n",
                        current.n, current.serialMs, current.parallelMs
                );
                found = true;
            }
        }

        if (!found) {
            boolean anyParallelFaster = results.stream()
                    .anyMatch(r -> r.parallelMs < r.serialMs);
            boolean anySerialFaster = results.stream()
                    .anyMatch(r -> r.parallelMs >= r.serialMs);

            if (anyParallelFaster && anySerialFaster) {
                System.out.println(
                        "No adjacent crossover detected. Results switch "
                                + "between tested sizes, but not in a single "
                                + "slower-to-faster adjacent pair."
                );
            } else if (anyParallelFaster) {
                System.out.println(
                        "Parallel was faster at all tested sizes; "
                                + "crossover is at or below the smallest tested N."
                );
            } else {
                System.out.println(
                        "No crossover found: parallel was not faster "
                                + "at any tested size."
                );
            }
        }

        System.out.printf("Final checksum: %.6f%n", checksum);
    }

    private static void serial(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) {
            y[i] = A * x[i] + B;
        }

        checksum = y[y.length - 1];
    }

    private static void parallel(
            double[] x,
            double[] y,
            ExecutorService executor,
            int workers
    ) throws Exception {

        int chunkSize = (x.length + workers - 1) / workers;
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int worker = 0; worker < workers; worker++) {
            int start = worker * chunkSize;
            int end = Math.min(start + chunkSize, x.length);

            if (start >= x.length) {
                break;
            }

            tasks.add(() -> {
                for (int i = start; i < end; i++) {
                    y[i] = A * x[i] + B;
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);

        for (Future<Void> future : futures) {
            future.get();
        }

        checksum = y[y.length - 1];
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

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static class Result {
        final int n;
        final double serialMs;
        final double parallelMs;
        final double speedup;
        final String winner;

        Result(
                int n,
                double serialMs,
                double parallelMs,
                double speedup,
                String winner
        ) {
            this.n = n;
            this.serialMs = serialMs;
            this.parallelMs = parallelMs;
            this.speedup = speedup;
            this.winner = winner;
        }
    }
}

