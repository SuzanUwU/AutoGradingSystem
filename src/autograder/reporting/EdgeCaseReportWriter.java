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
        lines.add("Student,Q1a (/7.0),Q1b (/9.0),Q2a (/7.0),Q2b (/6.0),Q3 (/16.0),Total,Maximum,Failed Cases,Edge Test Status");

        for (EdgeCaseRunner.Result result : results) {
            String total = "";
            String maximum = "";
            String failedCases = "";
            String status;

            if (result.isSkipped()) {
                status = "Skipped";
                if (result.getError() != null && !result.getError().isBlank()) status += ": " + result.getError();
            } else {
                total = Integer.toString(result.getPassed());
                maximum = Integer.toString(result.getTotal());
                failedCases = Integer.toString(result.getFailed());
                status = "Completed";
            }

            lines.add(CorrectionHistoryWriter.csvValue(result.getUsername()) + ","
                    + score(result, "Q1a") + "," + score(result, "Q1b") + ","
                    + score(result, "Q2a") + "," + score(result, "Q2b") + ","
                    + score(result, "Q3") + "," + total + "," + maximum + ","
                    + failedCases + "," + CorrectionHistoryWriter.csvValue(status));
        }

        Files.createDirectories(output.getParent());
        Files.write(output, lines, StandardCharsets.UTF_8);
    }

    private String score(EdgeCaseRunner.Result result, String questionId) {
        EdgeCaseRunner.QuestionResult q = result.get(questionId);
        return q == null ? "" : Integer.toString(q.getPassed());
    }
}