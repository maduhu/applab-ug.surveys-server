package applab.server;

import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * Helper class that abstracts out the details of a select command to ease construction, execution, and cleanup
 * 
 * This also allows us to abstract out the mechanism for performing the query so that we can either perform a remote
 * query using our Select servlet, or a local query using the SQL APIs
 * 
 */
public class SelectCommand {
    DatabaseTable targetTable;
    HashMap<String, String> fieldsToSelect;
    ArrayList<String> whereClauses;
    String commandText;
    SqlImplementation.Instance sqlImplementation;
    ResultSet resultSet;

    public SelectCommand(DatabaseTable targetTable) {
        this.targetTable = targetTable;
        this.fieldsToSelect = new HashMap<String, String>();
        this.whereClauses = new ArrayList<String>();
    }

    public void addField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("fieldName must be non-empty");
        }

        this.fieldsToSelect.put(fieldName, null);
    }

    public void addField(String fieldName, String fieldAlias) {
        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("fieldName must be non-empty");
        }

        if (fieldAlias == null || fieldAlias.isEmpty()) {
            throw new IllegalArgumentException("fieldAlias must be non-empty");
        }

        this.fieldsToSelect.put(fieldName, fieldAlias);
    }

    public void whereEquals(String fieldName, String fieldValue) {
        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("fieldName must be non-empty");
        }

        if (fieldValue == null || fieldValue.isEmpty()) {
            throw new IllegalArgumentException("fieldValue must be non-empty");
        }

        this.whereClauses.add(fieldName + "=" + fieldValue);
    }

    public ResultSet execute() throws ClassNotFoundException, SQLException {
        if (this.fieldsToSelect.isEmpty()) {
            throw new IllegalStateException("You must first add fields to select by calling addField before calling execute");
        }
        if (this.resultSet != null) {
            throw new IllegalStateException("Can only call Execute once on a SelectCommand");
        }

        return this.sqlImplementation.executeQuery(this.targetTable.getDatabaseId(), getCommandText());
    }

    public String getCommandText() {
        if (this.commandText == null) {
            StringBuilder commandTextBuilder = new StringBuilder();
            commandTextBuilder.append("SELECT ");

            boolean isFirstField = true;
            for (Entry<String, String> fieldToSelect : this.fieldsToSelect.entrySet()) {
                if (isFirstField) {
                    isFirstField = false;
                }
                else {
                    commandTextBuilder.append(", ");
                }
                commandTextBuilder.append(fieldToSelect.getKey());
                if (fieldToSelect.getValue() != null) {
                    commandTextBuilder.append(" AS ");
                    commandTextBuilder.append(fieldToSelect.getValue());
                }
            }

            commandTextBuilder.append(" FROM ");
            commandTextBuilder.append(this.targetTable.getTableName());

            if (!this.whereClauses.isEmpty()) {
                commandTextBuilder.append(" WHERE ");

                boolean isFirstWhere = true;
                for (String whereClause : this.whereClauses) {
                    if (isFirstWhere) {
                        isFirstWhere = false;
                    }
                    else {
                        commandTextBuilder.append(" AND ");
                    }
                    commandTextBuilder.append(whereClause);
                }
            }

            this.commandText = commandTextBuilder.toString();
        }
        return this.commandText;
    }

    public void dispose() throws SQLException {
        this.sqlImplementation.dispose();
    }
}
