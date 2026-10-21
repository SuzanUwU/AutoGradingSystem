package autograder.grading;

import autograder.model.QuestionResult;
import autograder.model.QuestionSpec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GradingEngine {
    private final QuestionGrader questionGrader;
    private final List<QuestionSpec> questionSpecs;

    public GradingEngine(QuestionGrader questionGrader, List<QuestionSpec> questionSpecs) {
        this.questionGrader = questionGrader;
        this.questionSpecs = questionSpecs;
    }

    /**
     * Grades all questions (Q1a, Q1b, Q2a, Q2b, Q3) for one student submission.
     *
     * @param studentRoot Directory containing student question folders
     * @param username    Student username / ID
     * @return List of QuestionResult objects for Person 5 to aggregate
     */
    public List<QuestionResult> gradeStudent(File studentRoot, String username) {
        List<QuestionResult> results = new ArrayList<>();

        for (QuestionSpec spec : questionSpecs) {
            QuestionResult result = questionGrader.gradeQuestion(studentRoot, username, spec);
            results.add(result);
        }

        return results;
    }
}