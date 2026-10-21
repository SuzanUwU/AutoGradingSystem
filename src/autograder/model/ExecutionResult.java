package autograder.model;

/**
 * Outcome of running one external process (javac or a tester JVM): how it ended, its exit
 * code and the captured output. Output is kept even when the process is killed on timeout,
 * so callers can still inspect whatever was printed before the kill.
 */
public class ExecutionResult {
    /** How the process ended. */
    public enum Status {
        /** Exited by itself with exit code 0. */
        COMPLETED,
        /** Exited by itself with a non-zero exit code (e.g. uncaught error, System.exit(1)). */
        EXITED_WITH_ERROR,
        /** Exceeded the time limit and was forcibly terminated. */
        TIMED_OUT,
        /** Could not be launched at all (e.g. java or javac not found). */
        NOT_STARTED,
        /** The grader itself was interrupted while waiting; the process was terminated. */
        INTERRUPTED
    }

    public static final int NO_EXIT_CODE = -1;

    private final Status status;
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final long durationMillis;
    private final boolean outputTruncated;
    private final String message;

    public ExecutionResult(Status status, int exitCode, String stdout, String stderr,
            long durationMillis, boolean outputTruncated, String message) {
        this.status = status;
        this.exitCode = exitCode;
        this.stdout = stdout == null ? "" : stdout;
        this.stderr = stderr == null ? "" : stderr;
        this.durationMillis = durationMillis;
        this.outputTruncated = outputTruncated;
        this.message = message == null ? "" : message;
    }

    /** Creates a result for a process that could not be launched. */
    public static ExecutionResult notStarted(String message) {
        return new ExecutionResult(Status.NOT_STARTED, NO_EXIT_CODE, "", "", 0, false, message);
    }

    public Status getStatus() {
        return status;
    }

    /** Returns the process exit code, or {@link #NO_EXIT_CODE} if it never exited by itself. */
    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    /** Returns true if stdout or stderr exceeded the size limit and was cut short. */
    public boolean isOutputTruncated() {
        return outputTruncated;
    }

    /** Returns a short explanation for abnormal endings, or an empty string. */
    public String getMessage() {
        return message;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public boolean isTimedOut() {
        return status == Status.TIMED_OUT;
    }
}
