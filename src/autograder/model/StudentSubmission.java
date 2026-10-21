package autograder.model;

/**
 * Project scaffold only. TODO: Agree on this class's responsibilities and public
 * methods with the team before adding implementation.
 * Note: Suzan added this file 
*/

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class StudentSubmission {
    private final String studentId;
    private final File submissionDir; // Root directory where extracted student files live
    private final Map<String, File> questionDirectories;

    public StudentSubmission(String studentId, File submissionDir) {
        this.studentId = studentId;
        this.submissionDir = submissionDir;
        this.questionDirectories = new HashMap<>();
    }

    public String getStudentId() {
        return studentId;
    }

    public File getSubmissionDir() {
        return submissionDir;
    }

    // Helper to get or locate directory for "Q1a", "Q1b", etc.
    public File getQuestionDirectory(String questionId) {
        if (questionDirectories.containsKey(questionId)) {
            return questionDirectories.get(questionId);
        }
        File targetDir = new File(submissionDir, questionId);
        return targetDir.exists() ? targetDir : submissionDir; 
    }

    public void setQuestionDirectory(String questionId, File dir) {
        this.questionDirectories.put(questionId, dir);
    }
}