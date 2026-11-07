
package autograder.ui;

import autograder.grading.PythonGradingService;
import autograder.grading.PythonSubmissionExtractor;
import autograder.model.*;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Coordinates the Python grading workflow from the GUI.
 * Handles prerequisite checks, background grading,
 * progress updates, and displaying results.
 *
 * Actual grading is delegated to PythonGradingService.
 */
final class PythonGradingController {
    private final MainFrame f;

    PythonGradingController(MainFrame frame) {
        this.f = frame;
    }

    /** Starts grading the currently validated Python submissions. */
    void startPythonGrading() {
        File submissions = new File(f.submissionsField.getText().trim());
        File assessment = new File("assessment/python");

        // STEP 1: Check required folders and assessment files.
        if (!submissions.isDirectory()
                || !new File(assessment, "testers/grade_submission.py").isFile()
                || !new File(assessment, "reference").isDirectory()) {
            JOptionPane.showMessageDialog(f,
                    "Missing submission folder or IS1704 grading files.",
                    "Missing Files", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // STEP 2: Collect submissions from the current batch.
        List<SubmissionResult> batch = new ArrayList<>();
        for (StudentResult student : f.studentResults) {
            batch.add(student.getSubmission());
        }

        // STEP 3: Prepare the GUI for grading.
        f.uiState.setControlsEnabled(false);
        f.progressBar.setValue(0);
        f.progressBar.setMaximum(batch.size());
        f.progressBar.setString("Starting Python grading...");
        f.uiState.appendLog("=== IS1704 PYTHON GRADING STARTED ===");

        // STEP 4: Grade submissions on a background thread.
        new SwingWorker<Void, Update>() {
            @Override
            protected Void doInBackground() {
                PythonGradingService grader =
                        new PythonGradingService(assessment, "python");

                for (int row = 0; row < batch.size(); row++) {
                    SubmissionResult submission = batch.get(row);
                    String username = submission.getUsername();

                    try {
                        // Skip submissions that failed validation.
                        if ("Failed".equals(submission.getOverallStatus())) {
                            publish(new Update(row, List.of(),
                                    "Skipped: validation failed."));
                            continue;
                        }

                        // Prefer the corrected ZIP when available.
                        File original = new File(submissions, username + ".zip");
                        File corrected = new File(
                                "output/is1704-corrected", username + ".zip");

                        File selected =
                                "Corrected".equals(submission.getOverallStatus())
                                        && corrected.isFile()
                                        ? corrected : original;

                        if (!selected.isFile()) {
                            throw new IOException("ZIP not found: " + selected);
                        }

                        // Prepare a temporary folder for this submission.
                        Path temporary =
                                Files.createTempDirectory("is1704-grading-");

                        try {
                            File answers = temporary.resolve("answers").toFile();

                            // Extract the required Python answer files.
                            PythonSubmissionExtractor.extract(selected, answers);

                            // Delegate actual grading to the grading service.
                            List<QuestionResult> questions =
                                    grader.gradeStudent(answers);

                            publish(new Update(row, questions, ""));
                        } finally {
                            // Remove temporary grading files.
                            try (var paths = Files.walk(temporary)) {
                                paths.sorted(Comparator.reverseOrder())
                                        .forEach(path -> {
                                            try {
                                                Files.deleteIfExists(path);
                                            } catch (IOException ignored) {
                                                // Best-effort cleanup.
                                            }
                                        });
                            }
                        }
                    } catch (Exception ex) {
                        publish(new Update(row, List.of(), ex.toString()));
                    }
                }
                return null;
            }

            // STEP 5: Update the GUI on Swing's event dispatch thread.
            @Override
            protected void process(List<Update> updates) {
                for (Update update : updates) {
                    StudentResult student = f.studentResults.get(update.row);
                    student.setGradingResults(update.questions, update.error);

                    f.results.refreshStudentRow(update.row);
                    f.uiState.appendLog("Python grading: "
                            + student.getSubmission().getUsername());

                    for (QuestionResult q : update.questions) {
                        f.uiState.appendLog("  " + q);
                    }

                    if (!update.error.isEmpty()) {
                        f.uiState.appendLog("[ERROR] " + update.error);
                    } else {
                        f.uiState.appendLog("TOTAL: "
                                + student.getTotal() + " / "
                                + student.getMaximum());
                    }

                    f.uiState.updateProgress(update.row + 1, batch.size());
                    f.results.showSelectedStudent();
                }
            }

            // STEP 6: Restore GUI controls after grading.
            @Override
            protected void done() {
                String error = "";
                try {
                    get();
                } catch (Exception ex) {
                    error = ex.toString();
                }

                f.progressBar.setString(error.isEmpty()
                        ? "Python grading finished"
                        : "Python grading failed");

                if (!error.isEmpty()) {
                    f.uiState.appendLog("[ERROR] " + error);
                }

                f.uiState.appendLog("=== IS1704 GRADING FINISHED ===");
                f.uiState.setControlsEnabled(true);
            }
        }.execute();
    }

    /** Holds one student's results before updating the GUI. */
    private static final class Update {
        final int row;
        final List<QuestionResult> questions;
        final String error;

        Update(int row, List<QuestionResult> questions, String error) {
            this.row = row;
            this.questions = questions;
            this.error = error;
        }
    }
}
