package autograder;

import autograder.validation.SubmissionValidator;
import autograder.validation.ZipExtractor;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Main {

    private static final String ZIP_EXTENSION = ".zip";

    public static void main(String[] args) {

        File submissionsFolder = new File("submissions");
        File workspaceFolder = new File("workspace");

        ZipExtractor zipExtractor = new ZipExtractor();
        SubmissionValidator validator = new SubmissionValidator();

        try {
            File zipFile = findFirstZip(submissionsFolder);

            if (zipFile == null) {
                System.out.println("No ZIP submission found.");
                return;
            }

            processSubmission(
                    zipFile, workspaceFolder, zipExtractor, validator);

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void processSubmission(
            File zipFile, File workspaceFolder,
            ZipExtractor zipExtractor,
            SubmissionValidator validator)
            throws IOException {

        String fileName = zipFile.getName();
        String submissionName = removeZipExtension(fileName);

        File destination = new File(workspaceFolder, submissionName);

        System.out.println("Processing: " + fileName);
        System.out.println();
        System.out.println("Extracting submission...");

        zipExtractor.extract(zipFile, destination);

        System.out.println("Extraction successful.");
        System.out.println();
        System.out.println("Searching for question folders...");

        Map<String, File> questionFolders =
                validator.findQuestionFolders(destination);

        displayQuestionFolders(questionFolders, validator);
    }

    private static void displayQuestionFolders(
            Map<String, File> questionFolders,
            SubmissionValidator validator) {

        for (Map.Entry<String, File> entry
                : questionFolders.entrySet()) {

            String questionName = entry.getKey();
            File questionFolder = entry.getValue();

            if (questionFolder == null) {
                System.out.println(
                        "[MISSING] " + questionName
                        + " folder not found.");
            } else {
                System.out.println(
                        "[OK] " + questionName
                        + " found: " + questionFolder.getPath());

                displayRequiredFiles(
                        questionName, questionFolder, validator);
            }
        }
    }

    private static void displayRequiredFiles(
            String questionName, File questionFolder,
            SubmissionValidator validator) {

        String[] requiredFiles = getRequiredFiles(questionName);

        for (String fileName : requiredFiles) {
            displayFileStatus(
                    questionFolder, fileName, validator);
        }
    }

    private static String[] getRequiredFiles(String questionName) {

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

    private static void displayFileStatus(
            File questionFolder, String fileName,
            SubmissionValidator validator) {

        if (validator.hasFile(questionFolder, fileName)) {
            System.out.println("    [OK] " + fileName);
        } else {
            System.out.println("    [MISSING] " + fileName);
        }
    }

    private static File findFirstZip(File submissionsFolder) {

        if (!submissionsFolder.exists()) {
            return null;
        }

        File[] files = submissionsFolder.listFiles();

        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (isZipFile(file)) {
                return file;
            }
        }

        return null;
    }

    private static boolean isZipFile(File file) {

        if (!file.isFile()) {
            return false;
        }

        return file.getName().toLowerCase().endsWith(ZIP_EXTENSION);
    }

    private static String removeZipExtension(String fileName) {

        int extensionLength = ZIP_EXTENSION.length();

        return fileName.substring(
                0, fileName.length() - extensionLength);
    }
}