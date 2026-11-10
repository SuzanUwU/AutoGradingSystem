package autograder.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

final class MainFrameLayout {
    private final MainFrame f;
    private JSplitPane submissionsSplitPane;
    private JPanel studentTablePanel;
    private JPanel submissionDetailsPanel;

    MainFrameLayout(MainFrame frame) { this.f = frame; }

    // =====================================================
    // 1. BUILD THE MAIN APPLICATION INTERFACE
    // =====================================================

    void buildInterface() {
        f.setLayout(new BorderLayout(10, 10));
        f.titleLabel.setFont(f.titleLabel.getFont().deriveFont(Font.BOLD, 22f));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 15, 0, 15));
        headerPanel.add(f.titleLabel, BorderLayout.WEST);

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        modePanel.add(new JLabel("Assessment Mode:"));
        modePanel.add(f.assessmentMode);
        headerPanel.add(modePanel, BorderLayout.EAST);
        f.add(headerPanel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        mainPanel.add(createSetupPanel(), BorderLayout.NORTH);
        mainPanel.add(createTabs(), BorderLayout.CENTER);
        f.add(mainPanel, BorderLayout.CENTER);

        f.progressBar.setStringPainted(true);
        f.progressBar.setString("Ready");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 12, 15));
        bottomPanel.add(f.progressBar, BorderLayout.CENTER);
        f.add(bottomPanel, BorderLayout.SOUTH);
    }

    // =====================================================
    // 2. CREATE THE SUBMISSION SETUP PANEL
    // =====================================================

    JPanel createSetupPanel() {
        JPanel container = new JPanel(new BorderLayout(8, 8));
        JPanel setupPanel = new JPanel(new GridBagLayout());
        setupPanel.setBorder(BorderFactory.createTitledBorder("Submission Setup"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: submissions folder
        gbc.gridx = 0;
        gbc.gridy = 0;
        setupPanel.add(new JLabel("Submissions Folder:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        setupPanel.add(f.submissionsField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        setupPanel.add(f.browseButton, gbc);

        // Row 1: validation
        JPanel validatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        validatePanel.add(f.validateButton);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        setupPanel.add(validatePanel, gbc);

        // Row 2: grading options
        JPanel gradingFlows = f.gradingFlowsPanel;

        JPanel originalPanel = new JPanel(new BorderLayout(6, 6));
        originalPanel.setBorder(BorderFactory.createTitledBorder("Normal Grading - Professor's Original Testers"));
        originalPanel.add(new JLabel("After validation: click Start Grading."), BorderLayout.NORTH);

        JPanel originalButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        originalButtons.add(f.gradeOriginalButton);
        originalPanel.add(originalButtons, BorderLayout.CENTER);

        JPanel generatedPanel = f.generatedTesterPanel;
        generatedPanel.setBorder(BorderFactory.createTitledBorder("Bonus - Generated Testers (Comparison)"));
        generatedPanel.add(new JLabel("Generate Testers first, then Grade Generated. Comparison result is not exported."), BorderLayout.NORTH);

        JPanel generatedButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        generatedButtons.add(f.generateTestersButton);
        generatedButtons.add(f.gradeGeneratedButton);
        generatedPanel.add(generatedButtons, BorderLayout.CENTER);

        gradingFlows.add(originalPanel);
        gradingFlows.add(generatedPanel);

        gbc.gridy = 2;
        setupPanel.add(gradingFlows, gbc);

        // Row 3: other actions
        JPanel otherActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        otherActions.setBorder(BorderFactory.createTitledBorder("Other Actions"));
        otherActions.add(f.edgeCaseButton);
        otherActions.add(f.exportButton);
        otherActions.add(f.reviewButton);
        otherActions.add(f.manualButton);

        gbc.gridy = 3;
        setupPanel.add(otherActions, gbc);

        f.reviewButton.setEnabled(false);
        f.manualButton.setEnabled(false);
        f.gradeOriginalButton.setEnabled(false);
        f.generateTestersButton.setEnabled(false);
        f.gradeGeneratedButton.setEnabled(false);
        f.edgeCaseButton.setEnabled(false);
        f.exportButton.setEnabled(false);

        container.add(setupPanel, BorderLayout.NORTH);
        container.add(createSummaryPanel(), BorderLayout.SOUTH);
        return container;
    }

    // =====================================================
    // 3. CREATE SUBMISSION SUMMARY
    // =====================================================

    JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 8, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Submission Summary"));

        for (JLabel label : new JLabel[]{f.totalLabel, f.validLabel, f.correctedLabel, f.reviewLabel, f.failedLabel}) {
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
            panel.add(label);
        }

        f.validLabel.setForeground(new Color(34, 120, 58));
        f.correctedLabel.setForeground(new Color(37, 99, 235));
        f.reviewLabel.setForeground(new Color(180, 83, 9));
        f.failedLabel.setForeground(new Color(185, 28, 28));
        return panel;
    }

    // =====================================================
    // 4. TABLE STATUS FORMATTING
    // =====================================================

    DefaultTableCellRenderer createStatusRenderer() { return new StatusCellRenderer(); }

    void applyStatusColors() {
        DefaultTableCellRenderer statusRenderer = createStatusRenderer();
        int columnCount = f.resultTable.getColumnCount();

        if (columnCount > 1) f.resultTable.getColumnModel().getColumn(1).setCellRenderer(statusRenderer);
        if (columnCount > 2) f.resultTable.getColumnModel().getColumn(2).setCellRenderer(statusRenderer);
        if (columnCount > 3) f.resultTable.getColumnModel().getColumn(3).setCellRenderer(statusRenderer);

        // Java-only columns
        if (columnCount > 7) f.resultTable.getColumnModel().getColumn(7).setCellRenderer(statusRenderer);
        if (columnCount > 8) f.resultTable.getColumnModel().getColumn(8).setCellRenderer(statusRenderer);

        DefaultTableCellRenderer gradingRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                String gradingStatus = value == null ? "" : value.toString();
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setFont(table.getFont().deriveFont(gradingStatus.equals("Review") ? Font.BOLD : Font.PLAIN));
                return label;
            }
        };
        if (columnCount > 4) f.resultTable.getColumnModel().getColumn(4).setCellRenderer(gradingRenderer);

        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(JLabel.CENTER);
        if (columnCount > 5) f.resultTable.getColumnModel().getColumn(5).setCellRenderer(centeredRenderer);
        if (columnCount > 6) f.resultTable.getColumnModel().getColumn(6).setCellRenderer(centeredRenderer);

        if (f.correctionTable.getColumnModel().getColumnCount() > 7) {
            f.correctionTable.getColumnModel().getColumn(7).setCellRenderer(createStatusRenderer());
        }
    }

    // =====================================================
    // 5. CREATE MAIN TABS
    // =====================================================

    JTabbedPane createTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Submissions", createSubmissionsPanel());
        tabs.addTab("System Log", createLogPanel());
        return tabs;
    }

    // =====================================================
    // 6. CREATE SUBMISSIONS PANEL
    // =====================================================

    JPanel createSubmissionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Search student:"));
        filterPanel.add(f.searchField);
        filterPanel.add(new JLabel("Status:"));
        filterPanel.add(f.statusFilter);
        panel.add(filterPanel, BorderLayout.NORTH);

        // Student results table
        f.resultTable.setFillsViewportHeight(true);
        f.resultTable.setRowHeight(25);
        f.resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Keep fixed readable widths. If the pane is narrower, use the horizontal scrollbar.
        f.resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        f.resultTable.setFont(f.resultTable.getFont().deriveFont(11f));
        f.resultTable.getTableHeader().setFont(f.resultTable.getTableHeader().getFont().deriveFont(Font.BOLD, 11f));

        int columnCount = f.resultTable.getColumnCount();

        if (columnCount >= 9) {
            setColumnWidth(0, 135, 115); // Student
            setColumnWidth(1, 88, 78);   // Structure
            setColumnWidth(2, 84, 74);   // Headers
            setColumnWidth(3, 82, 72);   // Status
            setColumnWidth(4, 88, 78);   // Grading
            setColumnWidth(5, 108, 98);  // Original Total
            setColumnWidth(6, 118, 108); // Generated Total
            setColumnWidth(7, 122, 110); // Plagiarism Check
            setColumnWidth(8, 135, 120); // Review Status

            // Keep full, readable headers.
            f.resultTable.getColumnModel().getColumn(5).setHeaderValue("Original Total");
            f.resultTable.getColumnModel().getColumn(6).setHeaderValue("Generated Total");
            f.resultTable.getColumnModel().getColumn(7).setHeaderValue("Plagiarism Check");
            f.resultTable.getColumnModel().getColumn(8).setHeaderValue("Review Status");
        } else if (columnCount >= 6) {
            setColumnWidth(0, 150, 120); // Student
            setColumnWidth(1, 100, 80);  // Structure
            setColumnWidth(2, 100, 80);  // Headers
            setColumnWidth(3, 100, 80);  // Status
            setColumnWidth(4, 110, 90);  // Grading
            setColumnWidth(5, 120, 100); // Total
        }

        studentTablePanel = new JPanel(new BorderLayout());
        studentTablePanel.setBorder(BorderFactory.createTitledBorder("Students"));
        JScrollPane tableScrollPane = new JScrollPane(f.resultTable);
        tableScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        tableScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        tableScrollPane.getViewport().setBackground(Color.WHITE);

        // Remove the empty grey area to the right of Review Status.
        // The last column expands when there is spare viewport space, but returns to
        // its normal width when horizontal scrolling is needed.
        final int lastColumnBaseWidth = 120;

        tableScrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    int columnCount = f.resultTable.getColumnCount();
                    if (columnCount == 0) return;

                    int viewportWidth = tableScrollPane.getViewport().getWidth();
                    int otherColumnsWidth = 0;

                    for (int i = 0; i < columnCount - 1; i++) {
                        otherColumnsWidth +=
                                f.resultTable.getColumnModel()
                                        .getColumn(i)
                                        .getWidth();
                    }

                    TableColumn lastColumn =
                            f.resultTable.getColumnModel()
                                    .getColumn(columnCount - 1);

                    int newWidth =
                            Math.max(
                                    lastColumnBaseWidth,
                                    viewportWidth - otherColumnsWidth
                            );

                    lastColumn.setPreferredWidth(newWidth);
                    lastColumn.setWidth(newWidth);
                });
            }
        });
        studentTablePanel.add(tableScrollPane, BorderLayout.CENTER);

        // Student details
        f.detailsArea.setEditable(false);
        f.detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        f.detailsArea.setText("Select a student to view submission details.");

        submissionDetailsPanel = new JPanel(new BorderLayout());
        submissionDetailsPanel.setBorder(BorderFactory.createTitledBorder("Submission Details"));

        JTabbedPane detailsTabs = new JTabbedPane();
        detailsTabs.addTab("Correction Summary", f.correctionPanel.createCorrectionPanel());
        detailsTabs.addTab("Full Details", new JScrollPane(f.detailsArea));
        submissionDetailsPanel.add(detailsTabs, BorderLayout.CENTER);

        // Java starts with the wider results table. Python adjusts this split
        // after Java-only columns are hidden so the details panel gets more room.
        studentTablePanel.setMinimumSize(new Dimension(480, 0));
        submissionDetailsPanel.setMinimumSize(new Dimension(300, 0));

        submissionsSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, studentTablePanel, submissionDetailsPanel);
        submissionsSplitPane.setResizeWeight(0.67);
        submissionsSplitPane.setDividerLocation(0.67);
        submissionsSplitPane.setContinuousLayout(true);
        submissionsSplitPane.setOneTouchExpandable(true);

        panel.add(submissionsSplitPane, BorderLayout.CENTER);
        return panel;
    }

    /** Adjust the submissions/details split for the selected assessment mode. */
    void applyAssessmentModeLayout(boolean pythonMode) {
        if (submissionsSplitPane == null) return;

        if (pythonMode) {
            // Python only shows columns through Total. Keep the left side close to
            // the table's actual width and give all extra window space to details.
            studentTablePanel.setMinimumSize(new Dimension(360, 0));
            submissionDetailsPanel.setMinimumSize(new Dimension(360, 0));
            submissionsSplitPane.setResizeWeight(0.0);

            SwingUtilities.invokeLater(() -> {
                int tableWidth = 28; // border + scrollbar allowance
                for (int i = 0; i < f.resultTable.getColumnModel().getColumnCount(); i++) {
                    TableColumn column = f.resultTable.getColumnModel().getColumn(i);
                    tableWidth += column.getPreferredWidth();
                }

                int available = submissionsSplitPane.getWidth();
                int rightMinimum = submissionDetailsPanel.getMinimumSize().width;
                int maxLeft = Math.max(360, available - rightMinimum - submissionsSplitPane.getDividerSize());
                submissionsSplitPane.setDividerLocation(Math.min(tableWidth, maxLeft));
            });
        } else {
            studentTablePanel.setMinimumSize(new Dimension(480, 0));
            submissionDetailsPanel.setMinimumSize(new Dimension(300, 0));
            submissionsSplitPane.setResizeWeight(0.67);
            SwingUtilities.invokeLater(() -> submissionsSplitPane.setDividerLocation(0.67));
        }
    }

    private void setColumnWidth(int index, int preferredWidth, int minWidth) {
        TableColumn column = f.resultTable.getColumnModel().getColumn(index);
        column.setPreferredWidth(preferredWidth);
        column.setMinWidth(minWidth);
    }

    // =====================================================
    // 7. CREATE SYSTEM LOG PANEL
    // =====================================================

    JPanel createLogPanel() {
        f.logArea.setEditable(false);
        f.logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(f.clearLogButton);

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(f.logArea), BorderLayout.CENTER);
        return panel;
    }
}