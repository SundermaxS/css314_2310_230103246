package TasksPractice;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Task1 {

    private static final int STUDENT_ID = 230103246;
    private static final int SIZE = 8000;

    private static volatile double resultSink;

    public static void main(String[] args) throws Exception {
        int logicalCores =
                Runtime.getRuntime().availableProcessors();

        int physicalCores = getPhysicalCoreCount();
        int workers = Math.max(1, physicalCores);

        double[][] matrix = new double[SIZE][SIZE];
        Random random = new Random(STUDENT_ID);

        System.out.println("Initializing 8000 x 8000 matrix...");

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                matrix[i][j] = random.nextDouble();
            }
        }

        ExecutorService executor =
                Executors.newFixedThreadPool(workers);

        try {
            System.out.println("Student ID: " + STUDENT_ID);
            System.out.println("Logical cores: " + logicalCores);
            System.out.println("Physical cores: " + physicalCores);
            System.out.println("Workers: " + workers);
            System.out.println("Matrix size: " + SIZE + " x " + SIZE);
            System.out.println();

            // Warm-up both traversal modes.
            resultSink = parallelSum(
                    matrix, executor, workers, true
            );
            resultSink = parallelSum(
                    matrix, executor, workers, false
            );

            long startRow = System.nanoTime();
            double rowSum = parallelSum(
                    matrix, executor, workers, true
            );
            long rowTime = System.nanoTime() - startRow;

            long startColumn = System.nanoTime();
            double columnSum = parallelSum(
                    matrix, executor, workers, false
            );
            long columnTime = System.nanoTime() - startColumn;

            resultSink = rowSum + columnSum;

            double rowMs = rowTime / 1_000_000.0;
            double columnMs = columnTime / 1_000_000.0;
            double penalty = columnMs / rowMs;

            System.out.printf(
                    "Mode A (Row-Major): %.3f ms%n", rowMs
            );
            System.out.printf(
                    "Mode B (Column-Major / Strided): %.3f ms%n",
                    columnMs
            );
            System.out.printf(
                    "Cache penalty factor: %.3fx%n", penalty
            );
            System.out.printf("Row-Major sum: %.6f%n", rowSum);
            System.out.printf("Column-Major sum: %.6f%n", columnSum);

        } finally {
            executor.shutdown();
        }
    }

    private static double parallelSum(
            double[][] matrix,
            ExecutorService executor,
            int workers,
            boolean rowMajor
    ) throws Exception {

        List<Callable<Double>> tasks = new ArrayList<>();

        for (int worker = 0; worker < workers; worker++) {
            final int start = SIZE * worker / workers;
            final int end = SIZE * (worker + 1) / workers;

            tasks.add(() -> {
                double sum = 0.0;

                if (rowMajor) {
                    // Each worker processes a range of rows.
                    for (int i = start; i < end; i++) {
                        for (int j = 0; j < SIZE; j++) {
                            sum += matrix[i][j];
                        }
                    }
                } else {
                    // Each worker processes a range of columns.
                    for (int i = 0; i < SIZE; i++) {
                        for (int j = start; j < end; j++) {
                            sum += matrix[i][j];
                        }
                    }
                }

                return sum;
            });
        }

        List<Future<Double>> futures = executor.invokeAll(tasks);
        double total = 0.0;

        for (Future<Double> future : futures) {
            total += future.get();
        }

        return total;
    }

    private static int getPhysicalCoreCount() {
        try {
            Process process = new ProcessBuilder(
                    "sysctl", "-n", "hw.physicalcpu"
            ).redirectErrorStream(true).start();

            byte[] output = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return Math.max(
                        1,
                        Integer.parseInt(
                                new String(output).trim()
                        )
                );
            }
        } catch (Exception ignored) {
            // Fall back to the JVM's available processor count.
        }

        return Runtime.getRuntime().availableProcessors();
    }
}

