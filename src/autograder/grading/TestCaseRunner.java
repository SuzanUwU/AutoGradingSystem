package autograder.grading;

/**
 * Project scaffold only. TODO: Agree on this class's responsibilities and public
 * methods with the team before adding implementation.
 */

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

    /**
     * Executes a tester class safely using Person 3's ProgramExecutor.
     */
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