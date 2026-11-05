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
                new PythonValidationService(
                        new File("assessment/is1704/template")
                );

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() {

                JavaValidationService.Listener listener =
                        new JavaValidationService.Listener() {

                    @Override
                    public void onLog(String message) {
                        SwingUtilities.invokeLater(
                                () -> f.uiState.appendLog(message)
                        );
                    }

                    @Override
                    public void onSubmissionFinished(SubmissionResult result) {
                        SwingUtilities.invokeLater(
                                () -> f.results.addSubmissionResult(result)
                        );
                    }

                    @Override
                    public void onProgress(int processed, int total) {
                        SwingUtilities.invokeLater(
                                () -> f.uiState.updateProgress(processed, total)
                        );
                    }
                };

                // Choose the validation service based on assessment mode.
                if (pythonMode) {
                    pythonService.processSubmissions(
                            submissionsFolder,
                            listener
                    );
                } else {
                    service.processSubmissions(
                            submissionsFolder,
                            listener
                    );
                }

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    f.uiState.appendLog("Validation completed.");
                    saveCorrectionHistoryCsv();
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

    /** Persists correction history using the reporting layer, not the GUI. */
    private void saveCorrectionHistoryCsv() {
        java.nio.file.Path target = java.nio.file.Path.of("workspace", "correction_history.csv");
        try {
            new autograder.reporting.CorrectionHistoryWriter().export(target, f.studentResults);
            f.uiState.appendLog("Saved correction history: " + target);
        } catch (java.io.IOException ex) {
            f.uiState.appendLog("[WARNING] Could not update correction history: " + ex.getMessage());
            javax.swing.JOptionPane.showMessageDialog(f,
                "Could not update correction_history.csv. Close it in Excel and try again.",
                "Correction History Error", javax.swing.JOptionPane.WARNING_MESSAGE);
        }
    }
}
