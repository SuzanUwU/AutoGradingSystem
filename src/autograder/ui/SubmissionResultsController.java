package autograder.ui;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import autograder.model.*;

/** SubmissionResultsController responsibilities extracted from MainFrame. */
final class SubmissionResultsController {
    private final MainFrame f;
    private javax.swing.table.TableColumn plagiarismColumn;
    private javax.swing.table.TableColumn instructorReviewColumn;

    SubmissionResultsController(MainFrame frame) { this.f = frame; }

    /** Keep Java-only result columns exclusive to the Java assessment view. */
    void updatePlagiarismColumn() {
        int plagiarismViewColumn =
                f.resultTable.convertColumnIndexToView(7);

        if (plagiarismViewColumn >= 0) {
            plagiarismColumn =
                    f.resultTable.getColumnModel()
                            .getColumn(plagiarismViewColumn);

            if (f.isPythonMode()) {
                f.resultTable.removeColumn(plagiarismColumn);
            }

        } else if (!f.isPythonMode()
                && plagiarismColumn != null) {

            f.resultTable.addColumn(plagiarismColumn);
        }

        int reviewViewColumn =
                f.resultTable.convertColumnIndexToView(8);

        if (reviewViewColumn >= 0) {
            instructorReviewColumn =
                    f.resultTable.getColumnModel()
                            .getColumn(reviewViewColumn);

            if (f.isPythonMode()) {
                f.resultTable.removeColumn(instructorReviewColumn);
            }

        } else if (!f.isPythonMode()
                && instructorReviewColumn != null) {

            f.resultTable.addColumn(instructorReviewColumn);
        }
    }

    /** Handles add submission result. */
    void addSubmissionResult(SubmissionResult result) {
        StudentResult student = new StudentResult(result);
        f.studentResults.add(student);
        f.generatedTotals.add("");

        f.tableModel.addRow(new Object[]{
                result.getUsername(),
                result.getStructureStatus(),
                result.getHeaderStatus(),
                result.getOverallStatus(),
                "Not graded",
                "",
                "",
                f.isPythonMode() ? "" : "Checking...",
                "Review".equals(result.getOverallStatus())
                        ? "Pending"
                        : "-"
        });

        updateSummary();
        refreshCorrectionTable();

        if (f.resultTable.getSelectedRow() == -1 && f.resultTable.getRowCount() > 0)
            f.resultTable.setRowSelectionInterval(0, 0);
    }

    /** Handles show selected student. */
    void showSelectedStudent() {
        int viewRow = f.resultTable.getSelectedRow();

        if (viewRow < 0) {
            f.reviewButton.setEnabled(false);
            f.manualButton.setEnabled(false);
            f.detailsArea.setText("Select a student to view submission details.");
            f.correctionDetailArea.setText("Select a student to see exact correction files and anomalies.");
            return;
        }

        int modelRow = f.resultTable.convertRowIndexToModel(viewRow);
        if (modelRow >= f.studentResults.size()) return;

        StudentResult student = f.studentResults.get(modelRow);
        String details = student.getDetails();

        if (!f.isPythonMode()) {
            PlagarismCheck.Result similarity = f.plagiarismResults.get(student.getSubmission().getUsername());
            String similarityDetails = similarity == null
                    ? "No similarity result available yet."
                    : similarity.getStatus() + "\n" + similarity.getDetails();

            details += "\n\nCODE SIMILARITY CHECK\n" + similarityDetails;
        }

        f.detailsArea.setText(details);
        showCorrectionSummary(student.getSubmission());
        selectCorrectionRow(student.getSubmission().getUsername());
        f.reviewButton.setEnabled(
                !f.busy
                && "Review".equals(
                        student.getSubmission().getOverallStatus()
                )
        );

        f.manualButton.setEnabled(
                !f.busy
                && !student.getQuestions().isEmpty()
        );
        f.detailsArea.setCaretPosition(0);
    }

    /** Handles refresh correction table. */
    void refreshCorrectionTable() {
        f.correctionTableModel.setRowCount(0);

        for (StudentResult student : f.studentResults) {
            SubmissionResult s = student.getSubmission();

            f.correctionTableModel.addRow(new Object[]{
                    s.getUsername(), s.getFolderFixCount(), s.getNameFixCount(), s.getEmailFixCount(),
                    s.getOpenIssueCount(), s.getAnomalyCount(), s.getTotalFixCount(), s.getOverallStatus()
            });
        }

        f.correctionSorter.setSortKeys(Arrays.asList(
                new RowSorter.SortKey(6, SortOrder.DESCENDING),
                new RowSorter.SortKey(0, SortOrder.ASCENDING)));

        updateMostFixesLabel();
    }

    /** Handles update most fixes label. */
    void updateMostFixesLabel() {
        if (f.studentResults.isEmpty()) {
            f.mostFixesLabel.setText("Most fixes: -");
            return;
        }

        int max = f.studentResults.stream().mapToInt(s -> s.getSubmission().getTotalFixCount()).max().orElse(0);

        if (max == 0) {
            f.mostFixesLabel.setText("Most fixes: None");
            return;
        }

        ArrayList<String> names = new ArrayList<>();

        for (StudentResult student : f.studentResults)
            if (student.getSubmission().getTotalFixCount() == max) names.add(student.getSubmission().getUsername());

        f.mostFixesLabel.setText("Most fixes: " + String.join(", ", names) + " (" + max + ")");
    }

    /** Handles show selected correction. */
    void showSelectedCorrection() {
        int viewRow = f.correctionTable.getSelectedRow();
        if (viewRow < 0) return;

        int modelRow = f.correctionTable.convertRowIndexToModel(viewRow);
        String username = String.valueOf(f.correctionTableModel.getValueAt(modelRow, 0));

        for (StudentResult student : f.studentResults) {
            if (student.getSubmission().getUsername().equals(username)) {
                showCorrectionSummary(student.getSubmission());
                return;
            }
        }
    }

    /** Handles show correction summary. */
    void showCorrectionSummary(SubmissionResult submission) {
        StringBuilder text = new StringBuilder();

        text.append(submission.getUsername()).append(" | Total fixes: ").append(submission.getTotalFixCount())
                .append(" | Status: ").append(submission.getOverallStatus()).append('\n');

        text.append("Folder (").append(submission.getFolderFixCount()).append("): ")
                .append(joinDetails(submission.getFolderFixDetails())).append('\n');

        text.append("Name (").append(submission.getNameFixCount()).append("): ")
                .append(joinDetails(submission.getNameFixDetails())).append('\n');

        text.append("Email (").append(submission.getEmailFixCount()).append("): ")
                .append(joinDetails(submission.getEmailFixDetails())).append('\n');

        text.append("Open Issues (").append(submission.getOpenIssueCount()).append("): ")
                .append(joinDetails(submission.getOpenIssueDetails())).append('\n');

        text.append("Anomalies (").append(submission.getAnomalyCount()).append("): ")
                .append(joinDetails(submission.getAnomalyDetails()));

        f.correctionDetailArea.setText(text.toString());
        f.correctionDetailArea.setCaretPosition(0);
    }

    /** Handles join details. */
    String joinDetails(List<String> details) {
        return details.isEmpty() ? "None" : String.join(", ", details);
    }

    /** Handles select correction row. */
    void selectCorrectionRow(String username) {
        for (int modelRow = 0; modelRow < f.correctionTableModel.getRowCount(); modelRow++) {
            if (username.equals(f.correctionTableModel.getValueAt(modelRow, 0))) {
                int viewRow = f.correctionTable.convertRowIndexToView(modelRow);

                if (viewRow >= 0 && f.correctionTable.getSelectedRow() != viewRow)
                    f.correctionTable.setRowSelectionInterval(viewRow, viewRow);

                return;
            }
        }
    }

    /** Handles apply filters. */
    void applyFilters() {
        String search = f.searchField.getText().trim();
        String status = String.valueOf(f.statusFilter.getSelectedItem());

        ArrayList<RowFilter<Object, Object>> filters = new ArrayList<>();

        if (!search.isEmpty()) filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(search), 0));

        if (status.equals("Needs Attention")) {
            filters.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<? extends Object, ? extends Object> entry) {
                    return f.studentResults.get((Integer) entry.getIdentifier()).needsAttention();
                }
            });
        } else if (!status.equals("All")) {
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(status) + "$", 3));
        }

        f.sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));

        if (f.resultTable.getRowCount() > 0) f.resultTable.setRowSelectionInterval(0, 0);
        else f.detailsArea.setText("No students match the current search/filter.");
    }

    /** Handles update summary. */
    void updateSummary() {
        f.totalLabel.setText("Total: " + f.studentResults.size());
        f.validLabel.setText("Valid: " + countStatus("Valid"));
        f.correctedLabel.setText("Corrected: " + countStatus("Corrected"));
        f.reviewLabel.setText("Review: " + countStatus("Review"));
        f.failedLabel.setText("Failed: " + countStatus("Failed"));
    }

    /** Handles count status. */
    int countStatus(String status) {
        int count = 0;
        for (StudentResult student : f.studentResults)
            if (status.equals(student.getSubmission().getOverallStatus())) count++;

        return count;
    }

    /** Handles refresh student row. */
    void refreshStudentRow(int row) {
        StudentResult student = f.studentResults.get(row);
        SubmissionResult submission = student.getSubmission();

        f.tableModel.setValueAt(submission.getStructureStatus(), row, 1);
        f.tableModel.setValueAt(submission.getHeaderStatus(), row, 2);
        f.tableModel.setValueAt(submission.getOverallStatus(), row, 3);
        f.tableModel.setValueAt(student.getGradingStatus(), row, 4);
        f.tableModel.setValueAt(student.getQuestions().isEmpty() ? "" : student.getTotal() + " / " + student.getMaximum(), row, 5);
        f.tableModel.setValueAt(row < f.generatedTotals.size() ? f.generatedTotals.get(row) : "", row, 6);

        if (!f.isPythonMode()) {
            PlagarismCheck.Result similarity = f.plagiarismResults.get(submission.getUsername());
            f.tableModel.setValueAt(similarity == null ? "Could not check" : similarity.getStatus(), row, 7);
        }

        String instructorReview = "-";

        if ("Review".equals(
                submission.getOverallStatus())) {

            instructorReview =
                    student.isReviewResolved()
                            ? "Resolved"
                            : "Pending";
        }

        f.tableModel.setValueAt(
                instructorReview,
                row,
                8
        );
    }
}