package applab.surveys;

public enum CustomerCareStatus {

    NotReviewed("Not Reviewed", "notreviewed"), 
    FirstLevelApproved("First Level Approved", "firstlevelapproved"), 
    Flagged("Flagged", "flagged"); 
    
    // the "friendly name" used for UI display purposes (also used in the database)
    private String displayName;

    // the parameter value used in GET requests for this status
    private String htmlParameterValue;
    
    private CustomerCareStatus(String displayName, String htmlParameterValue) {
        this.displayName = displayName;
        this.htmlParameterValue = htmlParameterValue;
    }
    
    public String getDisplayName() {
        return this.displayName;
    }
    
    public String getHtmlParameterValue() {
        return this.htmlParameterValue;
    }
    
    public String getName() {
        return this.name();
    }

    // accessor based on html parameter name
    public static CustomerCareStatus parseHtmlParameter(String htmlParameter) {
        for (CustomerCareStatus status : CustomerCareStatus.values()) {
            if (status.getHtmlParameterValue().equals(htmlParameter)) {
                return status;
            }
        }

        return null;
    }

    // accessor based on display name
    public static CustomerCareStatus parseDisplayName(String displayName) {
        for (CustomerCareStatus status : CustomerCareStatus.values()) {
            if (status.getDisplayName().equals(displayName)) {
                return status;
            }
        }

        return null;
    }
}