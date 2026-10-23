package TasksPractice;


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class Task4 {

    private static final int[] SIZES = {
            100_000, 1_000_000, 10_000_000
    };

    private static final int STUDENT_ID = 230103246;
    private static final int REPEATS = 3;

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("receiver")) {
            receivePipe();
            return;
        }

        if (args.length > 0 && args[0].equals("mapped-reader")) {
            readMapped(Path.of(args[1]), Integer.parseInt(args[2]));
            return;
        }

        System.out.println("TASK 4: IPC vs Memory-Mapped Transfer");
        System.out.println("Student ID: " + STUDENT_ID);
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println();

        System.out.printf(
                "%-14s %18s %22s%n",
                "Array size", "Pipe IPC (ms)", "Mapped file (ms)"
        );

        for (int size : SIZES) {
            int[] values = new int[size];

            for (int i = 0; i < size; i++) {
                values[i] = i + STUDENT_ID;
            }

            double pipeMs = medianPipeTime(values);
            double mappedMs = medianMappedTime(values);

            System.out.printf(
                    "%-14d %18.3f %22.3f%n",
                    size, pipeMs, mappedMs
            );
        }
    }

    private static double medianPipeTime(int[] values)
            throws Exception {

        long[] times = new long[REPEATS];

        for (int r = 0; r < REPEATS; r++) {
            Process process = new ProcessBuilder(
                    javaExecutable(),
                    "-cp", System.getProperty("java.class.path"),
                    Task4.class.getName(),
                    "receiver"
            ).redirectError(ProcessBuilder.Redirect.INHERIT).start();

            long start = System.nanoTime();

            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(process.getOutputStream()))) {

                out.writeInt(values.length);

                for (int value : values) {
                    out.writeInt(value);
                }
            }

            long receivedChecksum;
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(process.getInputStream()))) {
                receivedChecksum = in.readLong();
            }

            if (process.waitFor() != 0) {
                throw new IOException("Pipe receiver process failed");
            }

            times[r] = System.nanoTime() - start;

            if (receivedChecksum != checksum(values)) {
                throw new IllegalStateException("Pipe verification failed");
            }
        }

        return median(times);
    }

    private static void receivePipe() throws Exception {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(System.in))) {

            int length = in.readInt();
            long checksum = 0;

            for (int i = 0; i < length; i++) {
                checksum += in.readInt();
            }

            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(System.out))) {
                out.writeLong(checksum);
                out.flush();
            }
        }
    }

    private static double medianMappedTime(int[] values)
            throws Exception {

        long[] times = new long[REPEATS];

        for (int r = 0; r < REPEATS; r++) {
            Path file = Files.createTempFile("task4-map-", ".bin");

            try {
                long bytes = (long) values.length * Integer.BYTES;

                long start = System.nanoTime();

                try (RandomAccessFile raf =
                             new RandomAccessFile(file.toFile(), "rw");
                     FileChannel channel = raf.getChannel()) {

                    raf.setLength(bytes);

                    MappedByteBuffer mapped = channel.map(
                            FileChannel.MapMode.READ_WRITE, 0, bytes
                    );
                    mapped.order(ByteOrder.nativeOrder());

                    for (int value : values) {
                        mapped.putInt(value);
                    }

                    mapped.force();
                }

                Process process = new ProcessBuilder(
                        javaExecutable(),
                        "-cp", System.getProperty("java.class.path"),
                        Task4.class.getName(),
                        "mapped-reader",
                        file.toAbsolutePath().toString(),
                        Integer.toString(values.length)
                ).redirectError(ProcessBuilder.Redirect.INHERIT).start();

                long receivedChecksum;
                try (DataInputStream in = new DataInputStream(
                        new BufferedInputStream(process.getInputStream()))) {
                    receivedChecksum = in.readLong();
                }

                if (process.waitFor() != 0) {
                    throw new IOException("Mapped reader process failed");
                }

                times[r] = System.nanoTime() - start;

                if (receivedChecksum != checksum(values)) {
                    throw new IllegalStateException(
                            "Mapped-file verification failed"
                    );
                }
            } finally {
                Files.deleteIfExists(file);
            }
        }

        return median(times);
    }

    private static void readMapped(Path path, int length)
            throws Exception {

        long checksum = 0;
        long bytes = (long) length * Integer.BYTES;

        try (RandomAccessFile raf =
                     new RandomAccessFile(path.toFile(), "r");
             FileChannel channel = raf.getChannel()) {

            MappedByteBuffer mapped = channel.map(
                    FileChannel.MapMode.READ_ONLY, 0, bytes
            );
            mapped.order(ByteOrder.nativeOrder());

            for (int i = 0; i < length; i++) {
                checksum += mapped.getInt();
            }
        }

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(System.out))) {
            out.writeLong(checksum);
            out.flush();
        }
    }

    private static long checksum(int[] values) {
        long sum = 0;

        for (int value : values) {
            sum += value;
        }

        return sum;
    }

    private static double median(long[] values) {
        java.util.Arrays.sort(values);
        return values[values.length / 2] / 1_000_000.0;
    }

    private static String javaExecutable() {
        String executable = System.getProperty("os.name")
                .toLowerCase().contains("win") ? "java.exe" : "java";

        return Path.of(
                System.getProperty("java.home"), "bin", executable
        ).toString();
    }
}

