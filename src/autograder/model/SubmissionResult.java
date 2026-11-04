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

    private final List<String> folderFixDetails;
    private final List<String> nameFixDetails;
    private final List<String> emailFixDetails;
    private final List<String> openIssueDetails;
    private final List<String> anomalyDetails;

    public SubmissionResult(String username, String structureStatus, String headerStatus, String overallStatus, String details,
            List<String> submissionIssues, List<String> q1Issues, List<String> q2Issues, List<String> q3Issues,
            List<String> folderFixDetails, List<String> nameFixDetails, List<String> emailFixDetails,
            List<String> openIssueDetails, List<String> anomalyDetails) {
        this.username = username;
        this.structureStatus = structureStatus;
        this.headerStatus = headerStatus;
        this.overallStatus = overallStatus;
        this.details = details;
        this.submissionIssues = new ArrayList<>(submissionIssues);
        this.q1Issues = new ArrayList<>(q1Issues);
        this.q2Issues = new ArrayList<>(q2Issues);
        this.q3Issues = new ArrayList<>(q3Issues);
        this.folderFixDetails = new ArrayList<>(folderFixDetails);
        this.nameFixDetails = new ArrayList<>(nameFixDetails);
        this.emailFixDetails = new ArrayList<>(emailFixDetails);
        this.openIssueDetails = new ArrayList<>(openIssueDetails);
        this.anomalyDetails = new ArrayList<>(anomalyDetails);
    }

    public String getUsername() { return username; }
    public String getStructureStatus() { return structureStatus; }
    public String getHeaderStatus() { return headerStatus; }
    public String getOverallStatus() { return overallStatus; }
    public String getDetails() { return details; }

    public List<String> getSubmissionIssues() { return Collections.unmodifiableList(submissionIssues); }
    public List<String> getQ1Issues() { return Collections.unmodifiableList(q1Issues); }
    public List<String> getQ2Issues() { return Collections.unmodifiableList(q2Issues); }
    public List<String> getQ3Issues() { return Collections.unmodifiableList(q3Issues); }

    public List<String> getFolderFixDetails() { return Collections.unmodifiableList(folderFixDetails); }
    public List<String> getNameFixDetails() { return Collections.unmodifiableList(nameFixDetails); }
    public List<String> getEmailFixDetails() { return Collections.unmodifiableList(emailFixDetails); }
    public List<String> getOpenIssueDetails() { return Collections.unmodifiableList(openIssueDetails); }
    public List<String> getAnomalyDetails() { return Collections.unmodifiableList(anomalyDetails); }

    public int getFolderFixCount() { return folderFixDetails.size(); }
    public int getNameFixCount() { return nameFixDetails.size(); }
    public int getEmailFixCount() { return emailFixDetails.size(); }
    public int getOpenIssueCount() { return openIssueDetails.size(); }
    public int getAnomalyCount() { return anomalyDetails.size(); }
    public int getTotalFixCount() { return getFolderFixCount() + getNameFixCount() + getEmailFixCount(); }
}
