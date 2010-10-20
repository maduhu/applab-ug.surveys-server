package applab.surveys;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.xml.rpc.ServiceException;

import applab.server.DatabaseHelpers;
import applab.server.DatabaseId;
import applab.server.DatabaseTable;
import applab.server.FileHelpers;
import applab.server.SelectCommand;
import applab.surveys.server.SurveysSalesforceProxy;

import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;

public class Survey {
    private int primaryKey;

    // salesforceId is a String, since we cannot ensure the display form is an int (like CKW-201003007)
    private String salesforceId;

    private String name;

    private ArrayList<String> questionOrder;
    private HashMap<String, Question> questions;

    private SubmissionStatus cachedSubmissionFilter;
    private ArrayList<Integer> submissionOrder;
    private HashMap<Integer, Submission> cachedSubmissions;
    
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

    public HashMap<String, Question> getQuestions() throws ClassNotFoundException, SQLException {
        if (this.questions == null) {
            this.questions = loadQuestions(getPrimaryKey());
        }
        return this.questions;
    }

    public ArrayList<Integer> getSubmissionOrder () {
        return this.submissionOrder;
    }

    public void setSubmissionOrder(ArrayList<Integer> submissionOrder) {
        this.submissionOrder = submissionOrder;
    }

    public ArrayList<String> getQuestionOrder() {
        return this.questionOrder;
    }

    public void setQuestionOrder(ArrayList<String> questionOrder) {
        this.questionOrder = questionOrder;
    }
    
    /**
     * Forcibly clear our cache of submissions and reload from the database
     */
    public HashMap<Integer,Submission> refreshSubmissions() throws ClassNotFoundException, SQLException, ParseException {
        this.cachedSubmissions = null;
        return this.getSubmissions();
    }

    public HashMap<Integer,Submission> getSubmissions() throws ClassNotFoundException, SQLException, ParseException {
        return getSubmissions(null, null, null, true);
    }

    public HashMap<Integer,Submission> getSubmissions(SubmissionStatus submissionFilter, java.sql.Date startDate, java.sql.Date endDate, boolean basic) throws ClassNotFoundException, SQLException, ParseException {
        if (submissionFilter != this.cachedSubmissionFilter) {
            this.cachedSubmissions = null;
            this.cachedSubmissionFilter = submissionFilter;
        }

        if (this.cachedSubmissions == null) {
            this.cachedSubmissions = loadSubmissions(submissionFilter, startDate, endDate, basic);
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
     * Load the submissions for this survey.
     * 
     * @param basic - if true only load the basic details. Doesn't join to the answers table.
     */
    public void load(SubmissionStatus submissionStatus, java.sql.Date startDate, java.sql.Date endDate, boolean basic) throws ClassNotFoundException, SQLException, ParseException {

        if (!basic) {
            this.getQuestions();
        }
        
        this.getSubmissions(submissionStatus, startDate, endDate, basic);
    }

    /**
     * Generates a CSV file for the survey loaded along with its submissions
     * 
     * @return - a string of the csv.
     */
    public String generateCsv() throws IOException {

        // This has the capability to write to file but for the moment that is too much
        // So I will comment this out for the moment
        //String fileName = "submissionReview" + FileHelpers.generateFileNumber() + ".csv";
        //String fullPath = FileHelpers.getFilePath("SurveyCsv") + fileName;
        //File targetFile = new File(fullPath);

        // Create the parent directories if necessary
        //File parentDirectory = targetFile.getParentFile();
        //if (parentDirectory != null) {
        //    parentDirectory.mkdirs();
        //}
        
        //FileWriter writer = new FileWriter(targetFile);
        StringBuilder writer = new StringBuilder();
        
        // Extract and save the standard column headers
        writer.append("Survey ID,");
        writer.append("Submission Id,");
        writer.append("Server Entry Date,");
        writer.append("Handset Submission Date,");
        writer.append("CKW ID,");
        writer.append("CKW Name,");
        writer.append("Customer Care Review,");
        writer.append("Data Team Review,");
        
        // Extract and save the question names
        for (String questionName : this.questionOrder) {
            Question question = this.questions.get(questionName);

            for (int i = 1; i <= question.getTotalInstances(); i++) {
                String questionDisplayName = questionName;
                if (question.getTotalInstances() > 1) {
                    questionDisplayName = questionDisplayName + "_" + i;
                }

                writer.append(questionDisplayName + ",");
            }
        }
        writer.append('\n');
        
        // Extract and save the answers
        for (int submissionId : this.submissionOrder) {
            Submission submission = this.cachedSubmissions.get(submissionId);

            // Add the standard answers
            writer.append(String.valueOf(submission.getSurveyId()) +',');
            writer.append(String.valueOf(submissionId) +',');
            writer.append(submission.getServerSubmissionTime() +',');
            writer.append(submission.getHandsetSubmissionTime() +',');
            writer.append(submission.getInterviewerId() +',');
            writer.append(submission.getInterviewerName() +',');
            writer.append(submission.getCustomerCareStatus().toString() +',');
            writer.append(submission.getStatus().toString() +',');
            for (String questionName : this.questionOrder) {
                Question question = this.questions.get(questionName);
                for (int i = 1; i <= question.getTotalInstances(); i++) {
                    String answerKey = questionName + "_" + i;
                    String answerText = "";
                    Answer answer = submission.getAnswers().get(answerKey);
                    if (answer == null || answer.getFriendlyAnswerText() == null || answer.getFriendlyAnswerText().length() == 0){
                        answerText = "[No Answer]";
                    }
                    else {
                        answerText = answer.getFriendlyAnswerText();
                    }
                    writer.append(answerText + ',');
                }
            }
            writer.append('\n');
        }

        //writer.flush();
        //writer.close();
        //return fileName;
        return writer.toString();
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
    HashMap<String, Question> loadQuestions(int surveyId) throws ClassNotFoundException, SQLException {
        HashMap<String, Question> questions = new HashMap<String, Question>();
        ArrayList<String> questionOrder = new ArrayList<String>();

        Connection connection = DatabaseHelpers.createReaderConnection(DatabaseId.Surveys);
        Statement statement = connection.createStatement();
        StringBuilder commandText = new StringBuilder();
        commandText.append("SELECT SUBSTRING(xform_param_var, 2) AS qNumber, xform_param_var AS questionName, xform_param_name AS questionValue");
        commandText.append(" FROM zebrasurveyquestions where survey_id=" + surveyId);
        commandText.append(" ORDER BY qNumber");
        ResultSet resultSet = statement.executeQuery(commandText.toString());
        while (resultSet.next()) {
            String questionName = resultSet.getString("questionName");
            questionOrder.add(questionName);
            questions.put(questionName, new Question(questionName, resultSet.getString("questionValue")));
        }
        statement.close();
        connection.close();
        this.setQuestionOrder(questionOrder);
        return questions;
    }

    
    HashMap<Integer, Submission> loadSubmissions(SubmissionStatus statusFilter, java.sql.Date startDate, java.sql.Date endDate, boolean basic)  throws ClassNotFoundException, SQLException, ParseException{
        
        // Build the query that gets the submissions for a given survey, status and within given dates.
        Connection connection = DatabaseHelpers.createReaderConnection(DatabaseId.Surveys);
        
        // Build the query
        StringBuilder commandText = new StringBuilder();
        commandText.append("SELECT s.id AS Id,");
        commandText.append(" s.survey_id AS surveyId,");
        commandText.append(" s.interviewer_id AS interviewerId,");
        commandText.append(" s.interviewer_name AS interviewerName,");
        commandText.append(" s.survey_status AS status,");
        commandText.append(" s.customer_care_status AS customerCareStatus,");
        commandText.append(" s.mobile_number AS mobileNumber,");
        commandText.append(" s.handset_submit_time AS handsetSubmitTime,");
        commandText.append(" s.server_entry_time AS serverEntryTime");
        
        if (!basic) {
            commandText.append(", a.answer AS answer,");
            commandText.append(" a.question_name AS name,");
            commandText.append(" a.position AS position");
        }
        
        commandText.append(" FROM zebrasurveysubmissions AS s");
        
        if (!basic) {
            commandText.append(", answers AS a");
        }
        
        commandText.append(" WHERE s.survey_id = ? ");
        
        if (!basic) {
            commandText.append(" AND a.submission_id = s.id ");
        }
        
        // Add the status filter if needed
        if (statusFilter != null) {
            commandText.append(" AND s.survey_status = ? ");
        }

        // Add the date filters if needed
        if (startDate != null && endDate != null) {
            commandText.append(" AND s.server_entry_time >= ?");
            commandText.append(" AND s.server_entry_time <= ?");
        }
        commandText.append(" ORDER BY s.server_entry_time");
        
        if (!basic) {
            commandText.append(", a.submission_id, a.question_number, a.position");
        }

        PreparedStatement preparedStatement = connection.prepareStatement(commandText.toString());

        // Pass in the arguments required
        int index = 1;
        preparedStatement.setInt(index, getPrimaryKey());
        index++;
        
        // Add the status filter if needed
        if (statusFilter != null) {
            preparedStatement.setString(index, statusFilter.getDisplayName());
            index++;
        }

        // Add the date filters if needed
        if (startDate != null && endDate != null) {
            preparedStatement.setDate(index, startDate);
            index++;
            preparedStatement.setDate(index, endDate);
        }

        // Execute the query.
        ResultSet resultSet = preparedStatement.executeQuery();
        
        // Loop through the result set and pull out the submission.
        HashMap<Integer , Submission> submissions = new HashMap<Integer,Submission>();
        ArrayList<Integer> order = new ArrayList<Integer>();
        
        // Set up a id counter
        while (resultSet.next()) {
            
            
            int currentSubmissionId = resultSet.getInt("Id");

            // Is this is new submission
            if (!order.contains(currentSubmissionId)) {
                
                // Add the submission to the ordering array.  Create the new submission and set the basic
                // information for it
                order.add(currentSubmissionId);
                submissions.put(currentSubmissionId, new Submission(this)); 
                Submission submission = submissions.get(currentSubmissionId);
                submission.setId(currentSubmissionId);
                submission.setSurveyId(resultSet.getInt("surveyId"));
                submission.setInterviewerId(resultSet.getString("interviewerId"));
                submission.setInterviewerName(resultSet.getString("interviewerName"));
                submission.setPhoneNumber(resultSet.getString("mobileNumber"));
                submission.setHandsetSubmissionTime(DatabaseHelpers.getJavaDateFromString(resultSet.getString("handsetSubmitTime")));
                submission.setServerSubmissionTime(DatabaseHelpers.getJavaDateFromString(resultSet.getString("serverEntryTime")));

                // Avoid parsing if we already know the value
                SubmissionStatus submissionStatus = statusFilter;
                if (submissionStatus == null) {
                    submissionStatus = SubmissionStatus.parseDisplayName(resultSet.getString("status") == null ? "Not Reviewed" : resultSet.getString("status"));
                }
                submission.setStatus(submissionStatus);
                
                CustomerCareStatus customerCareStatus = CustomerCareStatus.parseDisplayName(resultSet.getString("customerCareStatus"));
                submission.setCustomerCareStatus(customerCareStatus);
            }

            // This may have already been done but we need to make sure we have got it out.
            Submission submission = submissions.get(currentSubmissionId);

            // Only include the answers if we are not looking at the basic view
            if (!basic) {
                int position = resultSet.getInt("position");
                String questionName = resultSet.getString("name");

                // The addition here is that the DB is zero ordered but the display 
                // should start at 1
                String name = questionName + "_" + Integer.toString(position + 1);
                submission.addAnswer(name, Answer.create(this.questions.get(questionName), resultSet.getString("answer"), position));
            
                // Check that the instance of this answer if not higher than any instance we have seen before
                if (position + 1 > this.questions.get(questionName).getTotalInstances()) {
                    this.questions.get(questionName).setTotalInstances(position + 1);
                }
            }
        }
        connection.close();
        this.setSubmissionOrder(order);
        return submissions;
    }
}
