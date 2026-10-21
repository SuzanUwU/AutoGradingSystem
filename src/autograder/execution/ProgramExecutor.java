package autograder.execution;

import autograder.compilation.QuestionWorkspace;
import autograder.model.ExecutionResult;
import autograder.model.QuestionSpec;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runs compiled tester programs in a separate JVM with a time limit.
 *
 * <p>Running each tester in its own process (instead of a thread inside the grader) means an
 * infinite loop can always be killed and a student's {@code System.exit()} cannot stop the grader.
 * The working directory is the question workspace, so testers find data files such as
 * personstester.txt by relative path.
 */
public class ProgramExecutor {
    private static final String CLASS_EXTENSION = ".class";
    private static final String OUTPUT_ENCODING = "UTF-8";

    private final String javaExecutable;
    private final long timeoutSeconds;
    private final List<String> jvmOptions;
    private final ProcessRunner processRunner;

    /**
     * Uses the java launcher of the JDK running the grader.
     *
     * @param timeoutSeconds maximum time each program may run before it is killed
     * @param maxOutputBytes maximum bytes kept from each of stdout and stderr
     * @param jvmOptions     extra JVM options for the student program, e.g. "-Xmx256m"
     */
    public ProgramExecutor(long timeoutSeconds, int maxOutputBytes, List<String> jvmOptions) {
        this(JdkTools.find("java"), timeoutSeconds, jvmOptions, new ProcessRunner(maxOutputBytes));
    }

    public ProgramExecutor(String javaExecutable, long timeoutSeconds, List<String> jvmOptions,
            ProcessRunner processRunner) {
        this.javaExecutable = javaExecutable;
        this.timeoutSeconds = timeoutSeconds;
        this.jvmOptions = Collections.unmodifiableList(new ArrayList<>(jvmOptions));
        this.processRunner = processRunner;
    }

    /** Runs the question's tester in its compiled workspace. */
    public ExecutionResult runTester(QuestionWorkspace workspace, QuestionSpec question) {
        return run(workspace.getDirectory(), question.getTesterClassName(), Collections.emptyList());
    }

    /**
     * Runs {@code mainClass} from {@code workingDirectory} with the given program arguments.
     * Never throws: a missing class, launch failure or timeout is described by the returned result.
     *
     * @param workingDirectory folder holding the compiled classes and any data files
     * @param mainClass        class whose main method is run, e.g. "Q1aTester"
     * @param programArgs      arguments passed to main, e.g. a test-case number
     */
    public ExecutionResult run(File workingDirectory, String mainClass, List<String> programArgs) {
        File compiledClass = new File(workingDirectory, mainClass + CLASS_EXTENSION);
        if (!compiledClass.isFile()) {
            return ExecutionResult.notStarted("Compiled class not found: " + compiledClass.getPath());
        }
        return processRunner.run(buildCommand(mainClass, programArgs), workingDirectory, timeoutSeconds);
    }

    private List<String> buildCommand(String mainClass, List<String> programArgs) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.addAll(jvmOptions);
        // ProcessRunner decodes output as UTF-8, and English messages keep reports consistent.
        command.add("-Dstdout.encoding=" + OUTPUT_ENCODING);
        command.add("-Dstderr.encoding=" + OUTPUT_ENCODING);
        command.add("-Duser.language=en");
        command.add("-cp");
        command.add(".");
        command.add(mainClass);
        command.addAll(programArgs);
        return command;
    }
}
