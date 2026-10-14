package autograder.validation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SubmissionValidator {
    private static final int SEARCH_DEPTH = 3;
    private static final String ZIP_EXTENSION = ".zip";

    private final Consumer<String> logger;

    public SubmissionValidator() {
        this(System.out::println);
    }

    public SubmissionValidator(Consumer<String> logger) {
        this.logger =
                logger == null
                        ? System.out::println
                        : logger;
    }

    public String validateAndGetUsername(File zipFile) throws IOException {
        String fileName = zipFile.getName();

        if (!zipFile.isFile()
                || !fileName.toLowerCase().endsWith(ZIP_EXTENSION)) {
            throw new IOException("Invalid submission file: " + fileName);
        }

        String submissionName =
                fileName.substring(
                        0,
                        fileName.length() - ZIP_EXTENSION.length());

        String username =
                submissionName.replaceFirst("^\\d{4}-\\d{4}-", "");

        if (username.trim().isEmpty()) {
            throw new IOException(
                    "ZIP filename does not identify a student.");
        }

        if (username.contains(" ")) {
            throw new IOException(
                    "Invalid ZIP naming convention: " + fileName);
        }

        logger.accept("[OK] ZIP identifies student: " + username);
        return username;
    }

    public Map<String, File> findQuestionFolders(
            File submissionFolder) throws IOException {

        Map<String, File> questionFolders =
                new LinkedHashMap<String, File>();

        questionFolders.put(
                "Q1",
                findQuestionFolder(submissionFolder, "Q1"));

        questionFolders.put(
                "Q2",
                findQuestionFolder(submissionFolder, "Q2"));

        questionFolders.put(
                "Q3",
                findQuestionFolder(submissionFolder, "Q3"));

        return questionFolders;
    }

    private File findQuestionFolder(
            File submissionFolder,
            String questionName) throws IOException {

        ArrayList<File> matches =
                new ArrayList<File>();

        searchQuestionFolders(
                submissionFolder,
                questionName,
                0,
                matches);

        if (matches.size() > 1) {
            throw new IOException(
                    "Multiple "
                            + questionName
                            + " folders found.");
        }

        if (matches.size() == 0) return null;

        return matches.get(0);
    }

    private void searchQuestionFolders(
            File folder,
            String questionName,
            int depth,
            ArrayList<File> matches) {

        if (folder == null
                || !folder.isDirectory()
                || depth > SEARCH_DEPTH) {
            return;
        }

        if (folder.getName().equalsIgnoreCase(questionName)) {
            matches.add(folder);
            return;
        }

        File[] files = folder.listFiles();

        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                searchQuestionFolders(
                        file,
                        questionName,
                        depth + 1,
                        matches);
            }
        }
    }

    public boolean hasFile(
            File questionFolder,
            String fileName) {

        return questionFolder != null
                && new File(questionFolder, fileName).isFile();
    }

    public void validateHeader(
            File javaFile,
            String expectedUsername) throws IOException {

        String[] header =
                getHeaderValues(javaFile);

        String name = header[0];
        String emailId = header[1];

        if (name == null || name.isEmpty()) {
            logger.accept(
                    "        [MISSING] Name in header");
        } else {
            logger.accept(
                    "        [OK] Name: " + name);
        }

        if (emailId == null || emailId.isEmpty()) {
            logger.accept(
                    "        [MISSING] Email ID in header");

        } else if (!emailId.equalsIgnoreCase(expectedUsername)) {
            logger.accept(
                    "        [WARNING] Email ID does not match username: "
                            + emailId);

        } else {
            logger.accept(
                    "        [OK] Email ID: " + emailId);
        }
    }

    public boolean hasValidHeader(
            File javaFile,
            String expectedUsername) throws IOException {

        String[] header =
                getHeaderValues(javaFile);

        return header[0] != null
                && !header[0].isEmpty()
                && header[1] != null
                && header[1].equalsIgnoreCase(expectedUsername);
    }

    public String[] getHeaderValues(
            File javaFile) throws IOException {

        List<String> lines =
                Files.readAllLines(javaFile.toPath());

        String name = null;
        String emailId = null;

        for (String line : lines) {
            String text =
                    cleanHeaderLine(line);

            if (text.startsWith("Name:")) {
                name =
                        text.substring(
                                "Name:".length())
                                .trim();

            } else if (text.startsWith("Email ID:")) {
                emailId =
                        text.substring(
                                "Email ID:".length())
                                .trim();
            }
        }

        return new String[] {
                name,
                emailId
        };
    }

    private String cleanHeaderLine(String line) {
        String text = line.trim();

        if (text.startsWith("/*")) {
            text =
                    text.substring(2)
                            .trim();
        }

        if (text.startsWith("//")) {
            text =
                    text.substring(2)
                            .trim();
        }

        if (text.startsWith("*")) {
            text =
                    text.substring(1)
                            .trim();
        }

        return text;
    }
}