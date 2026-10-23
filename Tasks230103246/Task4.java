package Tasks230103246;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class Task4 {

    private static final int ITEMS = 1000;
    private static final int WORKERS = 4;
    private static final long DELAY_PER_UNIT_NS = 100_000L;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Items: " + ITEMS);
        System.out.println("Workers: " + WORKERS);
        System.out.println("Dynamic chunk size: 1");
        System.out.println();

        Result staticResult = runStatic();
        Result dynamicResult = runDynamic();

        printResult("Strategy A: Static chunks", staticResult);
        printResult("Strategy B: Dynamic scheduling", dynamicResult);
    }

    private static void simulateWork(int item) {
        long delay = DELAY_PER_UNIT_NS * (item % 20);

        if (delay > 0) {
            LockSupport.parkNanos(delay);
        }
    }

    private static Result runStatic() throws InterruptedException {
        long[] completionTimes = new long[WORKERS];
        Thread[] threads = new Thread[WORKERS];

        CountDownLatch ready = new CountDownLatch(WORKERS);
        CountDownLatch start = new CountDownLatch(1);

        long wallStart = System.nanoTime();

        for (int worker = 0; worker < WORKERS; worker++) {
            final int workerId = worker;

            threads[worker] = new Thread(() -> {
                int from = workerId * (ITEMS / WORKERS);
                int to = (workerId + 1) * (ITEMS / WORKERS);

                ready.countDown();

                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                for (int i = from; i < to; i++) {
                    simulateWork(i);
                }

                completionTimes[workerId] = System.nanoTime();
            });
            threads[worker].start();
        }

        ready.await();
        wallStart = System.nanoTime();
        start.countDown();

        for (Thread thread : threads) {
            thread.join();
        }

        long wallEnd = System.nanoTime();

        return new Result(
                (wallEnd - wallStart) / 1_000_000.0,
                completionDeltaMs(completionTimes)
        );
    }

    private static Result runDynamic() throws InterruptedException {
        AtomicInteger nextItem = new AtomicInteger(0);

        long[] completionTimes = new long[WORKERS];
        Thread[] threads = new Thread[WORKERS];

        CountDownLatch ready = new CountDownLatch(WORKERS);
        CountDownLatch start = new CountDownLatch(1);

        long wallStart;

        for (int worker = 0; worker < WORKERS; worker++) {
            final int workerId = worker;

            threads[worker] = new Thread(() -> {
                ready.countDown();

                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                while (true) {
                    int item = nextItem.getAndIncrement();

                    if (item >= ITEMS) {
                        break;
                    }

                    simulateWork(item);
                }

                completionTimes[workerId] = System.nanoTime();
            });
            threads[worker].start();
        }

        ready.await();
        wallStart = System.nanoTime();
        start.countDown();

        for (Thread thread : threads) {
            thread.join();
        }

        long wallEnd = System.nanoTime();

        return new Result(
                (wallEnd - wallStart) / 1_000_000.0,
                completionDeltaMs(completionTimes)
        );
    }

    private static double completionDeltaMs(long[] completionTimes) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        for (long time : completionTimes) {
            min = Math.min(min, time);
            max = Math.max(max, time);
        }

        return (max - min) / 1_000_000.0;
    }

    private static void printResult(String name, Result result) {
        System.out.println(name);
        System.out.printf("Total wall time: %.3f ms%n", result.wallTimeMs);
        System.out.printf(
                "Thread completion delta (min to max): %.3f ms%n",
                result.completionDeltaMs
        );
        System.out.println();
    }

    private static class Result {
        final double wallTimeMs;
        final double completionDeltaMs;

        Result(double wallTimeMs, double completionDeltaMs) {
            this.wallTimeMs = wallTimeMs;
            this.completionDeltaMs = completionDeltaMs;
        }
    }
}

