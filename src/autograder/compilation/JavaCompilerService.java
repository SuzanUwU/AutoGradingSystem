package autograder.compilation;

import autograder.execution.JdkTools;
import autograder.execution.ProcessRunner;
import autograder.model.CompilationResult;
import autograder.model.ExecutionResult;
import autograder.model.QuestionSpec;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compiles a prepared {@link QuestionWorkspace} by running {@code javac} as an external process.
 *
 * <p>Only the tester is passed to javac. With {@code -sourcepath .} javac then compiles just the
 * student sources the tester actually uses, so a broken Q1b.java sitting next to Q1a.java cannot
 * cause Q1a to fail. Trusted .class dependencies (e.g. Shape.class) are found through {@code -cp .}.
 */
public class JavaCompilerService {
    // Constants keep compiler output checks and source encoding consistent.
    private static final String CLASS_EXTENSION = ".class";
    private static final String SOURCE_ENCODING = "UTF-8";

    // Dependencies used to launch javac as a separate, time-limited process.
    private final String javacExecutable;
    private final long timeoutSeconds;
    private final ProcessRunner processRunner;

    /**
     * Uses the javac of the JDK running the grader.
     *
     * @param timeoutSeconds maximum time javac may run before it is killed
     * @param maxOutputBytes maximum bytes of javac output kept
     */
    public JavaCompilerService(long timeoutSeconds, int maxOutputBytes) {
        this(JdkTools.find("javac"), timeoutSeconds, new ProcessRunner(maxOutputBytes));
    }

    // Alternate constructor allows the compiler executable and process runner to be supplied.
    public JavaCompilerService(String javacExecutable, long timeoutSeconds, ProcessRunner processRunner) {
        this.javacExecutable = javacExecutable;
        this.timeoutSeconds = timeoutSeconds;
        this.processRunner = processRunner;
    }

    /**
     * Compiles the question's tester and the student code it depends on inside the workspace.
     * Never throws: every failure is described by the returned result.
     */
    public CompilationResult compile(QuestionWorkspace workspace, QuestionSpec question) {
        return compile(workspace, question, false);
    }

    /**
     * Same as {@link #compile(QuestionWorkspace, QuestionSpec)}, but with {@code compileAllSources}
     * every .java file in the workspace is compiled, not only what the tester reaches. Needed when a
     * student puts helper classes in files whose names do not match the class (used by edge cases for Q3).
     */
    public CompilationResult compile(QuestionWorkspace workspace, QuestionSpec question, boolean compileAllSources) {
        // Do not attempt compilation when a required student source file is missing.
        if (!workspace.isReady()) return CompilationResult.missingSources(workspace.getMissingSources());

        File directory = workspace.getDirectory();
        // Default: only the tester, javac finds what it needs via -sourcepath. Optionally every source.
        List<String> sources = compileAllSources ? listJavaSources(directory) : List.of(question.getTesterSourceFile());
        // Run javac inside the isolated question folder, not in the original submission.
        ExecutionResult javac = processRunner.run(buildCommand(sources), directory, timeoutSeconds);
        String output = combineOutput(javac);

        // Translate process outcomes into a consistent CompilationResult for the grader.
        switch (javac.getStatus()) {
            case COMPLETED:
                // A normal process exit is only considered successful if the tester .class exists.
                File testerClass = new File(directory, question.getTesterClassName() + CLASS_EXTENSION);
                if (testerClass.isFile()) return CompilationResult.success(output);
                return CompilationResult.systemError("javac exited normally but did not produce " + testerClass.getName());
            case EXITED_WITH_ERROR:
                return CompilationResult.compilationError(javac.getExitCode(), output);
            default:
                return CompilationResult.systemError("javac did not finish (" + javac.getStatus() + "): "
                        + javac.getMessage() + System.lineSeparator() + output);
        }
    }

    // List every .java file in the workspace, sorted so the javac command is deterministic.
    private List<String> listJavaSources(File directory) {
        File[] files = directory.listFiles(file -> file.isFile() && file.getName().endsWith(".java"));
        List<String> names = new ArrayList<>();
        if (files != null) for (File file : files) names.add(file.getName());
        Collections.sort(names);
        return names;
    }

    // Build a command that compiles the given sources (normally just the tester) and what they reference.
    private List<String> buildCommand(List<String> sourceFiles) {
        List<String> command = new ArrayList<>();
        command.add(javacExecutable);
        command.add("-encoding");
        command.add(SOURCE_ENCODING);
        // Keep diagnostics in English and UTF-8 whatever the instructor's OS locale is.
        command.add("-J-Duser.language=en");
        command.add("-J-Dstdout.encoding=" + SOURCE_ENCODING);
        command.add("-J-Dstderr.encoding=" + SOURCE_ENCODING);
        command.add("-nowarn");
        // Resolve dependent .java sources from the current workspace.
        command.add("-sourcepath");
        command.add(".");
        // Find trusted precompiled dependencies in the same workspace.
        command.add("-cp");
        command.add(".");
        command.add("-d");
        command.add(".");
        command.addAll(sourceFiles);
        return command;
    }

    // Merge standard output and error output for readable compilation feedback.
    private String combineOutput(ExecutionResult javac) {
        String stdout = javac.getStdout().trim();
        String stderr = javac.getStderr().trim();
        if (stdout.isEmpty()) return stderr;
        if (stderr.isEmpty()) return stdout;
        return stdout + System.lineSeparator() + stderr;
    }
}
