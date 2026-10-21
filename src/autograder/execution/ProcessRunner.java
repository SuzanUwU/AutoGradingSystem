package autograder.execution;

import autograder.model.ExecutionResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs an external command with a hard time limit and captures its output.
 *
 * <p>stdout and stderr are drained on separate threads so a chatty process can never block on a
 * full pipe, and each stream keeps at most {@code maxOutputBytes} so an infinite print loop cannot
 * exhaust memory. stdin is closed immediately, so a program waiting for keyboard input sees
 * end-of-file instead of hanging. A process that outlives its time limit is killed together with
 * any child processes it started.
 */
public class ProcessRunner {
    private static final long STREAM_DRAIN_WAIT_MILLIS = 1000;
    private static final long KILL_WAIT_SECONDS = 5;

    private final int maxOutputBytes;

    /**
     * @param maxOutputBytes maximum bytes kept from each of stdout and stderr; extra output is discarded
     */
    public ProcessRunner(int maxOutputBytes) {
        this.maxOutputBytes = maxOutputBytes;
    }

    /**
     * Runs {@code command} in {@code workingDirectory}, waiting at most {@code timeoutSeconds}.
     * Never throws: launch failures and interruptions are reported through the result status.
     */
    public ExecutionResult run(List<String> command, File workingDirectory, long timeoutSeconds) {
        long startTime = System.nanoTime();
        Process process;
        try {
            process = new ProcessBuilder(command).directory(workingDirectory).start();
        } catch (IOException e) {
            return ExecutionResult.notStarted("Could not start " + command.get(0) + ": " + e.getMessage());
        }

        OutputCollector stdout = new OutputCollector(process.getInputStream(), maxOutputBytes);
        OutputCollector stderr = new OutputCollector(process.getErrorStream(), maxOutputBytes);
        stdout.start();
        stderr.start();
        closeStandardInput(process);

        ExecutionResult.Status status;
        String message = "";
        try {
            if (process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                status = process.exitValue() == 0 ? ExecutionResult.Status.COMPLETED
                        : ExecutionResult.Status.EXITED_WITH_ERROR;
            } else {
                kill(process);
                status = ExecutionResult.Status.TIMED_OUT;
                message = "Terminated after exceeding the " + timeoutSeconds + "s time limit.";
            }
            stdout.join(STREAM_DRAIN_WAIT_MILLIS);
            stderr.join(STREAM_DRAIN_WAIT_MILLIS);
        } catch (InterruptedException e) {
            kill(process);
            Thread.currentThread().interrupt();
            status = ExecutionResult.Status.INTERRUPTED;
            message = "Grading was interrupted; the process was terminated.";
        }

        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        int exitCode = status == ExecutionResult.Status.COMPLETED || status == ExecutionResult.Status.EXITED_WITH_ERROR
                ? process.exitValue() : ExecutionResult.NO_EXIT_CODE;
        boolean truncated = stdout.isTruncated() || stderr.isTruncated();
        return new ExecutionResult(status, exitCode, stdout.getText(), stderr.getText(),
                durationMillis, truncated, message);
    }

    private void closeStandardInput(Process process) {
        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
            // The process may already have exited; nothing to close.
        }
    }

    private void kill(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        try {
            process.waitFor(KILL_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Reads one process stream to the end, keeping only the first {@code limit} bytes. */
    private static class OutputCollector extends Thread {
        private static final int BUFFER_SIZE = 8192;

        private final InputStream stream;
        private final int limit;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private volatile boolean truncated;

        OutputCollector(InputStream stream, int limit) {
            this.stream = stream;
            this.limit = limit;
            setDaemon(true);
        }

        @Override
        public void run() {
            byte[] chunk = new byte[BUFFER_SIZE];
            try (InputStream input = stream) {
                int read;
                while ((read = input.read(chunk)) != -1) {
                    // Keep draining past the limit so the process never blocks on a full pipe.
                    int room = limit - buffer.size();
                    if (room > 0) {
                        synchronized (buffer) {
                            buffer.write(chunk, 0, Math.min(read, room));
                        }
                    }
                    if (read > room) truncated = true;
                }
            } catch (IOException ignored) {
                // Stream closed because the process was killed; keep what was collected.
            }
        }

        String getText() {
            synchronized (buffer) {
                return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            }
        }

        boolean isTruncated() {
            return truncated;
        }
    }
}
