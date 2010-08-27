package applab.surveys;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import javax.xml.rpc.ServiceException;

import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;

import applab.server.*;
import applab.surveys.server.SurveysSalesforceProxy;

public class Survey {
    private int primaryKey;

    // salesforceId is a String, since we cannot ensure the display form is an int (like CKW-201003007)
    private String salesforceId;

    private String name;

    private Collection<Question> questions;

    private SubmissionStatus cachedSubmissionFilter;
    private Collection<Submission> cachedSubmissions;

    public Survey(int primaryKey) {
        if (primaryKey < 0) {
            throw new IllegalArgumentException("primaryKey must be a non-negative value");
        }
        this.primaryKey = primaryKey;
    }

    public Survey(String salesforceId) {
        if (salesforceId == null) {
            throw new IllegalArgumentException("salesforceId must be non-null");
        }
        this.salesforceId = salesforceId;
    }

    public Survey(String salesforceId, String name) {
        this(salesforceId);
        
        if (name == null) {
            throw new IllegalArgumentException("name must be non-null");
        }
        this.name = name;
    }

    public Survey(int primaryKey, String salesforceId) {
        this(primaryKey);
        if (salesforceId == null) {
            throw new IllegalArgumentException("salesforceId must be non-null");
        }
        this.salesforceId = salesforceId;
    }

    public String getName() throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException,
            ClassNotFoundException, SQLException {
        if (this.name == null) {
            SurveysSalesforceProxy salesforceProxy = new SurveysSalesforceProxy();
            try {
                this.name = salesforceProxy.getSurveyName(this.getSalesforceId());
            }
            finally {
                salesforceProxy.dispose();
            }
        }

        return this.name;
    }

    public Collection<Question> getQuestions() throws ClassNotFoundException, SQLException {
        if (this.questions == null) {
            this.questions = loadQuestions(getPrimaryKey());
        }
        return this.questions;
    }

    /**
     * forcibly clear our cache of submissions and reload from the database
     */
    public Collection<Submission> refreshSubmissions() throws ClassNotFoundException, SQLException {
        this.cachedSubmissions = null;
        return this.getSubmissions();
    }

    public Collection<Submission> getSubmissions() throws ClassNotFoundException, SQLException {
        return getSubmissions(null);
    }

    public Collection<Submission> getSubmissions(SubmissionStatus submissionFilter) throws ClassNotFoundException, SQLException {
        if (submissionFilter != this.cachedSubmissionFilter) {
            this.cachedSubmissions = null;
            this.cachedSubmissionFilter = submissionFilter;
        }

        if (this.cachedSubmissions == null) {
            this.cachedSubmissions = loadSubmissions(submissionFilter);
        }
        return this.cachedSubmissions;
    }

    public int getPrimaryKey() throws ClassNotFoundException, SQLException {
        if (this.primaryKey == -1) {
            this.primaryKey = loadPrimaryKey();
        }
        return this.primaryKey;
    }

    public String getSalesforceId() throws ClassNotFoundException, SQLException {
        if (this.salesforceId == null) {
            this.salesforceId = loadSalesforceId();
        }
        return this.salesforceId;
    }

    /**
     * get the primary key from the Salesforce ID
     * 
     */
    int loadPrimaryKey() throws ClassNotFoundException, SQLException {
        assert (this.salesforceId != null) : "We should only reach this code if we have a valid salesforce id";
        SelectCommand selectCommand = new SelectCommand(DatabaseTable.Survey);

        try {
            selectCommand.addField("id", "PrimaryKey");
            selectCommand.whereEquals("survey_id", this.salesforceId);
            ResultSet resultSet = selectCommand.execute();
            if (!resultSet.next()) {
                // we should have exactly one row, with our primary key in it
                throw new SQLDataException("Salesforce ID (survey_id) not found: " + this.salesforceId);
            }

            int primaryKey = resultSet.getInt("PrimaryKey");

            if (resultSet.next()) {
                // we should have exactly one row, with our primary key in it
                throw new SQLDataException("Found more than one row with this Salesforce ID: " + this.salesforceId);
            }

            return primaryKey;
        }
        finally {
            selectCommand.dispose();
        }
    }

    /**
     * get the Salesforce ID from the primary key
     * 
     */
    String loadSalesforceId() throws ClassNotFoundException, SQLException {
        assert (this.primaryKey != -1) : "We should only reach this code if we have a valid primary key";
        Connection connection = DatabaseHelpers.createReaderConnection(DatabaseId.Surveys);
        Statement statement = connection.createStatement();
        String commandText = "SELECT survey_id AS SalesforceSurveyId from zebrasurvey where id=" + this.primaryKey;
        ResultSet resultSet = statement.executeQuery(commandText);
        if (!resultSet.next()) {
            // we should have exactly one row, with our Salesforce id in it
            throw new SQLDataException("Primary key (id) not found: " + this.primaryKey);
        }

        String salesforceId = resultSet.getString("SalesforceSurveyId");

        if (resultSet.next()) {
            // we should have exactly one row, with our Salesforce id in it
            throw new SQLDataException("Found more than one row with this primary key: " + this.primaryKey);
        }

        statement.close();
        connection.close();
        return salesforceId;
    }

    /**
     * Get the questions associated with a particular survey from the surveyquestions table
     */
    static ArrayList<Question> loadQuestions(int surveyId) throws ClassNotFoundException, SQLException {
        ArrayList<Question> questions = new ArrayList<Question>();

        Connection connection = DatabaseHelpers.createReaderConnection(DatabaseId.Surveys);
        Statement statement = connection.createStatement();
        StringBuilder commandText = new StringBuilder();
        commandText.append("SELECT xform_param_var AS questionName, xform_param_name AS questionValue");
        commandText.append(" FROM zebrasurveyquestions where survey_id=" + surveyId);
        ResultSet resultSet = statement.executeQuery(commandText.toString());
        while (resultSet.next()) {
            questions.add(new Question(resultSet.getString("questionName"), resultSet.getString("questionValue")));
        }
        statement.close();
        connection.close();
        return questions;
    }

    ArrayList<Submission> loadSubmissions(SubmissionStatus statusFilter) throws ClassNotFoundException, SQLException {
        ArrayList<Submission> submissions = new ArrayList<Submission>();
        Connection connection = DatabaseHelpers.createReaderConnection(DatabaseId.Surveys);
        Statement statement = connection.createStatement();
        StringBuilder commandText = new StringBuilder();
        commandText.append("SELECT id AS Id, interviewer_id AS interviewerId, interviewer_name AS interviewerName");
        commandText.append(", survey_status AS status");

        for (Question question : getQuestions()) {
            commandText.append(", " + question.getName());
        }

        commandText.append(" from zebrasurveysubmissions");
        commandText.append(" WHERE survey_id=" + getPrimaryKey());
        if (statusFilter != null) {
            commandText.append(" and survey_status='" + statusFilter.getDisplayName() + "'");
        }
        commandText.append(" GROUP BY interviewer_id");

        ResultSet resultSet = statement.executeQuery(commandText.toString());
        while (resultSet.next()) {
            Submission submission = new Submission(this);
            submission.setId(resultSet.getInt("Id"));
            submission.setInterviewerId(resultSet.getString("interviewerId"));
            submission.setInterviewerName(resultSet.getString("interviewerName"));
            // avoid parsing if we already know the value
            SubmissionStatus submissionStatus = statusFilter;
            if (submissionStatus == null) {
                submissionStatus = SubmissionStatus.parseDisplayName(resultSet.getString("status"));
            }
            submission.setStatus(submissionStatus);
            for (Question question : getQuestions()) {
                submission.addAnswer(Answer.create(question, resultSet.getString(question.getName())));
            }
            submissions.add(submission);
        }
        return submissions;
    }
}
