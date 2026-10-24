package autograder.grading;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreParser {
    private static final Pattern TEST_BLOCK_PATTERN = Pattern.compile("(?im)^[ \\t]*Test[ \\t]+\\d+[ \\t]*:");
    private static final Pattern PASSED_PATTERN = Pattern.compile("(?im)^[ \\t]*Passed[ \\t]*$");

    /**
     * Parses the tester's stdout to calculate the earned score out of maxScore.
     * If the tester times out before printing the final numeric score,
     * completed "Passed" test cases are still awarded.
     */
    public static double parseScore(String stdout, double maxScore) {
        if (stdout == null || stdout.trim().isEmpty()) {
            return 0.0;
        }

        String trimmedStdout = stdout.trim();
        String[] lines = trimmedStdout.split("\\R");

        // Normal case: tester completed and printed the final score.
        try {
            double passedCount = Double.parseDouble(lines[lines.length - 1].trim());

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

        } catch (NumberFormatException e) {
            // Tester probably stopped early, e.g. timeout.
            // Count completed test cases that already printed "Passed".
            Matcher passedMatcher = PASSED_PATTERN.matcher(stdout);
            int passedCount = 0;

            while (passedMatcher.find()) {
                passedCount++;
            }

            if (passedCount <= 0) {
                return 0.0;
            }

            return Math.min(passedCount, maxScore);
        }
    }
}