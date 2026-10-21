package autograder.compilation;

import autograder.execution.JdkTools;
import autograder.execution.ProcessRunner;
import autograder.model.CompilationResult;
import autograder.model.ExecutionResult;
import autograder.model.QuestionSpec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Compiles a prepared {@link QuestionWorkspace} by running {@code javac} as an external process.
 *
 * <p>Only the tester is passed to javac. With {@code -sourcepath .} javac then compiles just the
 * student sources the tester actually uses, so a broken Q1b.java sitting next to Q1a.java cannot
 * cause Q1a to fail. Trusted .class dependencies (e.g. Shape.class) are found through {@code -cp .}.
 */
public class JavaCompilerService {
    private static final String CLASS_EXTENSION = ".class";
    private static final String SOURCE_ENCODING = "UTF-8";

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
        if (!workspace.isReady()) return CompilationResult.missingSources(workspace.getMissingSources());

        File directory = workspace.getDirectory();
        ExecutionResult javac = processRunner.run(buildCommand(question.getTesterSourceFile()), directory, timeoutSeconds);
        String output = combineOutput(javac);

        switch (javac.getStatus()) {
            case COMPLETED:
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

    private List<String> buildCommand(String testerSourceFile) {
        List<String> command = new ArrayList<>();
        command.add(javacExecutable);
        command.add("-encoding");
        command.add(SOURCE_ENCODING);
        // Keep diagnostics in English and UTF-8 whatever the instructor's OS locale is.
        command.add("-J-Duser.language=en");
        command.add("-J-Dstdout.encoding=" + SOURCE_ENCODING);
        command.add("-J-Dstderr.encoding=" + SOURCE_ENCODING);
        command.add("-nowarn");
        command.add("-sourcepath");
        command.add(".");
        command.add("-cp");
        command.add(".");
        command.add("-d");
        command.add(".");
        command.add(testerSourceFile);
        return command;
    }

    private String combineOutput(ExecutionResult javac) {
        String stdout = javac.getStdout().trim();
        String stderr = javac.getStderr().trim();
        if (stdout.isEmpty()) return stderr;
        if (stderr.isEmpty()) return stdout;
        return stdout + System.lineSeparator() + stderr;
    }
}
