package applab.surveys;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

import org.apache.commons.fileupload.FileItem;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import applab.CommunityKnowledgeWorker;
import applab.Farmer;
import applab.Location;
import applab.Person;
import applab.server.ApplabConfiguration;
import applab.server.DatabaseHelpers;
import applab.server.HashHelpers;
import applab.server.ServletRequestContext;
import applab.server.WebAppId;
import applab.server.XmlHelpers;
import applab.surveys.server.SurveyDatabaseHelpers;

import com.google.gson.Gson;
import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.enterprise.SessionHeader;
import com.sforce.soap.enterprise.SforceServiceLocator;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.enterprise.fault.InvalidFieldFault;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.InvalidQueryLocatorFault;
import com.sforce.soap.enterprise.fault.InvalidSObjectFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.MalformedQueryFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;
import com.sforce.soap.schemas._class.ProcessSurveySubmission.ProcessSurveySubmissionBindingStub;
import com.sforce.soap.schemas._class.ProcessSurveySubmission.ProcessSurveySubmissionServiceLocator;
import com.sforce.soap.schemas._class.ProcessSurveySubmission.SurveySubmission;

/**
 * Container object to represent a submission that is in the process of being submitted
 * 
 */

public class ProcessedSubmission {
    // The xml that represents the submission
    private Document xml;

    // The XML string that is to be passed to an external server
    private String xmlString;

    // The JSON string that is to be passed to an external server
    private String jsonString;

    // A HashMap containing the answers to the survey
    private HashMap<String, SubmissionAnswer> surveyResponses;

    // A Hashmap containing the attachment references for the submission
    private HashMap<String, String> attachmentReferences;

    // The file paths to the attachments on the file system. Needed if we are going to bounce the submission
    // to another system
    private HashMap<String, String> attachmentFilePaths;

    // The size of the submission XML in bytes
    private long size;

    // The survey object that this submission is for
    private Survey survey;

    // The imei for the phone that submitted the survey
    private String imei;

    // The Interviewee Id
    private String intervieweeId;

    // The location that the physical interview was carried out
    private String interviewLocation;

    // The location that the submission was sent to the survey
    private String submissionLocation;

    // The id that is retrieved from the root node of the XML. Cannot always be sure it is there due to legacy.
    // Use the injected id if this does not exist
    private String backendSurveyIdFromRootNode = null;
    private String salesforceIdFromRootNode = null;

    // The validated backend survey Id
    private int backendSurveyId = -1;
    private String salesforceId = null;

    // The handset time for the submission
    private Date handsetSubmissionTime;
    private Date submissionStartTime;

    // The duplicate hash of the submission
    private String duplicateHash;

    // The person who carried out the survey
    private Actor interviewer;

    private Random attachmentNameGenerator = new Random();

    public ProcessedSubmission () {
        this.surveyResponses = new HashMap<String, SubmissionAnswer>();
        this.attachmentReferences = new HashMap<String, String>();
        this.attachmentFilePaths = new HashMap<String, String>();
    }

    public ProcessedSubmission(String imei, String intervieweeId, String interviewLocation, String submissionLocation) {
        this();
        this.imei = imei;
        this.intervieweeId = intervieweeId;
        this.interviewLocation = interviewLocation;
        this.submissionLocation = submissionLocation;
    }

    public void setXml(Document xml) {
        this.xml = xml;
    }

    public HashMap<String, SubmissionAnswer> getSurveyResponses() {
        return surveyResponses;
    }

    public void setSurveyResponses(HashMap<String, SubmissionAnswer> surveyResponses) {
        this.surveyResponses = surveyResponses;
    }

    public void addAttachment(String name, String reference) {
        this.attachmentReferences.put(name, reference);
    }
    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getImei() {
        return imei;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Survey getSurvey() {
        return survey;
    }

    public void setSurvey(Survey survey) {
        this.survey = survey;
    }

    public Boolean loadSurvey()
            throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException,
                SQLException, ClassNotFoundException, ServiceException {

        this.survey = new Survey(getSalesforceId());
        return this.survey.loadSurvey(true);
    }

    public void checkIntervieweeId(String fileName) {

        if (this.intervieweeId != null || !this.intervieweeId.equals("")) {
            return;
        }
        this.intervieweeId = extractIntervieweeId(fileName);
    }

    /**
     * Extract the intervieweeId from the filename. TODO - Remove in > 3.2
     * 
     * @param fileName
     * @return
     */
    public String extractIntervieweeIdTest(String fileName) {
        return extractIntervieweeId(fileName);
    }
    private String extractIntervieweeId(String fileName) {

        String intervieweeId = "";
        String regex = "(.*)\\_\\[(.*)\\]\\_[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}\\_[0-9]{2}\\-[0-9]{2}\\-[0-9]{2}\\.xml$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.matches()) {
            intervieweeId = matcher.group(2).toUpperCase();
        }
        return intervieweeId;
    }

    /**
     * Get the backend survey id from the submission. This could be taken from one of two places The root node of the
     * submission (Ideal situation) The injected id element (legacy) Need to have parsed the submission before this can
     * be called
     * 
     * @return - The backend survey id for this submission.
     */
    public int getBackendSurveyId() {

        // -1 is the default. It has not been set if this is the case
        if (this.backendSurveyId != -1) {
            return this.backendSurveyId;
        }

        // The <name>:0 notation for the key is used here as these are special case answers
        // that cannot be duplicated so will always have the instance number of 0
        // Use the old method first if possible
        if (this.surveyResponses.containsKey("survey_id:0")) {
            this.backendSurveyId = Integer.parseInt(this.surveyResponses.get("survey_id:0").getAnswerText(this.attachmentReferences, true));
        }
        else {
            this.backendSurveyId = Integer.parseInt(this.backendSurveyIdFromRootNode);
        }
        return this.backendSurveyId;
    }

    public String getSalesforceId() {

        if (this.salesforceId != null) {
            return this.salesforceId;
        }

        // -1 is the default. It has not been set if this is the case
        if (this.backendSurveyId == -1) {
            getBackendSurveyId();
        }

        // Get the salesforceId from the DB. I would like to get it from the XML as it should be there
        // However the form designer is not robust enough to ensure that we can trust this. Once the cloning process
        // and passing in the id is stable the we can try to use it and not hit the DB
        this.salesforceId = SurveyDatabaseHelpers.verifySurveyID(backendSurveyId);
        return this.salesforceId;
    }

    /**
     * Check that the SF id exists in our DB
     * 
     * @return a boolean indicating if the form exists
     */
    public Boolean checkSalesforceId() {

        if (getSalesforceId() == null) {
            return false;
        }
        return true;
    }

    /**
     * Store the submission in our database
     * 
     * @return - response code
     */
    public int storeSurveySubmission()
            throws NoSuchAlgorithmException, ServiceException,
            ClassNotFoundException, SQLException, ParseException, SAXException, IOException, ParserConfigurationException {

        // Create the connection to the database
        Connection connection = SurveyDatabaseHelpers.getWriterConnection();
        connection.setAutoCommit(false);

        StringBuilder commandText = new StringBuilder();
        commandText.append("insert into zebrasurveysubmissions ");
        commandText.append("(survey_id, server_entry_time, handset_submit_time, handset_id, interviewer_id, ");
        commandText.append("interviewer_name, result_hash, submission_size");
        commandText.append(", mobile_number");
        commandText.append(", location");
        commandText.append(", interviewee_name");
        commandText.append(", is_draft");
        commandText.append(", submission_location");
        commandText.append(", submission_start_time");
        commandText.append(") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        // Create the prepared statement
        PreparedStatement submissionStatement = connection.prepareStatement(commandText.toString(), Statement.RETURN_GENERATED_KEYS);

        // Add the params to the query
        submissionStatement.setInt(1, this.backendSurveyId);
        submissionStatement.setTimestamp(2, DatabaseHelpers.getTimestamp(new Date()));
        submissionStatement.setTimestamp(3, DatabaseHelpers.getTimestamp(this.handsetSubmissionTime));
        submissionStatement.setString(4, this.imei);
        submissionStatement.setString(5, this.interviewer.getId());
        submissionStatement.setString(6, this.interviewer.getFirstName() + " " + this.interviewer.getLastName());
        submissionStatement.setString(7, this.duplicateHash);
        submissionStatement.setLong(8, this.size);
        submissionStatement.setString(9, this.interviewer.getMobileNumber());
        submissionStatement.setString(10, this.interviewLocation);
        submissionStatement.setString(11, this.intervieweeId);
        if ("Draft".equals(survey.getSurveyStatus().toString())) {
            submissionStatement.setString(12, "Y");
        }
        else {
            submissionStatement.setString(12, "N");
        }
        submissionStatement.setString(13, this.submissionLocation);
        submissionStatement.setTimestamp(14, DatabaseHelpers.getTimestamp(this.submissionStartTime));

        try {
            submissionStatement.execute();
        }
        catch (SQLException e) {
            submissionStatement.close();
            connection.setAutoCommit(true);
            if (e.getErrorCode() == 1062) {

                // The error is a duplicate key error so allow to be told as good
                return HttpServletResponse.SC_CREATED;
            }
            else {
                e.printStackTrace();
                throw new SQLException(e.getMessage());
            }
        }

        // Grab the key for the submission
        ResultSet primaryKey = submissionStatement.getGeneratedKeys();
        primaryKey.next();
        int submissionId = Integer.parseInt(primaryKey.getString(1));
        primaryKey.close();

        // Generate a batch of SQL statements for the answers
        String startString = "insert into answers (submission_id, question_number, question_name, answer, parent, parent_position, position) values (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement answerStatements = connection.prepareStatement(startString);
        for (Entry<String, SubmissionAnswer> answerKey : surveyResponses.entrySet()) {

            SubmissionAnswer answer = answerKey.getValue();
            if (!answer.getIsValid()) {
                continue;
            }

            // Get the question name and instance number
            String questionName = answer.getQuestionName();

            String questionNumber = answer.getQuestion().getQuestionNumber().toString();

            answerStatements.setInt(1, submissionId);
            answerStatements.setString(2, questionNumber);
            answerStatements.setString(3, questionName);
            answerStatements.setString(4, answer.getAnswerText(attachmentReferences, true));

            // Check to see if there is a parent
            if (answer.hasParent()) {
                answerStatements.setString(5, answer.getParent().getQuestionName());
                answerStatements.setInt(6, answer.getParent().getInstance());
            }
            else {
                answerStatements.setNull(5, java.sql.Types.VARCHAR);
                answerStatements.setNull(6, java.sql.Types.INTEGER);
            }
            answerStatements.setInt(7, answer.getInstance());
            answerStatements.addBatch();
        }

        // Try to execute the batch
        try {
            answerStatements.executeBatch();
        }
        catch (Exception e) {
            e.printStackTrace();
            connection.rollback();
            connection.setAutoCommit(true);
            submissionStatement.close();
            answerStatements.close();
            return HttpServletResponse.SC_BAD_REQUEST;
        }

        connection.commit();
        connection.setAutoCommit(true);
        submissionStatement.close();
        answerStatements.close();

        return HttpServletResponse.SC_CREATED;
    }

    /**
     * Parse the submission into a JSON string that can the be sent to salesforce. The answers should have been
     * validated against the survey already
     * 
     * @param resolveAnswerForAttachment
     *            - Resolve attachment name to file path
     * 
     * @return - The JSON string
     */
    public String testParseSubmissionToJson(Boolean resolveAnswerForAttachment) {
        return this.parseSubmissionToJson(resolveAnswerForAttachment);
    }

    private String parseSubmissionToJson(Boolean resolveAnswerForAttachment) {

        JsonSubmission jsonSubmission = new JsonSubmission();
        for (Entry<String, SubmissionAnswer> answerKey : this.surveyResponses.entrySet()) {
            SubmissionAnswer answer = answerKey.getValue();
            if (answer.getIsValid()) {

                // Make sure that these possible null pointers are caught
                String parentName = answer.getParent() == null ? "null" : answer.getParent().getQuestionName();
                String parentInstance = answer.getParent() == null ? "0" : String.valueOf(answer.getParent().getInstance());
                String answerText = answer.getAnswerText(this.attachmentReferences, resolveAnswerForAttachment) == null ? "null" : answer
                        .getAnswerText(this.attachmentReferences, resolveAnswerForAttachment);
                JsonAnswer jsonAnswer = new JsonAnswer(answerText, answer.getQuestionName(),
                        answer.getQuestion().getType().toString(), parentName, String.valueOf(answer.getInstance()),
                        String.valueOf(answer.getQuestion().getQuestionNumber()), survey.getBackEndSurveyXml().getXlation("en",
                                answer.getQuestion()),
                        parentInstance);
                jsonSubmission.addAnswer(jsonAnswer);
            }
        }
        String json = null;
        ArrayList<JsonAnswer> jsonAnswers = jsonSubmission.getAnswers();
        int numberOfAnswers = jsonAnswers.size();
        if (numberOfAnswers > 1) {
            Gson gson = new Gson();
            json = "{ \"answers\" : [ ";
            int counter = 1;
            for (JsonAnswer jsonAnswer : jsonAnswers) {
                json = json + gson.toJson(jsonAnswer);
                if (counter != numberOfAnswers) {
                    json = json + ", ";
                }
                counter++;
            }
            json = json + " ] }";
        }
        return json;
    }

    /**
     * Parse the submission into a Xml string that can the be sent to salesforce. The answers should have been validated
     * against the survey already
     * 
     * @param resolveAnswerForAttachment
     *            - Resolve attachment name to file path
     * 
     * @return - The XML string
     */
    public String testParseSubmissionToXml(Boolean resolveAnswerForAttachment, Boolean includeHeader) {
        return this.parseSubmissionToXml(resolveAnswerForAttachment);
    }

    private String parseSubmissionToXml(Boolean resolveAnswerForAttachment) {

        StringBuilder xml = new StringBuilder();

        HashMap<String, String> attributes = new HashMap<String, String>();
        xml.append(XmlHelpers.generateStartElement("answers", attributes));
        for (Entry<String, SubmissionAnswer> answerKey : this.surveyResponses.entrySet()) {
            SubmissionAnswer answer = answerKey.getValue();
            if (!answer.getIsValid()) {
                continue;
            }

            // Start the answer
            xml.append(XmlHelpers.generateStartElement("answer", attributes));

            // Make sure that these possible null pointers are caught
            String parentName = answer.getParent() == null ? "null" : answer.getParent().getQuestionName();
            String parentInstance = answer.getParent() == null ? "0" : String.valueOf(answer.getParent().getInstance());
            String answerText = answer.getAnswerText(this.attachmentReferences, resolveAnswerForAttachment) == null ? "null" : answer
                    .getAnswerText(this.attachmentReferences, resolveAnswerForAttachment);

            // Add an element for each piece of data that is included
            xml.append(XmlHelpers.generateStartElement("binding", attributes));
            xml.append(XmlHelpers.escapeText(answer.getQuestionName()));
            xml.append(XmlHelpers.generateEndElement("binding"));
            xml.append(XmlHelpers.generateStartElement("answerText", attributes));
            xml.append(XmlHelpers.escapeText(answerText));
            xml.append(XmlHelpers.generateEndElement("answerText"));
            xml.append(XmlHelpers.generateStartElement("instance", attributes));
            xml.append(XmlHelpers.escapeText(String.valueOf(answer.getInstance())));
            xml.append(XmlHelpers.generateEndElement("instance"));
            xml.append(XmlHelpers.generateStartElement("questionNumber", attributes));
            xml.append(XmlHelpers.escapeText(String.valueOf(answer.getQuestion().getQuestionNumber())));
            xml.append(XmlHelpers.generateEndElement("questionNumber"));
            xml.append(XmlHelpers.generateStartElement("questionType", attributes));
            xml.append(XmlHelpers.escapeText(answer.getQuestion().getType().toString()));
            xml.append(XmlHelpers.generateEndElement("questionType"));
            xml.append(XmlHelpers.generateStartElement("parentBinding", attributes));
            xml.append(XmlHelpers.escapeText(parentName));
            xml.append(XmlHelpers.generateEndElement("parentBinding"));
            xml.append(XmlHelpers.generateStartElement("parentInstance", attributes));
            xml.append(XmlHelpers.escapeText(parentInstance));
            xml.append(XmlHelpers.generateEndElement("parentInstance"));

            // End the answer
            xml.append(XmlHelpers.generateEndElement("answer"));
        }
        xml.append(XmlHelpers.generateEndElement("answers"));
        return xml.toString();
    }

    /**
     * Save the submission to Salesforce
     * 
     * @return - A String pair with the following format 1st element = String value of the respose code 2nd element =
     *         Message string.
     */
    public String[] saveToSalesforce()
                   throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ClassNotFoundException, SQLException,
                   ServiceException {

        ProcessSurveySubmissionServiceLocator serviceLocator = new ProcessSurveySubmissionServiceLocator();
        ProcessSurveySubmissionBindingStub serviceStub = (ProcessSurveySubmissionBindingStub)serviceLocator.getProcessSurveySubmission();

        // Use soap api to login and get session info
        SforceServiceLocator soapServiceLocator = new SforceServiceLocator();
        soapServiceLocator.setSoapEndpointAddress((String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceAddress", ""));
        SoapBindingStub binding = (SoapBindingStub)soapServiceLocator.getSoap();
        LoginResult loginResult = binding.login((String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceUsername", ""),
                (String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforcePassword", "")
                        + (String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceToken", ""));
        SessionHeader sessionHeader = new SessionHeader(loginResult.getSessionId());

        // Share the session info with our webservice
        serviceStub.setHeader("http://soap.sforce.com/schemas/class/ProcessSurveySubmission", "SessionHeader", sessionHeader);

        // Generate the String that is to be sent to Salesforce. Always send as XML for the moment
        this.xmlString = XmlHelpers.getXmlHeader() + this.parseSubmissionToXml(true);
        this.jsonString = "none";

        // Create and populate the webservice
        SurveySubmission surveySubmission = new SurveySubmission();
        surveySubmission.setJson(this.jsonString);
        surveySubmission.setXml(this.xmlString);
        surveySubmission.setImei(this.imei);
        surveySubmission.setFarmerId(this.intervieweeId);
        surveySubmission.setSurveyId(this.survey.getSalesforceId());
        surveySubmission.setResultHash(this.duplicateHash);
        surveySubmission.setSurveySize(String.valueOf(this.size));
        surveySubmission.setHandsetSubmitTime(String.valueOf(this.handsetSubmissionTime.getTime()));
        surveySubmission.setSubmissionStartTime(String.valueOf(this.submissionStartTime.getTime()));

        // Generate the interview location
        Location locationObject = null;

        locationObject = Location.parseLocation(this.interviewLocation);
        surveySubmission.setInterviewLatitude(locationObject.latitude.toString());
        surveySubmission.setInterviewLongitude(locationObject.longitude.toString());
        surveySubmission.setInterviewAltitude(locationObject.altitude.toString());
        surveySubmission.setInterviewAccuracy(locationObject.accuracy.toString());
        surveySubmission.setInterviewGPSTimestamp(String.valueOf(locationObject.timestamp));
        locationObject = null;

        // Generate the submission location
        locationObject = Location.parseLocation(this.submissionLocation);
        surveySubmission.setSubmissionLatitude(locationObject.latitude.toString());
        surveySubmission.setSubmissionLongitude(locationObject.longitude.toString());
        surveySubmission.setSubmissionAltitude(locationObject.altitude.toString());
        surveySubmission.setSubmissionAccuracy(locationObject.accuracy.toString());
        surveySubmission.setSubmissionGPSTimestamp(String.valueOf(locationObject.timestamp));

        // Send the submission to salesforce
        SurveySubmission resultSurveySubmission = serviceStub.processSurveySubmission(surveySubmission);

        // Check the result and return the correct response code
        String returnCode = String.valueOf(HttpServletResponse.SC_CREATED);
        if (!resultSurveySubmission.getSuccess()) {
            returnCode = String.valueOf(HttpServletResponse.SC_BAD_REQUEST);
        }
        String[] returnValues = { returnCode, resultSurveySubmission.getErrorMessage() };
        return returnValues;
    }

    /**
     * Get the details of the interviewer
     * 
     */
    private Boolean getInterviewerDetails() throws InvalidSObjectFault, MalformedQueryFault, InvalidFieldFault,
            InvalidIdFault, UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException, ServiceException {

        this.interviewer = new Actor(ActorType.CKW, null, this.imei);
        return this.interviewer.load();
    }

    /**
     * Validate the submission. This also creates the handset submit time and the duplicate hash
     */
    public String[] validateSubmission() throws ParseException, SAXException, IOException, ParserConfigurationException, ServiceException {

        String[] returnValues = new String[2];

        // Get the details of the interviewer that are needed.
        if (!getInterviewerDetails()) {
            returnValues[0] = "0";
            returnValues[1] = "The Interviewer does not exist in our system";
            return returnValues;
        }

        if (!this.setHandsetSubmitTime()) {
            returnValues[0] = "0";
            returnValues[1] = "There is no handset submit time for this submission";
            return returnValues;
        }

        // Take out the handset submit time
        this.surveyResponses.remove("handset_submit_time:0");

        // Add the submission start time. TODO - Add success to this in 4.3 as all surveys will have this value
        this.setSubmissionStartTime();
        this.surveyResponses.remove("submission_start_time:0");

        // Create the duplicate hash
        this.setDuplicateHash();

        // Validate the answers
        if (!this.validateAnswers()) {
            returnValues[0] = "0";
            returnValues[1] = "The answers do not match the survey question";
            return returnValues;
        }
        returnValues[0] = "1";
        return returnValues;
    }

    /**
     * Get the handset submission time for the submission
     */
    private Boolean setHandsetSubmitTime() throws ParseException {

        this.handsetSubmissionTime = new Date();
        boolean success = false;
        if (surveyResponses.containsKey("handset_submit_time:0")) {
            String tempTime = surveyResponses.get("handset_submit_time:0").getAnswerText(attachmentReferences, true);
            String date = tempTime.substring(0, 10);
            String time = tempTime.substring(11, 19);
            String dateTime = date + " " + time;
            this.handsetSubmissionTime = DatabaseHelpers.getJavaDateFromString(dateTime, 0);
            success = true;
        }
        return success;
    }

    /**
     * Get the submission start time for the submission
     */
    private Boolean setSubmissionStartTime() throws ParseException {

        this.submissionStartTime = new Date();
        boolean success = false;
        if (surveyResponses.containsKey("submission_start_time:0")) {
            String tempTime = surveyResponses.get("submission_start_time:0").getAnswerText(attachmentReferences, true);
            String date = tempTime.substring(0, 10);
            String time = tempTime.substring(11, 19);
            String dateTime = date + " " + time;
            this.submissionStartTime = DatabaseHelpers.getJavaDateFromString(dateTime, 0);
            success = true;
        }
        return success;
    }

    /**
     * Generate the hex string that detects duplicate submissions
     */
    private void setDuplicateHash() {

        String hashSource = this.imei;
        for (SubmissionAnswer responseValue : this.surveyResponses.values()) {
            hashSource += responseValue.getAnswerText(this.attachmentReferences, false);
        }
        this.duplicateHash = HashHelpers.createSHA1(hashSource);
    }

    /**
     * Validate the answers that have been submitted. If an answer does not match a question then take it out of the
     * answers hash map
     * 
     * @return - A boolean indicating that there are some valid answers.
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    private boolean validateAnswers() throws SAXException,
            IOException, ParserConfigurationException {

        Boolean hasAnswers = false;

        // There are some hangovers that we need to take out
        this.surveyResponses.remove("location:0");
        Set<String> answerKeys = surveyResponses.keySet();
        for (String answerKey : answerKeys) {
            SubmissionAnswer answer = this.surveyResponses.get(answerKey);

            // Verify that the question is in the survey
            if (this.survey.getBackEndSurveyXml().hasQuestion(answerKey.split(":")[0])) {
                hasAnswers = true;

                // Now that we have parsed the survey we can add the question to the answer
                answer.setQuestion(this.survey.getBackEndSurveyXml().getQuestions().get(answerKey.split(":")[0]));
            }
            else {
                answer.setIsValid(false);
                break;
            }
            this.surveyResponses.put(answerKey, answer);
        }
        return hasAnswers;
    }

    /**
     * Save an attachment to file
     * 
     * @param attachment
     *            - The file to be saved
     * @param context
     *            - Servlet context
     * @param fullPath
     *            - The full path on the file system to where this will be saved
     * @param attachmentReference
     *            - The unique reference to the file
     * @throws Exception
     */
    public void saveAttachment(FileItem attachment, ServletRequestContext context,
                                String fullPath, String attachmentReference)
            throws Exception {

        File targetFile = new File(fullPath);

        // Create the parent directories if necessary
        File parentDirectory = targetFile.getParentFile();
        if (parentDirectory != null) {
            parentDirectory.mkdirs();
        }
        attachment.write(targetFile);

        // Store the full public path here, not the relative one. Need to remove the leading '/' from the path reference
        String publicAttachmentReference = context.getFullPath(attachmentReference.substring(1));
        this.addAttachment(attachment.getName(), publicAttachmentReference);
        this.attachmentFilePaths.put(attachment.getName(), fullPath);
    }

    /**
     * Create a unique reference for an attachment
     * 
     * @param contentType
     * 
     * @return - The reference string
     */
    public String createAttachmentReference(String contentType) {

        // Grab everything after the forward slash and convert to file extension (e.g. image/gif -> .gif)
        // use everything before the forward slash and use it for our directory (e.g. image/gif -> survey_images)
        int separatorIndex = contentType.lastIndexOf("/");
        assert (separatorIndex > 0 && separatorIndex < contentType.length()) : "callers should only pass valid content types";

        String fileExtension = "." + contentType.substring(separatorIndex + 1);

        // For certain extensions, we can make substitutions here if it proves necessary (e.g. jpeg->jpg)
        return this.getDiretoryName(contentType, separatorIndex) + this.generateAttachmentName() + fileExtension;
    }

    /**
     * Generates a random number to use as the file name for attachments
     */
    private String generateAttachmentName() {
        StringBuilder imageName = new StringBuilder();
        for (int digitIndex = 0; digitIndex < 30; digitIndex++) {
            int nextRandomNumber = this.attachmentNameGenerator.nextInt(10);
            imageName.append(Integer.toString(nextRandomNumber));
        }
        return imageName.toString();
    }

    private String getSavedFileName(String webPath) {
         return webPath.substring(webPath.lastIndexOf("/", webPath.lastIndexOf("/") - 1) + 1);
    }

    /**
     * Get the directory to save the files in
     *
     * @param contentType
     * @param separatorIndex
     * @return
     */
    private String getDiretoryName(String contentType, Integer separatorIndex) {
        return  "/survey_" + contentType.substring(0, separatorIndex) + "s/";
    }

    /**
     * Parses the XML for a survey response. Delegates most of its work to parseSurveySubmissionElement
     */
    public void parseSurveySubmission() {

        // Normalize the root node
        Element rootNode = this.xml.getDocumentElement();
        rootNode.normalize();

        HashMap<String, Integer> instanceRecord = new HashMap<String, Integer>();

        // Dig out the survey id from the submission
        String salesforceSurveyId = rootNode.getAttribute("id");
        if (!salesforceSurveyId.equals("") && SurveyDatabaseHelpers.getZebraSurveyId(salesforceSurveyId) != null) {
            this.salesforceIdFromRootNode = salesforceSurveyId;
            this.backendSurveyIdFromRootNode = SurveyDatabaseHelpers.getZebraSurveyId(salesforceSurveyId);
        }

        // Now parse the tree and populate surveyResponses with the raw data
        for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                parseSurveySubmissionElement((Element)childNode, null, instanceRecord);
            }
            else {

                // Don't care about other types of nodes
            }
        }
    }

    /**
     * Given an XML node, processes the contents into a SurveyItemResponse
     */
    private void parseSurveySubmissionElement(Element submissionElement,
                                                     SubmissionAnswer parentItem, HashMap<String, Integer> instanceRecord) {

        // Question name is always the name of the start element
        String questionName = submissionElement.getNodeName();

        int newInstance = 0;
        if (instanceRecord.containsKey(questionName)) {
            newInstance = instanceRecord.get(questionName) + 1;
        }

        // Create a new answer and increase the instance count for this question name
        SubmissionAnswer submissionAnswer = new SubmissionAnswer(null, questionName, newInstance, null, parentItem);
        String answerKey = submissionAnswer.getKey();
        this.surveyResponses.put(answerKey, submissionAnswer);
        instanceRecord.put(questionName, newInstance);

        // Walk the child nodes, and either populate with a text-answer, or recurse for the multiple-answer case
        for (Node childNode = submissionElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            switch (childNode.getNodeType()) {
                case Node.ELEMENT_NODE:
                    parseSurveySubmissionElement((Element)childNode, submissionAnswer, instanceRecord);
                    break;

                case Node.TEXT_NODE:
                    submissionAnswer.setAnswerText(childNode.getNodeValue());
                    break;

                default:

                    // Don't care about other types of nodes
                    break;
            }
        }
    }

    /**
     * Export the submission to another server via XML
     * 
     * @return
     */
    public String[] exportSubmissionToExternalServer() throws ServiceException, IOException {

        // Create the return array and init it to being a failure
        String[] returnValues = new String[2];
        returnValues[0] = String.valueOf(HttpServletResponse.SC_BAD_REQUEST);

        // Need to send this as a MultiPart Post as need to bounce the attachments across as well
        HttpClient httpclient = new DefaultHttpClient();
        String output = "";

        try {
            HttpPost post = new HttpPost(this.survey.getExportUrl());
            MultipartEntity reqEntity = new MultipartEntity();

            // Add the xml to the request
            StringBody xmlBody = new StringBody(this.generateExternalXml().toString(), "text/xml", Charset.forName("UTF-8"));
            reqEntity.addPart("submissionXml", xmlBody);

            // Loop through any attachments and add them to the request.
            for (Entry<String, String> attachment : this.attachmentReferences.entrySet()) {

                // Add the attachment to the request.
                // TODO - If ODK expands their list of supported formats then this list needs expanding
                String type;
                String fileName = this.attachmentFilePaths.get(attachment.getKey());
                if (attachment.getKey().endsWith(".jpg")) {
                    type = "image/jpeg";
                    output += "added image file " + fileName + ": ";
                }
                else if (attachment.getKey().endsWith(".3gpp")) {
                    type = "audio/3gpp";
                    output += "added audio file " + fileName + ": ";
                }
                else if (attachment.getKey().endsWith(".3gp")) {
                    type = "video/3gpp";
                    output += "added video file " + fileName + ": ";
                }
                else if (attachment.getKey().endsWith(".mp4")) {
                    type = "video/mp4";
                    output += "added video file " + fileName + ": ";
                }
                else {
                    output += "Unknown type of attachment for: ";
                    continue;
                }
                reqEntity.addPart(attachment.getKey(), new FileBody(new File(this.attachmentFilePaths.get(attachment.getKey())), type));
            }

            // Add the entity to the post request
            post.setEntity(reqEntity);

            // Execute the request
            HttpResponse response = httpclient.execute(post);
            HttpEntity resEntity = response.getEntity();

            // Parse the Response status line to look for success
            // TODO - Consider situations where something should be resent. Any suggestions from reviewers?
            StatusLine responseLine = response.getStatusLine();
            output += " " + responseLine.getReasonPhrase();
            returnValues[1] = output;
            switch (responseLine.getStatusCode()) {
                case HttpServletResponse.SC_CREATED:
                case HttpServletResponse.SC_OK:
                    returnValues[0] = String.valueOf(HttpServletResponse.SC_CREATED);
                    break;
                default :
                    returnValues[0] = String.valueOf(responseLine.getStatusCode());
                    break;
            }

            EntityUtils.consume(resEntity);
        }
        catch (Exception e) {

            // When HttpClient instance is no longer needed, shut down the connection manager to ensure
            // immediate deallocation of all system resources
            try {
                httpclient.getConnectionManager().shutdown();
            }
            catch (Exception ignore) {}
            output += " " + e.getMessage();
            returnValues[1] = output;
        }
        return returnValues;
    }

    /**
     * Create the XML string that has all the data that is needed to export to an external server
     * 
     * @return - The XML StringBuilder
     */
    private StringBuilder generateExternalXml() throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException {

        StringBuilder xml = new StringBuilder();
        xml.append(XmlHelpers.getXmlHeader());
        HashMap<String, String> attributes = new HashMap<String, String>();

        // Add the submission meta data as attributes to the submission element
        attributes.put("duplicateHash", this.duplicateHash);
        attributes.put("handsetSubmitTime", String.valueOf(this.handsetSubmissionTime.getTime()));
        attributes.put("submissionStartTime", String.valueOf(this.submissionStartTime.getTime()));
        attributes.put("serverEntryTime", String.valueOf(new Date().getTime()));
        attributes.put("interviewLocation", this.interviewLocation);
        attributes.put("submissionLocation", this.submissionLocation);
        attributes.put("submissionSize", this.submissionLocation);
        xml.append(XmlHelpers.generateStartElement("submission", attributes));

        attributes.clear();

        // Add the answers. If we do have attachments then the answers to those questions
        // need to be the name of the file and not the location on our box as that makes no sense
        if (this.attachmentReferences.size() == 0) {
            xml.append(this.xmlString);
        }
        else {

            // Going to need to rebuild the XML string to allow for attachments
            xml.append(this.parseSubmissionToXml(false));
        }

        // Add the interviewer data
        xml.append(XmlHelpers.generateStartElement("interviewerId", attributes));
        xml.append(XmlHelpers.escapeText(this.interviewer.getId()));
        xml.append(XmlHelpers.generateEndElement("interviewerId"));
        xml.append(XmlHelpers.generateStartElement("interviewerFirstName", attributes));
        xml.append(XmlHelpers.escapeText(this.interviewer.getFirstName()));
        xml.append(XmlHelpers.generateEndElement("interviewerFirstName"));
        xml.append(XmlHelpers.generateStartElement("interviewerLastName", attributes));
        xml.append(XmlHelpers.escapeText(this.interviewer.getLastName()));
        xml.append(XmlHelpers.generateEndElement("interviewerLastName"));
        xml.append(XmlHelpers.generateStartElement("interviewerMobileNumber", attributes));
        xml.append(XmlHelpers.escapeText(this.interviewer.getMobileNumber()));
        xml.append(XmlHelpers.generateEndElement("interviewerMobileNumber"));

        // Add the interviewee data. For the moment we only accept farmers TODO - Allow anyone but need a new way of ID
        Actor actor = new Actor(ActorType.FARMER, this.intervieweeId, null);
        actor.load();
        xml.append(XmlHelpers.generateStartElement("intervieweeId", attributes));
        xml.append(XmlHelpers.escapeText(this.intervieweeId));
        xml.append(XmlHelpers.generateEndElement("intervieweeId"));
        xml.append(XmlHelpers.generateStartElement("intervieweeFirstName", attributes));
        xml.append(XmlHelpers.escapeText(actor.getFirstName()));
        xml.append(XmlHelpers.generateEndElement("intervieweeFirstName"));
        xml.append(XmlHelpers.generateStartElement("intervieweeLastName", attributes));
        xml.append(XmlHelpers.escapeText(actor.getLastName()));
        xml.append(XmlHelpers.generateEndElement("intervieweeLastName"));

        // Close the submission
        xml.append(XmlHelpers.generateEndElement("submission"));
        return xml;
    }

    /**
     * Private class to represent an actor in the submission. Used as could be anyone on our system. Not a set object I
     * am aware that at the moment we are only getting data from the Person object so CKW and PERSON could be flattened
     * but might as well leave the room to improve in the future
     */
    private class Actor {

        ActorType type;
        String actorImei;
        String id;
        String firstName = "Not Found";
        String lastName = "Not Found";
        String mobileNumber = "Not Found";

        public Actor(ActorType type, String id, String actorImei) {
            this.type = type;
            this.id = id;
            this.actorImei = actorImei;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getId() {
            return id;
        }

        public String getMobileNumber() {
            return mobileNumber;
        }

        public String getActorImei() {
            return actorImei;
        }

        public Boolean load() throws InvalidSObjectFault, MalformedQueryFault, InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault,
                InvalidQueryLocatorFault, RemoteException, ServiceException {

            switch (this.type) {
                case CKW:
                    return loadCKW();
                case FARMER:
                    return loadFarmer();
                case PERSON:
                    return loadPerson(null);
                default:
                    return false;
            }
        }

        private Boolean loadCKW() throws InvalidSObjectFault, MalformedQueryFault, InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault,
                InvalidQueryLocatorFault, RemoteException, ServiceException {
            CommunityKnowledgeWorker ckw = CommunityKnowledgeWorker.load(this.actorImei);

            // See if this could be a normal person
            if (ckw == null) {
                return loadPerson(this.actorImei);
            }
            this.id = ckw.getCkwSalesforceName();
            this.firstName = ckw.getFirstName();
            this.lastName = ckw.getLastName();
            this.mobileNumber = ckw.getMobileNumber();
            return true;
        }

        private Boolean loadFarmer() throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException {
            Boolean success = false;
            Farmer farmer = Farmer.load(this.id);
            if (farmer != null) {
                this.id = farmer.getFarmerSalesforceName();
                this.firstName = farmer.getFirstName();
                this.lastName = farmer.getLastName();
                this.mobileNumber = farmer.getMobileNumber();
                success = true;
            }
            return success;
        }

        private Boolean loadPerson(String imei) throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException {
            Boolean success = false;
            Person person = null;
            if (imei != null || this.actorImei != null) {
                person = Person.load(this.actorImei);
            }
            else if (this.id != null) {
                person = Person.loadID(this.id);
            }
            if (person != null) {
                this.id = person.getSalesforceName();
                this.firstName = person.getFirstName();
                this.lastName = person.getLastName();
                this.mobileNumber = person.getMobileNumber();
                success = true;
            }
            return success;
        }
    }

    private enum ActorType {
        CKW,
        FARMER,
        PERSON
    }
}
