package autograder.grading;

import autograder.execution.ProgramExecutor;
import autograder.model.ExecutionResult;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class TestCaseRunner {
    private final ProgramExecutor executor;

    public TestCaseRunner(ProgramExecutor executor) {
        this.executor = executor;
    }

    public ExecutionResult runTestSafely(File workingDir, String mainClass) {
        return runTestSafely(workingDir, mainClass, Collections.emptyList());
    }

    public ExecutionResult runTestSafely(File workingDir, String mainClass, List<String> programArgs) {
        try {
            List<String> args = (programArgs != null) ? programArgs : Collections.emptyList();
            return executor.run(workingDir, mainClass, args);
        } catch (Exception e) {
            return ExecutionResult.notStarted("Execution failed to launch: " + e.getMessage());
        }
    }
}