package applab.surveys.server;

import java.io.File;
import java.io.IOException;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import applab.CommunityKnowledgeWorker;
import applab.Location;
import applab.Person;
import applab.server.ApplabConfiguration;
import applab.server.ApplabServlet;
import applab.server.DatabaseHelpers;
import applab.server.HashHelpers;
import applab.server.ServletRequestContext;
import applab.server.WebAppId;
import applab.server.XmlHelpers;
import applab.surveys.JsonAnswer;
import applab.surveys.JsonSubmission;
import applab.surveys.SubmissionAnswer;
import applab.surveys.Survey;

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
 * Server method that receives a survey submission and inserts the data into our database
 * 
 * Input: survey submission as XML (provided in POST body)
 * 
 * Output: Empty body, status code distinguishes success from failure
 * 
 */
public class ProcessSubmission extends ApplabServlet {

    private static final long serialVersionUID = 1L;
    private static Random attachmentNameGenerator = new Random();
    private static String backendSurveyIdFromRootNode = null;

    @Override
    protected void doApplabPost(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {

        response.setContentType("text/html");
        response.setHeader("Location", request.getRequestURI());
        String intervieweeId = request.getHeader("x-applab-interviewee-id");
        String location = request.getHeader("x-applab-survey-location");
        String submissionLocation = context.getSubmissionLocation();
        String imei = context.getHandsetId();

        // We are only expecting multi-part content
        if (!ServletFileUpload.isMultipartContent(request)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "request must be MIME encoded");
            return;
        }
        ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());
        List<?> fileList = servletFileUpload.parseRequest(request);

        HashMap<String, SubmissionAnswer> surveyResponses = null;

        // Store the paths to the attachments in attachment path
        HashMap<String, String> attachmentReferences = new HashMap<String, String>();
        Iterator<?> fileIterator = fileList.iterator();

        long totalSize = 0;
        while (fileIterator.hasNext()) {
            FileItem fileItem = (FileItem)fileIterator.next();
            totalSize = totalSize + fileItem.getSize();

            String contentType = fileItem.getContentType().toLowerCase(Locale.ENGLISH);
            contentType = contentType.intern(); // so that == works for comparison

            // Survey answer content
            if (contentType == "text/xml") {

                // This has the farmerId embedded in the old system. TODO - remove in > 3.2
                String fileName = fileItem.getName();

                // Check that we have the id. If not try and get it the old way
                if (intervieweeId == null || intervieweeId.equals("")) {
                    intervieweeId = getIntervieweeId(fileName);
                }
                Document xmlDocument = XmlHelpers.parseXml(fileItem.getString());
                surveyResponses = parseSurveySubmission(xmlDocument);

                // Log the XML input
                log(imei + " Has submitted from - " + fileItem.getString());
            }

            // Attachments (TODO: open this further?)
            else if (contentType == "image/jpeg" || contentType == "image/gif" || contentType == "image/png" || contentType == "image/bmp"
                    || contentType == "video/3gp" || contentType == "video/mp4" || contentType == "video/3gpp"
                    || contentType == "audio/3gp" || contentType == "audio/mp4" || contentType == "audio/m4a"
                    || contentType == "audio/3gpp") {
                saveAttachment(fileItem, attachmentReferences, context);
            }
        }

        int backendSurveyId = getBackendSurveyId(surveyResponses, attachmentReferences);
        String salesforceId = SurveyDatabaseHelpers.verifySurveyID(backendSurveyId);
        if (salesforceId == null) {
            log("Cannot submit this survey for handset " + imei + " because the backend survey id " + backendSurveyId
                    + " does not exist on our system");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The survey does not exist");
            return;
        }

        Date handsetSubmissionTime = getHandsetSubmissionTime(surveyResponses, attachmentReferences);

        // The following permanent fields should not be included in creating a hex string
        if (surveyResponses.containsKey("handset_submit_time:0")) {
            surveyResponses.remove("handset_submit_time:0");
        }

        // Generate the duplicate hash
        String duplicateDetectionHash = getDuplicateHash(surveyResponses, attachmentReferences, imei);

        // Load the survey so we can verify the answer columns against the questions in the Xml.
        Survey survey = new Survey(salesforceId);
        if (!survey.loadSurvey(salesforceId, true)) {

            // Someone has deleted the form from salesforce. Should never be done. Fail silently so the
            // submission is off the phone. Then find who deleted the form and lightly kill them.
            log("Handset with IMEI: " + imei + " submitted survey id " + salesforceId
                    + ". It no longer exists in SF. Find out what happened");
            response.setStatus(HttpServletResponse.SC_CREATED);
            return;
        }

        int httpResponseCode = -1;

        // Validate the survey against the questions. This will remove any answers that are not in the survey.
        if (validateAnswers(surveyResponses, survey)) {

            Boolean saveToBackend = true;

            // Decide where this survey should be saved to
            if (survey.getSaveToSalesforce()) {

                // We may want to pass data to salesforce and then save the actual submission to backend.
                saveToBackend = survey.getSaveToBackend();

                // Comment the JSON out as it causes script limit issues in Salesforce
                // String json = parseSubmissionToJson(surveyResponses, attachmentReferences, survey);
                String json = "none";
                String xml = parseSubmissionToXml(surveyResponses, attachmentReferences, survey);
                String[] returnValues = saveToSalesforce(xml, json, imei, totalSize, intervieweeId, location,
                        submissionLocation, handsetSubmissionTime, duplicateDetectionHash, survey);

                httpResponseCode = Integer.valueOf(returnValues[0]);
                log("Handset with IMEI: " + imei + " submitted a survey with the following result " + returnValues[1]);
            }
            if (saveToBackend && (httpResponseCode == -1 || httpResponseCode == 201)) {
                httpResponseCode = storeSurveySubmission(surveyResponses, attachmentReferences, imei,
                        totalSize, intervieweeId, location, submissionLocation, backendSurveyId, survey,
                        handsetSubmissionTime, duplicateDetectionHash);
            }
        }
        else {
            httpResponseCode = HttpServletResponse.SC_BAD_REQUEST;
        }
        response.setStatus(httpResponseCode);
    }

    /**
     * Store the submission in our database
     * 
     * @param surveyResponses
     *            - HashMap of the answers to the survey
     * @param attachmentReferences
     *            - HashMap of the attachment locations on the server
     * @param imei
     *            - IMEI of the submitting phone
     * @param submissionSize
     *            - In bytes
     * @param intervieweeName
     *            - Id of the person being interviewed
     * @param location
     *            - The location that the survey was carried out
     * @param submissionLocation
     *            - The location that the survey was submitted
     * @param backendSurveyId
     *            - The primary key for the survey
     * @param survey
     *            - The survey we are parsing
     * @param handsetSubmissionTime
     *            - A date for the time the submission was made
     * @param duplicateDetectionHash
     *            - Hash of the submission to check for duplicates
     * 
     * @return - response code
     */
    public static int storeSurveySubmission(HashMap<String, SubmissionAnswer> surveyResponses,
                                            HashMap<String, String> attachmentReferences, String imei, long submissionSize,
                                            String intervieweeName, String location, String submissionLocation,
                                            int backendSurveyId, Survey survey,
                                            Date handsetSubmissionTime, String duplicateDetectionHash)
            throws NoSuchAlgorithmException, ServiceException,
            ClassNotFoundException, SQLException, ParseException, SAXException, IOException, ParserConfigurationException {

        // Get the details of the interviewer that are needed.
        String[] interviewerDetails = getInterviewerDetails(imei);
        if (interviewerDetails == null) {
            return HttpServletResponse.SC_NOT_FOUND;
        }
        String interviewerId = interviewerDetails[0];
        String fullName = interviewerDetails[1];
        String mobileNumber = interviewerDetails[2];

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
        commandText.append(") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        // Create the prepared statement
        PreparedStatement submissionStatement = connection.prepareStatement(commandText.toString(), Statement.RETURN_GENERATED_KEYS);

        // Add the params to the query
        submissionStatement.setInt(1, backendSurveyId);
        submissionStatement.setTimestamp(2, DatabaseHelpers.getTimestamp(new Date()));
        submissionStatement.setTimestamp(3, DatabaseHelpers.getTimestamp(handsetSubmissionTime));
        submissionStatement.setString(4, imei);
        submissionStatement.setString(5, interviewerId);
        submissionStatement.setString(6, fullName);
        submissionStatement.setString(7, duplicateDetectionHash);
        submissionStatement.setLong(8, submissionSize);
        submissionStatement.setString(9, mobileNumber);
        submissionStatement.setString(10, location);
        submissionStatement.setString(11, intervieweeName);
        if ("Draft".equals(survey.getSurveyStatus().toString())) {
            submissionStatement.setString(12, "Y");
        }
        else {
            submissionStatement.setString(12, "N");
        }
        submissionStatement.setString(13, submissionLocation);

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
            answerStatements.setString(4, answer.getAnswerText(attachmentReferences));

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
     * @param surveyResponses
     *            - HashMap of the answers to the survey
     * @param attachmentReferences
     *            - HashMap of the attachment locations on the server
     * 
     * @return - The JSON string
     */
    public static String testParseSubmissionToJson(HashMap<String, SubmissionAnswer> surveyResponses,
                                                   HashMap<String, String> attachmentReferences, Survey survey) {
        return parseSubmissionToJson(surveyResponses, attachmentReferences, survey);
    }

    private static String parseSubmissionToJson(HashMap<String, SubmissionAnswer> surveyResponses,
                                                HashMap<String, String> attachmentReferences, Survey survey) {

        JsonSubmission jsonSubmission = new JsonSubmission();
        for (Entry<String, SubmissionAnswer> answerKey : surveyResponses.entrySet()) {
            SubmissionAnswer answer = answerKey.getValue();
            if (answer.getIsValid()) {

                // Make sure that these possible null pointers are caught
                String parentName = answer.getParent() == null ? "null" : answer.getParent().getQuestionName();
                String parentInstance = answer.getParent() == null ? "0" : String.valueOf(answer.getParent().getInstance());
                String answerText = answer.getAnswerText(attachmentReferences) == null ? "null" : answer
                        .getAnswerText(attachmentReferences);
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
     * @param surveyResponses
     *            - HashMap of the answers to the survey
     * @param attachmentReferences
     *            - HashMap of the attachment locations on the server
     * 
     * @return - The XML string
     */
    public static String testParseSubmissionToXml(HashMap<String, SubmissionAnswer> surveyResponses,
                                                  HashMap<String, String> attachmentReferences, Survey survey) {
        return parseSubmissionToXml(surveyResponses, attachmentReferences, survey);
    }

    private static String parseSubmissionToXml(HashMap<String, SubmissionAnswer> surveyResponses,
                                               HashMap<String, String> attachmentReferences, Survey survey) {

        StringBuilder xml = new StringBuilder();
        xml.append(XmlHelpers.getXmlHeader());

        HashMap<String, String> attributes = new HashMap<String, String>();
        xml.append(XmlHelpers.generateStartElement("answers", attributes));
        for (Entry<String, SubmissionAnswer> answerKey : surveyResponses.entrySet()) {
            SubmissionAnswer answer = answerKey.getValue();
            if (!answer.getIsValid()) {
                continue;
            }

            // Start the answer
            xml.append(XmlHelpers.generateStartElement("answer", attributes));

            // Make sure that these possible null pointers are caught
            String parentName = answer.getParent() == null ? "null" : answer.getParent().getQuestionName();
            String parentInstance = answer.getParent() == null ? "0" : String.valueOf(answer.getParent().getInstance());
            String answerText = answer.getAnswerText(attachmentReferences) == null ? "null" : answer.getAnswerText(attachmentReferences);

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
     * @param xml
     *            - The Xml String for the submission (pass "none" if using Json)
     * @param json
     *            - The JSON string to be sent to the webservice (pass "none" if using XML)
     * @param imei
     *            - IMEI of the submitting phone
     * @param submissionSize
     *            - In bytes
     * @param intervieweeName
     *            - Id of the person being interviewed
     * @param location
     *            - The location that the survey was carried out
     * @param submissionLocation
     *            - The location that the survey was submitted
     * @param handsetSubmissionTime
     *            - A date for the time the submission was made
     * @param duplicateDetectionHash
     *            - Hash of the submission to check for duplicates
     * @param survey
     *            - Survey object that is being saved
     * 
     * @return - A String pair with the following format 1st element = String value of the respose code 2nd element =
     *         Message string.
     */
    public static String[] saveToSalesforcePublic(String xml, String json, String imei, long submissionSize,
                                                  String intervieweeName, String location, String submissionLocation,
                                                  Date handsetSubmissionTime, String duplicateDetectionHash, Survey survey)
                   throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ClassNotFoundException, SQLException,
                   ServiceException {
        return saveToSalesforce(xml, json, imei, submissionSize, intervieweeName, location, submissionLocation, handsetSubmissionTime,
                duplicateDetectionHash, survey);
    }

    private static String[] saveToSalesforce(String xml, String json, String imei, long submissionSize,
                                             String intervieweeName, String location, String submissionLocation,
                                             Date handsetSubmissionTime, String duplicateDetectionHash, Survey survey)
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

        // Create and populate the webservice
        SurveySubmission surveySubmission = new SurveySubmission();
        surveySubmission.setJson(json);
        surveySubmission.setXml(xml);
        surveySubmission.setImei(imei);
        surveySubmission.setFarmerId(intervieweeName);
        surveySubmission.setSurveyId(survey.getSalesforceId());
        surveySubmission.setResultHash(duplicateDetectionHash);
        surveySubmission.setSurveySize(String.valueOf(submissionSize));
        surveySubmission.setHandsetSubmitTime(String.valueOf(handsetSubmissionTime.getTime()));

        // Generate the interview location
        Location locationObject = null;

        locationObject = Location.parseLocation(location);
        surveySubmission.setInterviewLatitude(locationObject.latitude.toString());
        surveySubmission.setInterviewLongitude(locationObject.longitude.toString());
        surveySubmission.setInterviewAltitude(locationObject.altitude.toString());
        surveySubmission.setInterviewAccuracy(locationObject.accuracy.toString());
        surveySubmission.setInterviewGPSTimestamp(String.valueOf(locationObject.timestamp));
        locationObject = null;

        // Generate the submission location
        locationObject = Location.parseLocation(submissionLocation);
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
     * Get the backend survey id from the submission
     * 
     * @param surveyResponses
     *            - Map containing the answers.
     * @param attachmentReferences
     *            - Map containing the paths to attachments.
     * 
     * @return - The backend survey id for this submission.
     */
    private static int getBackendSurveyId(HashMap<String, SubmissionAnswer> surveyResponses, HashMap<String, String> attachmentReferences) {

        // The <name>:0 notation for the key is used here as these are special case answers
        // that cannot be duplicated so will always have the instance number of 0
        // Use the old method first if possible
        int backendSurveyId;
        if (surveyResponses.containsKey("survey_id:0")) {
            backendSurveyId = Integer.parseInt(surveyResponses.get("survey_id:0").getAnswerText(attachmentReferences));
        }
        else {
            backendSurveyId = Integer.parseInt(backendSurveyIdFromRootNode);
        }
        return backendSurveyId;
    }

    /**
     * Get the handset submission time for the submission
     * 
     * @param surveyResponses
     *            - Map containing the answers.
     * @param attachmentReferences
     *            - Map containing the paths to attachments.
     * 
     * @return - A date representing the handset submission time
     */
    public static Date getHandsetSubmissionTimePublic(HashMap<String, SubmissionAnswer> surveyResponses,
                                                      HashMap<String, String> attachmentReferences) throws ParseException {
        return getHandsetSubmissionTime(surveyResponses, attachmentReferences);
    }

    private static Date getHandsetSubmissionTime(HashMap<String, SubmissionAnswer> surveyResponses,
                                                 HashMap<String, String> attachmentReferences) throws ParseException {

        Date handsetSubmissionTime = new Date();
        if (surveyResponses.containsKey("handset_submit_time:0")) {
            String tempTime = surveyResponses.get("handset_submit_time:0").getAnswerText(attachmentReferences);
            String date = tempTime.substring(0, 10);
            String time = tempTime.substring(11, 19);
            String dateTime = date + " " + time;
            handsetSubmissionTime = DatabaseHelpers.getJavaDateFromString(dateTime, 0);
        }
        return handsetSubmissionTime;
    }

    /**
     * Generate the hex string that detects duplicate submissions
     * 
     * @param surveyResponses
     *            - Map containing the answers.
     * @param attachmentReferences
     *            - Map containing the paths to attachments.
     * 
     * @return - The hash string
     */
    public static String getDuplicateHashPublic(HashMap<String, SubmissionAnswer> surveyResponses,
                                                HashMap<String, String> attachmentReferences, String imei) {
        return getDuplicateHash(surveyResponses, attachmentReferences, imei);
    }

    private static String getDuplicateHash(HashMap<String, SubmissionAnswer> surveyResponses, HashMap<String, String> attachmentReferences,
                                           String imei) {
        String hashSource = imei;
        for (SubmissionAnswer responseValue : surveyResponses.values()) {
            hashSource += responseValue.getAnswerText(attachmentReferences);
        }
        return HashHelpers.createSHA1(hashSource);
    }

    /**
     * Get the details of the interviewer
     * 
     * @param imei
     *            - of the handset that submitted the survey
     * 
     * @return - An array of Strings with the following format element 1 = interviewerId element 2 = fullName element 3
     *         = mobileNumber Or return null if fail to get details
     */
    private static String[] getInterviewerDetails(String imei) throws InvalidSObjectFault, MalformedQueryFault, InvalidFieldFault,
            InvalidIdFault, UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException, ServiceException {

        String interviewerId = "";
        String fullName = "";
        String mobileNumber = "";
        CommunityKnowledgeWorker interviewer = CommunityKnowledgeWorker.load(imei);
        Person interviewerPerson = null;

        // If we don't find the CKW try to load as a person
        if (interviewer == null) {
            interviewerPerson = Person.load(imei);

            // If we don't have an interviewer the use the test person.
            if (interviewerPerson == null) {
                interviewerPerson = Person.loadTestPerson();
            }

            // Still don't have an interviewer then bail out. If you ever get here you need to add a
            // Test person to Salesforce
            if (interviewerPerson == null) {
                return null;
            }
            else {
                if (interviewerId.equals("")) {
                    interviewerId = interviewerPerson.getSalesforceName();
                    fullName = interviewerPerson.getFullName();
                    mobileNumber = interviewerPerson.getMobileNumber();
                }
            }
        }
        else {
            interviewerId = interviewer.getCkwSalesforceName();
            fullName = interviewer.getFullName();
            mobileNumber = interviewer.getMobileNumber();
        }
        String[] values = { interviewerId, fullName, mobileNumber };
        return values;
    }

    /**
     * Extract the intervieweeId from the filename. TODO - Remove in > 3.2
     * 
     * @param fileName
     * @return
     */
    public static String getIntervieweeId(String fileName) {

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
     * Validate the answers that have been submitted. If an answer does not match a question then take it out of the
     * answers hash map
     * 
     * @param surveyResponses
     *            - Map containing the answers.
     * @param survey
     *            - The survey we are checking the answers against
     * 
     * @return - A boolean indicating that there are some valid answers.
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static boolean validateAnswersPublic(HashMap<String, SubmissionAnswer> surveyResponses, Survey survey) throws SAXException,
            IOException, ParserConfigurationException {
        return validateAnswers(surveyResponses, survey);
    }

    private static boolean validateAnswers(HashMap<String, SubmissionAnswer> surveyResponses, Survey survey) throws SAXException,
            IOException, ParserConfigurationException {

        Boolean hasAnswers = false;

        // There are some hangovers that we need to take out
        surveyResponses.remove("location:0");
        Set<String> answerKeys = surveyResponses.keySet();
        for (String answerKey : answerKeys) {
            SubmissionAnswer answer = surveyResponses.get(answerKey);

            // Verify that the question is in the survey
            if (survey.getBackEndSurveyXml().hasQuestion(answerKey.split(":")[0])) {
                hasAnswers = true;

                // Now that we have parsed the survey we can add the question to the answer
                answer.setQuestion(survey.getBackEndSurveyXml().getQuestions().get(answerKey.split(":")[0]));
            }
            else {
                answer.setIsValid(false);
            }
            surveyResponses.put(answerKey, answer);
        }
        return hasAnswers;
    }

    private void saveAttachment(FileItem attachment, HashMap<String, String> attachmentReferences, ServletRequestContext context)
            throws Exception {
        String attachmentReference = createAttachmentReference(attachment.getContentType());

        String fullPath = this.getServletContext().getRealPath(attachmentReference);
        File targetFile = new File(fullPath);

        // Create the parent directories if necessary
        File parentDirectory = targetFile.getParentFile();
        if (parentDirectory != null) {
            parentDirectory.mkdirs();
        }
        attachment.write(targetFile);

        // Store the full public path here, not the relative one. Need to remove the leading '/' from the path reference
        String publicAttachmentReference = context.getFullPath(attachmentReference.substring(1));
        attachmentReferences.put(attachment.getName(), publicAttachmentReference);
    }

    // Given a content type, generate a relative file reference to the generated name
    // public so that we can test this functionality
    public static String createAttachmentReference(String contentType) {

        // Grab everything after the forward slash and convert to file extension (e.g. image/gif -> .gif)
        // use everything before the forward slash and use it for our directory (e.g. image/gif -> survey_images)
        int separatorIndex = contentType.lastIndexOf("/");
        assert (separatorIndex > 0 && separatorIndex < contentType.length()) : "callers should only pass valid content types";

        String directoryName = "/survey_" + contentType.substring(0, separatorIndex) + "s/";
        String fileExtension = "." + contentType.substring(separatorIndex + 1);

        // For certain extensions, we can make substitutions here if it proves necessary (e.g. jpeg->jpg)
        return directoryName + generateAttachmentName() + fileExtension;
    }

    /**
     * Generates a random number to use as the file name for attachments
     */
    private static String generateAttachmentName() {
        StringBuilder imageName = new StringBuilder();
        for (int digitIndex = 0; digitIndex < 30; digitIndex++) {
            int nextRandomNumber = attachmentNameGenerator.nextInt(10);
            imageName.append(Integer.toString(nextRandomNumber));
        }
        return imageName.toString();
    }

    public static HashMap<String, SubmissionAnswer> parseSurveySubmissionPublic(Document xmlDocument) {
        return parseSurveySubmission(xmlDocument);
    }

    /**
     * parses the XML for a survey response. Delegates most of its work to parseSurveySubmissionElement
     * 
     * @param xmlDocument
     *            DOM containing the submission XML
     * @param survey
     *            - The survey that the submission is for
     */
    private static HashMap<String, SubmissionAnswer> parseSurveySubmission(Document xmlDocument) {

        // Normalize the root node
        Element rootNode = xmlDocument.getDocumentElement();
        rootNode.normalize();

        HashMap<String, SubmissionAnswer> parsedSubmission = new HashMap<String, SubmissionAnswer>();
        HashMap<String, Integer> instanceRecord = new HashMap<String, Integer>();

        // Dig out the survey id from the submission
        String salesforceSurveyId = rootNode.getAttribute("id");
        if (!salesforceSurveyId.equals("") && SurveyDatabaseHelpers.getZebraSurveyId(salesforceSurveyId) != null) {
            backendSurveyIdFromRootNode = SurveyDatabaseHelpers.getZebraSurveyId(salesforceSurveyId);
        }

        // Now parse the tree and populate surveyResponses with the raw data
        for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                parseSurveySubmissionElement((Element)childNode, parsedSubmission, null, instanceRecord);
            }
            else {
                // Don't care about other types of nodes
            }
        }

        return parsedSubmission;
    }

    /**
     * given an XML node, processes the contents into a SurveyItemResponse
     */
    private static void parseSurveySubmissionElement(Element submissionElement, HashMap<String, SubmissionAnswer> existingSubmission,
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
        existingSubmission.put(answerKey, submissionAnswer);
        instanceRecord.put(questionName, newInstance);

        // Walk the child nodes, and either populate with a text-answer, or recurse for the multiple-answer case
        for (Node childNode = submissionElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            switch (childNode.getNodeType()) {
                case Node.ELEMENT_NODE:
                    parseSurveySubmissionElement((Element)childNode, existingSubmission, submissionAnswer, instanceRecord);
                    break;

                case Node.TEXT_NODE:
                    submissionAnswer.setAnswerText(childNode.getNodeValue());
                    break;

                default:
                    // don't care about other types of nodes
                    break;
            }
        }
    }
}
