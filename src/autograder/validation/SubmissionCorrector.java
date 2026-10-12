package autograder.validation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SubmissionCorrector {

    public File correctStructure(
            File extractedRoot,
            String expectedUsername)
            throws IOException {

        File expectedFolder =
                new File(extractedRoot, expectedUsername);

        // Case 1: correct username folder already exists
        if (expectedFolder.isDirectory()) {
            System.out.println(
                    "[OK] Username folder is correct: "
                    + expectedUsername);

            return expectedFolder;
        }

        // Case 2: Q1/Q2/Q3 are directly inside extracted root
        if (hasDirectQuestionFolder(extractedRoot)) {

            if (!expectedFolder.mkdirs()
                    && !expectedFolder.exists()) {

                throw new IOException(
                        "Cannot create corrected folder: "
                        + expectedFolder.getPath());
            }

            moveQuestionFolder(
                    extractedRoot,
                    expectedFolder,
                    "Q1");

            moveQuestionFolder(
                    extractedRoot,
                    expectedFolder,
                    "Q2");

            moveQuestionFolder(
                    extractedRoot,
                    expectedFolder,
                    "Q3");

            System.out.println(
                    "[CORRECTED] Added missing username folder: "
                    + expectedUsername);

            return expectedFolder;
        }

        // Case 3: one top-level folder exists,
        // but its name is wrong
        File singleFolder =
                getSingleTopLevelFolder(extractedRoot);

        if (singleFolder != null) {

            String oldName = singleFolder.getName();

            Files.move(
                    singleFolder.toPath(),
                    expectedFolder.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            System.out.println(
                    "[CORRECTED] Renamed top-level folder from "
                    + oldName
                    + " to "
                    + expectedUsername);

            return expectedFolder;
        }

        // Cannot safely determine how to correct it
        System.out.println(
                "[WARNING] Folder structure could not "
                + "be corrected automatically.");

        return extractedRoot;
    }

    private boolean hasDirectQuestionFolder(File root) {

        return new File(root, "Q1").isDirectory()
                || new File(root, "Q2").isDirectory()
                || new File(root, "Q3").isDirectory();
    }

    private void moveQuestionFolder(
            File sourceRoot,
            File destinationRoot,
            String questionName)
            throws IOException {

        File source =
                new File(sourceRoot, questionName);

        if (!source.isDirectory()) {
            return;
        }

        File destination =
                new File(destinationRoot, questionName);

        Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private File getSingleTopLevelFolder(File root) {

        File[] children = root.listFiles();

        if (children == null) {
            return null;
        }

        File onlyFolder = null;
        int folderCount = 0;

        for (File child : children) {

            if (child.isDirectory()) {
                onlyFolder = child;
                folderCount++;
            }
        }

        if (folderCount == 1) {
            return onlyFolder;
        }

        return null;
    }
}