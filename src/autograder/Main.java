package autograder;

import autograder.validation.SubmissionCorrector;
import autograder.validation.SubmissionValidator;
import autograder.validation.ZipExtractor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

public class Main {
    private static final String ZIP_EXTENSION = ".zip";

    public static void main(String[] args) {
        File submissionsFolder = new File("submissions");
        File workspaceFolder = new File("workspace");

        ZipExtractor zipExtractor = new ZipExtractor();
        SubmissionValidator validator = new SubmissionValidator();
        SubmissionCorrector corrector = new SubmissionCorrector();

        File[] zipFiles = findZipFiles(submissionsFolder);

        if (zipFiles.length == 0) {
            System.out.println("No ZIP submissions found.");
            return;
        }

        System.out.println("Found " + zipFiles.length + " submission(s).");
        System.out.println();

        int completed = 0;
        int failed = 0;

        for (File zipFile : zipFiles) {
            try {
                processSubmission(zipFile, workspaceFolder, zipExtractor, corrector, validator);
                completed++;
            } catch (IOException e) {
                failed++;
                System.out.println("[ERROR] Could not process " + zipFile.getName() + ": " + e.getMessage());
            }

            System.out.println("----------------------------------------");
        }

        System.out.println("Batch processing finished.");
        System.out.println("Completed: " + completed);
        System.out.println("Failed: " + failed);
    }

    private static void processSubmission(File zipFile, File workspaceFolder, ZipExtractor zipExtractor, SubmissionCorrector corrector, SubmissionValidator validator) throws IOException {
        String fileName = zipFile.getName();
        String submissionName = removeZipExtension(fileName);
        String expectedUsername = validator.validateAndGetUsername(zipFile);
        File destination = new File(workspaceFolder, submissionName);

        System.out.println("Processing: " + fileName);
        System.out.println("Expected username: " + expectedUsername);
        System.out.println();
        System.out.println("Extracting submission...");

        zipExtractor.extract(zipFile, destination);
        System.out.println("Extraction successful.");

        File correctedRoot = corrector.correctStructure(destination, expectedUsername);

        System.out.println();
        System.out.println("Checking submission...");
        Map<String, File> questionFolders = validator.findQuestionFolders(correctedRoot);
        displayQuestionFolders(questionFolders, validator, expectedUsername);

        System.out.println();
        System.out.println("Correcting Java headers...");
        corrector.correctHeaders(correctedRoot, expectedUsername);
    }

    private static void displayQuestionFolders(Map<String, File> questionFolders, SubmissionValidator validator, String expectedUsername) throws IOException {
        for (Map.Entry<String, File> entry : questionFolders.entrySet()) {
            String questionName = entry.getKey();
            File questionFolder = entry.getValue();

            if (questionFolder == null) {
                System.out.println("[MISSING] " + questionName + " folder not found.");
            } else {
                System.out.println("[OK] " + questionName + " found: " + questionFolder.getPath());
                displayRequiredFiles(questionName, questionFolder, validator, expectedUsername);
            }
        }
    }

    private static void displayRequiredFiles(String questionName, File questionFolder, SubmissionValidator validator, String expectedUsername) throws IOException {
        String[] requiredFiles = getRequiredFiles(questionName);
        for (String fileName : requiredFiles) displayFileStatus(questionFolder, fileName, validator, expectedUsername);
    }

    private static String[] getRequiredFiles(String questionName) {
        if (questionName.equals("Q1")) {
            return new String[] {"Q1a.java", "Q1b.java"};
        }

        if (questionName.equals("Q2")) {
            return new String[] {"Q2a.java", "Q2b.java"};
        }

        if (questionName.equals("Q3")) {
            return new String[] {"Q3.java", "ShapeComparator.java"};
        }

        return new String[0];
    }

    private static void displayFileStatus(File questionFolder, String fileName, SubmissionValidator validator, String expectedUsername) throws IOException {
        File javaFile = new File(questionFolder, fileName);

        if (validator.hasFile(questionFolder, fileName)) {
            System.out.println("    [OK] " + fileName);
            validator.validateHeader(javaFile, expectedUsername);
        } else {
            System.out.println("    [MISSING] " + fileName);
        }
    }

    private static File[] findZipFiles(File submissionsFolder) {
        if (!submissionsFolder.isDirectory()) return new File[0];

        File[] files = submissionsFolder.listFiles(file -> isZipFile(file));
        if (files == null) return new File[0];

        Arrays.sort(files, Comparator.comparing(File::getName));
        return files;
    }

    private static boolean isZipFile(File file) {
        return file.isFile() && file.getName().toLowerCase().endsWith(ZIP_EXTENSION);
    }

    private static String removeZipExtension(String fileName) {
        return fileName.substring(0, fileName.length() - ZIP_EXTENSION.length());
    }
}