package autograder.compilation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An isolated folder prepared for compiling and running one student's question part,
 * together with any required student sources that could not be found.
 */

public class QuestionWorkspace {
    // Immutable description of one student/question workspace.
    private final String username;
    private final String questionId;
    private final File directory;
    private final List<String> missingSources;

    public QuestionWorkspace(String username, String questionId, File directory, List<String> missingSources) {
        this.username = username;
        this.questionId = questionId;
        this.directory = directory;
        // Defensive copy: callers cannot change the recorded missing-file list later.
        this.missingSources = Collections.unmodifiableList(new ArrayList<>(missingSources));
    }

    // Accessors let the compiler and grader read workspace metadata.
    public String getUsername() {
        return username;
    }

    public String getQuestionId() {
        return questionId;
    }

    public File getDirectory() {
        return directory;
    }

    public List<String> getMissingSources() {
        return missingSources;
    }

    // Readiness means all required source files exist; it does not mean they compile.
    /** Returns true when every required student source is present, so compilation can proceed. */
    public boolean isReady() {
        return missingSources.isEmpty();
    }
}
