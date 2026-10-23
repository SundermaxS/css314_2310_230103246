package Tasks230103246;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Task8 {

    private static final int N = 1024;
    private static final int STUDENT_ID = 230103246;
    private static final int REPEATS = 3;

    private static volatile double checksumSink;

    public static void main(String[] args) throws Exception {
        int cores = Runtime.getRuntime().availableProcessors();

        double[] a = new double[N * N];
        double[] b = new double[N * N];
        double[] c = new double[N * N];

        Random random = new Random(STUDENT_ID);

        for (int i = 0; i < N * N; i++) {
            a[i] = random.nextDouble();
            b[i] = random.nextDouble();
        }

        ExecutorService pool = Executors.newFixedThreadPool(cores);

        System.out.println("TASK 8: Matrix Multiplication");
        System.out.println("Student ID: " + STUDENT_ID);
        System.out.println("Matrix size: " + N + " x " + N);
        System.out.println("CPU threads: " + cores);
        System.out.println();

        try {
            // Warm-up runs for the JVM.
            multiply(a, b, c, pool, cores, 0);
            multiply(a, b, c, pool, cores, 16);
            multiply(a, b, c, pool, cores, 64);

            double nonTiledMs =
                    measure(a, b, c, pool, cores, 0);

            double tiled16Ms =
                    measure(a, b, c, pool, cores, 16);

            double tiled64Ms =
                    measure(a, b, c, pool, cores, 64);

            System.out.printf(
                    "%-18s %15s %15s%n",
                    "Method", "Runtime (ms)", "Speedup"
            );

            System.out.printf(
                    "%-18s %15.3f %15.3f%n",
                    "Non-tiled", nonTiledMs, 1.0
            );

            System.out.printf(
                    "%-18s %15.3f %15.3f%n",
                    "Tiled 16x16",
                    tiled16Ms,
                    nonTiledMs / tiled16Ms
            );

            System.out.printf(
                    "%-18s %15.3f %15.3f%n",
                    "Tiled 64x64",
                    tiled64Ms,
                    nonTiledMs / tiled64Ms
            );

            System.out.println();
            System.out.printf(
                    "Tiled 16x16 vs non-tiled: %.2f%% %s%n",
                    Math.abs(nonTiledMs - tiled16Ms)
                            / nonTiledMs * 100.0,
                    tiled16Ms < nonTiledMs
                            ? "faster" : "slower"
            );

            System.out.printf(
                    "Tiled 64x64 vs non-tiled: %.2f%% %s%n",
                    Math.abs(nonTiledMs - tiled64Ms)
                            / nonTiledMs * 100.0,
                    tiled64Ms < nonTiledMs
                            ? "faster" : "slower"
            );

            System.out.println();
            System.out.println(
                    "Note: Cache misses were not directly measured."
            );
            System.out.println(
                    "Runtime differences are used as a performance proxy."
            );

        } finally {
            pool.shutdown();
        }
    }

    private static double measure(
            double[] a,
            double[] b,
            double[] c,
            ExecutorService pool,
            int cores,
            int tileSize
    ) throws Exception {

        long[] times = new long[REPEATS];

        for (int r = 0; r < REPEATS; r++) {
            java.util.Arrays.fill(c, 0.0);

            long start = System.nanoTime();

            multiply(a, b, c, pool, cores, tileSize);

            times[r] = System.nanoTime() - start;

            // Consume a result to prevent dead-code elimination.
            checksumSink = c[N * N - 1];
        }

        java.util.Arrays.sort(times);

        return times[REPEATS / 2] / 1_000_000.0;
    }

    private static void multiply(
            double[] a,
            double[] b,
            double[] c,
            ExecutorService pool,
            int cores,
            int tileSize
    ) throws Exception {

        List<Callable<Void>> tasks = new ArrayList<>();

        for (int worker = 0; worker < cores; worker++) {
            final int startRow = N * worker / cores;
            final int endRow = N * (worker + 1) / cores;

            tasks.add(() -> {
                if (tileSize == 0) {
                    // Non-tiled multiplication.
                    for (int i = startRow; i < endRow; i++) {
                        int rowA = i * N;

                        for (int k = 0; k < N; k++) {
                            double value = a[rowA + k];
                            int rowB = k * N;

                            for (int j = 0; j < N; j++) {
                                c[rowA + j] +=
                                        value * b[rowB + j];
                            }
                        }
                    }
                } else {
                    // Tiled multiplication.
                    for (int i0 = startRow;
                         i0 < endRow;
                         i0 += tileSize) {

                        int iMax = Math.min(
                                i0 + tileSize, endRow
                        );

                        for (int k0 = 0; k0 < N;
                             k0 += tileSize) {

                            int kMax = Math.min(
                                    k0 + tileSize, N
                            );

                            for (int j0 = 0; j0 < N;
                                 j0 += tileSize) {

                                int jMax = Math.min(
                                        j0 + tileSize, N
                                );

                                for (int i = i0; i < iMax; i++) {
                                    int rowA = i * N;

                                    for (int k = k0;
                                         k < kMax; k++) {

                                        double value =
                                                a[rowA + k];
                                        int rowB = k * N;

                                        for (int j = j0;
                                             j < jMax; j++) {

                                            c[rowA + j] +=
                                                    value * b[rowB + j];
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                return null;
            });
        }

        List<Future<Void>> futures = pool.invokeAll(tasks);

        for (Future<Void> future : futures) {
            future.get();
        }
    }
}

