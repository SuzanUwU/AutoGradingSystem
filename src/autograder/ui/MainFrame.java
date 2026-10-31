package autograder.ui;
import autograder.service.AutoGradingService;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import autograder.reporting.CsvExporter;
import autograder.reporting.PdfReportExporter;
import java.util.regex.Pattern;
import autograder.compilation.*;
import autograder.execution.*;
import autograder.grading.*;
import autograder.model.*;
import autograder.EdgeCaseRunner;

public class MainFrame extends JFrame {
    private final JTextField submissionsField = new JTextField(Path.of(System.getProperty("user.dir"), "submissions").toAbsolutePath().normalize().toString(), 35);
    private final JTextField searchField = new JTextField(22);
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[] {"All", "Needs Attention", "Valid", "Corrected", "Review", "Failed"});
    private final JButton browseButton = new JButton("Browse");
    private final JButton validateButton = new JButton("Validate Submissions");
    private final JButton startGradingButton = new JButton("Start Grading");
    private final JButton edgeCaseButton = new JButton("Run Edge Cases");
    private final JButton exportButton = new JButton("Export Results");
    private final JButton manualButton = new JButton("Enter Manual Score");
    private boolean busy;
    private final ArrayList<StudentResult> studentResults = new ArrayList<>();
    private final JButton clearLogButton = new JButton("Clear Log");
    private final JLabel totalLabel = new JLabel("Total: 0");
    private final JLabel validLabel = new JLabel("Valid: 0");
    private final JLabel correctedLabel = new JLabel("Corrected: 0");
    private final JLabel reviewLabel = new JLabel("Review: 0");
    private final JLabel failedLabel = new JLabel("Failed: 0");
    private final JTextArea detailsArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();
    private final JProgressBar progressBar = new JProgressBar();
    private final DefaultTableModel tableModel;
    private final JTable resultTable;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final DefaultTableModel correctionTableModel;
    private final JTable correctionTable;
    private final TableRowSorter<DefaultTableModel> correctionSorter;
    private final JTextArea correctionDetailArea = new JTextArea(6, 40);
    private final JLabel mostFixesLabel = new JLabel("Most fixes: -");
    
    public MainFrame() {
        setTitle("CS102 Auto Grading System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 680));
        setSize(1200, 760);
        setLocationRelativeTo(null);
        tableModel = new DefaultTableModel(new Object[] {"Student", "Structure", "Headers", "Status", "Grading", "Final Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        sorter.setSortsOnUpdates(true);
        resultTable.setRowSorter(sorter);

        correctionTableModel = new DefaultTableModel(
                new Object[] {"Student", "Folder", "Name", "Email", "Open Issues", "Anomalies", "Total Fixes", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex >= 1 && columnIndex <= 6) return Integer.class;
                return String.class;
            }
        };
        correctionTable = new JTable(correctionTableModel);
        correctionSorter = new TableRowSorter<>(correctionTableModel);
        correctionSorter.setSortsOnUpdates(true);
        correctionTable.setRowSorter(correctionSorter);
        applyStatusColors();
        buildInterface();
        connectActions();
    }
    private void buildInterface() {
        setLayout(new BorderLayout(10, 10));
        JLabel titleLabel = new JLabel("CS102 Auto Grading System");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 15, 0, 15));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        mainPanel.add(createSetupPanel(), BorderLayout.NORTH);
        mainPanel.add(createTabs(), BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 12, 15));
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createSetupPanel() {
        JPanel container = new JPanel(new BorderLayout(8, 8));
        JPanel setupPanel = new JPanel(new GridBagLayout());
        setupPanel.setBorder(BorderFactory.createTitledBorder("Submission Setup"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        setupPanel.add(new JLabel("Submissions Folder:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        setupPanel.add(submissionsField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        setupPanel.add(browseButton, gbc);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.add(validateButton);
        buttonPanel.add(startGradingButton);
        buttonPanel.add(edgeCaseButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(manualButton);
        manualButton.setEnabled(false);
        startGradingButton.setEnabled(false);
        edgeCaseButton.setEnabled(false);
        exportButton.setEnabled(false);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        setupPanel.add(buttonPanel, gbc);
        container.add(setupPanel, BorderLayout.NORTH);
        container.add(createSummaryPanel(), BorderLayout.SOUTH);
        return container;
    }
    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 8, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Submission Summary"));
        for (JLabel label : new JLabel[] {totalLabel, validLabel, correctedLabel, reviewLabel, failedLabel}) {
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
            panel.add(label);
        }
        validLabel.setForeground(new Color(34, 120, 58));
        correctedLabel.setForeground(new Color(37, 99, 235));
        reviewLabel.setForeground(new Color(180, 83, 9));
        failedLabel.setForeground(new Color(185, 28, 28));
        return panel;
    }
    private DefaultTableCellRenderer createStatusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value == null ? "" : value.toString();
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setFont(table.getFont().deriveFont(Font.BOLD));
                label.setForeground(Color.BLACK);
                label.setBackground(Color.WHITE);

                if (status.equals("Valid")) {
                    label.setBackground(new Color(220, 245, 224));
                    label.setForeground(new Color(28, 94, 45));
                } else if (status.equals("Corrected")) {
                    label.setBackground(new Color(219, 234, 254));
                    label.setForeground(new Color(30, 64, 175));
                } else if (status.equals("Review")) {
                    label.setBackground(new Color(254, 243, 199));
                    label.setForeground(new Color(146, 64, 14));
                } else if (status.equals("Failed")) {
                    label.setBackground(new Color(254, 226, 226));
                    label.setForeground(new Color(153, 27, 27));
                }

                if (isSelected) label.setBorder(BorderFactory.createLineBorder(table.getSelectionBackground(), 2));
                else label.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
                return label;
            }
        };
    }

    private void applyStatusColors() {
        DefaultTableCellRenderer statusRenderer = createStatusRenderer();
        resultTable.getColumnModel().getColumn(1).setCellRenderer(statusRenderer);
        resultTable.getColumnModel().getColumn(2).setCellRenderer(statusRenderer);
        resultTable.getColumnModel().getColumn(3).setCellRenderer(statusRenderer);

        DefaultTableCellRenderer gradingRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String gradingStatus = value == null ? "" : value.toString();
                label.setFont(table.getFont().deriveFont(gradingStatus.equals("Review") ? Font.BOLD : Font.PLAIN));
                return label;
            }
        };
        resultTable.getColumnModel().getColumn(4).setCellRenderer(gradingRenderer);
        correctionTable.getColumnModel().getColumn(7).setCellRenderer(createStatusRenderer());
    }

    private JTabbedPane createTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Submissions", createSubmissionsPanel());
        tabs.addTab("System Log", createLogPanel());
        return tabs;
    }
    private JPanel createSubmissionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Search student:"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("Status:"));
        filterPanel.add(statusFilter);
        panel.add(filterPanel, BorderLayout.NORTH);
        resultTable.setFillsViewportHeight(true);
        resultTable.setRowHeight(25);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Students"));
        tablePanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detailsArea.setText("Select a student to view submission details.");
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Submission Details"));
        JTabbedPane detailsTabs = new JTabbedPane();
        detailsTabs.addTab("Correction Summary", createCorrectionPanel());
        detailsTabs.addTab("Full Details", new JScrollPane(detailsArea));
        detailsPanel.add(detailsTabs, BorderLayout.CENTER);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, detailsPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(560);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }
    private JPanel createCorrectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        mostFixesLabel.setFont(mostFixesLabel.getFont().deriveFont(Font.BOLD));
        panel.add(mostFixesLabel, BorderLayout.NORTH);

        correctionTable.setFillsViewportHeight(true);
        correctionTable.setRowHeight(24);
        correctionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        correctionTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        correctionTable.getColumnModel().getColumn(1).setPreferredWidth(55);
        correctionTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        correctionTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        correctionTable.getColumnModel().getColumn(4).setPreferredWidth(75);
        correctionTable.getColumnModel().getColumn(5).setPreferredWidth(70);
        correctionTable.getColumnModel().getColumn(6).setPreferredWidth(70);
        correctionTable.getColumnModel().getColumn(7).setPreferredWidth(85);

        DefaultTableCellRenderer countRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int count = value instanceof Number ? ((Number) value).intValue() : 0;
                int modelColumn = table.convertColumnIndexToModel(column);

                label.setHorizontalAlignment(JLabel.CENTER);
                label.setFont(table.getFont().deriveFont(Font.PLAIN));
                label.setForeground(Color.BLACK);
                label.setBackground(Color.WHITE);

                if (modelColumn == 4 || modelColumn == 5) {
                    // Open Issues / Anomalies: any positive number needs attention.
                    if (count == 1) {
                        label.setBackground(new Color(254, 243, 199));
                        label.setForeground(new Color(146, 64, 14));
                        label.setFont(table.getFont().deriveFont(Font.BOLD));
                    } else if (count >= 2) {
                        label.setBackground(new Color(254, 226, 226));
                        label.setForeground(new Color(153, 27, 27));
                        label.setFont(table.getFont().deriveFont(Font.BOLD));
                    }
                } else {
                    // Folder / Name / Email / Total Fixes: stronger colour = more automatic fixes.
                    if (count >= 1 && count <= 2) {
                        label.setBackground(new Color(254, 249, 195));
                    } else if (count >= 3 && count <= 5) {
                        label.setBackground(new Color(254, 215, 170));
                        label.setForeground(new Color(154, 52, 18));
                        label.setFont(table.getFont().deriveFont(Font.BOLD));
                    } else if (count >= 6) {
                        label.setBackground(new Color(254, 202, 202));
                        label.setForeground(new Color(153, 27, 27));
                        label.setFont(table.getFont().deriveFont(Font.BOLD));
                    }
                }

                if (isSelected) label.setBorder(BorderFactory.createLineBorder(table.getSelectionBackground(), 2));
                else label.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
                return label;
            }
        };
        for (int column = 1; column <= 6; column++) correctionTable.getColumnModel().getColumn(column).setCellRenderer(countRenderer);
        correctionTable.getColumnModel().getColumn(7).setCellRenderer(createStatusRenderer());

        panel.add(new JScrollPane(correctionTable), BorderLayout.CENTER);

        correctionDetailArea.setEditable(false);
        correctionDetailArea.setLineWrap(true);
        correctionDetailArea.setWrapStyleWord(true);
        correctionDetailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        correctionDetailArea.setText("Select a student to see exact correction files and anomalies.");
        JScrollPane correctionDetailsScroll = new JScrollPane(correctionDetailArea);
        correctionDetailsScroll.setBorder(BorderFactory.createTitledBorder("Selected Student"));
        correctionDetailsScroll.setPreferredSize(new Dimension(0, 150));
        panel.add(correctionDetailsScroll, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createLogPanel() {
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearLogButton);
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }
    private void connectActions() {
        browseButton.addActionListener(e -> chooseSubmissionsFolder());
        validateButton.addActionListener(e -> validateSubmissions());
        clearLogButton.addActionListener(e -> logArea.setText(""));
        statusFilter.addActionListener(e -> applyFilters());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });
        resultTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedStudent();
        });
        correctionTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedCorrection();
        });
        startGradingButton.addActionListener(e -> startGrading());
        edgeCaseButton.addActionListener(e -> runEdgeCases());
        manualButton.addActionListener(e -> enterManualScore());
        exportButton.addActionListener(e -> exportResults());
    }
    private void chooseSubmissionsFolder() {
        JFileChooser chooser = new JFileChooser(new File(submissionsField.getText()));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select submissions folder");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            submissionsField.setText(chooser.getSelectedFile().getPath());
        }
    }
    private void validateSubmissions() {
        File submissionsFolder = Path.of(submissionsField.getText().trim()).toAbsolutePath().normalize().toFile();
        submissionsField.setText(submissionsFolder.getAbsolutePath());
        if (!submissionsFolder.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Please select a valid submissions folder.",
                    "Invalid Folder", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (studentResults.stream().anyMatch(StudentResult::hasManualScores)
                && JOptionPane.showConfirmDialog(this,
                        "Validating again clears the current marks and comments. Export them first if needed. Continue?",
                        "Clear Current Results", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        resultTable.clearSelection();
        tableModel.setRowCount(0);
        correctionTableModel.setRowCount(0);
        studentResults.clear();
        mostFixesLabel.setText("Most fixes: -");
        correctionDetailArea.setText("Validation in progress...");
        searchField.setText("");
        statusFilter.setSelectedItem("All");
        detailsArea.setText("Validation in progress...");
        logArea.setText("");
        updateSummary();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setString("Starting...");
        setControlsEnabled(false);
        AutoGradingService service = new AutoGradingService(new File("workspace"));
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                service.processSubmissions(submissionsFolder, new AutoGradingService.Listener() {
                    @Override
                    public void onLog(String message) {
                        SwingUtilities.invokeLater(() -> appendLog(message));
                    }
                    @Override
                    public void onSubmissionFinished(SubmissionResult result) {
                        SwingUtilities.invokeLater(() -> addSubmissionResult(result));
                    }
                    @Override
                    public void onProgress(int processed, int total) {
                        SwingUtilities.invokeLater(() -> updateProgress(processed, total));
                    }
                });
                return null;
            }
            @Override
            protected void done() {
                setControlsEnabled(true);
                try {
                    get();
                    if (!studentResults.isEmpty()) {
                        refreshCorrectionTable();
                        saveCorrectionHistoryCsv();
                        progressBar.setString("Validation complete - click Start Grading");
                    } else {
                        detailsArea.setText("No submissions were processed.\nCheck the System Log for details.");
                        progressBar.setString("No submissions processed");
                    }
                } catch (Exception e) {
                    progressBar.setString("Validation failed");
                    JOptionPane.showMessageDialog(MainFrame.this, "Unexpected error: " + e.getMessage(),
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    private void addSubmissionResult(SubmissionResult result) {
        StudentResult student = new StudentResult(result);
        studentResults.add(student);
        tableModel.addRow(new Object[] {
                result.getUsername(), result.getStructureStatus(), result.getHeaderStatus(), result.getOverallStatus(), "Not graded", ""
        });
        updateSummary();
        refreshCorrectionTable();
        if (resultTable.getSelectedRow() == -1 && resultTable.getRowCount() > 0) {
            resultTable.setRowSelectionInterval(0, 0);
        }
    }
    private void showSelectedStudent() {
        int viewRow = resultTable.getSelectedRow();
        if (viewRow < 0) {
            manualButton.setEnabled(false);
            detailsArea.setText("Select a student to view submission details.");
            correctionDetailArea.setText("Select a student to see exact correction files and anomalies.");
            return;
        }
        int modelRow = resultTable.convertRowIndexToModel(viewRow);
        if (modelRow >= studentResults.size()) return;
        StudentResult student = studentResults.get(modelRow);
        detailsArea.setText(student.getDetails());
        showCorrectionSummary(student.getSubmission());
        selectCorrectionRow(student.getSubmission().getUsername());
        manualButton.setEnabled(!busy && !student.getQuestions().isEmpty());
        detailsArea.setCaretPosition(0);
    }
    private void refreshCorrectionTable() {
        correctionTableModel.setRowCount(0);
        for (StudentResult student : studentResults) {
            SubmissionResult s = student.getSubmission();
            correctionTableModel.addRow(new Object[] {
                    s.getUsername(), s.getFolderFixCount(), s.getNameFixCount(), s.getEmailFixCount(),
                    s.getOpenIssueCount(), s.getAnomalyCount(), s.getTotalFixCount(), s.getOverallStatus()
            });
        }
        correctionSorter.setSortKeys(Arrays.asList(
                new RowSorter.SortKey(6, SortOrder.DESCENDING),
                new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        updateMostFixesLabel();
    }

    private void updateMostFixesLabel() {
        if (studentResults.isEmpty()) {
            mostFixesLabel.setText("Most fixes: -");
            return;
        }
        int max = studentResults.stream().mapToInt(s -> s.getSubmission().getTotalFixCount()).max().orElse(0);
        if (max == 0) {
            mostFixesLabel.setText("Most fixes: None");
            return;
        }
        ArrayList<String> names = new ArrayList<>();
        for (StudentResult student : studentResults) {
            if (student.getSubmission().getTotalFixCount() == max) names.add(student.getSubmission().getUsername());
        }
        mostFixesLabel.setText("Most fixes: " + String.join(", ", names) + " (" + max + ")");
    }

    private void showSelectedCorrection() {
        int viewRow = correctionTable.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = correctionTable.convertRowIndexToModel(viewRow);
        String username = String.valueOf(correctionTableModel.getValueAt(modelRow, 0));
        for (StudentResult student : studentResults) {
            if (student.getSubmission().getUsername().equals(username)) {
                showCorrectionSummary(student.getSubmission());
                return;
            }
        }
    }

    private void showCorrectionSummary(SubmissionResult submission) {
        StringBuilder text = new StringBuilder();
        text.append(submission.getUsername()).append(" | Total fixes: ").append(submission.getTotalFixCount())
                .append(" | Status: ").append(submission.getOverallStatus()).append('\n');
        text.append("Folder (").append(submission.getFolderFixCount()).append("): ").append(joinDetails(submission.getFolderFixDetails())).append('\n');
        text.append("Name (").append(submission.getNameFixCount()).append("): ").append(joinDetails(submission.getNameFixDetails())).append('\n');
        text.append("Email (").append(submission.getEmailFixCount()).append("): ").append(joinDetails(submission.getEmailFixDetails())).append('\n');
        text.append("Open Issues (").append(submission.getOpenIssueCount()).append("): ").append(joinDetails(submission.getOpenIssueDetails())).append('\n');
        text.append("Anomalies (").append(submission.getAnomalyCount()).append("): ").append(joinDetails(submission.getAnomalyDetails()));
        correctionDetailArea.setText(text.toString());
        correctionDetailArea.setCaretPosition(0);
    }

    private String joinDetails(List<String> details) {
        return details.isEmpty() ? "None" : String.join(", ", details);
    }

    private void selectCorrectionRow(String username) {
        for (int modelRow = 0; modelRow < correctionTableModel.getRowCount(); modelRow++) {
            if (username.equals(correctionTableModel.getValueAt(modelRow, 0))) {
                int viewRow = correctionTable.convertRowIndexToView(modelRow);
                if (viewRow >= 0 && correctionTable.getSelectedRow() != viewRow) correctionTable.setRowSelectionInterval(viewRow, viewRow);
                return;
            }
        }
    }

    private void saveCorrectionHistoryCsv() {
        Path output = Path.of("workspace", "correction_history.csv");
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Student,Folder Fixes,Name Fixes,Email Fixes,Open Issues,Anomalies,Total Fixes,Status,Folder Details,Name Details,Email Details,Open Issue Details,Anomaly Details");
        for (StudentResult student : studentResults) {
            SubmissionResult s = student.getSubmission();
            lines.add(csvValue(s.getUsername()) + "," + csvCount(s.getFolderFixCount()) + "," + csvCount(s.getNameFixCount()) + ","
                    + csvCount(s.getEmailFixCount()) + "," + csvCount(s.getOpenIssueCount()) + "," + csvCount(s.getAnomalyCount()) + ","
                    + csvCount(s.getTotalFixCount()) + "," + csvValue(s.getOverallStatus()) + ","
                    + csvValue(csvDetails(s.getFolderFixDetails())) + "," + csvValue(csvDetails(s.getNameFixDetails())) + ","
                    + csvValue(csvDetails(s.getEmailFixDetails())) + "," + csvValue(csvDetails(s.getOpenIssueDetails())) + ","
                    + csvValue(csvDetails(s.getAnomalyDetails())));
        }
        try {
            Files.createDirectories(output.getParent());
            Files.write(output, lines, StandardCharsets.UTF_8);
            appendLog("Saved correction history: " + output);
        } catch (IOException e) {
            appendLog("[WARNING] Could not update correction_history.csv: " + e.getMessage());

            JOptionPane.showMessageDialog(this,
                    "Could not update correction_history.csv.\n\n"
                            + "Please close the CSV file if it is open in Excel, then validate again.",
                    "Correction History File In Use",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private String csvCount(int count) {
        return count == 0 ? "" : Integer.toString(count);
    }

    private String csvDetails(List<String> details) {
        return details.isEmpty() ? "" : String.join(", ", details);
    }

    private String csvValue(String value) {
        String safe = value == null ? "" : value.replace("\r", " ").replace("\n", " ");
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private void applyFilters() {
        String search = searchField.getText().trim();
        String status = String.valueOf(statusFilter.getSelectedItem());
        ArrayList<RowFilter<Object, Object>> filters = new ArrayList<>();
        if (!search.isEmpty()) filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(search), 0));
        if (status.equals("Needs Attention")) {
            filters.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<? extends Object, ? extends Object> entry) {
                    return studentResults.get((Integer) entry.getIdentifier()).needsAttention();
                }
            });
        } else if (!status.equals("All")) {
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(status) + "$", 3));
        }
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        if (resultTable.getRowCount() > 0) resultTable.setRowSelectionInterval(0, 0);
        else detailsArea.setText("No students match the current search/filter.");
    }
    private void updateSummary() {
        totalLabel.setText("Total: " + studentResults.size());
        validLabel.setText("Valid: " + countStatus("Valid"));
        correctedLabel.setText("Corrected: " + countStatus("Corrected"));
        reviewLabel.setText("Review: " + countStatus("Review"));
        failedLabel.setText("Failed: " + countStatus("Failed"));
    }
    private int countStatus(String status) {
        int count = 0;
        for (StudentResult student : studentResults) {
            if (status.equals(student.getSubmission().getOverallStatus())) count++;
        }
        return count;
    }
    private void appendLog(String message) {
        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    private void updateProgress(int processed, int total) {
        if (total <= 0) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(100);
            progressBar.setValue(0);
            progressBar.setString("No submissions found");
            return;
        }
        progressBar.setMinimum(0);
        progressBar.setMaximum(total);
        progressBar.setValue(processed);
        progressBar.setString(processed + " / " + total + " processed");
    }
    private void setControlsEnabled(boolean enabled) {
        busy = !enabled;
        browseButton.setEnabled(enabled);
        validateButton.setEnabled(enabled);
        submissionsField.setEnabled(enabled);
        startGradingButton.setEnabled(enabled && studentResults.stream()
                .anyMatch(student -> !"Failed".equals(student.getSubmission().getOverallStatus())));
        edgeCaseButton.setEnabled(enabled && studentResults.stream()
                .anyMatch(student -> !"Failed".equals(student.getSubmission().getOverallStatus())));
        exportButton.setEnabled(enabled && studentResults.stream().anyMatch(StudentResult::isGraded));
        showSelectedStudent();
    }
    private void runEdgeCases() {
        File workspace = new File("workspace");
        File edgecases = new File("edgecases");
        File resources = new File("assessment/resources");

        if (!workspace.isDirectory() || !edgecases.isDirectory() || !resources.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                    "Cannot find workspace, edgecases, or assessment/resources.\nRun the application from the project folder.",
                    "Missing Folder", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<SubmissionResult> currentBatch = new ArrayList<>();
        for (StudentResult student : studentResults) currentBatch.add(student.getSubmission());

        setControlsEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("Starting edge-case tests...");
        appendLog("\n=== EDGE CASE TESTING STARTED ===");

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
                        File studentRoot = findStudentRoot(workspace, submission.getUsername());
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
                    appendLog("Edge cases: " + result.getUsername() + " -> " + result.getSummary());

                    if (!result.isSkipped()) {
                        for (String qid : List.of("Q1a", "Q1b", "Q2a", "Q2b", "Q3")) {
                            EdgeCaseRunner.QuestionResult q = result.get(qid);
                            appendLog("  " + qid + ": " + q.getScoreText() + " (" + q.getStatus() + ")");
                        }
                    }

                    updateProgress(update.processed, update.total);
                }
            }

            @Override
            protected void done() {
                try {
                    List<EdgeCaseRunner.Result> results = get();
                    saveEdgeCaseResultsCsv(results);

                    int passed = results.stream().mapToInt(EdgeCaseRunner.Result::getPassed).sum();
                    int total = results.stream().mapToInt(EdgeCaseRunner.Result::getTotal).sum();

                    progressBar.setString("Edge cases finished - " + passed + "/" + total + " passed");
                    appendLog("=== EDGE CASE TESTING FINISHED ===");

                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Edge-case testing finished.\nPassed: " + passed + " / " + total
                                    + "\nSaved: workspace\\edge_case_results.csv"
                                    + "\n\nOfficial grading scores were not changed.",
                            "Edge Cases Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    progressBar.setString("Edge-case testing could not complete");
                    appendLog("[ERROR] Edge cases: " + ex);

                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Edge-case testing could not complete. Check the System Log.",
                            "Edge Case Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setControlsEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void saveEdgeCaseResultsCsv(List<EdgeCaseRunner.Result> results) {
        Path output = Path.of("workspace", "edge_case_results.csv");
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Student,Q1a,Q1b,Q2a,Q2b,Q3,Passed,Failed,Status");

        for (EdgeCaseRunner.Result result : results) {
            lines.add(csvValue(result.getUsername()) + ","
                    + edgeScore(result, "Q1a") + ","
                    + edgeScore(result, "Q1b") + ","
                    + edgeScore(result, "Q2a") + ","
                    + edgeScore(result, "Q2b") + ","
                    + edgeScore(result, "Q3") + ","
                    + result.getPassed() + ","
                    + result.getFailed() + ","
                    + csvValue(result.isSkipped() ? result.getError() : "Completed"));
        }

        try {
            Files.write(output, lines, StandardCharsets.UTF_8);
            appendLog("Saved edge-case results: " + output);
        } catch (IOException e) {
            appendLog("[WARNING] Could not save edge_case_results.csv: " + e.getMessage());
        }
    }

    private String edgeScore(EdgeCaseRunner.Result result, String qid) {
        EdgeCaseRunner.QuestionResult q = result.get(qid);
        return q == null ? "" : csvValue(q.getScoreText());
    }

    private void startGrading() {
        File workspace = new File("workspace");
        File testers = new File("assessment/testers");
        File resources = new File("assessment/resources");

        if (!workspace.isDirectory()
                || !testers.isDirectory()
                || !resources.isDirectory()) {

            JOptionPane.showMessageDialog(this,
                    "Cannot find workspace, assessment/testers, "
                            + "or assessment/resources.\n"
                            + "Run the application from the project folder.",
                    "Missing Folder",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<SubmissionResult> currentBatch = new ArrayList<>();
        for (StudentResult student : studentResults) {
            currentBatch.add(student.getSubmission());
        }

        setControlsEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("Starting grading...");
        appendLog("\n=== GRADING STARTED ===");

        SwingWorker<Void, GradingUpdate> worker = new SwingWorker<Void, GradingUpdate>() {
            @Override
            protected Void doInBackground() {
                WorkspaceManager manager = new WorkspaceManager(new File("temp_workspaces"), testers, resources);
                JavaCompilerService compiler = new JavaCompilerService(10, 1024 * 1024);
                ProgramExecutor executor = new ProgramExecutor(5, 1024 * 1024, new ArrayList<>());
                QuestionGrader grader = new QuestionGrader(compiler, new TestCaseRunner(executor), manager);
                List<QuestionSpec> specs = Arrays.asList(
                        new QuestionSpec("Q1a", "Q1", "Q1aTester", List.of("Q1a.java")),
                        new QuestionSpec("Q1b", "Q1", "Q1bTester", List.of("Q1b.java")),
                        new QuestionSpec("Q2a", "Q2", "Q2aTester", List.of("Q2a.java")),
                        new QuestionSpec("Q2b", "Q2", "Q2bTester", List.of("Q2b.java")),
                        new QuestionSpec("Q3", "Q3", "Q3Tester", List.of("Q3.java")));
                GradingEngine engine = new GradingEngine(grader, specs);
                for (int row = 0; row < currentBatch.size(); row++) {
                    SubmissionResult submission = currentBatch.get(row);
                    try {
                        if ("Failed".equals(submission.getOverallStatus())) {
                            publish(new GradingUpdate(row, List.of(), "Skipped: submission validation failed."));
                            continue;
                        }
                        File studentRoot = findStudentRoot(workspace, submission.getUsername());
                        if (studentRoot == null) {
                            publish(new GradingUpdate(row, List.of(), "Cannot uniquely locate the student folder."));
                            continue;
                        }
                        publish(new GradingUpdate(row, engine.gradeStudent(studentRoot, submission.getUsername()), ""));
                    } catch (Exception ex) {
                        publish(new GradingUpdate(row, List.of(), ex.toString()));
                    }
                }
                return null;
            }
            @Override
            protected void process(List<GradingUpdate> updates) {
                for (GradingUpdate update : updates) {
                    StudentResult student = studentResults.get(update.row);
                    student.setGradingResults(update.questions, update.error);
                    refreshStudentRow(update.row);
                    appendLog("Grading: " + student.getSubmission().getUsername());
                    for (QuestionResult q : update.questions) appendLog("  " + q);
                    if (!update.error.isEmpty()) appendLog("[ERROR] " + update.error);
                    else appendLog("Final total: " + student.getTotal() + " / " + student.getMaximum());
                    updateProgress(update.row + 1, currentBatch.size());
                }
                showSelectedStudent();
            }
            @Override
            protected void done() {
                try {
                    get();
                    long attention = studentResults.stream().filter(StudentResult::needsAttention).count();
                    progressBar.setString("Grading finished - " + attention + " student(s) need attention");
                    appendLog("=== GRADING FINISHED ===");
                } catch (Exception ex) {
                    progressBar.setString("Grading could not complete");
                    appendLog("[ERROR] " + ex);
                    JOptionPane.showMessageDialog(MainFrame.this, "Grading could not complete. Check the System Log.",
                            "Grading Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setControlsEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void refreshStudentRow(int row) {
        StudentResult student = studentResults.get(row);
        SubmissionResult submission = student.getSubmission();
        tableModel.setValueAt(submission.getStructureStatus(), row, 1);
        tableModel.setValueAt(submission.getHeaderStatus(), row, 2);
        tableModel.setValueAt(submission.getOverallStatus(), row, 3);
        tableModel.setValueAt(student.getGradingStatus(), row, 4);
        tableModel.setValueAt(student.getQuestions().isEmpty() ? "" : student.getTotal() + " / " + student.getMaximum(), row, 5);
    }

    private void enterManualScore() {
        int viewRow = resultTable.getSelectedRow();
        if (busy || viewRow < 0) return;
        int row = resultTable.convertRowIndexToModel(viewRow);
        StudentResult student = studentResults.get(row);
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
        while (JOptionPane.showConfirmDialog(this, form,
                "Manual Score - " + student.getSubmission().getUsername(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                double mark;
                try { mark = Double.parseDouble(score.getText().trim()); }
                catch (NumberFormatException ex) { throw new IllegalArgumentException("Enter a numeric score."); }
                student.setManualScore((String) questions.getSelectedItem(), mark, comment.getText());
                refreshStudentRow(row);
                showSelectedStudent();
                appendLog("Manual score saved: " + student.getSubmission().getUsername() + " / " + questions.getSelectedItem());
                return;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Manual Score", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportResults() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Results (CSV + PDF)");
        chooser.setSelectedFile(new File("gradedscores.csv"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Path csvTarget = chooser.getSelectedFile().toPath().toAbsolutePath();
        if (!csvTarget.toString().toLowerCase(Locale.ROOT).endsWith(".csv")) csvTarget = Path.of(csvTarget + ".csv");

        Path pdfTarget = csvTarget.resolveSibling(
                csvTarget.getFileName().toString().replaceFirst("(?i)\\.csv$", "_report.pdf"));

        Path template = Path.of("IS442-ScoreSheet.csv").toAbsolutePath().normalize();

        if (csvTarget.normalize().equals(template)) {
            JOptionPane.showMessageDialog(this,
                    "Choose a different filename to preserve the original scoresheet.");
            return;
        }

        if ((Files.exists(csvTarget) || Files.exists(pdfTarget))
                && JOptionPane.showConfirmDialog(this,
                        "Replace existing export file(s)?\n" + csvTarget + "\n" + pdfTarget,
                        "Replace Export", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        try {
            new CsvExporter(template).export(csvTarget, studentResults);
            new PdfReportExporter().export(pdfTarget, studentResults);

            appendLog("Saved scores: " + csvTarget);
            appendLog("Saved PDF report: " + pdfTarget);

            JOptionPane.showMessageDialog(this,
                    "Saved:\n" + csvTarget + "\n" + pdfTarget,
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            appendLog("[ERROR] Export: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Could not export the CSV/PDF.\n" + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class EdgeCaseUpdate {
        final int processed;
        final int total;
        final EdgeCaseRunner.Result result;

        EdgeCaseUpdate(int processed, int total, EdgeCaseRunner.Result result) {
            this.processed = processed;
            this.total = total;
            this.result = result;
        }
    }

    private static class GradingUpdate {
        final int row;
        final List<QuestionResult> questions;
        final String error;
        GradingUpdate(int row, List<QuestionResult> questions, String error) {
            this.row = row; this.questions = questions; this.error = error;
        }
    }

    private File findStudentRoot(File workspace, String username) {
        File[] folders = workspace.listFiles(File::isDirectory);

        if (folders == null) {
            return null;
        }

        List<File> matches = new ArrayList<>();

        for (File folder : folders) {
            if (folder.getName().equals(username)) {
                matches.add(folder);
                continue;
            }

            File nestedStudent = new File(folder, username);

            if (nestedStudent.isDirectory()) {
                matches.add(nestedStudent);
            }
        }

        // Avoid selecting an arbitrary folder if duplicates exist.
        if (matches.size() != 1) {
            return null;
        }

        return resolveStudentRoot(matches.get(0));
    }

    private File resolveStudentRoot(File folder) {
        if (new File(folder, "Q1").isDirectory()
                || new File(folder, "Q2").isDirectory()
                || new File(folder, "Q3").isDirectory()) {
            return folder;
        }

        File[] children = folder.listFiles(File::isDirectory);

        if (children != null && children.length == 1) {
            File child = children[0];

            if (new File(child, "Q1").isDirectory()
                    || new File(child, "Q2").isDirectory()
                    || new File(child, "Q3").isDirectory()) {
                return child;
            }
        }

        return folder;
    }
}
