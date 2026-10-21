package autograder.model;

/**
 * Project scaffold only. TODO: Agree on this class's responsibilities and public
 * methods with the team before adding implementation.
 * Note: Suzan added this file
*/

public class QuestionResult {
    private final String questionId;
    private final double score;
    private final double maxScore;
    private final String statusMessage;
    private final boolean passed;

    public QuestionResult(String questionId, double score, double maxScore, String statusMessage) {
        this.questionId = questionId;
        this.score = score;
        this.maxScore = maxScore;
        this.statusMessage = statusMessage;
        this.passed = score > 0;
    }

    public String getQuestionId() { return questionId; }
    public double getScore() { return score; }
    public double getMaxScore() { return maxScore; }
    public String getStatusMessage() { return statusMessage; }
    public boolean isPassed() { return passed; }

    @Override
    public String toString() {
        return String.format("[%s] Score: %.1f/%.1f - Status: %s", questionId, score, maxScore, statusMessage);
    }
}