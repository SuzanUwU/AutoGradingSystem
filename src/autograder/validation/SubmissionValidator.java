package autograder.validation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class SubmissionValidator {

    private static final int SEARCH_DEPTH = 3;

    public Map<String, File> findQuestionFolders(File submissionFolder) throws IOException {
        Map<String, File> questionFolders = new LinkedHashMap<String, File>();
        File q1Folder = findQuestionFolder(submissionFolder, "Q1");
        File q2Folder = findQuestionFolder(submissionFolder, "Q2");
        File q3Folder = findQuestionFolder(submissionFolder, "Q3");
        questionFolders.put("Q1", q1Folder);
        questionFolders.put("Q2", q2Folder);
        questionFolders.put("Q3", q3Folder);
        return questionFolders;
    }

    private File findQuestionFolder(File submissionFolder, String questionName) throws IOException {
        ArrayList<File> matches = new ArrayList<File>();
        searchQuestionFolders(submissionFolder, questionName, 0, matches);

        if (matches.size() > 1) {
            throw new IOException("Multiple " + questionName + " folders found.");
        }

        if (matches.size() == 0) {
            return null;
        }

        return matches.get(0);
    }

    private void searchQuestionFolders(File folder, String questionName, int depth, ArrayList<File> matches) {
        if (folder == null || !folder.isDirectory()) {
            return;
        }

        if (depth > SEARCH_DEPTH) {
            return;
        }

        if (folder.getName().equalsIgnoreCase(questionName)) {
            matches.add(folder);
            return;
        }

        File[] files = folder.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                searchQuestionFolders(file, questionName, depth + 1, matches);
            }
        }
    }

    public boolean hasFile(File questionFolder, String fileName) {
        if (questionFolder == null) {
            return false;
        }

        File requiredFile = new File(questionFolder, fileName);
        return requiredFile.isFile();
    }
}