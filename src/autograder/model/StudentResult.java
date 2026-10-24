package autograder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class StudentResult {
    private final SubmissionResult submission;
    private final List<QuestionResult> questions = new ArrayList<>();
    private final Map<String, Double> manualScores = new LinkedHashMap<>();
    private final Map<String, String> comments = new LinkedHashMap<>();
    private String gradingError = "";
    private String identityWarning = "";
    private boolean graded;

    public StudentResult(SubmissionResult submission) { this.submission = submission; }
    public SubmissionResult getSubmission() { return submission; }
    public List<QuestionResult> getQuestions() { return Collections.unmodifiableList(questions); }
    public boolean isGraded() { return graded; }
    public void setIdentityWarning(String warning) { identityWarning = warning; }
    public String getIdentityWarning() { return identityWarning; }
    public String getGradingError() { return gradingError; }

    public void setGradingResults(List<QuestionResult> results, String error) {
        questions.clear();
        questions.addAll(results);
        gradingError = error == null ? "" : error;
        graded = true;
    }

    public void setManualScore(String questionId, double score, String comment) {
        QuestionResult question = questions.stream()
                .filter(q -> q.getQuestionId().equals(questionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Grade this question before entering a manual score."));
        if (!Double.isFinite(score) || score < 0 || score > question.getMaxScore()) {
            throw new IllegalArgumentException("Score must be between 0 and " + question.getMaxScore() + ".");
        }
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a comment explaining the manual score.");
        }
        manualScores.put(questionId, score);
        comments.put(questionId, comment.trim());
    }

    public boolean hasManualScore(String questionId) { return manualScores.containsKey(questionId); }
    public String getComment(String questionId) { return comments.getOrDefault(questionId, ""); }
    public double getFinalScore(QuestionResult question) {
        return manualScores.getOrDefault(question.getQuestionId(), question.getScore());
    }
    public double getTotal() { return questions.stream().mapToDouble(this::getFinalScore).sum(); }
    public double getMaximum() { return questions.stream().mapToDouble(QuestionResult::getMaxScore).sum(); }
    public boolean hasManualScores() { return !manualScores.isEmpty(); }
    public boolean needsAttention() {
        return "Review".equals(submission.getOverallStatus())
                || "Failed".equals(submission.getOverallStatus()) || !identityWarning.isEmpty()
                || !gradingError.isEmpty() || questions.stream()
                .anyMatch(q -> !"Graded successfully".equals(q.getStatusMessage()));
    }
    public String getGradingStatus() {
        if (!graded) return "Not graded";
        if (!gradingError.isEmpty()) return "Failed";
        return needsAttention() ? "Review" : hasManualScores() ? "Manual" : "Graded";
    }
    public String getDetails() {
        StringBuilder text = new StringBuilder(submission.getDetails());
        if (!identityWarning.isEmpty()) text.append("\nIDENTITY WARNING\n").append(identityWarning).append('\n');
        text.append("\nGRADING\n").append(getGradingStatus()).append('\n');
        if (!gradingError.isEmpty()) text.append(gradingError).append('\n');
        for (QuestionResult q : questions) {
            text.append(q).append('\n');
            if (hasManualScore(q.getQuestionId())) {
                text.append("  Manual final score: ").append(getFinalScore(q))
                    .append("\n  Reason: ").append(getComment(q.getQuestionId())).append('\n');
            }
        }
        if (!questions.isEmpty()) text.append("Final total: ").append(getTotal()).append(" / ").append(getMaximum());
        return text.toString();
    }
}
