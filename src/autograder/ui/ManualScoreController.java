package autograder.ui;

import javax.swing.*;
import java.awt.*;
import autograder.model.*;


/** ManualScoreController responsibilities extracted from MainFrame. */
final class ManualScoreController {
    private final MainFrame f;
    ManualScoreController(MainFrame frame) { this.f = frame; }

    /** Handles enter manual score. */
    void enterManualScore() {
        int viewRow = f.resultTable.getSelectedRow();
        if (f.busy || viewRow < 0) return;
        int row = f.resultTable.convertRowIndexToModel(viewRow);
        StudentResult student = f.studentResults.get(row);
        if (student.getQuestions().isEmpty()) return;
        JComboBox<String> questions = new JComboBox<>(student.getQuestions().stream()
                .map(QuestionResult::getQuestionId).toArray(String[]::new));
        JTextField score = new JTextField(12);
        JTextArea comment = new JTextArea(4, 28);
        comment.setLineWrap(true);
        comment.setWrapStyleWord(true);
        JLabel range = new JLabel();
        Runnable loadQuestion = () -> {
            QuestionResult q = student.getQuestions().get(questions.getSelectedIndex());
            range.setText("Automatic: " + q.getScore() + " | Allowed: 0 to " + q.getMaxScore());
            score.setText(Double.toString(student.getFinalScore(q)));
            comment.setText(student.getComment(q.getQuestionId()));
        };
        questions.addActionListener(e -> loadQuestion.run());
        loadQuestion.run();
        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.add(new JLabel("Question:")); form.add(questions); form.add(range);
        form.add(new JLabel("Final score for this question:")); form.add(score);
        form.add(new JLabel("Reason (required):")); form.add(new JScrollPane(comment));
        while (JOptionPane.showConfirmDialog(f, form,
                "Manual Score - " + student.getSubmission().getUsername(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                double mark;
                try { mark = Double.parseDouble(score.getText().trim()); }
                catch (NumberFormatException ex) { throw new IllegalArgumentException("Enter a numeric score."); }
                student.setManualScore((String) questions.getSelectedItem(), mark, comment.getText());
                f.results.refreshStudentRow(row);
                f.results.showSelectedStudent();
                f.uiState.appendLog("Manual score saved: " + student.getSubmission().getUsername() + " / " + questions.getSelectedItem());
                return;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(f, ex.getMessage(), "Invalid Manual Score", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
