package autograder.ui;

import autograder.model.QuestionResult;
import autograder.model.StudentResult;
import autograder.model.SubmissionResult;

import javax.swing.*;
import java.util.List;

/** Handles validation and grading review in one dialog. */
final class ReviewController {
    private final MainFrame f;

    ReviewController(MainFrame frame) { this.f = frame; }

    void reviewSubmission() {
        int viewRow = f.resultTable.getSelectedRow();
        if (f.busy || viewRow < 0) return;

        int row = f.resultTable.convertRowIndexToModel(viewRow);
        StudentResult student = f.studentResults.get(row);
        SubmissionResult submission = student.getSubmission();

        boolean validationIssue = "Review".equals(submission.getOverallStatus());
        boolean gradingIssue = student.hasGradingReviewIssue();

        if (!validationIssue && !gradingIssue) {
            JOptionPane.showMessageDialog(
                    f,
                    "This student has no validation or grading review issue.",
                    "Review Issues",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("Student: " + submission.getUsername()));
        panel.add(Box.createVerticalStrut(8));

        // =====================================================
        // VALIDATION REVIEW
        // =====================================================

        JCheckBox validationResolved = null;
        JTextArea validationNote = null;

        if (validationIssue) {
            JTextArea findings = createReadOnlyArea(buildValidationFindings(submission));
            validationResolved = new JCheckBox("Validation review resolved", student.isReviewResolved());
            validationNote = createNoteArea(student.getReviewNote());

            panel.add(sectionTitle("Validation Review"));
            panel.add(new JScrollPane(findings));
            panel.add(validationResolved);
            panel.add(new JLabel("Instructor note:"));
            panel.add(new JScrollPane(validationNote));
            panel.add(Box.createVerticalStrut(10));
        }

        // =====================================================
        // GRADING REVIEW
        // =====================================================

        JCheckBox gradingResolved = null;
        JTextArea gradingNote = null;

        if (gradingIssue) {
            JTextArea findings = createReadOnlyArea(buildGradingFindings(student));
            gradingResolved = new JCheckBox("Grading review resolved", student.isGradingReviewResolved());
            gradingNote = createNoteArea(student.getGradingReviewNote());

            panel.add(sectionTitle("Grading Review"));
            panel.add(new JScrollPane(findings));
            panel.add(gradingResolved);
            panel.add(new JLabel("Instructor note:"));
            panel.add(new JScrollPane(gradingNote));
        }

        int choice = JOptionPane.showConfirmDialog(
                f,
                panel,
                "Review Issues - " + submission.getUsername(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) return;

        try {
            // Validation review
            if (validationIssue) {
                if (validationResolved.isSelected()) {
                    student.resolveReview(validationNote.getText());
                } else if (student.isReviewResolved()) {
                    student.reopenReview();
                }
            }

            // Grading review
            if (gradingIssue) {
                if (gradingResolved.isSelected()) {
                    student.resolveGradingReview(gradingNote.getText());
                } else if (student.isGradingReviewResolved()) {
                    student.reopenGradingReview();
                }
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(f, ex.getMessage(), "Review Note Required", JOptionPane.ERROR_MESSAGE);
            return;
        }

        f.uiState.appendLog("Review updated: " + submission.getUsername());
        f.results.refreshStudentRow(row);
        f.results.showSelectedStudent();
        f.results.updateSummary();
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
        return label;
    }

    private JTextArea createReadOnlyArea(String text) {
        JTextArea area = new JTextArea(7, 46);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setText(text);
        area.setCaretPosition(0);
        return area;
    }

    private JTextArea createNoteArea(String text) {
        JTextArea area = new JTextArea(3, 46);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setText(text);
        return area;
    }

    private String buildValidationFindings(SubmissionResult submission) {
        StringBuilder text = new StringBuilder();

        appendIssues(text, "Submission", submission.getSubmissionIssues());
        appendIssues(text, "Q1", submission.getQ1Issues());
        appendIssues(text, "Q2", submission.getQ2Issues());
        appendIssues(text, "Q3", submission.getQ3Issues());

        if (text.length() == 0) {
            text.append("No detailed validation findings were recorded. Check Full Details before resolving.");
        }

        return text.toString();
    }

    private String buildGradingFindings(StudentResult student) {
        StringBuilder text = new StringBuilder();

        if (!student.getIdentityWarning().isEmpty()) {
            text.append("Identity warning: ").append(student.getIdentityWarning()).append("\n\n");
        }

        for (QuestionResult question : student.getQuestions()) {
            if (!"Graded successfully".equals(question.getStatusMessage())) {
                text.append(question).append('\n');
            }
        }

        if (text.length() == 0) {
            text.append("No detailed grading finding was recorded. Check Full Details before resolving.");
        }

        return text.toString();
    }

    private void appendIssues(StringBuilder text, String scope, List<String> issues) {
        if (issues == null || issues.isEmpty()) return;

        text.append(scope).append(":\n");
        for (String issue : issues) {
            if (issue != null && !issue.isBlank()) text.append(" - ").append(issue).append('\n');
        }
        text.append('\n');
    }
}
