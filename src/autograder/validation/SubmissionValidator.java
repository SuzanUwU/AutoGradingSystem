package autograder.validation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SubmissionValidator {
    private static final int SEARCH_DEPTH = 3;
    private static final String ZIP_EXTENSION = ".zip";

    public String validateAndGetUsername(File zipFile) throws IOException {
        String fileName = zipFile.getName();

        if (!zipFile.isFile() || !fileName.toLowerCase().endsWith(ZIP_EXTENSION)) {
            throw new IOException("Invalid submission file: " + fileName);
        }

        String submissionName = fileName.substring(0, fileName.length() - ZIP_EXTENSION.length());
        String username = submissionName.replaceFirst("^\\d{4}-\\d{4}-", "");

        if (username.trim().isEmpty()) throw new IOException("ZIP filename does not identify a student.");
        if (username.contains(" ")) throw new IOException("Invalid ZIP naming convention: " + fileName);

        System.out.println("[OK] ZIP identifies student: " + username);
        return username;
    }

    public Map<String, File> findQuestionFolders(File submissionFolder) throws IOException {
        Map<String, File> questionFolders = new LinkedHashMap<String, File>();
        questionFolders.put("Q1", findQuestionFolder(submissionFolder, "Q1"));
        questionFolders.put("Q2", findQuestionFolder(submissionFolder, "Q2"));
        questionFolders.put("Q3", findQuestionFolder(submissionFolder, "Q3"));
        return questionFolders;
    }

    private File findQuestionFolder(File submissionFolder, String questionName) throws IOException {
        ArrayList<File> matches = new ArrayList<File>();
        searchQuestionFolders(submissionFolder, questionName, 0, matches);

        if (matches.size() > 1) throw new IOException("Multiple " + questionName + " folders found.");
        if (matches.size() == 0) return null;
        return matches.get(0);
    }

    private void searchQuestionFolders(File folder, String questionName, int depth, ArrayList<File> matches) {
        if (folder == null || !folder.isDirectory() || depth > SEARCH_DEPTH) return;

        if (folder.getName().equalsIgnoreCase(questionName)) {
            matches.add(folder);
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) searchQuestionFolders(file, questionName, depth + 1, matches);
        }
    }

    public boolean hasFile(File questionFolder, String fileName) {
        if (questionFolder == null) return false;
        return new File(questionFolder, fileName).isFile();
    }

    public void validateHeader(File javaFile, String expectedUsername) throws IOException {
        List<String> lines = Files.readAllLines(javaFile.toPath());
        String name = null;
        String emailId = null;

        for (String line : lines) {
            String text = line.trim();
            if (text.startsWith("*")) text = text.substring(1).trim();

            if (text.startsWith("Name:")) name = text.substring("Name:".length()).trim();
            else if (text.startsWith("Email ID:")) emailId = text.substring("Email ID:".length()).trim();
        }

        if (name == null || name.isEmpty()) System.out.println("        [MISSING] Name in header");
        else System.out.println("        [OK] Name: " + name);

        if (emailId == null || emailId.isEmpty()) {
            System.out.println("        [MISSING] Email ID in header");
        } else if (!emailId.equalsIgnoreCase(expectedUsername)) {
            System.out.println("        [WARNING] Email ID does not match username: " + emailId);
        } else {
            System.out.println("        [OK] Email ID: " + emailId);
        }
    }
}