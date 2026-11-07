package autograder.ui;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import autograder.model.*;
import autograder.validation.JavaValidationService;
import autograder.validation.PythonValidationService;

/** ValidationController responsibilities extracted from MainFrame. */
final class ValidationController {
    private final MainFrame f;
    ValidationController(MainFrame frame) { this.f = frame; }

    /** Validates student archives in the background and updates the UI. */
    void validateSubmissions() {
        File submissionsFolder = Path.of(f.submissionsField.getText().trim()).toAbsolutePath().normalize().toFile();
        f.submissionsField.setText(submissionsFolder.getAbsolutePath());

        if (!submissionsFolder.isDirectory()) {
            JOptionPane.showMessageDialog(f, "Please select a valid submissions folder.",
                    "Invalid Folder", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (f.studentResults.stream().anyMatch(StudentResult::hasManualScores)
                && JOptionPane.showConfirmDialog(f,
                        "Validating again clears the current marks and comments. Export them first if needed. Continue?",
                        "Clear Current Results", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        f.resultTable.clearSelection();
        f.tableModel.setRowCount(0);
        f.correctionTableModel.setRowCount(0);
        f.studentResults.clear();
        f.generatedTotals.clear();
        f.edgeCaseResults.clear();
        f.plagiarismResults.clear();
        f.mostFixesLabel.setText("Most fixes: -");
        f.correctionDetailArea.setText("Validation in progress...");
        f.searchField.setText("");
        f.statusFilter.setSelectedItem("All");
        f.detailsArea.setText("Validation in progress...");
        f.logArea.setText("");
        f.results.updateSummary();

        f.progressBar.setMinimum(0);
        f.progressBar.setMaximum(100);
        f.progressBar.setValue(0);
        f.progressBar.setString("Starting...");

        f.generatedTestersReadyForCurrentRun = false;
        f.uiState.setControlsEnabled(false);

        File workspace = new File("workspace");
        JavaValidationService service = new JavaValidationService(workspace);
        final boolean pythonMode = f.isPythonMode();

        PythonValidationService pythonService =
                new PythonValidationService(new File("assessment/python/template"));

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private java.util.Map<String, PlagarismCheck.Result> similarity = java.util.Map.of();
            private String similarityError;

            @Override
            protected Void doInBackground() {
                JavaValidationService.Listener listener = new JavaValidationService.Listener() {

                    @Override
                    public void onLog(String message) {
                        SwingUtilities.invokeLater(() -> f.uiState.appendLog(message));
                    }

                    @Override
                    public void onSubmissionFinished(SubmissionResult result) {
                        SwingUtilities.invokeLater(() -> f.results.addSubmissionResult(result));
                    }

                    @Override
                    public void onProgress(int processed, int total) {
                        SwingUtilities.invokeLater(() -> f.uiState.updateProgress(processed, total));
                    }
                };

                // Choose the validation service based on assessment mode.
                if (pythonMode) pythonService.processSubmissions(submissionsFolder, listener);
                else service.processSubmissions(submissionsFolder, listener);

                // Similarity checking is available only for Java assessments.
                if (!pythonMode) {
                    listener.onLog("Checking code similarity across matching Java questions...");

                    try {
                        similarity = new PlagarismCheck().check(submissionsFolder.toPath());
                    } catch (Exception ex) {
                        similarityError = ex.getMessage();
                    }
                }

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    if (!pythonMode) {
                        f.plagiarismResults.putAll(similarity);

                        for (int row = 0; row < f.studentResults.size(); row++)
                            f.results.refreshStudentRow(row);

                        f.results.showSelectedStudent();

                        if (similarityError != null) {
                            f.uiState.appendLog("[WARNING] Similarity check failed: " + similarityError);
                        } else {
                            long flagged = similarity.values().stream()
                                    .filter(result -> result.getStatus().equals("Suspected - review"))
                                    .count();

                            f.uiState.appendLog("Similarity check complete: " + flagged + " student(s) flagged for review.");
                        }
                    }

                    f.uiState.appendLog("Validation completed.");

                } catch (Exception ex) {
                    f.uiState.appendLog("[ERROR] Validation failed: " + ex);
                    f.progressBar.setString("Validation failed");

                } finally {
                    // Previously missing: buttons stayed disabled after validation.
                    f.uiState.setControlsEnabled(true);
                }
            }
        };

        worker.execute();
    }
}