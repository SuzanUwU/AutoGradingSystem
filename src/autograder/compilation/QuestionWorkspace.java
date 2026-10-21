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
    private final String username;
    private final String questionId;
    private final File directory;
    private final List<String> missingSources;

    public QuestionWorkspace(String username, String questionId, File directory, List<String> missingSources) {
        this.username = username;
        this.questionId = questionId;
        this.directory = directory;
        this.missingSources = Collections.unmodifiableList(new ArrayList<>(missingSources));
    }

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

    /** Returns true when every required student source is present, so compilation can proceed. */
    public boolean isReady() {
        return missingSources.isEmpty();
    }
}
