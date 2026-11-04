package autograder.service;

import autograder.validation.SubmissionValidator;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;

/** Performs before/after correction checks and records unresolved issues. */
class SubmissionInspector {
    void recordHeaderIssues(File root, String username, SubmissionValidator validator, SubmissionState state) throws IOException {
        ArrayList<File> allJavaFiles = new ArrayList<>();
        SubmissionFileUtils.collectJavaFiles(root, allJavaFiles);

        for (File javaFile : allJavaFiles) {
            String[] header = validator.getHeaderValues(javaFile);
            String fileLabel = javaFile.getName().replace(".java", "");
            String questionId = SubmissionFileUtils.getQuestionForFile(javaFile, root);

            if (questionId == null) {
                continue;
            }

            if (header[0] == null || header[0].isBlank()) {
                state.addQuestionIssue(questionId, fileLabel + " Missing Name");
            }

            if (header[1] == null || header[1].isBlank()) {
                state.addQuestionIssue(questionId, fileLabel + " Missing Email ID");
            } else if (!header[1].equalsIgnoreCase(username)) {
                state.addQuestionIssue(questionId, fileLabel + " Email ID Mismatch");
            }
        }
    }

    FinalInspection inspectFinalSubmission(File root, String username, SubmissionValidator validator) throws IOException {
        FinalInspection inspection = new FinalInspection();
        inspection.structureIssue = !root.isDirectory() || !root.getName().equalsIgnoreCase(username);
        inspection.questionFolders = validator.findQuestionFolders(root);
        for (Map.Entry<String, File> entry : inspection.questionFolders.entrySet()) {
            File questionFolder = entry.getValue();
            if (questionFolder == null) {
                inspection.structureIssue = true;
                continue;
            }
            for (String fileName : SubmissionFileUtils.getRequiredFiles(entry.getKey())) {
                if (!new File(questionFolder, fileName).isFile()) inspection.structureIssue = true;
            }
        }
        SubmissionFileUtils.collectJavaFiles(root, inspection.javaFiles);
        inspection.javaFiles.sort(Comparator.comparing(File::getPath));
        for (File javaFile : inspection.javaFiles) {
            if (!validator.hasValidHeader(javaFile, username)) inspection.headerIssue = true;
        }
        return inspection;
    }

    void recordOpenIssues(File root, String username, SubmissionValidator validator, FinalInspection inspection, SubmissionState state) throws IOException {
        if (root == null || !root.isDirectory() || !root.getName().equalsIgnoreCase(username)) {
            state.addOpenIssue("Username folder/path still needs review");
        }

        for (Map.Entry<String, File> entry : inspection.questionFolders.entrySet()) {
            File questionFolder = entry.getValue();
            if (questionFolder == null) {
                state.addOpenIssue(entry.getKey() + " folder missing");
                continue;
            }
            for (String fileName : SubmissionFileUtils.getRequiredFiles(entry.getKey())) {
                if (!new File(questionFolder, fileName).isFile()) {
                    state.addOpenIssue(entry.getKey() + "/" + fileName + " missing");
                }
            }
        }

        if (inspection.javaFiles.isEmpty()) state.addOpenIssue("No Java files found");
        for (File javaFile : inspection.javaFiles) {
            String path = root.toPath().relativize(javaFile.toPath()).toString().replace("\\", "/");
            String[] header = validator.getHeaderValues(javaFile);
            if (header[0] == null || header[0].isBlank()) state.addOpenIssue(path + " Name missing");
            if (header[1] == null || header[1].isBlank()) state.addOpenIssue(path + " Email ID missing");
            else if (!header[1].equalsIgnoreCase(username)) state.addOpenIssue(path + " Email ID mismatch");
        }
    }

    void displayQuestionFolders(Map<String, File> folders, SubmissionValidator validator,
            String username, Consumer<String> logger, SubmissionState state) throws IOException {
        for (Map.Entry<String, File> entry : folders.entrySet()) {
            String question = entry.getKey();
            File folder = entry.getValue();
            if (folder == null) {
                logger.accept("[MISSING] " + question + " folder not found.");
                // record missing files for details csv
                for (String fileName : SubmissionFileUtils.getRequiredFiles(question)) {
                    String questionPart = fileName.replace(".java", "");
                    state.addQuestionIssue(questionPart, questionPart + " Missing File");
                }

                continue;
            }
            logger.accept("[OK] " + question + " found: " + folder.getPath());
            for (String fileName : SubmissionFileUtils.getRequiredFiles(question)) {
                File javaFile = new File(folder, fileName);
                if (validator.hasFile(folder, fileName)) {
                    logger.accept("    [OK] " + fileName);
                    validator.validateHeader(javaFile, username);                    

                } else {
                    logger.accept("    [MISSING] " + fileName);
                    // to record issue for detail csv
                    String questionPart = fileName.replace(".java", "");
                    state.addQuestionIssue(questionPart, questionPart + " Missing File");
                }
            }
        }
    }
}
