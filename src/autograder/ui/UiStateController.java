package autograder.ui;

/** UiStateController responsibilities extracted from MainFrame. */
final class UiStateController {
    private final MainFrame f;
    UiStateController(MainFrame frame) { this.f = frame; }

    /** Handles append log. */
    void appendLog(String message) {
        f.logArea.append(message + System.lineSeparator());
        f.logArea.setCaretPosition(f.logArea.getDocument().getLength());
    }

    /** Handles update progress. */
    void updateProgress(int processed, int total) {
        if (total <= 0) {
            f.progressBar.setMinimum(0);
            f.progressBar.setMaximum(100);
            f.progressBar.setValue(0);
            f.progressBar.setString("No submissions found");
            return;
        }
        f.progressBar.setMinimum(0);
        f.progressBar.setMaximum(total);
        f.progressBar.setValue(processed);
        f.progressBar.setString(processed + " / " + total + " processed");
    }

    /** Handles set controls enabled. */
    void setControlsEnabled(boolean enabled) {
        f.busy = !enabled;
        f.browseButton.setEnabled(enabled);

        // Prevent switching between CS102 and IS1704
        // while validation or grading is running.
        f.assessmentMode.setEnabled(enabled);

        f.validateButton.setEnabled(enabled);
        f.submissionsField.setEnabled(enabled);
        boolean hasValidSubmission = f.studentResults.stream()
                .anyMatch(student -> !"Failed".equals(student.getSubmission().getOverallStatus()));
        // gradeOriginalButton.setEnabled(enabled && hasValidSubmission);
        // generateTestersButton.setEnabled(enabled && hasValidSubmission);
        // gradeGeneratedButton.setEnabled(enabled && hasValidSubmission && generatedTestersReadyForCurrentRun && generatedTestersReady());
        // edgeCaseButton.setEnabled(enabled && hasValidSubmission);
        
        f.gradeOriginalButton.setEnabled(
            enabled && hasValidSubmission
        );
        f.generateTestersButton.setEnabled(
                enabled && !f.isPythonMode() && hasValidSubmission
        );

        f.gradeGeneratedButton.setEnabled(
                enabled && !f.isPythonMode()
                && hasValidSubmission
                && f.generatedTestersReadyForCurrentRun
                && f.testers.generatedTestersReady()
        );

        f.edgeCaseButton.setEnabled(
                enabled && !f.isPythonMode() && hasValidSubmission
        );
        
        f.exportButton.setEnabled(
            enabled
            && f.studentResults.stream().anyMatch(
                student -> !student.getQuestions().isEmpty()
            )
        );

        //====================
        f.results.showSelectedStudent();
    }
}
