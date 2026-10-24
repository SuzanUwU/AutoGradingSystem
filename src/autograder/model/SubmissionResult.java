package autograder.model;

import java.util.*;

public class SubmissionResult {
    private final String username;
    private final String structureStatus;
    private final String headerStatus;
    private final String overallStatus;
    private final String details;
    private final List<String> submissionIssues;
    private final List<String> q1Issues;
    private final List<String> q2Issues;
    private final List<String> q3Issues;

    public SubmissionResult(String username, String structureStatus, String headerStatus, String overallStatus, String details, 
        List<String> submissionIssues, List<String> q1Issues, List<String> q2Issues, List<String> q3Issues) {
        this.username = username;
        this.structureStatus = structureStatus;
        this.headerStatus = headerStatus;
        this.overallStatus = overallStatus;
        this.details = details;
        this.submissionIssues = submissionIssues;
        this.q1Issues = q1Issues;
        this.q2Issues = q2Issues;
        this.q3Issues = q3Issues;
    }

    public String getUsername() {
        return username;
    }

    public String getStructureStatus() {
        return structureStatus;
    }

    public String getHeaderStatus() {
        return headerStatus;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public String getDetails() {
        return details;
    }

    public List<String> getSubmissionIssues() {
        return Collections.unmodifiableList(submissionIssues);
    }

    public List<String> getQ1Issues() {
        return Collections.unmodifiableList(q1Issues);
    }
    public List<String> getQ2Issues() {
        return Collections.unmodifiableList(q2Issues);
    }
    public List<String> getQ3Issues() {
        return Collections.unmodifiableList(q3Issues);
    }
}