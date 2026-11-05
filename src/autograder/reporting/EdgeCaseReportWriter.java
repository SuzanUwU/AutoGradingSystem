package autograder.reporting;

import autograder.EdgeCaseRunner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Exports optional edge-case diagnostics separately from official marks. */
public final class EdgeCaseReportWriter {
    /** Writes a CSV containing each question's edge-case score. */
    public void export(Path output, List<EdgeCaseRunner.Result> results) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("Student,Q1a,Q1b,Q2a,Q2b,Q3,Passed,Failed,Status");
        for (EdgeCaseRunner.Result result : results) {
            lines.add(CorrectionHistoryWriter.csvValue(result.getUsername()) + ","
                + score(result, "Q1a") + "," + score(result, "Q1b") + ","
                + score(result, "Q2a") + "," + score(result, "Q2b") + ","
                + score(result, "Q3") + "," + result.getPassed() + ","
                + result.getFailed() + ","
                + CorrectionHistoryWriter.csvValue(result.isSkipped() ? result.getError() : "Completed"));
        }
        Files.createDirectories(output.getParent());
        Files.write(output, lines, StandardCharsets.UTF_8);
    }

    private String score(EdgeCaseRunner.Result result, String questionId) {
        EdgeCaseRunner.QuestionResult q = result.get(questionId);
        return q == null ? "" : CorrectionHistoryWriter.csvValue(q.getScoreText());
    }
}
