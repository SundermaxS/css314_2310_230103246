package TasksPractice;


import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Task2 {

    private static final int STUDENT_ID = 230103246;
    private static final int THREADS = 4;
    private static final long TOTAL_SAMPLES = 40_000_000L;
    private static final long SAMPLES_PER_THREAD =
            TOTAL_SAMPLES / THREADS;

    private static final Random SHARED_RANDOM =
            new Random(STUDENT_ID);

    private static synchronized double nextSharedDouble() {
        return SHARED_RANDOM.nextDouble();
    }

    private static class Result {
        final long inside;
        final long total;

        Result(long inside, long total) {
            this.inside = inside;
            this.total = total;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("TASK 2: PRNG State Bottleneck");
        System.out.println("Student ID: " + STUDENT_ID);
        System.out.println("Threads: " + THREADS);
        System.out.println("Total samples: " + TOTAL_SAMPLES);
        System.out.println();

        ExecutorService executor =
                Executors.newFixedThreadPool(THREADS);

        try {
            // Shared synchronized PRNG.
            long sharedStart = System.nanoTime();

            Future<Result>[] sharedFutures = new Future[THREADS];

            for (int t = 0; t < THREADS; t++) {
                sharedFutures[t] = executor.submit(() -> {
                    long inside = 0;

                    for (long i = 0; i < SAMPLES_PER_THREAD; i++) {
                        double x = nextSharedDouble();
                        double y = nextSharedDouble();

                        if (x * x + y * y <= 1.0) {
                            inside++;
                        }
                    }

                    return new Result(inside, SAMPLES_PER_THREAD);
                });
            }

            long sharedInside = 0;

            for (Future<Result> future : sharedFutures) {
                sharedInside += future.get().inside;
            }

            long sharedTime = System.nanoTime() - sharedStart;
            double sharedPi =
                    4.0 * sharedInside / TOTAL_SAMPLES;
            double sharedError = Math.abs(sharedPi - Math.PI);

            // Independent PRNG per thread.
            long independentStart = System.nanoTime();

            Future<Result>[] independentFutures =
                    new Future[THREADS];

            for (int t = 0; t < THREADS; t++) {
                final int threadIndex = t;

                independentFutures[t] = executor.submit(() -> {
                    long seed =
                            (long) STUDENT_ID * 10 + threadIndex;
                    Random random = new Random(seed);
                    long inside = 0;

                    for (long i = 0; i < SAMPLES_PER_THREAD; i++) {
                        double x = random.nextDouble();
                        double y = random.nextDouble();

                        if (x * x + y * y <= 1.0) {
                            inside++;
                        }
                    }

                    return new Result(inside, SAMPLES_PER_THREAD);
                });
            }

            long independentInside = 0;

            for (Future<Result> future : independentFutures) {
                independentInside += future.get().inside;
            }

            long independentTime =
                    System.nanoTime() - independentStart;

            double independentPi =
                    4.0 * independentInside / TOTAL_SAMPLES;
            double independentError =
                    Math.abs(independentPi - Math.PI);

            double sharedMs = sharedTime / 1_000_000.0;
            double independentMs =
                    independentTime / 1_000_000.0;
            double speedup = (double) sharedTime / independentTime;

            System.out.printf(
                    "Shared PRNG:       Pi = %.10f, Error = %.10f, Time = %.3f ms%n",
                    sharedPi, sharedError, sharedMs
            );

            System.out.printf(
                    "Independent PRNG:  Pi = %.10f, Error = %.10f, Time = %.3f ms%n",
                    independentPi, independentError, independentMs
            );

            System.out.printf(
                    "Speedup: %.3fx%n", speedup
            );

        } finally {
            executor.shutdown();
        }
    }
}

