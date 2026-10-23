package Tasks230103246;

import java.lang.management.ManagementFactory;
import java.util.Random;


public class Task1 {

    private static final int SIZE = 20_000_000;
    private static final long STUDENT_ID = 230103246L;

    public static void main(String[] args) {

        System.out.println("Student ID: " + STUDENT_ID);
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("OS Architecture: " + System.getProperty("os.arch"));
        System.out.println("CPU: " + getCpuModel());
        System.out.println("Logical cores: "
                + Runtime.getRuntime().availableProcessors());
        System.out.println("Array size: " + SIZE);

        long startTime = System.nanoTime();

        double[] data = new double[SIZE];
        Random random = new Random(STUDENT_ID);

        for (int i = 0; i < SIZE; i++) {
            data[i] = random.nextDouble();
        }

        double sum = 0.0;

        for (int i = 0; i < SIZE; i++) {
            sum += data[i];
        }

        double mean = sum / SIZE;
        double squaredDifferences = 0.0;

        for (int i = 0; i < SIZE; i++) {
            double difference = data[i] - mean;
            squaredDifferences += difference * difference;
        }

        double standardDeviation = Math.sqrt(squaredDifferences / SIZE);

        long endTime = System.nanoTime();
        double executionTimeMs = (endTime - startTime) / 1_000_000.0;

        System.out.println("Sum: " + sum);
        System.out.println("Mean: " + mean);
        System.out.println("Population standard deviation: "
                + standardDeviation);
        System.out.printf("Execution time: %.3f ms%n", executionTimeMs);
    }

    private static String getCpuModel() {
        try {
            Process process = new ProcessBuilder(
                    "sysctl", "-n", "machdep.cpu.brand_string"
            ).start();

            String result = new String(
                    process.getInputStream().readAllBytes()
            ).trim();

            process.waitFor();

            if (!result.isEmpty()) {
                return result;
            }
        } catch (Exception ignored) {
        }

        return ManagementFactory.getOperatingSystemMXBean().getName();
    }
}
