package applab.surveys;

public enum SubmissionStatus {
    NotReviewed("Not Reviewed", "notreviewed"), 
    Pending("Pending", "pending"), 
    Approved("Approved", "approved"), 
    Rejected("Rejected", "rejected"), 
    Duplicate("Duplicate", "duplicate");
    
    // the "friendly name" used for UI display purposes (also used in the database)
    private String displayName;

    // the parameter value used in GET requests for this status
    private String htmlParameterValue;
    
    private SubmissionStatus(String displayName, String htmlParameterValue) {
        this.displayName = displayName;
        this.htmlParameterValue = htmlParameterValue;
    }
    
    public String getDisplayName() {
        return this.displayName;
    }
    
    public String getHtmlParameterValue() {
        return this.htmlParameterValue;
    }

    // accessor based on html parameter name
    public static SubmissionStatus parseHtmlParameter(String htmlParameter) {
        for (SubmissionStatus status : SubmissionStatus.values()) {
            if (status.getHtmlParameterValue().equals(htmlParameter)) {
                return status;
            }
        }

        return null;
    }

    // accessor based on display name
    public static SubmissionStatus parseDisplayName(String displayName) {
        for (SubmissionStatus status : SubmissionStatus.values()) {
            if (status.getDisplayName().equals(displayName)) {
                return status;
            }
        }

        return null;
    }
}