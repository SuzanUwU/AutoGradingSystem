package autograder.ui;

import autograder.model.StudentResult;
import autograder.model.SubmissionResult;

import javax.swing.*;
import java.util.List;

/** Handles professor decisions for submissions flagged as Review. */
final class ReviewController {
    private final MainFrame f;

    ReviewController(MainFrame frame) {
        this.f = frame;
    }

    void reviewSubmission() {
        int viewRow = f.resultTable.getSelectedRow();

        if (f.busy || viewRow < 0) {
            return;
        }

        int row = f.resultTable.convertRowIndexToModel(viewRow);

        StudentResult student = f.studentResults.get(row);
        SubmissionResult submission = student.getSubmission();

        if (!"Review".equals(submission.getOverallStatus())) {
            JOptionPane.showMessageDialog(
                    f,
                    "This submission is not flagged for Review.",
                    "Review Submission",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JTextArea findings = new JTextArea(10, 46);
        findings.setEditable(false);
        findings.setLineWrap(true);
        findings.setWrapStyleWord(true);
        findings.setText(buildFindings(submission));
        findings.setCaretPosition(0);

        JTextArea note = new JTextArea(4, 46);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setText(student.getReviewNote());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(new JLabel(
                "Student: " + submission.getUsername()
        ));

        panel.add(Box.createVerticalStrut(6));

        panel.add(new JLabel(
                "Validation findings:"
        ));

        panel.add(new JScrollPane(findings));

        panel.add(Box.createVerticalStrut(8));

        panel.add(new JLabel(
                "Instructor review note:"
        ));

        panel.add(new JScrollPane(note));

        Object[] options = student.isReviewResolved()
                ? new Object[] {
                        "Update Resolution",
                        "Reopen Review",
                        "Cancel"
                }
                : new Object[] {
                        "Mark Reviewed",
                        "Keep for Review",
                        "Cancel"
                };

        int choice = JOptionPane.showOptionDialog(
                f,
                panel,
                "Review Submission - " + submission.getUsername(),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            try {
                student.resolveReview(note.getText());

                f.uiState.appendLog(
                        "Review resolved: "
                                + submission.getUsername()
                );

            } catch (IllegalArgumentException
                     | IllegalStateException ex) {

                JOptionPane.showMessageDialog(
                        f,
                        ex.getMessage(),
                        "Review Note Required",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }

        } else if (choice == 1
                && student.isReviewResolved()) {

            student.reopenReview();

            f.uiState.appendLog(
                    "Review reopened: "
                            + submission.getUsername()
            );

        } else {
            return;
        }

        f.results.refreshStudentRow(row);
        f.results.showSelectedStudent();
        f.results.updateSummary();
    }

    private String buildFindings(
            SubmissionResult submission) {

        StringBuilder text = new StringBuilder();

        appendIssues(
                text,
                "Submission",
                submission.getSubmissionIssues()
        );

        appendIssues(
                text,
                "Q1",
                submission.getQ1Issues()
        );

        appendIssues(
                text,
                "Q2",
                submission.getQ2Issues()
        );

        appendIssues(
                text,
                "Q3",
                submission.getQ3Issues()
        );

        if (text.length() == 0) {
            text.append(
                    "No detailed validation findings were recorded. "
                    + "Check Full Details before resolving."
            );
        }

        return text.toString();
    }

    private void appendIssues(
            StringBuilder text,
            String scope,
            List<String> issues) {

        if (issues == null || issues.isEmpty()) {
            return;
        }

        text.append(scope).append(":\n");

        for (String issue : issues) {
            if (issue != null && !issue.isBlank()) {
                text.append(" - ")
                        .append(issue)
                        .append('\n');
            }
        }

        text.append('\n');
    }
}