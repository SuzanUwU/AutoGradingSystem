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
                String username = getUsernameFromFileName(zipFile.getName());
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
    private SubmissionResult processSubmission(File zipFile, SubmissionState state, Consumer<String> logger) throws IOException {
        ZipExtractor zipExtractor = new ZipExtractor();
        SubmissionValidator validator = new SubmissionValidator(logger);
        SubmissionCorrector corrector = new SubmissionCorrector(logger);
        String fileName = zipFile.getName();
        String submissionName = removeZipExtension(fileName);
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
        displayQuestionFolders(questionFolders, validator, expectedUsername, logger);
        logger.accept("\nCorrecting Java headers...");
        state.stage = Stage.HEADERS;
        corrector.correctHeaders(correctedRoot, expectedUsername);
        state.stage = Stage.NONE;
        FinalInspection inspection = inspectFinalSubmission(correctedRoot, expectedUsername, validator);
        String structureStatus = inspection.structureIssue ? "Review" : state.structureCorrected ? "Corrected" : "Valid";
        String headerStatus = inspection.headerIssue ? "Review" : state.headerCorrected ? "Corrected" : "Valid";
        String overallStatus = getOverallStatus(structureStatus, headerStatus);
        String details = buildDetails(zipFile, expectedUsername, correctedRoot, structureStatus, headerStatus,
                overallStatus, inspection, state, validator);
        return new SubmissionResult(expectedUsername, structureStatus, headerStatus, overallStatus, details);
    }
    private FinalInspection inspectFinalSubmission(File root, String username, SubmissionValidator validator) throws IOException {
        FinalInspection inspection = new FinalInspection();
        inspection.structureIssue = !root.isDirectory() || !root.getName().equalsIgnoreCase(username);
        inspection.questionFolders = validator.findQuestionFolders(root);
        for (Map.Entry<String, File> entry : inspection.questionFolders.entrySet()) {
            File questionFolder = entry.getValue();
            if (questionFolder == null) {
                inspection.structureIssue = true;
                continue;
            }
            for (String fileName : getRequiredFiles(entry.getKey())) {
                if (!new File(questionFolder, fileName).isFile()) inspection.structureIssue = true;
            }
        }
        collectJavaFiles(root, inspection.javaFiles);
        inspection.javaFiles.sort(Comparator.comparing(File::getPath));
        for (File javaFile : inspection.javaFiles) {
            if (!validator.hasValidHeader(javaFile, username)) inspection.headerIssue = true;
        }
        return inspection;
    }
    private String buildDetails(File zipFile, String username, File root, String structureStatus, String headerStatus,
            String overallStatus, FinalInspection inspection, SubmissionState state, SubmissionValidator validator) throws IOException {
        StringBuilder details = new StringBuilder();
        details.append("STUDENT\nUsername: ").append(username)
                .append("\nOverall status: ").append(overallStatus).append("\n");
        details.append("\nSUBMISSION\n[OK] ZIP filename identifies: ").append(username)
                .append("\nZIP file: ").append(zipFile.getName())
                .append("\nUsername folder: ").append(structureStatus)
                .append("\nWorkspace path: ").append(root.getPath()).append("\n");
        details.append("\nQUESTION FOLDERS\n");
        for (Map.Entry<String, File> entry : inspection.questionFolders.entrySet()) {
            details.append(entry.getValue() == null ? "[MISSING] " : "[OK] ")
                    .append(entry.getKey()).append("\n");
        }
        details.append("\nREQUIRED JAVA FILES\n");
        for (Map.Entry<String, File> entry : inspection.questionFolders.entrySet()) {
            if (entry.getValue() == null) continue;
            for (String fileName : getRequiredFiles(entry.getKey())) {
                File javaFile = new File(entry.getValue(), fileName);
                details.append(javaFile.isFile() ? "[OK] " : "[MISSING] ")
                        .append(entry.getKey()).append("/").append(fileName).append("\n");
            }
        }
        details.append("\nJAVA HEADERS - FINAL STATE\n");
        if (inspection.javaFiles.isEmpty()) {
            details.append("[MISSING] No Java files found\n");
        } else {
            for (File javaFile : inspection.javaFiles) {
                String path = root.toPath().relativize(javaFile.toPath()).toString();
                String[] header = validator.getHeaderValues(javaFile);
                boolean valid = validator.hasValidHeader(javaFile, username);
                details.append(valid ? "[OK] " : "[REVIEW] ").append(path).append("\n")
                        .append("    Name: ").append(valueOrMissing(header[0])).append("\n")
                        .append("    Email ID: ").append(valueOrMissing(header[1])).append("\n");
            }
        }
        details.append("\nSUMMARY\nStructure: ").append(structureStatus)
                .append("\nHeaders: ").append(headerStatus).append("\n")
                .append("\nCHANGES / WARNINGS\n");
        if (state.notableMessages.isEmpty()) details.append("None\n");
        else for (String message : state.notableMessages) details.append(message).append("\n");
        return details.toString();
    }
    private SubmissionResult createFailedResult(String username, File zipFile, IOException e) {
        String details = "STUDENT\nUsername: " + username
                + "\nOverall status: Failed"
                + "\n\nSUBMISSION\nZIP file: " + zipFile.getName()
                + "\n\nERROR\n" + e.getMessage();
        return new SubmissionResult(username, "Failed", "Failed", "Failed", details);
    }
    private void displayQuestionFolders(Map<String, File> folders, SubmissionValidator validator,
            String username, Consumer<String> logger) throws IOException {
        for (Map.Entry<String, File> entry : folders.entrySet()) {
            String question = entry.getKey();
            File folder = entry.getValue();
            if (folder == null) {
                logger.accept("[MISSING] " + question + " folder not found.");
                continue;
            }
            logger.accept("[OK] " + question + " found: " + folder.getPath());
            for (String fileName : getRequiredFiles(question)) {
                File javaFile = new File(folder, fileName);
                if (validator.hasFile(folder, fileName)) {
                    logger.accept("    [OK] " + fileName);
                    validator.validateHeader(javaFile, username);
                } else {
                    logger.accept("    [MISSING] " + fileName);
                }
            }
        }
    }
    private String[] getRequiredFiles(String question) {
        if (question.equals("Q1")) return new String[] {"Q1a.java", "Q1b.java"};
        if (question.equals("Q2")) return new String[] {"Q2a.java", "Q2b.java"};
        if (question.equals("Q3")) return new String[] {"Q3.java"};
        return new String[0];
    }
    private String getOverallStatus(String structureStatus, String headerStatus) {
        if (structureStatus.equals("Review") || headerStatus.equals("Review")) return "Review";
        if (structureStatus.equals("Corrected") || headerStatus.equals("Corrected")) return "Corrected";
        return "Valid";
    }
    private String valueOrMissing(String value) {
        return value == null || value.isEmpty() ? "MISSING" : value;
    }
    private void collectJavaFiles(File folder, ArrayList<File> javaFiles) {
        if (folder == null || !folder.isDirectory()) return;
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) collectJavaFiles(file, javaFiles);
            else if (file.getName().toLowerCase().endsWith(".java")) javaFiles.add(file);
        }
    }
    private File[] findZipFiles(File folder) {
        if (folder == null || !folder.isDirectory()) return new File[0];
        File[] files = folder.listFiles(this::isZipFile);
        if (files == null) return new File[0];
        Arrays.sort(files, Comparator.comparing(File::getName));
        return files;
    }
    private boolean isZipFile(File file) {
        return file.isFile() && file.getName().toLowerCase().endsWith(ZIP_EXTENSION);
    }
    private String removeZipExtension(String fileName) {
        return fileName.substring(0, fileName.length() - ZIP_EXTENSION.length());
    }
    private String getUsernameFromFileName(String fileName) {
        String name = fileName.toLowerCase().endsWith(ZIP_EXTENSION) ? removeZipExtension(fileName) : fileName;
        return name.replaceFirst("^\\d{4}-\\d{4}-", "");
    }
    private enum Stage { NONE, STRUCTURE, VALIDATION, HEADERS }
    private static class SubmissionState {
        private Stage stage = Stage.NONE;
        private boolean structureCorrected;
        private boolean headerCorrected;
        private final ArrayList<String> notableMessages = new ArrayList<>();
        private void track(String message) {
            String text = message.trim();
            if (text.startsWith("[CORRECTED]")) {
                if (stage == Stage.STRUCTURE) structureCorrected = true;
                if (stage == Stage.HEADERS) headerCorrected = true;
                notableMessages.add(text);
            } else if (text.startsWith("[WARNING]") || text.startsWith("[MISSING]") || text.startsWith("[ERROR]")) {
                notableMessages.add(text);
            }
        }
    }
    private static class FinalInspection {
        private boolean structureIssue;
        private boolean headerIssue;
        private Map<String, File> questionFolders;
        private final ArrayList<File> javaFiles = new ArrayList<>();
    }
    public interface Listener {
        void onLog(String message);
        void onSubmissionFinished(SubmissionResult result);
        void onProgress(int processed, int total);
    }
}
