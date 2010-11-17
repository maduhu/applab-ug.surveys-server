package applab.surveys;

public enum SurveyStatus {
    Draft("Draft"),
    Published("Published"),
    Completed("Completed");
    
    private String salesforceName;

    private SurveyStatus(String salesforceName) {
        this.salesforceName = salesforceName;
    }

    private String getSalesforceName() {
        return this.salesforceName;
    }

    // Accessor based on salesforce name
    public static SurveyStatus parseSalesforceName(String salesforceName) {
        for (SurveyStatus status : SurveyStatus.values()) {
            if (status.getSalesforceName().equals(salesforceName)) {
                return status;
            }
        }

        return null;
    }
}
