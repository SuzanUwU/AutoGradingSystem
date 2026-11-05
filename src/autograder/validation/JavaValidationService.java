
package autograder.validation;

import autograder.model.SubmissionResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

public class JavaValidationService {

    // Location of the original assignment template and student submissions
    private static final String TEMPLATE_FOLDER = "assessment/template";
    private final File workspaceFolder; 

    // Checks whether a submission is identical to the template.
    private final TemplateComparator templateComparator;

    // Inspects submission structure, headers, and remaining issues.
    private final SubmissionInspector inspector =
            new SubmissionInspector();

    // Builds detailed validation feedback for each student.
    private final SubmissionDetailsBuilder detailsBuilder =
            new SubmissionDetailsBuilder();

    /**
     * Initializes the workspace and template comparator.
     */
    public JavaValidationService(File workspaceFolder) {
        this.workspaceFolder = workspaceFolder;
        this.templateComparator =
                new TemplateComparator(new File(TEMPLATE_FOLDER));
    }

    // =====================================================
    // 1. PROCESS ALL STUDENT SUBMISSIONS
    // =====================================================

    /**
     * Validates all ZIP submissions in the selected folder.
     * Reports logs, results, and progress through Listener.
     */
    public void processSubmissions(
            File submissionsFolder,
            Listener listener) {

        // Find all ZIP files in the selected folder.
        File[] zipFiles =
                SubmissionFileUtils.findZipFiles(submissionsFolder);

        // Stop if there are no submissions.
        if (zipFiles.length == 0) {
            listener.onLog(
                    "No ZIP submissions found in: "
                    + submissionsFolder.getAbsolutePath()
            );
            listener.onProgress(0, 0);
            return;
        }

        // Remove files left from previous validation runs.
        try {
            new WorkspaceCleaner(workspaceFolder).cleanWorkspace();
        } catch (IOException e) {
            listener.onLog(
                    "[ERROR] Could not clear old workspace: "
                    + e.getMessage()
            );
            listener.onProgress(0, zipFiles.length);
            return;
        }

        listener.onLog(
                "Found " + zipFiles.length + " submission(s).\n"
        );

        int completed = 0;
        int failed = 0;

        // Process each student's ZIP file individually.
        for (File zipFile : zipFiles) {

            // Stores issues and corrections for this submission.
            SubmissionState state = new SubmissionState();

            // Logger records messages in both the submission state
            // and the GUI through the listener.
            Consumer<String> logger = message -> {
                state.track(message);
                listener.onLog(message);
            };

            try {
                // Validate and correct the current submission.
                SubmissionResult result =
                        processSubmission(zipFile, state, logger);

                // Send the completed result to the GUI.
                listener.onSubmissionFinished(result);
                completed++;

            } catch (IOException e) {

                // Record a failed submission without stopping
                // the entire batch.
                String username =
                        SubmissionFileUtils.getUsernameFromFileName(
                                zipFile.getName()
                        );

                listener.onLog(
                        "[ERROR] Could not process "
                        + zipFile.getName()
                        + ": "
                        + e.getMessage()
                );

                listener.onSubmissionFinished(
                        createFailedResult(username, zipFile, e)
                );

                failed++;
            }

            // Update progress after processing each submission.
            listener.onLog("----------------------------------------");
            listener.onProgress(
                    completed + failed,
                    zipFiles.length
            );
        }

        // Display final batch statistics.
        listener.onLog("Batch processing finished.");
        listener.onLog("Completed: " + completed);
        listener.onLog("Failed: " + failed);
    }

    // =====================================================
    // 2. PROCESS ONE STUDENT SUBMISSION
    // =====================================================

    /**
     * Extracts, validates, corrects, and inspects one ZIP file.
     * Returns a SubmissionResult containing statuses and issues.
     */
    private SubmissionResult processSubmission(
            File zipFile,
            SubmissionState state,
            Consumer<String> logger) throws IOException {

        // Create the tools needed for this submission.
        ZipExtractor zipExtractor = new ZipExtractor();
        SubmissionValidator validator =
                new SubmissionValidator(logger);

        SubmissionCorrector corrector =
                new SubmissionCorrector(
                        logger,
                        state::addSubmissionIssue
                );

        // Identify the submission and expected username.
        String fileName = zipFile.getName();

        String submissionName =
                SubmissionFileUtils.removeZipExtension(fileName);

        String expectedUsername =
                validator.validateAndGetUsername(zipFile);

        File destination =
                new File(workspaceFolder, submissionName);

        // STEP 1: Extract the ZIP into the workspace.
        logger.accept("Processing: " + fileName);
        logger.accept(
                "Expected username: " + expectedUsername + "\n"
        );
        logger.accept("Extracting submission...");

        zipExtractor.extract(zipFile, destination);

        logger.accept("Extraction successful.");

        // STEP 2: Correct folder and file structure.
        state.stage = Stage.STRUCTURE;

        File correctedRoot =
                corrector.correctStructure(
                        destination,
                        expectedUsername
                );

        // STEP 3: Validate question folders and contents.
        logger.accept("\nChecking submission...");
        state.stage = Stage.VALIDATION;

        Map<String, File> questionFolders =
                validator.findQuestionFolders(correctedRoot);

        inspector.displayQuestionFolders(
                questionFolders,
                validator,
                expectedUsername,
                logger,
                state
        );

        // STEP 4: Detect submissions that contain only the original assignment template.
        // This must happen BEFORE header correction, because correction modifies student files.
        boolean templateSubmission =
                templateComparator.isUnmodifiedTemplate(
                        correctedRoot
                );

        if (templateSubmission) {
            logger.accept(
                    "[WARNING] Submission appears to be the original resource folder: "
                    + "no Java file differs from the template."
            );

            state.addSubmissionIssue(
                    "Original resource folder submitted"
            );

            state.addAnomaly(
                    "Original resource/template folder submitted"
            );
        }

        // STEP 5: Inspect existing Java header problems.
        logger.accept("\nCorrecting Java headers...");

        inspector.recordHeaderIssues(
                correctedRoot,
                expectedUsername,
                validator,
                state
        );

        // STEP 6: Automatically correct Java headers.
        state.stage = Stage.HEADERS;

        corrector.correctHeaders(
                correctedRoot,
                expectedUsername
        );

        // Reset the current processing stage.
        state.stage = Stage.NONE;

        // STEP 7: Reinspect the corrected submission.
        // Check whether any issues remain unresolved.
        FinalInspection inspection =
                inspector.inspectFinalSubmission(
                        correctedRoot,
                        expectedUsername,
                        validator
                );

        inspector.recordOpenIssues(
                correctedRoot,
                expectedUsername,
                validator,
                inspection,
                state
        );

        // STEP 8: Determine final validation statuses.
        //
        // Review    = Unresolved issues remain.
        // Corrected = Issues were automatically fixed.
        // Valid     = No issues detected in that category.
        String structureStatus =
                inspection.structureIssue
                        ? "Review"
                        : state.structureCorrected
                                ? "Corrected"
                                : "Valid";

        String headerStatus =
                inspection.headerIssue
                        ? "Review"
                        : state.headerCorrected
                                ? "Corrected"
                                : "Valid";

        // Original template submissions require review.
        String overallStatus =
                templateSubmission
                        ? "Review"
                        : SubmissionFileUtils.getOverallStatus(
                                structureStatus,
                                headerStatus
                        );

        // STEP 9: Build detailed feedback for the student.
        String details = detailsBuilder.buildDetails(
                zipFile,
                expectedUsername,
                correctedRoot,
                structureStatus,
                headerStatus,
                overallStatus,
                inspection,
                state,
                validator
        );

        // STEP 10: Return all validation results.
        return new SubmissionResult(
                expectedUsername,
                structureStatus,
                headerStatus,
                overallStatus,
                details,
                state.submissionIssues,
                state.q1Issues,
                state.q2Issues,
                state.q3Issues,
                state.folderFixes,
                state.nameFixes,
                state.emailFixes,
                state.openIssues,
                state.anomalies
        );
    }

    // =====================================================
    // 3. CREATE A RESULT FOR FAILED SUBMISSIONS
    // =====================================================

    /**
     * Creates a failed SubmissionResult when a ZIP file
     * cannot be processed due to an IOException.
     */
    private SubmissionResult createFailedResult(
            String username,
            File zipFile,
            IOException e) {

        // Build a readable error description.
        String details =
                "STUDENT\nUsername: " + username
                + "\nOverall status: Failed"
                + "\n\nSUBMISSION\nZIP file: "
                + zipFile.getName()
                + "\n\nERROR\n"
                + e.getMessage();

        // Record the processing failure as an open issue.
        ArrayList<String> openIssues = new ArrayList<>();

        openIssues.add(
                "Processing failed: " + e.getMessage()
        );

        // Return a result marked as Failed.
        return new SubmissionResult(
                username,
                "Failed",
                "Failed",
                "Failed",
                details,
                new ArrayList<String>(),
                new ArrayList<String>(),
                new ArrayList<String>(),
                new ArrayList<String>(),
                new ArrayList<String>(),
                new ArrayList<String>(),
                new ArrayList<String>(),
                openIssues,
                new ArrayList<String>()
        );
    }

    // =====================================================
    // 4. LISTENER INTERFACE
    // =====================================================

    /**
     * Defines callbacks used to communicate with the GUI.
     *
     * ValidationController implements these methods to:
     * - Display log messages.
     * - Add completed student results.
     * - Update the progress bar.
     *
     * This keeps AutoGradingService independent
     * of Swing GUI components.
     */
    public interface Listener {

        void onLog(String message);

        void onSubmissionFinished(SubmissionResult result);

        void onProgress(int processed, int total);
    }
}
