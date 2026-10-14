package autograder.ui;
import autograder.model.SubmissionResult;
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
import java.util.regex.Pattern;
public class MainFrame extends JFrame {
    private final JTextField submissionsField = new JTextField("submissions", 35);
    private final JTextField searchField = new JTextField(22);
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[] {"All", "Needs Attention", "Valid", "Corrected", "Review", "Failed"});
    private final JButton browseButton = new JButton("Browse");
    private final JButton validateButton = new JButton("Validate Submissions");
    private final JButton startGradingButton = new JButton("Start Grading");
    private final JButton exportButton = new JButton("Export Results");
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
    private final ArrayList<SubmissionResult> results = new ArrayList<>();
    public MainFrame() {
        setTitle("CS102 Auto Grading System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 680));
        setSize(1200, 760);
        setLocationRelativeTo(null);
        tableModel = new DefaultTableModel(new Object[] {"Student", "Structure", "Headers", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        resultTable.setRowSorter(sorter);
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
        buttonPanel.add(exportButton);
        startGradingButton.setEnabled(false);
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
        panel.setBorder(BorderFactory.createTitledBorder("Validation Summary"));
        for (JLabel label : new JLabel[] {totalLabel, validLabel, correctedLabel, reviewLabel, failedLabel}) {
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
            panel.add(label);
        }
        return panel;
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
        StatusRenderer renderer = new StatusRenderer();
        for (int column = 1; column <= 3; column++) {
            resultTable.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Students"));
        tablePanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detailsArea.setText("Select a student to view submission details.");
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Submission Details"));
        detailsPanel.add(new JScrollPane(detailsArea), BorderLayout.CENTER);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, detailsPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(560);
        panel.add(splitPane, BorderLayout.CENTER);
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
        File submissionsFolder = new File(submissionsField.getText().trim());
        if (!submissionsFolder.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Please select a valid submissions folder.",
                    "Invalid Folder", JOptionPane.ERROR_MESSAGE);
            return;
        }
        results.clear();
        tableModel.setRowCount(0);
        searchField.setText("");
        statusFilter.setSelectedItem("All");
        detailsArea.setText("Validation in progress...");
        logArea.setText("");
        resetSummary();
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
                    if (!results.isEmpty()) progressBar.setString("Validation complete");
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
        results.add(result);
        tableModel.addRow(new Object[] {
                result.getUsername(), result.getStructureStatus(), result.getHeaderStatus(), result.getOverallStatus()
        });
        updateSummary();
        if (resultTable.getSelectedRow() == -1 && resultTable.getRowCount() > 0) {
            resultTable.setRowSelectionInterval(0, 0);
        }
    }
    private void showSelectedStudent() {
        int viewRow = resultTable.getSelectedRow();
        if (viewRow < 0) {
            detailsArea.setText("Select a student to view submission details.");
            return;
        }
        int modelRow = resultTable.convertRowIndexToModel(viewRow);
        detailsArea.setText(results.get(modelRow).getDetails());
        detailsArea.setCaretPosition(0);
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
                    String value = String.valueOf(entry.getValue(3));
                    return value.equals("Review") || value.equals("Failed");
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
        totalLabel.setText("Total: " + results.size());
        validLabel.setText("Valid: " + countStatus("Valid"));
        correctedLabel.setText("Corrected: " + countStatus("Corrected"));
        reviewLabel.setText("Review: " + countStatus("Review"));
        failedLabel.setText("Failed: " + countStatus("Failed"));
    }
    private int countStatus(String status) {
        int count = 0;
        for (SubmissionResult result : results) {
            if (result.getOverallStatus().equals(status)) count++;
        }
        return count;
    }
    private void resetSummary() {
        totalLabel.setText("Total: 0");
        validLabel.setText("Valid: 0");
        correctedLabel.setText("Corrected: 0");
        reviewLabel.setText("Review: 0");
        failedLabel.setText("Failed: 0");
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
        browseButton.setEnabled(enabled);
        validateButton.setEnabled(enabled);
        submissionsField.setEnabled(enabled);
    }
    private static class StatusRenderer extends DefaultTableCellRenderer {
        private final Color validColor = new Color(220, 245, 225);
        private final Color correctedColor = new Color(220, 235, 250);
        private final Color reviewColor = new Color(255, 239, 190);
        private final Color failedColor = new Color(255, 220, 220);
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(CENTER);
            if (!isSelected) {
                String status = String.valueOf(value);
                if (status.equals("Valid")) component.setBackground(validColor);
                else if (status.equals("Corrected")) component.setBackground(correctedColor);
                else if (status.equals("Review")) component.setBackground(reviewColor);
                else if (status.equals("Failed")) component.setBackground(failedColor);
                else component.setBackground(table.getBackground());
            }
            return component;
        }
    }
}
