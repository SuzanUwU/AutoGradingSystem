
package autograder.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

final class MainFrameLayout {

    // Reference to the main application window.
    // "f" allows this class to access MainFrame's buttons, labels, tables, and other components.
    private final MainFrame f;
    
    // Constructor receives the existing MainFrame object.
    MainFrameLayout(MainFrame frame) {
        this.f = frame;
    }

    // =====================================================
    // 1. BUILD THE MAIN APPLICATION INTERFACE
    // =====================================================

    /**
     * Called by MainFrame's constructor.
     *
     * Organizes the window into three main regions:
     *
     * NORTH  - Application title and assessment mode
     * CENTER - Submission setup and tabs
     * SOUTH  - Progress bar
     */
    void buildInterface() {

        // BorderLayout divides the window into NORTH, SOUTH, EAST, WEST, and CENTER.
        f.setLayout(new BorderLayout(10, 10));

        // Make the application title bold and 22pt.
        f.titleLabel.setFont(
            f.titleLabel.getFont().deriveFont(Font.BOLD, 22f)
        );

        JPanel headerPanel = new JPanel(new BorderLayout());

        headerPanel.setBorder(
            BorderFactory.createEmptyBorder(12, 15, 0, 15)
        );
        headerPanel.add(f.titleLabel, BorderLayout.WEST);
        JPanel modePanel =
            new JPanel(new FlowLayout(FlowLayout.RIGHT));

        modePanel.add(new JLabel("Assessment Mode:"));
        modePanel.add(f.assessmentMode);

        headerPanel.add(modePanel, BorderLayout.EAST);
        f.add(headerPanel, BorderLayout.NORTH);

        // Create the main content area.
        JPanel mainPanel =
            new JPanel(new BorderLayout(10, 10));

        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 15, 0, 15)
        );

        // Place submission setup at the top.
        mainPanel.add(createSetupPanel(), BorderLayout.NORTH);

        // Place the tabbed content in the center.
        mainPanel.add(createTabs(), BorderLayout.CENTER);

        f.add(mainPanel, BorderLayout.CENTER);

        // Configure the progress bar to display text.
        f.progressBar.setStringPainted(true);
        f.progressBar.setString("Ready");

        // Create the bottom panel for the progress bar.
        JPanel bottomPanel = new JPanel(new BorderLayout());

        bottomPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 15, 12, 15)
        );

        bottomPanel.add(
            f.progressBar,
            BorderLayout.CENTER
        );

        f.add(bottomPanel, BorderLayout.SOUTH);
    }

    // =====================================================
    // 2. CREATE THE SUBMISSION SETUP PANEL
    // =====================================================

    /**
     * Creates the section containing:
     * - Submission folder selection
     * - Validation button
     * - Original and generated grading buttons
     * - Other actions
     * - Submission statistics
     */
    JPanel createSetupPanel() {

        // Outer container for setup and summary sections.
        JPanel container =
            new JPanel(new BorderLayout(8, 8));

        // GridBagLayout allows flexible positioning
        // of components using rows and columns.
        JPanel setupPanel = new JPanel(new GridBagLayout());

        setupPanel.setBorder(
            BorderFactory.createTitledBorder("Submission Setup")
        );

        // GridBagConstraints controls where components
        // appear inside the GridBagLayout.
        GridBagConstraints gbc = new GridBagConstraints();

        // Add 6px spacing around each component.
        gbc.insets = new Insets(6, 6, 6, 6);

        // Components expand horizontally when space is available.
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ----- ROW 0: SUBMISSIONS FOLDER -----

        // Column 0, Row 0: Folder label.
        gbc.gridx = 0;
        gbc.gridy = 0;

        setupPanel.add(
            new JLabel("Submissions Folder:"),
            gbc
        );

        // Column 1: Text field containing the folder path.
        gbc.gridx = 1;

        // Give the text field additional horizontal space.
        gbc.weightx = 1;

        setupPanel.add(f.submissionsField, gbc);

        // Column 2: Browse button.
        gbc.gridx = 2;
        gbc.weightx = 0;

        setupPanel.add(f.browseButton, gbc);

        // ----- ROW 1: VALIDATION BUTTON -----

        // FlowLayout arranges components horizontally.
        JPanel validatePanel =
            new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        validatePanel.add(f.validateButton);

        gbc.gridx = 0;
        gbc.gridy = 1;

        // Make this panel span all three columns.
        gbc.gridwidth = 3;

        setupPanel.add(validatePanel, gbc);

        // ----- ROW 2: GRADING OPTIONS -----

        // GridLayout creates two equally sized columns:
        // Original Testers | Generated Testers
        JPanel gradingFlows = f.gradingFlowsPanel;

        // ORIGINAL TESTERS SECTION
        JPanel originalPanel = new JPanel(new BorderLayout(6, 6));

        originalPanel.setBorder(
            BorderFactory.createTitledBorder(
                "Normal Grading - Professor's Original Testers"
            )
        );

        JLabel originalFlow =
            new JLabel("After validation: click Start Grading.");

        originalPanel.add(originalFlow, BorderLayout.NORTH);

        JPanel originalButtons =
            new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        originalButtons.add(f.gradeOriginalButton);

        originalPanel.add(
            originalButtons,
            BorderLayout.CENTER
        );

        // GENERATED TESTERS SECTION
        JPanel generatedPanel = f.generatedTesterPanel;

        generatedPanel.setBorder(
            BorderFactory.createTitledBorder(
                "Bonus - Generated Testers (Comparison)"
            )
        );

        JLabel generatedFlow = new JLabel(
            "Generate Testers first, then Grade Generated. Comparison result is not exported."
        );

        generatedPanel.add(
            generatedFlow,
            BorderLayout.NORTH
        );

        JPanel generatedButtons =
            new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        generatedButtons.add(f.generateTestersButton);
        generatedButtons.add(f.gradeGeneratedButton);

        generatedPanel.add(
            generatedButtons,
            BorderLayout.CENTER
        );

        // Add both grading sections side by side.
        gradingFlows.add(originalPanel);
        gradingFlows.add(generatedPanel);

        gbc.gridy = 2;
        setupPanel.add(gradingFlows, gbc);

        // ----- ROW 3: OTHER ACTIONS -----

        JPanel otherActions =
            new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        otherActions.setBorder(
            BorderFactory.createTitledBorder("Other Actions")
        );

        otherActions.add(f.edgeCaseButton);
        otherActions.add(f.exportButton);
        otherActions.add(f.manualButton);

        gbc.gridy = 3;
        setupPanel.add(otherActions, gbc);

        // ----- INITIAL BUTTON STATES -----

        // Disable actions that should not run before
        // the necessary validation/grading steps.
        //
        // Other controllers can enable them later.
        f.manualButton.setEnabled(false);
        f.gradeOriginalButton.setEnabled(false);
        f.generateTestersButton.setEnabled(false);
        f.gradeGeneratedButton.setEnabled(false);
        f.edgeCaseButton.setEnabled(false);
        f.exportButton.setEnabled(false);

        // Put setup controls at the top of the container.
        container.add(setupPanel, BorderLayout.NORTH);

        // Put the submission summary below the controls.
        container.add(
            createSummaryPanel(),
            BorderLayout.SOUTH
        );

        return container;
    }

    // =====================================================
    // 3. CREATE SUBMISSION SUMMARY
    // =====================================================

    /**
     * Displays counts for: Total, Valid, Corrected, Review, Failed.
     */
    JPanel createSummaryPanel() {

        // One row with five equally sized columns.
        JPanel panel = new JPanel(new GridLayout(1, 5, 8, 0));

        panel.setBorder(
            BorderFactory.createTitledBorder("Submission Summary")
        );

        // Loop through the five labels created in MainFrame.
        for (JLabel label : new JLabel[] {
                f.totalLabel,
                f.validLabel,
                f.correctedLabel,
                f.reviewLabel,
                f.failedLabel
        }) {

            // Center each label horizontally.
            label.setHorizontalAlignment(JLabel.CENTER);

            // Make the text bold and 15pt.
            label.setFont(
                label.getFont().deriveFont(Font.BOLD, 15f)
            );

            panel.add(label);
        }

        // Apply different colours for each status.
        f.validLabel.setForeground(new Color(34, 120, 58));
        f.correctedLabel.setForeground(new Color(37, 99, 235));
        f.reviewLabel.setForeground(new Color(180, 83, 9));
        f.failedLabel.setForeground(new Color(185, 28, 28));

        return panel;
    }

    // =====================================================
    // 4. TABLE STATUS FORMATTING
    // =====================================================

    /**
     * Creates a custom renderer for status cells.
     *
     * StatusCellRenderer is another class in ui/.
     * It controls how status values are visually displayed.
     */
    DefaultTableCellRenderer createStatusRenderer() {
        return new StatusCellRenderer();
    }

    void applyStatusColors() {

        // Create a renderer for status-related values.
        DefaultTableCellRenderer statusRenderer =
            createStatusRenderer();

        // Apply it to columns:
        // 1 = Structure
        // 2 = Headers
        // 3 = Status
        f.resultTable.getColumnModel()
            .getColumn(1).setCellRenderer(statusRenderer);

        f.resultTable.getColumnModel()
            .getColumn(2).setCellRenderer(statusRenderer);

        f.resultTable.getColumnModel()
            .getColumn(3).setCellRenderer(statusRenderer);

        // Create a separate renderer for grading status.
        DefaultTableCellRenderer gradingRenderer =
            new DefaultTableCellRenderer() {

                // Override the standard Swing cell renderer.
                @Override
                public Component getTableCellRendererComponent(
                        JTable table,
                        Object value,
                        boolean isSelected,
                        boolean hasFocus,
                        int row,
                        int column) {

                    // Get the default rendered cell component.
                    JLabel label = (JLabel)
                        super.getTableCellRendererComponent(
                            table,
                            value,
                            isSelected,
                            hasFocus,
                            row,
                            column
                        );

                    // Convert the grading value into a string.
                    // Use an empty string if the value is null.
                    String gradingStatus =
                        value == null ? "" : value.toString();

                    // Make "Review" bold to draw attention.
                    // Other grading statuses use normal text.
                    label.setFont(
                        table.getFont().deriveFont(
                            gradingStatus.equals("Review")
                                ? Font.BOLD
                                : Font.PLAIN
                        )
                    );

                    return label;
                }
            };

        // Apply the grading renderer to column 4.
        f.resultTable.getColumnModel()
            .getColumn(4).setCellRenderer(gradingRenderer);

        // Apply status formatting to the correction table.
        // Column 7 contains the correction status.
        f.correctionTable.getColumnModel()
            .getColumn(7).setCellRenderer(createStatusRenderer());
    }

    // =====================================================
    // 5. CREATE MAIN TABS
    // =====================================================

    /**
     * Creates two tabs:
     * 1. Submissions
     * 2. System Log
     */
    JTabbedPane createTabs() {

        // JTabbedPane allows multiple pages within one area.
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab(
            "Submissions",
            createSubmissionsPanel()
        );

        tabs.addTab(
            "System Log",
            createLogPanel()
        );

        return tabs;
    }

    // =====================================================
    // 6. CREATE SUBMISSIONS PANEL
    // =====================================================

    /**
     * Displays:
     * - Search and status filter
     * - Student results table
     * - Correction summary and full details
     */
    JPanel createSubmissionsPanel() {

        JPanel panel =
            new JPanel(new BorderLayout(8, 8));

        // ----- SEARCH AND FILTER -----

        JPanel filterPanel =
            new JPanel(new FlowLayout(FlowLayout.LEFT));

        filterPanel.add(new JLabel("Search student:"));
        filterPanel.add(f.searchField);

        filterPanel.add(new JLabel("Status:"));
        filterPanel.add(f.statusFilter);

        panel.add(filterPanel, BorderLayout.NORTH);

        // ----- STUDENT RESULTS TABLE -----

        // Make the table fill the viewport height.
        f.resultTable.setFillsViewportHeight(true);

        // Set each table row's height to 25 pixels.
        f.resultTable.setRowHeight(25);

        // Allow only one student row to be selected at a time.
        f.resultTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        // Set preferred widths for selected columns.
        f.resultTable.getColumnModel()
            .getColumn(0).setPreferredWidth(220);

        f.resultTable.getColumnModel()
            .getColumn(5).setPreferredWidth(145);

        f.resultTable.getColumnModel()
            .getColumn(6).setPreferredWidth(155);

        JPanel tablePanel = new JPanel(new BorderLayout());

        tablePanel.setBorder(
            BorderFactory.createTitledBorder("Students")
        );

        // JScrollPane adds scrolling when table data
        // exceeds the available space.
        tablePanel.add(
            new JScrollPane(f.resultTable),
            BorderLayout.CENTER
        );

        // ----- STUDENT DETAILS -----

        // Make details read-only.
        f.detailsArea.setEditable(false);

        // Use a fixed-width font for easier reading.
        f.detailsArea.setFont(
            new Font(Font.MONOSPACED, Font.PLAIN, 12)
        );

        // Display a default message before selection.
        f.detailsArea.setText(
            "Select a student to view submission details."
        );

        JPanel detailsPanel =
            new JPanel(new BorderLayout());

        detailsPanel.setBorder(
            BorderFactory.createTitledBorder(
                "Submission Details"
            )
        );

        // Create two tabs for detailed student information.
        JTabbedPane detailsTabs = new JTabbedPane();

        // Calls CorrectionPanelBuilder.java to build
        // the correction summary interface.
        detailsTabs.addTab(
            "Correction Summary",
            f.correctionPanel.createCorrectionPanel()
        );

        detailsTabs.addTab(
            "Full Details",
            new JScrollPane(f.detailsArea)
        );

        detailsPanel.add(
            detailsTabs,
            BorderLayout.CENTER
        );

        // ----- SPLIT VIEW -----

        // JSplitPane displays two resizable panels side by side.
        //
        // LEFT  = Student results table
        // RIGHT = Student details
        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            tablePanel,
            detailsPanel
        );

        // Distribute extra resizing space equally.
        splitPane.setResizeWeight(0.5);

        // Set the initial divider position to 560 pixels.
        splitPane.setDividerLocation(560);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    // =====================================================
    // 7. CREATE SYSTEM LOG PANEL
    // =====================================================

    /**
     * Displays system messages generated during operations.
     *
     * The actual log messages are added by other controllers.
     */
    JPanel createLogPanel() {

        // Prevent users from editing log messages.
        f.logArea.setEditable(false);

        // Use a fixed-width font for log readability.
        f.logArea.setFont(
            new Font(Font.MONOSPACED, Font.PLAIN, 12)
        );

        JPanel panel =
            new JPanel(new BorderLayout(8, 8));

        JPanel buttonPanel =
            new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // Add the Clear Log button.
        buttonPanel.add(f.clearLogButton);

        panel.add(buttonPanel, BorderLayout.NORTH);

        // Make the log area scrollable.
        panel.add(
            new JScrollPane(f.logArea),
            BorderLayout.CENTER
        );

        return panel;
    }
}
