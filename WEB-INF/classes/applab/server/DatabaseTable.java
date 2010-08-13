package applab.server;

public enum DatabaseTable {
    Survey(DatabaseId.Surveys, "zebrasurvey"), 
    Questions(DatabaseId.Surveys, "zebrasurveyquestions"), 
    Submissions(DatabaseId.Surveys, "zebrasurveysubmissions"),
    ;

    // the physical table name used in SQL
    private String sqlTableName;
    
    // the database which contains this table
    private DatabaseId database;

    private DatabaseTable(DatabaseId database, String sqlTableName) {
        this.database = database;
        this.sqlTableName = sqlTableName;
    }
    
    public DatabaseId getDatabaseId() {
        return this.database;
    }
    
    public String getTableName() {
        return this.sqlTableName;
    }
}