package autograder.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import java.awt.Container;
import java.io.File;

/** MainFrameActions responsibilities extracted from MainFrame. */
final class MainFrameActions {
    private final MainFrame f;

    // Original CS102 GUI containers, remembered so Java layout can be restored after Python mode.
    private JPanel validatePanel;
    private JPanel originalButtonsPanel;
    private JPanel gradingFlowsPanel;
    private JPanel otherActionsPanel;

    MainFrameActions(MainFrame frame) { this.f = frame; }

    void connectActions() {
        f.assessmentMode.addActionListener(e -> switchAssessmentMode());
        f.browseButton.addActionListener(e -> chooseSubmissionsFolder());
        f.validateButton.addActionListener(e -> f.validation.validateSubmissions());
        f.clearLogButton.addActionListener(e -> f.logArea.setText(""));
        f.statusFilter.addActionListener(e -> f.results.applyFilters());

        f.searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { f.results.applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { f.results.applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { f.results.applyFilters(); }
        });

        f.resultTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) f.results.showSelectedStudent();
        });
        f.correctionTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) f.results.showSelectedCorrection();
        });

        f.gradeOriginalButton.addActionListener(e -> {
            if (f.isPythonMode()) f.pythonGrading.startPythonGrading();
            else f.javaGrading.startGrading(new File("assessment/java/testers"), "Original testers", false);
        });

        f.generateTestersButton.addActionListener(e -> f.testers.generateTesters());
        f.gradeGeneratedButton.addActionListener(e -> f.javaGrading.startGrading(new File("generated_testers"), "Generated testers", true));
        f.edgeCaseButton.addActionListener(e -> f.edgeCases.runEdgeCases());
        f.reviewButton.addActionListener(e -> f.review.reviewSubmission());
        f.manualButton.addActionListener(e -> f.manualScore.enterManualScore());
        f.exportButton.addActionListener(e -> f.export.exportResults());
    }

    void chooseSubmissionsFolder() {
        JFileChooser chooser = new JFileChooser(new File(f.submissionsField.getText()));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select submissions folder");
        if (chooser.showOpenDialog(f) == JFileChooser.APPROVE_OPTION)
            f.submissionsField.setText(chooser.getSelectedFile().getPath());
    }

    void switchAssessmentMode() {
        if (f.busy) return;

        boolean python = f.isPythonMode();
        f.setTitle(python ? "IS1704 Auto Grading System - Python" : "CS102 Auto Grading System - Java");
        f.titleLabel.setText(python ? "IS1704 Auto Grading System" : "CS102 Auto Grading System");

        f.studentResults.clear();
        f.generatedTotals.clear();
        f.edgeCaseResults.clear();
        f.plagiarismResults.clear();
        f.tableModel.setRowCount(0);
        f.correctionTableModel.setRowCount(0);
        f.resultTable.clearSelection();
        f.correctionTable.clearSelection();
        f.detailsArea.setText("");
        f.correctionDetailArea.setText("");
        f.logArea.setText("");
        f.generatedTestersReadyForCurrentRun = false;
        f.progressBar.setValue(0);
        f.progressBar.setString("Ready");
        f.results.updateSummary();

        if (python) {
            f.gradeOriginalButton.setText("Start Python Grading");
            showPythonLayout();
            f.logArea.append("IS1704 Python mode selected.\nValidate submissions, then start Python grading.\n");
        } else {
            f.gradeOriginalButton.setText("Start Grading (Original Testers)");
            showJavaLayout();
            f.logArea.append("CS102 Java mode selected.\n");
        }

        f.results.updatePlagiarismColumn();
        f.uiState.setControlsEnabled(true);
        f.revalidate();
        f.repaint();
    }

    private void rememberOriginalLayout() {
        if (validatePanel != null) return;

        validatePanel = (JPanel) f.validateButton.getParent();
        originalButtonsPanel = (JPanel) f.gradeOriginalButton.getParent();

        Container originalPanel = originalButtonsPanel.getParent();
        gradingFlowsPanel = (JPanel) originalPanel.getParent();
        otherActionsPanel = (JPanel) f.exportButton.getParent();
    }

    private void showPythonLayout() {
        rememberOriginalLayout();

        validatePanel.add(f.gradeOriginalButton);
        validatePanel.add(f.exportButton);
        gradingFlowsPanel.setVisible(false);
        otherActionsPanel.setVisible(false);

        TableColumn originalTotal = f.resultTable.getColumnModel().getColumn(5);
        TableColumn generatedTotal = f.resultTable.getColumnModel().getColumn(6);

        originalTotal.setHeaderValue("Total");
        generatedTotal.setMinWidth(0);
        generatedTotal.setMaxWidth(0);
        generatedTotal.setPreferredWidth(0);
        generatedTotal.setResizable(false);

        f.resultTable.getTableHeader().repaint();
    }

    private void showJavaLayout() {
        rememberOriginalLayout();

        originalButtonsPanel.add(f.gradeOriginalButton);
        otherActionsPanel.add(f.exportButton, 1);
        gradingFlowsPanel.setVisible(true);
        otherActionsPanel.setVisible(true);

        TableColumn originalTotal = f.resultTable.getColumnModel().getColumn(5);
        TableColumn generatedTotal = f.resultTable.getColumnModel().getColumn(6);

        originalTotal.setHeaderValue("Original Total");
        generatedTotal.setHeaderValue("Generated Total");

        generatedTotal.setMinWidth(80);
        generatedTotal.setMaxWidth(Integer.MAX_VALUE);
        generatedTotal.setPreferredWidth(105);
        generatedTotal.setResizable(true);

        f.resultTable.getTableHeader().repaint();
    }
}
