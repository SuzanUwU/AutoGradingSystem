package autograder.grading;

import autograder.execution.ProcessRunner;
import autograder.model.ExecutionResult;
import autograder.model.QuestionResult;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Runs the AY23/24 LT1 Python demonstration rubric for one extracted student folder. */
public class PythonGradingService {
    private final File assessmentDirectory;
    private final String pythonCommand;
    public PythonGradingService(File assessmentDirectory, String pythonCommand) {
        this.assessmentDirectory = assessmentDirectory;
        this.pythonCommand = pythonCommand;
    }
    public List<QuestionResult> gradeStudent(File studentDirectory) {
        File script = new File(assessmentDirectory, "testers/grade_submission.py");
        File reference = new File(assessmentDirectory, "reference");
        if (!studentDirectory.isDirectory() || !script.isFile() || !reference.isDirectory())
            throw new IllegalArgumentException("Missing extracted student directory, tester or reference folder");
        List<String> command = Arrays.asList(pythonCommand, "-u", script.getAbsolutePath(),
            studentDirectory.getAbsolutePath(), reference.getAbsolutePath(),
            new File(assessmentDirectory,"testers").getAbsolutePath());
        // Overall limit covers all test cases. Per-case timeouts are enforced by the Python runner.
        ExecutionResult execution = new ProcessRunner(65536).run(command, studentDirectory, 240);
        List<QuestionResult> results = new ArrayList<>();
        for (String line : execution.getStdout().split("\\R")) {
            String[] parts = line.split("\\t",6);
            if (parts.length != 6 || !parts[0].matches("q[1-4][abc]?")) continue;
            try {
                results.add(new QuestionResult(parts[0], Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]), parts[5]));
            } catch (NumberFormatException ignored) { }
        }
        if (results.size() != 8 || !execution.isCompleted())
            throw new IllegalStateException("Incomplete Python grading: " + execution.getStatus()
                + "; completed " + results.size() + "/8 questions. "
                + execution.getMessage() + " " + execution.getStderr());
        return results;
    }
}
