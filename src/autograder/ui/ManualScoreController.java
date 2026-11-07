package autograder.ui;

import javax.swing.*;
import java.awt.*;
import autograder.model.*;

/** ManualScoreController responsibilities extracted from MainFrame. */
final class ManualScoreController {
    private final MainFrame f;

    ManualScoreController(MainFrame frame) { this.f = frame; }

    /** Handles instructor review and manual score adjustment. */
    void enterManualScore() {
        int viewRow = f.resultTable.getSelectedRow();
        if (f.busy || viewRow < 0) return;

        int row = f.resultTable.convertRowIndexToModel(viewRow);
        StudentResult student = f.studentResults.get(row);

        if (student.getQuestions().isEmpty()) return;

        JComboBox<String> questions = new JComboBox<>(student.getQuestions().stream()
                .map(QuestionResult::getQuestionId)
                .toArray(String[]::new));

        JLabel automaticScore = new JLabel();
        JLabel currentScore = new JLabel();
        JLabel allowedRange = new JLabel();
        JLabel scoreStatus = new JLabel();

        JTextField score = new JTextField(10);

        JTextArea comment = new JTextArea(4, 28);
        comment.setLineWrap(true);
        comment.setWrapStyleWord(true);

        Runnable loadQuestion = () -> {
            QuestionResult q = student.getQuestions().get(questions.getSelectedIndex());

            automaticScore.setText(q.getScore() + " / " + q.getMaxScore());

            currentScore.setText(
                    student.getFinalScore(q) + " / " + q.getMaxScore()
            );

            allowedRange.setText("0 to " + q.getMaxScore());

            scoreStatus.setText(
                    student.hasManualScore(q.getQuestionId())
                            ? "Manual adjustment applied"
                            : "Using automatic score"
            );

            score.setText(
                    Double.toString(student.getFinalScore(q))
            );

            comment.setText(
                    student.getComment(q.getQuestionId())
            );
        };

        questions.addActionListener(e -> loadQuestion.run());
        loadQuestion.run();

        // Student information
        JPanel studentPanel = new JPanel(new GridLayout(0, 2, 8, 4));

        studentPanel.setBorder(
                BorderFactory.createTitledBorder("Student Review")
        );

        studentPanel.add(new JLabel("Student:"));
        studentPanel.add(new JLabel(
                student.getSubmission().getUsername()
        ));

        studentPanel.add(new JLabel("Validation status:"));
        studentPanel.add(new JLabel(
                student.getSubmission().getOverallStatus()
        ));

        studentPanel.add(new JLabel("Grading status:"));
        studentPanel.add(new JLabel(
                student.getGradingStatus()
        ));

        // Question score section
        JPanel scorePanel = new JPanel(new GridBagLayout());

        scorePanel.setBorder(
                BorderFactory.createTitledBorder("Question Score")
        );

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        addRow(scorePanel, gbc, 0, "Question:", questions);
        addRow(scorePanel, gbc, 1, "Automatic score:", automaticScore);
        addRow(scorePanel, gbc, 2, "Current final score:", currentScore);
        addRow(scorePanel, gbc, 3, "Allowed range:", allowedRange);
        addRow(scorePanel, gbc, 4, "Score status:", scoreStatus);
        addRow(scorePanel, gbc, 5, "New final score:", score);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.weightx = 1;

        scorePanel.add(
                new JLabel("Reason for adjustment (required):"),
                gbc
        );

        gbc.gridy = 7;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;

        scorePanel.add(
                new JScrollPane(comment),
                gbc
        );

        // Full dialog
        JPanel content = new JPanel();

        content.setLayout(
                new BoxLayout(content, BoxLayout.Y_AXIS)
        );

        content.add(studentPanel);

        // Warning for students needing review
        if ("Review".equals(student.getSubmission().getOverallStatus())
                || "Review".equals(student.getGradingStatus())) {

            JLabel reviewNotice = new JLabel(
                    "This submission is flagged for Review. "
                    + "Check the submission details before adjusting the score."
            );

            reviewNotice.setForeground(
                    new Color(180, 83, 9)
            );

            reviewNotice.setBorder(
                    BorderFactory.createEmptyBorder(8, 4, 8, 4)
            );

            content.add(reviewNotice);
        }

        content.add(scorePanel);

        while (JOptionPane.showConfirmDialog(
                f,
                content,
                "Manual Score Review - "
                        + student.getSubmission().getUsername(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        ) == JOptionPane.OK_OPTION) {

            try {
                double mark;

                try {
                    mark = Double.parseDouble(
                            score.getText().trim()
                    );
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Enter a numeric score."
                    );
                }

                student.setManualScore(
                        (String) questions.getSelectedItem(),
                        mark,
                        comment.getText()
                );

                f.results.refreshStudentRow(row);
                f.results.showSelectedStudent();

                f.uiState.appendLog(
                        "Manual score saved: "
                                + student.getSubmission().getUsername()
                                + " / "
                                + questions.getSelectedItem()
                );

                return;

            } catch (IllegalArgumentException ex) {

                JOptionPane.showMessageDialog(
                        f,
                        ex.getMessage(),
                        "Invalid Manual Score",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void addRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String label,
            Component value) {

        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.weightx = 0;

        panel.add(
                new JLabel(label),
                gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;

        panel.add(
                value,
                gbc
        );
    }
}