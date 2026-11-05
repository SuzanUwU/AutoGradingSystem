package autograder.ui;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import autograder.model.*;

/** CorrectionHistoryWriter responsibilities extracted from MainFrame. */
final class CorrectionHistoryWriter {
    private final MainFrame f;
    CorrectionHistoryWriter(MainFrame frame) { this.f = frame; }

    /** Handles save correction history csv. */
    void saveCorrectionHistoryCsv() {
        Path output = Path.of("workspace", "correction_history.csv");
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Student,Folder Fixes,Name Fixes,Email Fixes,Open Issues,Anomalies,Total Fixes,Status,Folder Details,Name Details,Email Details,Open Issue Details,Anomaly Details");
        for (StudentResult student : f.studentResults) {
            SubmissionResult s = student.getSubmission();
            lines.add(csvValue(s.getUsername()) + "," + csvCount(s.getFolderFixCount()) + "," + csvCount(s.getNameFixCount()) + ","
                    + csvCount(s.getEmailFixCount()) + "," + csvCount(s.getOpenIssueCount()) + "," + csvCount(s.getAnomalyCount()) + ","
                    + csvCount(s.getTotalFixCount()) + "," + csvValue(s.getOverallStatus()) + ","
                    + csvValue(csvDetails(s.getFolderFixDetails())) + "," + csvValue(csvDetails(s.getNameFixDetails())) + ","
                    + csvValue(csvDetails(s.getEmailFixDetails())) + "," + csvValue(csvDetails(s.getOpenIssueDetails())) + ","
                    + csvValue(csvDetails(s.getAnomalyDetails())));
        }
        try {
            Files.createDirectories(output.getParent());
            Files.write(output, lines, StandardCharsets.UTF_8);
            f.uiState.appendLog("Saved correction history: " + output);
        } catch (IOException e) {
            f.uiState.appendLog("[WARNING] Could not update correction_history.csv: " + e.getMessage());

            JOptionPane.showMessageDialog(f,
                    "Could not update correction_history.csv.\n\n"
                            + "Please close the CSV file if it is open in Excel, then validate again.",
                    "Correction History File In Use",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Handles csv count. */
    String csvCount(int count) {
        return count == 0 ? "" : Integer.toString(count);
    }

    /** Handles csv details. */
    String csvDetails(List<String> details) {
        return details.isEmpty() ? "" : String.join(", ", details);
    }

    /** Handles csv value. */
    String csvValue(String value) {
        String safe = value == null ? "" : value.replace("\r", " ").replace("\n", " ");
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
