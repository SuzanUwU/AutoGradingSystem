package autograder.grading;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreParser {
    private static final Pattern TEST_BLOCK_PATTERN = Pattern.compile("(?im)^[ \\t]*Test[ \\t]+\\d+[ \\t]*:");

    /**
     * Parses the tester's stdout to calculate the earned score out of maxScore.
     */
    public static double parseScore(String stdout, double maxScore) {
        if (stdout == null || stdout.trim().isEmpty()) {
            return 0.0;
        }

        String trimmedStdout = stdout.trim();
        String[] lines = trimmedStdout.split("\\R");
        double passedCount;
        try {
            passedCount = Double.parseDouble(lines[lines.length - 1].trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }

        Matcher testMatcher = TEST_BLOCK_PATTERN.matcher(stdout);
        int totalTests = 0;
        while (testMatcher.find()) {
            totalTests++;
        }

        if (totalTests == 0
                || !Double.isFinite(passedCount)
                || passedCount < 0
                || passedCount != Math.floor(passedCount)
                || passedCount > totalTests) {
            return 0.0;
        }

        return (passedCount / totalTests) * maxScore;
    }
}