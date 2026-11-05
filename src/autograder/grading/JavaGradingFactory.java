package autograder.grading;

import autograder.compilation.JavaCompilerService;
import autograder.compilation.WorkspaceManager;
import autograder.execution.ProgramExecutor;
import autograder.model.QuestionSpec;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Configures the existing Java grading engine and its assessment questions. */
public final class JavaGradingFactory {
    private JavaGradingFactory() {}

    /** Builds a grading engine for the selected Java tester folder. */
    public static GradingEngine create(File testers, File resources) {
        WorkspaceManager manager = new WorkspaceManager(new File("temp_workspaces"), testers, resources);
        JavaCompilerService compiler = new JavaCompilerService(10, 1024 * 1024);
        ProgramExecutor executor = new ProgramExecutor(5, 1024 * 1024, new ArrayList<>());
        QuestionGrader grader = new QuestionGrader(compiler, new TestCaseRunner(executor), manager);
        List<QuestionSpec> specs = List.of(
            new QuestionSpec("Q1a", "Q1", "Q1aTester", List.of("Q1a.java")),
            new QuestionSpec("Q1b", "Q1", "Q1bTester", List.of("Q1b.java")),
            new QuestionSpec("Q2a", "Q2", "Q2aTester", List.of("Q2a.java")),
            new QuestionSpec("Q2b", "Q2", "Q2bTester", List.of("Q2b.java")),
            new QuestionSpec("Q3", "Q3", "Q3Tester", List.of("Q3.java")));
        return new GradingEngine(grader, specs);
    }
}
