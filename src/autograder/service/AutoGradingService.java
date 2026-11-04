package autograder.service;
import autograder.model.SubmissionResult;
import autograder.validation.SubmissionCorrector;
import autograder.validation.SubmissionValidator;
import autograder.validation.ZipExtractor;
import autograder.validation.TemplateComparator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Map;

/** Coordinates the CS102 validation batch. Delegates file operations and reporting. */
public class AutoGradingService {
    // TODO: read from config/grader.properties once ConfigLoader is implemented.
    private static final String TEMPLATE_FOLDER = "assessment/template";
    private final File workspaceFolder;
    private final TemplateComparator templateComparator;
    private final SubmissionInspector inspector = new SubmissionInspector();
    private final SubmissionDetailsBuilder detailsBuilder = new SubmissionDetailsBuilder();
    public AutoGradingService(File workspaceFolder) {
        this.workspaceFolder = workspaceFolder;
        this.templateComparator = new TemplateComparator(new File(TEMPLATE_FOLDER));
    }
    /** Validates each ZIP and reports progress to the Swing UI. */
    public void processSubmissions(File submissionsFolder, Listener listener) {
        File[] zipFiles = SubmissionFileUtils.findZipFiles(submissionsFolder);
        if (zipFiles.length == 0) {
            listener.onLog("No ZIP submissions found in: " + submissionsFolder.getAbsolutePath());
            listener.onProgress(0, 0);
            return;
        }

        try {
            new WorkspaceCleaner(workspaceFolder).cleanWorkspace();
        } catch (IOException e) {
            listener.onLog("[ERROR] Could not clear old workspace: " + e.getMessage());
            listener.onProgress(0, zipFiles.length);
            return;
        }

        listener.onLog("Found " + zipFiles.length + " submission(s).\n");
        int completed = 0;
        int failed = 0;
        for (File zipFile : zipFiles) {
            SubmissionState state = new SubmissionState();
            Consumer<String> logger = message -> {
                state.track(message);
                listener.onLog(message);
            };
            try {
                SubmissionResult result = processSubmission(zipFile, state, logger);
                listener.onSubmissionFinished(result);
                completed++;
            } catch (IOException e) {
                String username = SubmissionFileUtils.getUsernameFromFileName(zipFile.getName());
                listener.onLog("[ERROR] Could not process " + zipFile.getName() + ": " + e.getMessage());
                listener.onSubmissionFinished(createFailedResult(username, zipFile, e));
                failed++;
            }
            listener.onLog("----------------------------------------");
            listener.onProgress(completed + failed, zipFiles.length);
        }
        listener.onLog("Batch processing finished.");
        listener.onLog("Completed: " + completed);
        listener.onLog("Failed: " + failed);
    }

    // for details csv


    // record header issues


    /** Runs extraction, correction, reinspection and result construction for one ZIP. */
    private SubmissionResult processSubmission(File zipFile, SubmissionState state, Consumer<String> logger) throws IOException {
        ZipExtractor zipExtractor = new ZipExtractor();
        SubmissionValidator validator = new SubmissionValidator(logger);
        SubmissionCorrector corrector = new SubmissionCorrector(logger, state::addSubmissionIssue);
        String fileName = zipFile.getName();
        String submissionName = SubmissionFileUtils.removeZipExtension(fileName);
        String expectedUsername = validator.validateAndGetUsername(zipFile);
        File destination = new File(workspaceFolder, submissionName);
        logger.accept("Processing: " + fileName);
        logger.accept("Expected username: " + expectedUsername + "\n");
        logger.accept("Extracting submission...");
        zipExtractor.extract(zipFile, destination);
        logger.accept("Extraction successful.");
        state.stage = Stage.STRUCTURE;
        File correctedRoot = corrector.correctStructure(destination, expectedUsername);
        logger.accept("\nChecking submission...");
        state.stage = Stage.VALIDATION;
        Map<String, File> questionFolders = validator.findQuestionFolders(correctedRoot);
        inspector.displayQuestionFolders(questionFolders, validator, expectedUsername, logger, state);

        // Must run before correctHeaders(), which edits the students' files.
        boolean templateSubmission = templateComparator.isUnmodifiedTemplate(correctedRoot);
        if (templateSubmission) {
            logger.accept("[WARNING] Submission appears to be the original resource folder: "
                    + "no Java file differs from the template.");
            state.addSubmissionIssue("Original resource folder submitted");
            state.addAnomaly("Original resource/template folder submitted");
        }

        logger.accept("\nCorrecting Java headers...");

        // for details csv ; check file headers
        inspector.recordHeaderIssues(correctedRoot, expectedUsername, validator, state);

        state.stage = Stage.HEADERS;
        corrector.correctHeaders(correctedRoot, expectedUsername);
        state.stage = Stage.NONE;
        FinalInspection inspection = inspector.inspectFinalSubmission(correctedRoot, expectedUsername, validator);
        inspector.recordOpenIssues(correctedRoot, expectedUsername, validator, inspection, state);
        String structureStatus = inspection.structureIssue ? "Review" : state.structureCorrected ? "Corrected" : "Valid";
        String headerStatus = inspection.headerIssue ? "Review" : state.headerCorrected ? "Corrected" : "Valid";
        String overallStatus = templateSubmission ? "Review" : SubmissionFileUtils.getOverallStatus(structureStatus, headerStatus);
        String details = detailsBuilder.buildDetails(zipFile, expectedUsername, correctedRoot, structureStatus, headerStatus,
                overallStatus, inspection, state, validator);

        return new SubmissionResult(expectedUsername, structureStatus, headerStatus, overallStatus, details,
                state.submissionIssues, state.q1Issues, state.q2Issues, state.q3Issues,
                state.folderFixes, state.nameFixes, state.emailFixes, state.openIssues, state.anomalies);
    }

    private SubmissionResult createFailedResult(String username, File zipFile, IOException e) {
        String details = "STUDENT\nUsername: " + username
                + "\nOverall status: Failed"
                + "\n\nSUBMISSION\nZIP file: " + zipFile.getName()
                + "\n\nERROR\n" + e.getMessage();
        ArrayList<String> openIssues = new ArrayList<>();
        openIssues.add("Processing failed: " + e.getMessage());
        return new SubmissionResult(username, "Failed", "Failed", "Failed", details,
                new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(),
                new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), openIssues, new ArrayList<String>());
    }

    public interface Listener {
        void onLog(String message);
        void onSubmissionFinished(SubmissionResult result);
        void onProgress(int processed, int total);
    }
}
