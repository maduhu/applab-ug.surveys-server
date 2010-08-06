package applab.surveys;

public class Interviewer {
    private String firstName;
    private String lastName;
    private String id;

    public Interviewer(String firstName, String lastName, String id) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.id = id;
    }

    public String getFullName() {
        String name = "";

        if (this.firstName != null) {
            name += this.firstName;
        }
        if (this.lastName != null) {
            name += this.lastName;
        }

        return name;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    /**
     * Returns the Salesforce ID (e.g. CKW-10-000162)
     */
    public String getId() {
        return this.id;
    }
}
