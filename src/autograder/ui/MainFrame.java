package autograder.ui;

import javax.swing.*; // Swing components used to build the graphical user interface.
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*; // Used for GUI dimensions and window sizing.
import java.util.ArrayList;
import java.nio.file.Path;
import autograder.model.*; // Imports model classes, including StudentResult.

// JFrame is the Swing class used to create application windows.
// MainFrame inherits JFrame's window-related methods.
public class MainFrame extends JFrame {

    // =========================================================
    // 1. ASSESSMENT MODE SELECTION
    // =========================================================

    // Dropdown allowing users to choose between Java and Python grading.
    // JComboBox displays a list of selectable options.
    final JComboBox<String> assessmentMode = new JComboBox<>(new String[]{
            "CS102 - Java",
            "IS1704 - Python"
    });

    // Displays the current application title in the GUI.
    final JLabel titleLabel = new JLabel("CS102 Auto Grading System");

    boolean isPythonMode() {
        return assessmentMode.getSelectedIndex() == 1; // index 0 is java, 1 is python
    }

    // =========================================================
    // 2. USER INPUT COMPONENTS
    // =========================================================

    // Text field containing the submissions folder path.
    // System.getProperty("user.dir") gets the current working directory.
    // Path.of(...) adds the "submissions" folder to that directory.
    // toAbsolutePath() converts it into a full path.
    // normalize() removes unnecessary path elements.
    // 35 specifies the preferred text field width in columns.
    final JTextField submissionsField = new JTextField(
            Path.of(System.getProperty("user.dir"), "submissions", "java", "original")
                .toAbsolutePath().normalize().toString(), 35
    );

    // Allows users to search for specific student submissions.
    final JTextField searchField = new JTextField(22);

    // Dropdown for filtering students by validation status.
    final JComboBox<String> statusFilter = new JComboBox<>(new String[]{
            "All",
            "Needs Attention",
            "Valid",
            "Corrected",
            "Review",
            "Failed"
    });

    // =========================================================
    // 3. BUTTON COMPONENTS
    // =========================================================

    // Opens a folder selection dialog.
    final JButton browseButton = new JButton("Browse");

    // Starts submission validation.
    final JButton validateButton = new JButton("1. Validate Submissions");

    // Starts grading using the original assessment testers.
    final JButton gradeOriginalButton = new JButton("Start Grading (Original Testers)");

    // Generates additional testers.
    final JButton generateTestersButton = new JButton("Generate Testers");

    // Grades submissions using the generated testers.
    final JButton gradeGeneratedButton = new JButton("Grade with Generated Testers");

    // Runs additional edge-case tests.
    final JButton edgeCaseButton = new JButton("Run Edge Cases (Bonus)");

    // Exports grading results.
    final JButton exportButton = new JButton("Export Results");

    // Allows professor to resolve submissions flagged for review.
    final JButton reviewButton = new JButton("Review Issues");

    // Allows manual score adjustments.
    final JButton manualButton = new JButton("Manual Score (Optional)");

    // Java-only generated tester section.
    final JPanel generatedTesterPanel = new JPanel(new BorderLayout(6, 6));
    final JPanel gradingFlowsPanel = new JPanel(new GridLayout(1, 2, 10, 0));

    // =========================================================
    // 4. APPLICATION STATE
    // =========================================================

    // Tracks whether the application is currently performing an operation, such as validation or grading.
    boolean busy;

    // Tracks whether generated testers are ready to be used for the current grading run.
    boolean generatedTestersReadyForCurrentRun;

    // Stores StudentResult objects for processed students.
    // Each StudentResult contains information about a student's submission and grading outcome.
    final ArrayList<StudentResult> studentResults = new ArrayList<>();

    // Stores generated-tester total scores as strings.
    // These can be displayed alongside original tester scores.
    final ArrayList<String> generatedTotals = new ArrayList<>();

    // Stores latest edge-case results so they can be included in the PDF report.
    final java.util.Map<String, autograder.grading.EdgeCaseRunner.Result> edgeCaseResults =
            new java.util.LinkedHashMap<>();

    // GUI-only similarity results; grading and exported marks remain independent.
    final java.util.Map<String, PlagiarismCheck.Result> plagiarismResults =
            new java.util.LinkedHashMap<>();

    // =========================================================
    // 5. SUMMARY AND LOG COMPONENTS
    // =========================================================

    // Button for clearing messages displayed in the log area.
    final JButton clearLogButton = new JButton("Clear Log");

    // Labels showing submission statistics.
    // They initially display zero before validation begins.
    final JLabel totalLabel = new JLabel("Total: 0");
    final JLabel validLabel = new JLabel("Valid: 0");
    final JLabel correctedLabel = new JLabel("Corrected: 0");
    final JLabel reviewLabel = new JLabel("Review: 0");
    final JLabel failedLabel = new JLabel("Failed: 0");

    // Displays detailed information about a selected student.
    final JTextArea detailsArea = new JTextArea();

    // Displays application logs and progress messages.
    final JTextArea logArea = new JTextArea();

    // Displays progress during operations such as grading.
    final JProgressBar progressBar = new JProgressBar();

    // =========================================================
    // 6. STUDENT RESULTS TABLE
    // =========================================================

    // TableModel stores the data shown inside the student table.
    final DefaultTableModel tableModel;
    final JTable resultTable;

    // TableRowSorter enables sorting and filtering of table rows.
    final TableRowSorter<DefaultTableModel> sorter;

    // =========================================================
    // 7. CORRECTION SUMMARY TABLE
    // =========================================================

    // Stores correction-related data for each student.
    final DefaultTableModel correctionTableModel;

    // Displays the correction summary.
    final JTable correctionTable;

    // Enables sorting and filtering of correction records.
    final TableRowSorter<DefaultTableModel> correctionSorter;

    // Displays detailed correction information.
    // 6 = preferred number of rows.
    // 40 = preferred number of columns.
    final JTextArea correctionDetailArea = new JTextArea(6, 40);

    // Displays which student required the most fixes.
    final JLabel mostFixesLabel = new JLabel("Most fixes: -");

    // =========================================================
    // 8. CONTROLLERS AND HELPER OBJECTS
    // =========================================================

    /*
     * These objects handle different parts of the application.
     * Each constructor receives "this", which refers to the current MainFrame object
     * This allows the helper classes to access shared GUI components and application state in MainFrame.
     */

    // Builds the correction summary panel and its components.
    final CorrectionPanelBuilder correctionPanel = new CorrectionPanelBuilder(this);

    // Creates and arranges the main GUI layout.
    final MainFrameLayout layout = new MainFrameLayout(this);

    // Connects user actions to application functionality.
    final MainFrameActions actions = new MainFrameActions(this);

    // Manages displaying and filtering student results.
    final SubmissionResultsController results = new SubmissionResultsController(this);

    // Coordinates submission validation from the GUI.
    final ValidationController validation = new ValidationController(this);

    // Updates GUI state, progress indicators, and logs.
    final UiStateController uiState = new UiStateController(this);

    // Handles additional edge-case testing actions.
    final EdgeCaseController edgeCases = new EdgeCaseController(this);

    // Handles tester generation and availability checks.
    final TesterActionsController testers = new TesterActionsController(this);

    // Handles Python grading actions.
    final PythonGradingController pythonGrading = new PythonGradingController(this);

    // Handles Java grading actions.
    final JavaGradingController javaGrading = new JavaGradingController(this);

    // Handles professor review decisions.
    final ReviewController review = new ReviewController(this);

    // Handles manual score adjustments.
    final ManualScoreController manualScore = new ManualScoreController(this);

    // Handles exporting grading reports.
    final ExportController export = new ExportController(this);

    // =========================================================
    // 9. MAINFRAME CONSTRUCTOR
    // =========================================================

    /**
     * Constructor: MainFrame()
     * Automatically executes when Main.java calls:
     *     new MainFrame();
     * Initializes the window, creates the results tables,
     * builds the interface, and connects button actions.
     */
    public MainFrame() {
        setTitle("CS102 Auto Grading System"); // title of the application window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close the application when the window is closed.
        setMinimumSize(new Dimension(1050, 680));
        setSize(1200, 760);
        setLocationRelativeTo(null);

        // =====================================================
        // 10. INITIALIZE STUDENT RESULTS TABLE
        // =====================================================

        // Create a table model with eight columns.
        // The second argument (0) means the table starts with zero rows.
        tableModel = new DefaultTableModel(
                new Object[]{
                        "Student",
                        "Structure",
                        "Headers",
                        "Status",
                        "Grading",
                        "Total (Original Testers)",
                        "Total (Generated Testers)",
                        "Plagiarism Check",
                        "Review Status"
                },
                0
        ) {
            // Override the default method so users cannot
            // directly edit table cells by clicking them.
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create the visible JTable and a sorter using the table model.
        resultTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);

        // Automatically re-sort rows when table data changes.
        sorter.setSortsOnUpdates(true);
        resultTable.setRowSorter(sorter);

        // =====================================================
        // 11. INITIALIZE CORRECTION SUMMARY TABLE
        // =====================================================

        // Create the correction table with eight columns.
        // Initially, the table contains zero rows.
        correctionTableModel = new DefaultTableModel(
                new Object[]{
                        "Student",
                        "Folder",
                        "Name",
                        "Email",
                        "Open Issues",
                        "Anomalies",
                        "Total Fixes",
                        "Status"
                },
                0
        ) {
            // Prevent direct editing of correction table cells.
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            // Specify the data type of each column.
            // Columns 1-6 contain integer counts.
            // Other columns contain strings.
            // This helps the sorter compare numbers numerically rather than alphabetically.
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex >= 1 && columnIndex <= 6) return Integer.class;
                return String.class;
            }
        };

        // Create the correction JTable and a sorter
        correctionTable = new JTable(correctionTableModel);
        correctionSorter = new TableRowSorter<>(correctionTableModel);

        // Re-sort when correction data changes.
        correctionSorter.setSortsOnUpdates(true);
        correctionTable.setRowSorter(correctionSorter);

        // =====================================================
        // 12. FINAL GUI INITIALIZATION
        // =====================================================

        // Apply status-based colors to the tables.
        // Build and arrange all GUI panels and components.
        // Implementation is inside MainFrameLayout.java.
        layout.applyStatusColors();
        layout.buildInterface();

        // Register button listeners and other user actions.
        // Implementation is inside MainFrameActions.java.
        actions.connectActions();

        // The constructor finishes here.
        // Main.java then calls frame.setVisible(true) to display the completed window.
    }
}