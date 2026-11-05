package autograder.ui;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import autograder.model.*;
import autograder.EdgeCaseRunner;

/** EdgeCaseController responsibilities extracted from MainFrame. */
final class EdgeCaseController {
    private final MainFrame f;
    EdgeCaseController(MainFrame frame) { this.f = frame; }

    /** Runs optional edge-case tests without changing official grades. */
    void runEdgeCases() {
        File workspace = new File("workspace");
        File edgecases = new File("edgecases");
        File resources = new File("assessment/resources");

        if (workspace == null || !workspace.isDirectory() || !edgecases.isDirectory() || !resources.isDirectory()) {
            JOptionPane.showMessageDialog(f,
                    "Cannot find workspace, edgecases, or assessment/resources.\nRun the application from the project folder.",
                    "Missing Folder", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<SubmissionResult> currentBatch = new ArrayList<>();
        for (StudentResult student : f.studentResults) currentBatch.add(student.getSubmission());

        f.uiState.setControlsEnabled(false);
        f.progressBar.setValue(0);
        f.progressBar.setString("Starting edge-case tests...");
        f.uiState.appendLog("\n=== EDGE CASE TESTING STARTED ===");

        SwingWorker<List<EdgeCaseRunner.Result>, EdgeCaseUpdate> worker = new SwingWorker<List<EdgeCaseRunner.Result>, EdgeCaseUpdate>() {
            @Override
            protected List<EdgeCaseRunner.Result> doInBackground() {
                EdgeCaseRunner runner = new EdgeCaseRunner(edgecases, resources);
                ArrayList<EdgeCaseRunner.Result> results = new ArrayList<>();

                for (int row = 0; row < currentBatch.size(); row++) {
                    SubmissionResult submission = currentBatch.get(row);
                    EdgeCaseRunner.Result result;

                    if ("Failed".equals(submission.getOverallStatus())) {
                        result = EdgeCaseRunner.skipped(submission.getUsername(), "Submission validation failed");
                    } else {
                        File studentRoot = autograder.grading.WorkspaceLocator.findStudentRoot(workspace, submission.getUsername());
                        result = studentRoot == null
                                ? EdgeCaseRunner.skipped(submission.getUsername(), "Cannot uniquely locate the student folder")
                                : runner.run(studentRoot, submission.getUsername());
                    }

                    results.add(result);
                    publish(new EdgeCaseUpdate(row + 1, currentBatch.size(), result));
                }

                return results;
            }

            @Override
            protected void process(List<EdgeCaseUpdate> updates) {
                for (EdgeCaseUpdate update : updates) {
                    EdgeCaseRunner.Result result = update.result;
                    f.uiState.appendLog("Edge cases: " + result.getUsername() + " -> " + result.getSummary());

                    if (!result.isSkipped()) {
                        for (String qid : List.of("Q1a", "Q1b", "Q2a", "Q2b", "Q3")) {
                            EdgeCaseRunner.QuestionResult q = result.get(qid);
                            f.uiState.appendLog("  " + qid + ": " + q.getScoreText() + " (" + q.getStatus() + ")");
                        }
                    }

                    f.uiState.updateProgress(update.processed, update.total);
                }
            }

            @Override
            protected void done() {
                try {
                    List<EdgeCaseRunner.Result> results = get();
                    saveEdgeCaseResultsCsv(results);

                    int passed = results.stream().mapToInt(EdgeCaseRunner.Result::getPassed).sum();
                    int total = results.stream().mapToInt(EdgeCaseRunner.Result::getTotal).sum();

                    f.progressBar.setString("Edge cases finished - " + passed + "/" + total + " passed");
                    f.uiState.appendLog("=== EDGE CASE TESTING FINISHED ===");

                    JOptionPane.showMessageDialog(f,
                            "Edge-case testing finished.\nPassed: " + passed + " / " + total
                                    + "\nSaved: output\\edge_case_results.csv"
                                    + "\n\nOfficial grading scores were not changed.",
                            "Edge Cases Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    f.progressBar.setString("Edge-case testing could not complete");
                    f.uiState.appendLog("[ERROR] Edge cases: " + ex);

                    JOptionPane.showMessageDialog(f,
                            "Edge-case testing could not complete. Check the System Log.",
                            "Edge Case Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    f.uiState.setControlsEnabled(true);
                }
            }
        };

        worker.execute();
    }

    /** Exports diagnostic results without changing official grades. */
    private void saveEdgeCaseResultsCsv(List<EdgeCaseRunner.Result> results) throws IOException {
        new autograder.reporting.EdgeCaseReportWriter().export(
                Path.of("output", "edge_case_results.csv"), results);
        f.uiState.appendLog("Saved edge-case results: output/edge_case_results.csv");
    }

    static class EdgeCaseUpdate {
        final int processed;
        final int total;
        final EdgeCaseRunner.Result result;

        EdgeCaseUpdate(int processed, int total, EdgeCaseRunner.Result result) {
            this.processed = processed;
            this.total = total;
            this.result = result;
        }
    }
}