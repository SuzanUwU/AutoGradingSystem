package autograder.ui;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import autograder.grading.*;
import autograder.model.*;


/** JavaGradingActions responsibilities extracted from MainFrame. */
final class JavaGradingController {
    private final MainFrame f;
    JavaGradingController(MainFrame frame) { this.f = frame; }

    /** Grades Java submissions using the selected tester set. */
    void startGrading(File testers, String testerLabel, boolean generatedMode) {
        File workspace = new File("workspace");
        File resources = new File("assessment/resources");

        if (workspace == null
                || !workspace.isDirectory()
                || !testers.isDirectory()
                || !resources.isDirectory()) {

            JOptionPane.showMessageDialog(f,
                    "Cannot find workspace, tester folder, or assessment/resources.\n"
                            + "Tester folder: " + testers.getPath() + "\n"
                            + "Run the application from the project folder.",
                    "Missing Folder",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<SubmissionResult> currentBatch = new ArrayList<>();
        for (StudentResult student : f.studentResults) {
            currentBatch.add(student.getSubmission());
        }

        // Clear only the results for the grading mode being run
        if (generatedMode) {
            for (int row = 0; row < f.generatedTotals.size(); row++) {
                f.generatedTotals.set(row, "");
                f.results.refreshStudentRow(row);
            }
        } else {
            for (int row = 0; row < f.studentResults.size(); row++) {
                f.studentResults.get(row).clearGradingResults();
                f.results.refreshStudentRow(row);
            }
            f.results.showSelectedStudent();
        }

        f.uiState.setControlsEnabled(false);
        f.progressBar.setValue(0);
        f.progressBar.setString("Starting grading with " + testerLabel + "...");
        f.uiState.appendLog("\n=== GRADING STARTED: " + testerLabel + " ===");

        SwingWorker<Void, GradingUpdate> worker = new SwingWorker<Void, GradingUpdate>() {
            @Override
            protected Void doInBackground() {
                GradingEngine engine = autograder.grading.JavaGradingFactory.create(testers, resources);
                for (int row = 0; row < currentBatch.size(); row++) {
                    SubmissionResult submission = currentBatch.get(row);
                    try {
                        if ("Failed".equals(submission.getOverallStatus())) {
                            publish(new GradingUpdate(row, List.of(), "Skipped: submission validation failed."));
                            continue;
                        }
                        File studentRoot = autograder.grading.WorkspaceLocator.findStudentRoot(workspace, submission.getUsername());
                        if (studentRoot == null) {
                            publish(new GradingUpdate(row, List.of(), "Cannot uniquely locate the student folder."));
                            continue;
                        }
                        publish(new GradingUpdate(row, engine.gradeStudent(studentRoot, submission.getUsername()), ""));
                    } catch (Exception ex) {
                        publish(new GradingUpdate(row, List.of(), ex.toString()));
                    }
                }
                return null;
            }
            @Override
            protected void process(List<GradingUpdate> updates) {
                for (GradingUpdate update : updates) {
                    StudentResult student = f.studentResults.get(update.row);

                    if (generatedMode) {
                        if (!update.error.isEmpty() && update.questions.isEmpty()) {
                            f.generatedTotals.set(update.row, "Failed");
                        } else {
                            double total = update.questions.stream().mapToDouble(QuestionResult::getScore).sum();
                            double maximum = update.questions.stream().mapToDouble(QuestionResult::getMaxScore).sum();
                            f.generatedTotals.set(update.row, total + " / " + maximum);
                        }
                    } else {
                        student.setGradingResults(update.questions, update.error);
                    }

                    f.results.refreshStudentRow(update.row);
                    f.uiState.appendLog("Grading with " + testerLabel + ": " + student.getSubmission().getUsername());
                    for (QuestionResult q : update.questions) f.uiState.appendLog("  " + q);
                    if (!update.error.isEmpty()) {
                        f.uiState.appendLog("[ERROR] " + update.error);
                    } else if (generatedMode) {
                        f.uiState.appendLog("Generated tester total: " + f.generatedTotals.get(update.row));
                    } else {
                        f.uiState.appendLog("Original tester total: " + student.getTotal() + " / " + student.getMaximum());
                    }
                    f.uiState.updateProgress(update.row + 1, currentBatch.size());
                }
                if (!generatedMode) f.results.showSelectedStudent();
            }
            @Override
            protected void done() {
                try {
                    get();
                    if (generatedMode) {
                        f.progressBar.setString("Generated tester grading finished");
                    } else {
                        long attention = f.studentResults.stream().filter(StudentResult::needsAttention).count();
                        f.progressBar.setString("Original grading finished - " + attention + " student(s) need attention");
                    }
                    f.uiState.appendLog("=== GRADING FINISHED: " + testerLabel + " ===");
                } catch (Exception ex) {
                    f.progressBar.setString("Grading could not complete");
                    f.uiState.appendLog("[ERROR] " + ex);
                    JOptionPane.showMessageDialog(f, "Grading could not complete. Check the System Log.",
                            "Grading Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    f.uiState.setControlsEnabled(true);
                }
            }
        };

        worker.execute();
    }

    static class GradingUpdate {
        final int row;
        final List<QuestionResult> questions;
        final String error;
        GradingUpdate(int row, List<QuestionResult> questions, String error) {
            this.row = row; this.questions = questions; this.error = error;
        }
    }
}
