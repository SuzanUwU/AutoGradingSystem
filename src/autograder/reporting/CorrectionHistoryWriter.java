package autograder.reporting;

import autograder.model.StudentResult;
import autograder.model.SubmissionResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Writes submission corrections without depending on Swing or MainFrame. */
public final class CorrectionHistoryWriter {
    /** Exports validation fixes and outstanding issues to a CSV file. */
    public void export(Path output, List<StudentResult> studentResults) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Student,Folder Fixes,Name Fixes,Email Fixes,Open Issues,Anomalies,Total Fixes,Status,Folder Details,Name Details,Email Details,Open Issue Details,Anomaly Details");
        for (StudentResult student : studentResults) {
            SubmissionResult s = student.getSubmission();
            lines.add(csvValue(s.getUsername()) + "," + csvCount(s.getFolderFixCount()) + "," + csvCount(s.getNameFixCount()) + ","
                    + csvCount(s.getEmailFixCount()) + "," + csvCount(s.getOpenIssueCount()) + "," + csvCount(s.getAnomalyCount()) + ","
                    + csvCount(s.getTotalFixCount()) + "," + csvValue(s.getOverallStatus()) + ","
                    + csvValue(csvDetails(s.getFolderFixDetails())) + "," + csvValue(csvDetails(s.getNameFixDetails())) + ","
                    + csvValue(csvDetails(s.getEmailFixDetails())) + "," + csvValue(csvDetails(s.getOpenIssueDetails())) + ","
                    + csvValue(csvDetails(s.getAnomalyDetails())));
        }
        Files.createDirectories(output.getParent());
        Files.write(output, lines, StandardCharsets.UTF_8);
    }
    /** Handles csv count. */
    private static String csvCount(int count) {
        return count == 0 ? "" : Integer.toString(count);
    }

    /** Handles csv details. */
    private static String csvDetails(List<String> details) {
        return details.isEmpty() ? "" : String.join(", ", details);
    }

    /** Handles csv value. */
    public static String csvValue(String value) {
        String safe = value == null ? "" : value.replace("\r", " ").replace("\n", " ");
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
