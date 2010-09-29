package applab.surveys.server;

import java.io.File;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.rpc.ServiceException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import sun.util.logging.resources.logging;

import applab.CommunityKnowledgeWorker;
import applab.server.ApplabConfiguration;
import applab.server.ApplabServlet;
import applab.server.DatabaseHelpers;
import applab.server.DatabaseId;
import applab.server.HashHelpers;
import applab.server.ServletRequestContext;
import applab.server.XmlHelpers;
import applab.surveys.SubmissionAnswer;

import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;

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

    @Override
    protected void doApplabPost(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {
        response.setContentType("text/html");
        response.setHeader("Location", request.getRequestURI());

        // we are only expecting multi-part content
        if (!ServletFileUpload.isMultipartContent(request)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "request must be MIME encoded");
            return;
        }
        ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());
        List<?> fileList = servletFileUpload.parseRequest(request);

        HashMap<String, SubmissionAnswer> surveyResponses = null;

        // store the paths to the attachments in attachment path
        HashMap<String, String> attachmentPaths = new HashMap<String, String>();
        Iterator<?> fileIterator = fileList.iterator();

        long totalSize = 0;
        String farmerId = ""; // We extract the farmerId from the submission file name (TODO: Find better way to communicate it)
        while (fileIterator.hasNext()) {
            FileItem fileItem = (FileItem)fileIterator.next();
            totalSize = totalSize + fileItem.getSize();

            String contentType = fileItem.getContentType().toLowerCase(Locale.ENGLISH);
            contentType = contentType.intern(); // so that == works for comparison

            // survey answer content
            if (contentType == "text/xml") {
                String fileName = fileItem.getName(); // This has the farmerId embedded
                farmerId = getFarmerId(fileName);
                Document xmlDocument = XmlHelpers.parseXml(fileItem.getString());
                surveyResponses = parseSurveySubmission(xmlDocument);

                // Log the XML input
                log(fileItem.getString());
            }

            // attachments (TODO: open this further?)
            else if (contentType == "image/jpeg" || contentType == "image/gif" || contentType == "image/png" || contentType == "image/bmp"
                    || contentType == "video/3gp" || contentType == "video/mp4" || contentType == "video/3gpp"
                    || contentType == "audio/3gp" || contentType == "audio/mp4" || contentType == "audio/m4a"
                    || contentType == "audio/3gpp") {
                saveAttachment(fileItem, attachmentPaths);
            }
        }

        // now that we've processed all of the data, insert the contents into our database
        // and construct the HTTP response
        String imei = context.getHandsetId();
        int httpResponseCode = storeSurveySubmission(surveyResponses, attachmentPaths, imei, totalSize, farmerId);
        response.setStatus(httpResponseCode);
    }

    public static String getFarmerId(String fileName) {
        // Extract the farmerId from fileName
        String farmerId = "";
        String regex = "(.*)\\_\\[(.*)\\]\\_[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}\\_[0-9]{2}\\-[0-9]{2}\\-[0-9]{2}\\.xml$";
        Pattern pattern = Pattern.compile(regex); 
        Matcher matcher = pattern.matcher(fileName);
        if(matcher.matches()) {
            farmerId = matcher.group(2);
        }
        return farmerId;
    }

    /**
     * @param fileName 
     * 
     */
    public static int storeSurveySubmission(HashMap<String, SubmissionAnswer> surveyResponses,
                                            HashMap<String, String> attachmentReferences, String handsetId, long submissionSize, String intervieweeName)
            throws NoSuchAlgorithmException, InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException,
            ClassNotFoundException, SQLException {

        // The <name>:0 notation for the key is used here as these are special case answers
        // that cannot be duplicated so will always have the instance number of 0
        int backendSurveyId = Integer.parseInt(surveyResponses.get("survey_id:0").getAnswerText(attachmentReferences));
        if (!SurveyDatabaseHelpers.verifySurveyID(backendSurveyId)) {
            return HttpServletResponse.SC_NOT_FOUND;
        }
        
        // The following permanent fields should not be included in creating a hex string
        String handsetSubmissionTimestamp = surveyResponses.remove("handset_submit_time:0").getAnswerText(attachmentReferences);

        // create hex string
        String hashSource = "";
        for (SubmissionAnswer responseValue : surveyResponses.values()) {
            hashSource += responseValue.getAnswerText(attachmentReferences);
        }
        String duplicateDetectionHash = HashHelpers.createSHA1(hashSource);

        // Check that the location has been added to the survey
        String location = "";
        if (surveyResponses.containsKey("location:0")) {
            location = surveyResponses.remove("location:0").getAnswerText(attachmentReferences);
        }

        // Check if this is a legacy form and the farmerId is within the form.
        // If it's there and we do not have a farmerId, we use that instead
        if (surveyResponses.containsKey("interviewee_name:0")) {
            String legacyIntervieweeName = surveyResponses.remove("interviewee_name:0").getAnswerText(attachmentReferences);
            if(intervieweeName.isEmpty()) {
                intervieweeName = legacyIntervieweeName;
            }
        }

        // extract the permanent fields
        SurveysSalesforceProxy salesforceProxy = new SurveysSalesforceProxy();
        CommunityKnowledgeWorker interviewer = CommunityKnowledgeWorker.load(handsetId);
        salesforceProxy.dispose();

        // Lastly, we need to remove the survey id, since we're storing that explicitly already
        surveyResponses.remove("survey_id:0");

        boolean hasAnswers = false;
        ArrayList<String> answerColumns = new ArrayList<String>();
        for (String answerKey : surveyResponses.keySet()) {
            // the question name is used as our column names for survey answers
            SubmissionAnswer answer = surveyResponses.get(answerKey);
            String answerColumn = answer.getQuestionName();

            // verify that these questions have been created
            if (SurveyDatabaseHelpers.verifySurveyField(answerColumn, backendSurveyId)) {
                answerColumns.add(answerKey);
                hasAnswers = true;
            }
        }

        // make sure we have valid questions
        if (hasAnswers) {
            StringBuilder commandText = new StringBuilder();
            commandText.append("insert into zebrasurveysubmissions ");
            commandText.append("(survey_id, server_entry_time, handset_submit_time, handset_id, interviewer_id, ");
            commandText.append("interviewer_name, result_hash, submission_size");
            commandText.append(", mobile_number");
            commandText.append(", location");
            commandText.append(", interviewee_name");
            commandText.append(") values (");
            commandText.append(backendSurveyId);
            commandText.append(",'" + DatabaseHelpers.formatDateTime(new Date()) + "'");
            commandText.append(",'" + handsetSubmissionTimestamp + "'");
            commandText.append(",'" + handsetId + "'");
            commandText.append(",'" + interviewer.getCkwSalesforceName() + "'");
            commandText.append(",'" + interviewer.getFullName() + "'");
            commandText.append(",'" + duplicateDetectionHash + "'");
            commandText.append("," + submissionSize);
            commandText.append(", '" + interviewer.getMobileNumber() + "'");
            commandText.append(", '" + location + "'");
            commandText.append(", '" + intervieweeName + "'");
            commandText.append(")");

            Connection connection = DatabaseHelpers.createConnection(DatabaseId.Surveys);
            connection.setAutoCommit(false);

            // Add the submission.
            Statement statementSubmission = connection.createStatement();

            try {
                statementSubmission.execute(commandText.toString(), Statement.RETURN_GENERATED_KEYS);
            }
            catch (SQLException e) {
                statementSubmission.close();
                connection.close();
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
            ResultSet primaryKey = statementSubmission.getGeneratedKeys();
            primaryKey.next();
            int submissionId = Integer.parseInt(primaryKey.getString(1));
            primaryKey.close();

            // Generate a batch of SQL statements for the answers
            Statement answerStatements = connection.createStatement();
            String startString = "insert into answers (submission_id, question_number, question_name, answer, parent, parent_position, position) values (";
            for (int i = 0; i < answerColumns.size(); i++) {

                SubmissionAnswer answer = surveyResponses.get(answerColumns.get(i));

                StringBuilder answerCommand = new StringBuilder();
                answerCommand.append(startString);

                // Get the question name and instance number
                String questionName = answer.getQuestionName();

                // Trim of the q to allow us to organise the answers in numerical order
                String questionNumber = questionName.substring(1, questionName.length());
                answerCommand.append(submissionId + ", " + questionNumber + ", '" + questionName + "', ");
                answerCommand.append("'" + answer.getAnswerText(attachmentReferences) + "'");

                // Check to see if there is a parent
                if (answer.hasParent()) {
                    answerCommand.append(", '" + answer.getParent().getQuestionName() + "', " + answer.getParent().getInstance());
                }
                else {
                    answerCommand.append(", null, null");
                }
                answerCommand.append(", " + answer.getInstance() + ")");
                answerStatements.addBatch(answerCommand.toString());

            }

            // Try to execute the batch
            try {
                answerStatements.executeBatch();
            }
            catch (Exception e) {
                e.printStackTrace();
                connection.rollback();
                statementSubmission.close();
                answerStatements.close();
                connection.close();
                return HttpServletResponse.SC_BAD_REQUEST;
            }
            connection.commit();
            connection.close();
            statementSubmission.close();
            answerStatements.close();

            return HttpServletResponse.SC_CREATED;
        }
        else {
            return HttpServletResponse.SC_BAD_REQUEST;
        }
    }

    private void saveAttachment(FileItem attachment, HashMap<String, String> attachmentReferences) throws Exception {
        String attachmentReference = createAttachmentReference(attachment.getContentType());

        String fullPath = this.getServletContext().getRealPath(attachmentReference);
        File targetFile = new File(fullPath);

        // create the parent directories if necessary
        File parentDirectory = targetFile.getParentFile();
        if (parentDirectory != null) {
            parentDirectory.mkdirs();
        }
        attachment.write(targetFile);

        // store the full public path here, not the relative one. Need to remove the leading '/' from the path reference
        String publicAttachmentReference = ApplabConfiguration.getHostUrl() + attachmentReference.substring(1);
        attachmentReferences.put(attachment.getName(), publicAttachmentReference);
    }

    // given a content type, generate a relative file reference to the generated name
    // public so that we can test this functionality
    public static String createAttachmentReference(String contentType) {
        // grab everything after the forward slash and convert to file extension (e.g. image/gif -> .gif)
        // use everything before the forward slash and use it for our directory (e.g. image/gif -> survey_images)
        int separatorIndex = contentType.lastIndexOf("/");
        assert (separatorIndex > 0 && separatorIndex < contentType.length()) : "callers should only pass valid content types";

        String directoryName = "/survey_" + contentType.substring(0, separatorIndex) + "s/";
        String fileExtension = "." + contentType.substring(separatorIndex + 1);
        // for certain extensions, we can make substitutions here if it proves necessary (e.g. jpeg->jpg)

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
     */
    private static HashMap<String, SubmissionAnswer> parseSurveySubmission(Document xmlDocument) {
        // normalize the root node
        Element rootNode = xmlDocument.getDocumentElement();
        rootNode.normalize();

        HashMap<String, SubmissionAnswer> parsedSubmission = new HashMap<String, SubmissionAnswer>();
        HashMap<String, Integer> instanceRecord = new HashMap<String, Integer>();
        // now parse the tree and populate surveyResponses with the raw data
        for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                parseSurveySubmissionElement((Element)childNode, parsedSubmission, null, instanceRecord);
            }
            else {
                // don't care about other types of nodes
            }
        }

        return parsedSubmission;
    }

    /**
     * given an XML node, processes the contents into a SurveyItemResponse
     */
    private static void parseSurveySubmissionElement(Element submissionElement, HashMap<String, SubmissionAnswer> existingSubmission,
                                                     SubmissionAnswer parentItem, HashMap<String, Integer> instanceRecord) {
        // question name is always the name of the start element
        String questionName = submissionElement.getNodeName();

        int newInstance = 0;
        if (instanceRecord.containsKey(questionName)) {
            newInstance = instanceRecord.get(questionName) + 1;
        }

        // Create a new answer and increase the instance count for this question name
        SubmissionAnswer submissionAnswer = new SubmissionAnswer(questionName, newInstance, null, parentItem);
        String answerKey = submissionAnswer.getKey();
        existingSubmission.put(answerKey, submissionAnswer);
        instanceRecord.put(questionName, newInstance);

        // walk the child nodes, and either populate with a text-answer, or recurse for the multiple-answer case
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
