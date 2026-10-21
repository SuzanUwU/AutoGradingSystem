package autograder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Outcome of compiling one student's question part together with its tester.
 *
 * <p>{@link Status#COMPILATION_ERROR} and {@link Status#MISSING_SOURCES} are the student's fault and
 * normally score zero. {@link Status#SYSTEM_ERROR} means the grader itself could not compile
 * (e.g. javac missing or hung), so the question should be reported as not graded rather than zero.
 */
public class CompilationResult {
    /** Why compilation succeeded or failed. */
    public enum Status {
        SUCCESS,
        COMPILATION_ERROR,
        MISSING_SOURCES,
        SYSTEM_ERROR
    }

    private final Status status;
    private final int exitCode;
    private final String output;
    private final List<String> missingSources;

    private CompilationResult(Status status, int exitCode, String output, List<String> missingSources) {
        this.status = status;
        this.exitCode = exitCode;
        this.output = output == null ? "" : output;
        this.missingSources = Collections.unmodifiableList(new ArrayList<>(missingSources));
    }

    public static CompilationResult success(String output) {
        return new CompilationResult(Status.SUCCESS, 0, output, Collections.emptyList());
    }

    public static CompilationResult compilationError(int exitCode, String output) {
        return new CompilationResult(Status.COMPILATION_ERROR, exitCode, output, Collections.emptyList());
    }

    public static CompilationResult missingSources(List<String> missingSources) {
        return new CompilationResult(Status.MISSING_SOURCES, ExecutionResult.NO_EXIT_CODE,
                "Missing required source file(s): " + String.join(", ", missingSources), missingSources);
    }

    public static CompilationResult systemError(String message) {
        return new CompilationResult(Status.SYSTEM_ERROR, ExecutionResult.NO_EXIT_CODE, message, Collections.emptyList());
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /** Returns javac's exit code, or {@link ExecutionResult#NO_EXIT_CODE} if javac never ran to completion. */
    public int getExitCode() {
        return exitCode;
    }

    /** Returns javac's diagnostics (errors and warnings), or an explanation for non-javac failures. */
    public String getOutput() {
        return output;
    }

    public List<String> getMissingSources() {
        return missingSources;
    }
}
