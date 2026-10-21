package autograder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes one gradable question part (e.g. Q1a): which submission folder it lives in,
 * which tester grades it, and which student source files it needs.
 */
public class QuestionSpec {
    private final String questionId;
    private final String folderName;
    private final String testerClassName;
    private final List<String> requiredSources;

    /**
     * @param questionId      question part identifier, e.g. "Q1a"
     * @param folderName      submission folder holding the part, e.g. "Q1"
     * @param testerClassName tester class name, e.g. "Q1aTester"
     * @param requiredSources student source files the tester depends on, e.g. "Q1a.java"
     */
    public QuestionSpec(String questionId, String folderName, String testerClassName, List<String> requiredSources) {
        this.questionId = questionId;
        this.folderName = folderName;
        this.testerClassName = testerClassName;
        this.requiredSources = Collections.unmodifiableList(new ArrayList<>(requiredSources));
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getTesterClassName() {
        return testerClassName;
    }

    public String getTesterSourceFile() {
        return testerClassName + ".java";
    }

    public List<String> getRequiredSources() {
        return requiredSources;
    }
}
