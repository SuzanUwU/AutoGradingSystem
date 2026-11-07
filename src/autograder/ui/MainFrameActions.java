package autograder.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import java.awt.Container;
import java.io.File;

/** MainFrameActions responsibilities extracted from MainFrame. */
final class MainFrameActions {
    // Stores a reference to MainFrame to access shared GUI components.
    private final MainFrame f;

    // Original CS102 GUI containers. These are remembered so the Java layout
    // can be restored exactly after switching back from Python mode.
    private JPanel validatePanel;
    private JPanel originalButtonsPanel;
    private JPanel gradingFlowsPanel;
    private JPanel otherActionsPanel;

    MainFrameActions(MainFrame frame) { this.f = frame; }

    // 1. Registers event listeners and delegates user actions to controllers.
    void connectActions() {
        f.assessmentMode.addActionListener(e -> switchAssessmentMode()); // for Python mode
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
        // gradeOriginalButton.addActionListener(e -> startGrading(new File("assessment/testers"), "Original testers", false));
        f.gradeOriginalButton.addActionListener(e -> {
            if (f.isPythonMode()) {
                f.pythonGrading.startPythonGrading();
            } else {
                f.javaGrading.startGrading(
                    new File("assessment/testers"),
                    "Original testers",
                    false
                );
            }
        });
        f.generateTestersButton.addActionListener(e -> f.testers.generateTesters());
        f.gradeGeneratedButton.addActionListener(e -> f.javaGrading.startGrading(new File("generated_testers"), "Generated testers", true));
        f.edgeCaseButton.addActionListener(e -> f.edgeCases.runEdgeCases());
        f.manualButton.addActionListener(e -> f.manualScore.enterManualScore());
        f.exportButton.addActionListener(e -> f.export.exportResults());
    }

    // 2. Opens a folder chooser to select the submissions directory.
    void chooseSubmissionsFolder() {
        JFileChooser chooser = new JFileChooser(new File(f.submissionsField.getText()));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select submissions folder");
        if (chooser.showOpenDialog(f) == JFileChooser.APPROVE_OPTION) {
            f.submissionsField.setText(chooser.getSelectedFile().getPath());
        }
    }

    // 3. Switches between Java and Python modes and resets the GUI state.
    void switchAssessmentMode() {
        if (f.busy) {
            return;
        }

        boolean python = f.isPythonMode();

        f.setTitle(python
                ? "IS1704 Auto Grading System - Python"
                : "CS102 Auto Grading System - Java");

        f.titleLabel.setText(python
                ? "IS1704 Auto Grading System"
                : "CS102 Auto Grading System");

        // Clear results from the previous assessment.
        f.studentResults.clear();
        f.generatedTotals.clear();
        f.edgeCaseResults.clear();

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

            f.logArea.append(
                    "IS1704 Python mode selected.\n"
                    + "Validate submissions, then start Python grading.\n"
            );
        } else {
            f.gradeOriginalButton.setText(
                    "Start Grading (Original Testers)"
            );
            showJavaLayout();

            f.logArea.append(
                    "CS102 Java mode selected.\n"
            );
        }

        f.uiState.setControlsEnabled(true);
        f.revalidate();
        f.repaint();
    }

    // Remembers the original CS102 panel structure before buttons are moved.
    private void rememberOriginalLayout() {
        if (validatePanel != null) {
            return;
        }

        validatePanel = (JPanel) f.validateButton.getParent();
        originalButtonsPanel = (JPanel) f.gradeOriginalButton.getParent();

        Container originalPanel = originalButtonsPanel.getParent();
        gradingFlowsPanel = (JPanel) originalPanel.getParent();

        otherActionsPanel = (JPanel) f.exportButton.getParent();
    }

    // IS1704 uses one compact row: Validate | Start Python Grading | Export Results.
    private void showPythonLayout() {
        rememberOriginalLayout();

        validatePanel.add(f.gradeOriginalButton);
        validatePanel.add(f.exportButton);

        gradingFlowsPanel.setVisible(false);
        otherActionsPanel.setVisible(false);

        // Python uses only one grading total, so hide the generated-tester column.
        TableColumn originalTotal = f.resultTable.getColumnModel().getColumn(5);
        TableColumn generatedTotal = f.resultTable.getColumnModel().getColumn(6);

        originalTotal.setHeaderValue("Total");

        generatedTotal.setMinWidth(0);
        generatedTotal.setMaxWidth(0);
        generatedTotal.setPreferredWidth(0);
        generatedTotal.setResizable(false);

        f.resultTable.getTableHeader().repaint();
    }

    // Restores the original CS102 layout exactly.
    private void showJavaLayout() {
        rememberOriginalLayout();

        originalButtonsPanel.add(f.gradeOriginalButton);
        otherActionsPanel.add(f.exportButton, 1);

        gradingFlowsPanel.setVisible(true);
        otherActionsPanel.setVisible(true);

        // Restore the original CS102 result columns.
        TableColumn originalTotal = f.resultTable.getColumnModel().getColumn(5);
        TableColumn generatedTotal = f.resultTable.getColumnModel().getColumn(6);

        originalTotal.setHeaderValue("Total (Original Testers)");

        generatedTotal.setMinWidth(15);
        generatedTotal.setMaxWidth(Integer.MAX_VALUE);
        generatedTotal.setPreferredWidth(155);
        generatedTotal.setResizable(true);

        f.resultTable.getTableHeader().repaint();
    }
}
