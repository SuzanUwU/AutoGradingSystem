package autograder.ui;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import autograder.model.*;

/** PythonGradingActions responsibilities extracted from MainFrame. */
final class PythonGradingActions {
    private final MainFrame f;
    PythonGradingActions(MainFrame frame) { this.f = frame; }

    /** Starts the Python grading workflow. */
    void startPythonGrading() {
        File submissions = new File(f.submissionsField.getText().trim());
        File assessment = new File("assessment/is1704");
        if (!submissions.isDirectory()
                || !new File(assessment, "testers/grade_submission.py").isFile()
                || !new File(assessment, "reference").isDirectory()) {
            JOptionPane.showMessageDialog(f,
                    "Missing submission folder or IS1704 grading files.",
                    "Missing Files", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<SubmissionResult> batch = new ArrayList<>();
        for (StudentResult student : f.studentResults) batch.add(student.getSubmission());
        f.uiState.setControlsEnabled(false);
        f.progressBar.setValue(0);
        f.progressBar.setMaximum(batch.size());
        f.progressBar.setString("Starting Python grading...");
        f.uiState.appendLog("=== IS1704 PYTHON GRADING STARTED ===");
        new PythonGradingController(assessment, submissions,
                new PythonGradingController.Listener() {
                    @Override public void onResult(int row, List<QuestionResult> questions, String error) {
                        StudentResult student = f.studentResults.get(row);
                        student.setGradingResults(questions, error);
                        f.results.refreshStudentRow(row);
                        f.uiState.appendLog("Python grading: " + student.getSubmission().getUsername());
                        for (QuestionResult q : questions) f.uiState.appendLog("  " + q);
                        if (!error.isEmpty()) f.uiState.appendLog("[ERROR] " + error);
                        else f.uiState.appendLog("TOTAL: " + student.getTotal() + " / " + student.getMaximum());
                        f.uiState.updateProgress(row + 1, batch.size());
                        f.results.showSelectedStudent();
                    }
                    @Override public void onFinished(String error) {
                        f.progressBar.setString(error.isEmpty() ? "Python grading finished" : "Python grading failed");
                        if (!error.isEmpty()) f.uiState.appendLog("[ERROR] " + error);
                        f.uiState.appendLog("=== IS1704 GRADING FINISHED ===");
                        f.uiState.setControlsEnabled(true);
                    }
                }).grade(batch);
    }
}
