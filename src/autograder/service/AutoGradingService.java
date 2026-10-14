package autograder.service;

import autograder.model.SubmissionResult;
import autograder.validation.SubmissionCorrector;
import autograder.validation.SubmissionValidator;
import autograder.validation.ZipExtractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;

public class AutoGradingService {
    private static final String ZIP_EXTENSION = ".zip";
    private final File workspaceFolder;

    public AutoGradingService(File workspaceFolder) {
        this.workspaceFolder = workspaceFolder;
    }

    public void processSubmissions(File submissionsFolder, Listener listener) {
        File[] zipFiles = findZipFiles(submissionsFolder);

        if (zipFiles.length == 0) {
            listener.onLog("No ZIP submissions found.");
            listener.onProgress(0, 0);
            return;
        }

        listener.onLog("Found " + zipFiles.length + " submission(s).");
        listener.onLog("");

        int completed = 0;
        int failed = 0;

        for (File zipFile : zipFiles) {
            SubmissionState state = new SubmissionState();

            Consumer<String> logger = message -> {
                state.track(message);
                listener.onLog(message);
            };

            SubmissionValidator validator = new SubmissionValidator(logger);
            SubmissionCorrector corrector = new SubmissionCorrector(logger);
            ZipExtractor zipExtractor = new ZipExtractor();

            try {
                SubmissionResult result = processSubmission(zipFile, zipExtractor, corrector, validator, state, logger);
                completed++;
                listener.onSubmissionFinished(result);
            } catch (IOException e) {
                failed++;

                String username = getUsernameFromFileName(zipFile.getName());

                listener.onLog("[ERROR] Could not process " + zipFile.getName() + ": " + e.getMessage());

                String details = "STUDENT\nUsername: " + username
                        + "\nOverall status: Failed"
                        + "\n\nSUBMISSION\nZIP file: " + zipFile.getName()
                        + "\n\nERROR\n" + e.getMessage();

                listener.onSubmissionFinished(
                        new SubmissionResult(username, "Failed", "Failed", "Failed", details));
            }

            listener.onLog("----------------------------------------");
            listener.onProgress(completed + failed, zipFiles.length);
        }

        listener.onLog("Batch processing finished.");
        listener.onLog("Completed: " + completed);
        listener.onLog("Failed: " + failed);
    }

    private SubmissionResult processSubmission(
            File zipFile,
            ZipExtractor zipExtractor,
            SubmissionCorrector corrector,
            SubmissionValidator validator,
            SubmissionState state,
            Consumer<String> logger) throws IOException {

        String fileName = zipFile.getName();
        String submissionName = removeZipExtension(fileName);
        String expectedUsername = validator.validateAndGetUsername(zipFile);
        File destination = new File(workspaceFolder, submissionName);

        logger.accept("Processing: " + fileName);
        logger.accept("Expected username: " + expectedUsername);
        logger.accept("");
        logger.accept("Extracting submission...");

        zipExtractor.extract(zipFile, destination);
        logger.accept("Extraction successful.");

        state.stage = Stage.STRUCTURE;
        File correctedRoot = corrector.correctStructure(destination, expectedUsername);

        logger.accept("");
        logger.accept("Checking submission...");

        state.stage = Stage.VALIDATION;

        Map<String, File> questionFolders = validator.findQuestionFolders(correctedRoot);
        displayQuestionFolders(questionFolders, validator, expectedUsername, logger);

        logger.accept("");
        logger.accept("Correcting Java headers...");

        state.stage = Stage.HEADERS;
        corrector.correctHeaders(correctedRoot, expectedUsername);
        state.stage = Stage.NONE;

        FinalInspection inspection = inspectFinalSubmission(correctedRoot, expectedUsername, validator);

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

        String overallStatus =
                structureStatus.equals("Review") || headerStatus.equals("Review")
                        ? "Review"
                        : structureStatus.equals("Corrected") || headerStatus.equals("Corrected")
                        ? "Corrected"
                        : "Valid";

        String details = buildDetails(
                zipFile,
                expectedUsername,
                correctedRoot,
                structureStatus,
                headerStatus,
                overallStatus,
                inspection,
                state,
                validator);

        return new SubmissionResult(
                expectedUsername,
                structureStatus,
                headerStatus,
                overallStatus,
                details);
    }

    private FinalInspection inspectFinalSubmission(
            File submissionRoot,
            String expectedUsername,
            SubmissionValidator validator) throws IOException {

        FinalInspection inspection = new FinalInspection();

        if (!submissionRoot.isDirectory()
                || !submissionRoot.getName().equalsIgnoreCase(expectedUsername)) {
            inspection.structureIssue = true;
        }

        inspection.questionFolders = validator.findQuestionFolders(submissionRoot);

        for (Map.Entry<String, File> entry : inspection.questionFolders.entrySet()) {
            File questionFolder = entry.getValue();

            if (questionFolder == null) {
                inspection.structureIssue = true;
                continue;
            }

            for (String fileName : getRequiredFiles(entry.getKey())) {
                if (!new File(questionFolder, fileName).isFile()) {
                    inspection.structureIssue = true;
                }
            }
        }

        collectJavaFiles(submissionRoot, inspection.javaFiles);
        inspection.javaFiles.sort(Comparator.comparing(File::getPath));

        for (File javaFile : inspection.javaFiles) {
            if (!validator.hasValidHeader(javaFile, expectedUsername)) {
                inspection.headerIssue = true;
            }
        }

        return inspection;
    }

    private String buildDetails(
            File zipFile,
            String expectedUsername,
            File submissionRoot,
            String structureStatus,
            String headerStatus,
            String overallStatus,
            FinalInspection inspection,
            SubmissionState state,
            SubmissionValidator validator) throws IOException {

        StringBuilder details = new StringBuilder();

        details.append("STUDENT\n");
        details.append("Username: ").append(expectedUsername).append("\n");
        details.append("Overall status: ").append(overallStatus).append("\n");

        details.append("\nSUBMISSION\n");
        details.append("[OK] ZIP filename identifies: ").append(expectedUsername).append("\n");
        details.append("ZIP file: ").append(zipFile.getName()).append("\n");
        details.append("Username folder: ").append(structureStatus).append("\n");
        details.append("Workspace path: ").append(submissionRoot.getPath()).append("\n");

        details.append("\nQUESTION FOLDERS\n");

        for (Map.Entry<String, File> entry : inspection.questionFolders.entrySet()) {
            if (entry.getValue() == null) {
                details.append("[MISSING] ").append(entry.getKey()).append("\n");
            } else {
                details.append("[OK] ").append(entry.getKey()).append("\n");
            }
        }

        details.append("\nREQUIRED JAVA FILES\n");

        for (Map.Entry<String, File> entry : inspection.questionFolders.entrySet()) {
            if (entry.getValue() == null) continue;

            for (String fileName : getRequiredFiles(entry.getKey())) {
                File javaFile = new File(entry.getValue(), fileName);

                details.append(javaFile.isFile() ? "[OK] " : "[MISSING] ")
                        .append(entry.getKey())
                        .append("/")
                        .append(fileName)
                        .append("\n");
            }
        }

        details.append("\nJAVA HEADERS - FINAL STATE\n");

        if (inspection.javaFiles.isEmpty()) {
            details.append("[MISSING] No Java files found\n");
        } else {
            for (File javaFile : inspection.javaFiles) {
                String relativePath = submissionRoot
                        .toPath()
                        .relativize(javaFile.toPath())
                        .toString();

                String[] header = validator.getHeaderValues(javaFile);

                details.append(
                                validator.hasValidHeader(javaFile, expectedUsername)
                                        ? "[OK] "
                                        : "[REVIEW] ")
                        .append(relativePath)
                        .append("\n");

                details.append("    Name: ")
                        .append(
                                header[0] == null || header[0].isEmpty()
                                        ? "MISSING"
                                        : header[0])
                        .append("\n");

                details.append("    Email ID: ")
                        .append(
                                header[1] == null || header[1].isEmpty()
                                        ? "MISSING"
                                        : header[1])
                        .append("\n");
            }
        }

        details.append("\nSUMMARY\n");
        details.append("Structure: ").append(structureStatus).append("\n");
        details.append("Headers: ").append(headerStatus).append("\n");

        details.append("\nCHANGES / WARNINGS\n");

        if (state.notableMessages.isEmpty()) {
            details.append("None\n");
        } else {
            for (String message : state.notableMessages) {
                details.append(message).append("\n");
            }
        }

        return details.toString();
    }

    private void displayQuestionFolders(
            Map<String, File> questionFolders,
            SubmissionValidator validator,
            String expectedUsername,
            Consumer<String> logger) throws IOException {

        for (Map.Entry<String, File> entry : questionFolders.entrySet()) {
            String questionName = entry.getKey();
            File questionFolder = entry.getValue();

            if (questionFolder == null) {
                logger.accept("[MISSING] " + questionName + " folder not found.");
            } else {
                logger.accept("[OK] " + questionName + " found: " + questionFolder.getPath());

                for (String fileName : getRequiredFiles(questionName)) {
                    displayFileStatus(
                            questionFolder,
                            fileName,
                            validator,
                            expectedUsername,
                            logger);
                }
            }
        }
    }

    private void displayFileStatus(
            File questionFolder,
            String fileName,
            SubmissionValidator validator,
            String expectedUsername,
            Consumer<String> logger) throws IOException {

        File javaFile = new File(questionFolder, fileName);

        if (validator.hasFile(questionFolder, fileName)) {
            logger.accept("    [OK] " + fileName);
            validator.validateHeader(javaFile, expectedUsername);
        } else {
            logger.accept("    [MISSING] " + fileName);
        }
    }

    private String[] getRequiredFiles(String questionName) {
        if (questionName.equals("Q1")) {
            return new String[] {"Q1a.java", "Q1b.java"};
        }

        if (questionName.equals("Q2")) {
            return new String[] {"Q2a.java", "Q2b.java"};
        }

        if (questionName.equals("Q3")) {
            return new String[] {"Q3.java"};
        }

        return new String[0];
    }

    private void collectJavaFiles(File folder, ArrayList<File> javaFiles) {
        if (folder == null || !folder.isDirectory()) return;

        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectJavaFiles(file, javaFiles);
            } else if (file.getName().toLowerCase().endsWith(".java")) {
                javaFiles.add(file);
            }
        }
    }

    private File[] findZipFiles(File submissionsFolder) {
        if (submissionsFolder == null || !submissionsFolder.isDirectory()) {
            return new File[0];
        }

        File[] files = submissionsFolder.listFiles(this::isZipFile);

        if (files == null) return new File[0];

        Arrays.sort(files, Comparator.comparing(File::getName));
        return files;
    }

    private boolean isZipFile(File file) {
        return file.isFile()
                && file.getName().toLowerCase().endsWith(ZIP_EXTENSION);
    }

    private String removeZipExtension(String fileName) {
        return fileName.substring(
                0,
                fileName.length() - ZIP_EXTENSION.length());
    }

    private String getUsernameFromFileName(String fileName) {
        String submissionName =
                fileName.toLowerCase().endsWith(ZIP_EXTENSION)
                        ? removeZipExtension(fileName)
                        : fileName;

        return submissionName.replaceFirst("^\\d{4}-\\d{4}-", "");
    }

    private enum Stage {
        NONE,
        STRUCTURE,
        VALIDATION,
        HEADERS
    }

    private static class SubmissionState {
        private Stage stage = Stage.NONE;
        private boolean structureCorrected;
        private boolean headerCorrected;

        private final ArrayList<String> notableMessages =
                new ArrayList<String>();

        private void track(String message) {
            String text = message.trim();

            if (text.startsWith("[CORRECTED]")) {
                if (stage == Stage.STRUCTURE) structureCorrected = true;
                if (stage == Stage.HEADERS) headerCorrected = true;

                notableMessages.add(text);

            } else if (text.startsWith("[WARNING]")
                    || text.startsWith("[MISSING]")
                    || text.startsWith("[ERROR]")) {

                notableMessages.add(text);
            }
        }
    }

    private static class FinalInspection {
        private boolean structureIssue;
        private boolean headerIssue;

        private Map<String, File> questionFolders;

        private final ArrayList<File> javaFiles =
                new ArrayList<File>();
    }

    public interface Listener {
        void onLog(String message);

        void onSubmissionFinished(SubmissionResult result);

        void onProgress(int processed, int total);
    }
}