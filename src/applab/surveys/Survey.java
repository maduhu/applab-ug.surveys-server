package applab.surveys;

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
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

import org.xml.sax.SAXException;

import applab.server.DatabaseHelpers;
import applab.server.DatabaseId;
import applab.server.DatabaseTable;
import applab.server.SalesforceProxy;
import applab.server.SelectCommand;
import applab.surveys.server.SurveysSalesforceProxy;

import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;
import com.sforce.soap.enterprise.sobject.Survey__c;

public class Survey {
    private int primaryKey;

    // salesforceId is a String, since we cannot ensure the display form is an int (like CKW-201003007)
    private String salesforceId;

    private String name;

    // This variable stores the xml that is taken from the backend database.
    private ParsedSurveyXml backendSurveyXml;
    
    private boolean existsInDb = false;
    
    // Stores the xml that is passed in from SaveDesignerForm.
    private ParsedSurveyXml salesforceSurveyXml;
    
    private SurveyStatus surveyStatus;
    
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
    
    public ParsedSurveyXml getBackEndSurveyXml() {
        return this.backendSurveyXml;
    }
    public ParsedSurveyXml getSFXml() {
        return this.salesforceSurveyXml;
    }
    
    public boolean existsInDb() {
        return this.existsInDb;
    }
    
    public void setSurveyStatus(String status) {
        this.surveyStatus = SurveyStatus.parseSalesforceName(status);
    }
    
    public SurveyStatus getSurveyStatus() {
        return this.surveyStatus;
    }

    public ArrayList<Integer> getSubmissionOrder () {
        return this.submissionOrder;
    }

    public void setSubmissionOrder(ArrayList<Integer> submissionOrder) {
        this.submissionOrder = submissionOrder;
    }

    /**
     * Forcibly clear our cache of submissions and reload from the database
     */
    public HashMap<Integer,Submission> refreshSubmissions()
            throws ClassNotFoundException, SQLException, ParseException, SAXException, IOException, ParserConfigurationException {
        this.cachedSubmissions = null;
        return this.getSubmissions();
    }

    public HashMap<Integer,Submission> getSubmissions()
            throws ClassNotFoundException, SQLException, ParseException, SAXException, IOException, ParserConfigurationException {
        return getSubmissions(null, null, null, true, false);
    }

    public HashMap<Integer,Submission> getSubmissions(SubmissionStatus submissionFilter, java.sql.Date startDate, java.sql.Date endDate, boolean basic, boolean showDraft)
            throws ClassNotFoundException, SQLException, ParseException, SAXException, IOException, ParserConfigurationException {
        if (submissionFilter != this.cachedSubmissionFilter) {
            this.cachedSubmissions = null;
            this.cachedSubmissionFilter = submissionFilter;
        }

        if (this.cachedSubmissions == null) {
            this.cachedSubmissions = loadSubmissions(submissionFilter, startDate, endDate, basic, showDraft);
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
    public void loadSubmissions(SubmissionStatus submissionStatus, java.sql.Date startDate, java.sql.Date endDate, boolean basic, String salesforceId, boolean showDraft) 
            throws ClassNotFoundException, SQLException, ParseException, SAXException, IOException, ParserConfigurationException, ServiceException {

        if (!basic) {
            this.loadSurvey(salesforceId, true);
            this.parseBackEndXml();
        }
        
        this.getSubmissions(submissionStatus, startDate, endDate, basic, showDraft);
    }

    /**
     * Generates a CSV file for the survey loaded along with its submissions
     * 
     * @return - a string of the csv.
     */
    public String generateCsv() throws IOException, SAXException, ParserConfigurationException {

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
        
        // Extract and save the question bindings as these are used by the DC team
        for (String questionBinding : this.backendSurveyXml.getQuestionOrder()) {
            Question question = this.backendSurveyXml.getQuestions().get(questionBinding);
            
            for (int i = 1; i <= question.getTotalInstances(); i++) {
                String questionDisplayName = questionBinding;
                if (question.getTotalInstances() > 1) {
                    questionDisplayName = questionDisplayName + "_" + i;
                }

                if ("Select".equals(question.getType().toString())) {
                    for (int j = 1; j <= question.getNumberOfSelects(); j++) {
                        writer.append(questionDisplayName + "_" + j + ",");
                    }
                }
                else {
                    writer.append(questionDisplayName + ",");
                }
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
            for (String questionName : this.backendSurveyXml.getQuestionOrder()) {
                Question question = this.backendSurveyXml.getQuestions().get(questionName);
                for (int i = 1; i <= question.getTotalInstances(); i++) {
                    String answerKey = questionName + "_" + i;
                    String answerText = "";
                    Answer answer = submission.getAnswers().get(answerKey);
                    
                    if ("Select".equals(question.getType().toString())) {
                        boolean hasAnswerText = true;
                        if (answer == null || answer.getFriendlyAnswerText(true, this) == null || answer.getFriendlyAnswerText(true, this).length() == 0) {
                            hasAnswerText = false;
                        }
                        for (int j = 1; j <= question.getNumberOfSelects(); j++) {
                            if (!hasAnswerText) {
                                writer.append("[No Answer],");
                             }
                             else if (answer.getFriendlyAnswerText(true, this).contains(" " + j + " ")){
                                 writer.append(String.valueOf(j) + ",");
                             }
                             else {
                                 writer.append(",");
                             }
                        }
                    }
                    else {
                        if (answer == null || answer.getFriendlyAnswerText(true, this) == null || answer.getFriendlyAnswerText(true, this).length() == 0){
                            answerText = "[No Answer]";
                         }
                         else {
                             answerText = answer.getFriendlyAnswerText(true, this);
                         }
                         writer.append(answerText + ',');
                    }
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
     * Load the details of a survey that are stored in the salesforce cloud
     * 
     * @param salesforceId - The surveys id that is stored in salesforce
     * @return - boolean indicating success
     */
    public boolean loadSurvey(String salesforceId, boolean getFromSalesforce) 
            throws SQLException, ClassNotFoundException, ServiceException, InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException {

        // TODO - use object caching to check for the survey already loaded.

        this.existsInDb = false;
        if (getFromSalesforce) {
            SoapBindingStub binding = SalesforceProxy.createBinding();
            StringBuilder commandText = new StringBuilder();
            commandText.append("SELECT Name, Survey_Status__c");
            commandText.append(" FROM Survey__c");
            commandText.append(" WHERE Name = '");
            commandText.append(salesforceId);
            commandText.append("'");
            QueryResult query = binding.query(commandText.toString());
            if (query.getSize() > 1) {
                throw new RemoteException("We should have at most 1 survey with Name " + salesforceId + ", but we found " + query.getSize());
            } else if (query.getSize() == 0) {

                // Survey doesn't exist. Should not really happen as to get to this code the survey needs to be in salesforce already
                return false;
            }
            Survey__c salesforceSurvey = (Survey__c)query.getRecords(0);
            this.setSurveyStatus(salesforceSurvey.getSurvey_Status__c());
        }
        this.loadSurveyFromDatabase();
        return true;
    }

    /**
     * Load external xml submitted from salesforce
     * 
     * @param xml
     */
    public void loadSalesforceXml(String xml) throws SAXException, IOException, ParserConfigurationException {
        this.salesforceSurveyXml = new ParsedSurveyXml(xml);
        this.salesforceSurveyXml.parseQuestions();
    }
    
    /**
     * Compares the BackEndSurvey to the Salesforce survey.
     * For the changes to be considered acceptable the question orders must still match.
     * The question types must match.
     * 
     * The question text can change and we can add extra selects to a multiple select.
     * This does mean that a user could sabotage a survey if they wanted to but there is little
     * we can realistically do about that.
     * 
     * @return - true if there are only cosmetic differences between the forms.
     */
    public boolean comparePurcForms() 
            throws SAXException, IOException, ParserConfigurationException, SQLException, ClassNotFoundException, ServiceException {

        // Load the backend survey if we can
        if (this.backendSurveyXml == null) {
            if (!loadSurvey(this.salesforceId, true)) {
                return false;
            }
        }

        // Check that the salesforce xml has been loaded
        if (this.salesforceSurveyXml == null) {
            return false;
        }
        
        // Check that the surveys have the same number of questions
        if (this.backendSurveyXml.getQuestionOrder().size() != this.salesforceSurveyXml.getQuestionOrder().size()) {
            return false;
        }

        int index = 0;
        for (String questionKey : this.backendSurveyXml.getQuestionOrder()) {

            // Check that the question bindings/order still match
            if (!questionKey.equals(this.salesforceSurveyXml.getQuestionOrder().get(index))) {
                return false;
            }

            Question backendQuestion = this.backendSurveyXml.getQuestions().get(questionKey);
            Question salesforceQuestion = this.salesforceSurveyXml.getQuestions().get(questionKey);

            // Check that the question types still match
            if (backendQuestion.getType() != salesforceQuestion.getType()) {
                return false;
            }
            index++;
        }
        
        return true;
    }

    /**
     *  Load the details of a survey that are stored in the back end database
     * 
     */
    private void loadSurveyFromDatabase() throws ClassNotFoundException, SQLException {

        assert (this.salesforceId != null) : "We should only reach this code if we have a valid salesforce id";
        SelectCommand selectCommand = new SelectCommand(DatabaseTable.Survey);

        try {
            selectCommand.addField("xform", "xform");
            selectCommand.addField("id", "primaryKey");
            selectCommand.whereEquals("survey_id", this.salesforceId);
            ResultSet resultSet = selectCommand.execute();

            if (resultSet.next()) {
                this.primaryKey = resultSet.getInt("primaryKey"); 
                this.backendSurveyXml = new ParsedSurveyXml(resultSet.getString("xform"));
                this.existsInDb = true;
                if (resultSet.next()) {
        
                     // We should have exactly one row, with our primary key in it
                     throw new SQLDataException("Found more than one row with this Salesforce ID: " + this.salesforceId);
                }
            }
        }
        finally {
            selectCommand.dispose();
        }
    }

    private void parseBackEndXml() throws SAXException, IOException, ParserConfigurationException {
        this.backendSurveyXml.parseQuestions();
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

    HashMap<Integer, Submission> loadSubmissions(SubmissionStatus statusFilter, java.sql.Date startDate, java.sql.Date endDate, boolean basic, boolean showDraft)
            throws ClassNotFoundException, SQLException, ParseException, SAXException, IOException, ParserConfigurationException{
        
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
        commandText.append(" AND s.is_draft = ?");
        
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
        
        if (showDraft) {
            preparedStatement.setString(index, "Y");
        }
        else {
            preparedStatement.setString(index, "N");
        }
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
                Question question = this.backendSurveyXml.getQuestions().get(questionName);
                if (question != null) {

                    // The addition here is that the DB is zero ordered but the display 
                    // should start at 1
                    String name = questionName + "_" + Integer.toString(position + 1);
                    submission.addAnswer(name, Answer.create(question, resultSet.getString("answer"), position));
            
                    // Check that the instance of this answer if not higher than any instance we have seen before
                    if (position + 1 > question.getTotalInstances()) {
                        question.setTotalInstances(position + 1);
                    }
                }
            }
        }
        connection.close();
        this.setSubmissionOrder(order);
        return submissions;
    }
}
