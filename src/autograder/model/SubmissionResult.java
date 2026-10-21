package autograder.model;

public class SubmissionResult {
    private final String username;
    private final String structureStatus;
    private final String headerStatus;
    private final String overallStatus;
    private final String details;

    public SubmissionResult(String username, String structureStatus, String headerStatus, String overallStatus, String details) {
        this.username = username;
        this.structureStatus = structureStatus;
        this.headerStatus = headerStatus;
        this.overallStatus = overallStatus;
        this.details = details;
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
}