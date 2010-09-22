package applab.surveys.server;

import java.io.File;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.rpc.ServiceException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import applab.CommunityKnowledgeWorker;
import applab.server.ApplabConfiguration;
import applab.server.ApplabServlet;
import applab.server.DatabaseHelpers;
import applab.server.DatabaseId;
import applab.server.HashHelpers;
import applab.server.ServletRequestContext;
import applab.server.XmlHelpers;
import applab.surveys.SurveyAnswerGroup;
import applab.surveys.SurveyItemResponse;

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

        HashMap<String, SurveyItemResponse> surveyResponses = null;

        // store the paths to the attachments in attachment path
        HashMap<String, String> attachmentPaths = new HashMap<String, String>();
        Iterator<?> fileIterator = fileList.iterator();

        long totalSize = 0;
        while (fileIterator.hasNext()) {
            FileItem fileItem = (FileItem)fileIterator.next();
            totalSize = totalSize + fileItem.getSize();

            String contentType = fileItem.getContentType().toLowerCase(Locale.ENGLISH);
            contentType = contentType.intern(); // so that == works for comparison

            // survey answer content
            if (contentType == "text/xml") {
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
        
        // add the size as a response item to the responses, so that it gets saved to the db
        SurveyItemResponse submissionSize = new SurveyItemResponse("submission_size",null);
        String size = String.valueOf(totalSize);
        submissionSize.addAnswerText(size);
        surveyResponses.put("submission_size", submissionSize);
 
        // now that we've processed all of the data, insert the contents into our database
        // and construct the HTTP response
        int httpResponseCode = storeSurveySubmission(surveyResponses, attachmentPaths);
        response.setStatus(httpResponseCode);
    }

    public static int storeSurveySubmission(HashMap<String, SurveyItemResponse> surveyResponses, HashMap<String, String> attachmentReferences)
            throws NoSuchAlgorithmException, InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException,
            ClassNotFoundException, SQLException {
        int backendSurveyId = Integer.parseInt(surveyResponses.get("survey_id").getEncodedAnswer(attachmentReferences));
        if (!SurveyDatabaseHelpers.verifySurveyID(backendSurveyId)) {
            return HttpServletResponse.SC_NOT_FOUND;
        }
        // The following permanent fields should not be included in creating a hex string
        String handsetSubmissionTimestamp = surveyResponses.remove("handset_submit_time").getEncodedAnswer(attachmentReferences);
        String surveyLocation = surveyResponses.get("location").getEncodedAnswer(attachmentReferences);

        // create hex string
        String hashSource = "";
        for (SurveyItemResponse responseValue : surveyResponses.values()) {
            hashSource += responseValue.getEncodedAnswer(attachmentReferences);
        }
        String duplicateDetectionHash = HashHelpers.createMD5(hashSource);
        
        // extract the permanent fields
        // TODO: in 2.8 we'll be passing the IMEI as an HTTP header
        String handsetId = surveyResponses.remove("handset_id").getEncodedAnswer(attachmentReferences);
        SurveysSalesforceProxy salesforceProxy = new SurveysSalesforceProxy();
        CommunityKnowledgeWorker interviewer = CommunityKnowledgeWorker.load(handsetId);
        salesforceProxy.dispose();

        // even though the question name is interviewer_id, that is a typo and it actually stores the
        // interviewee's name
        String intervieweeName = surveyResponses.remove("interviewer_id").getEncodedAnswer(attachmentReferences);

        int submissionSize = Integer.parseInt(surveyResponses.remove("submission_size").getEncodedAnswer(attachmentReferences));

        // Lastly, we need to remove the survey id, since we're storing that explicitly already
        surveyResponses.remove("survey_id");
        surveyResponses.remove("location");

        String answerColumnsCommandText = "";
        String answerValueCommandText = "";
        for (Entry<String, SurveyItemResponse> surveyAnswer : surveyResponses.entrySet()) {
            // the question name is used as our column names for survey answers
            String answerColumn = surveyAnswer.getKey();
            // verify that these questions have been created
            if (SurveyDatabaseHelpers.verifySurveyField(answerColumn, backendSurveyId)) {
                answerColumnsCommandText += "," + answerColumn;
                answerValueCommandText += ",'" + surveyAnswer.getValue().getEncodedAnswer(attachmentReferences) + "'";
            }
        }

        // make sure we have valid questions
        if (answerColumnsCommandText.length() > 0) {
            StringBuilder commandText = new StringBuilder();
            commandText.append("insert into zebrasurveysubmissions ");
            commandText.append("(survey_id, server_entry_time, handset_submit_time, handset_id, interviewer_id, ");
            commandText.append("interviewer_name, interviewee_name, result_hash, location, submission_size");
            commandText.append(answerColumnsCommandText);
            commandText.append(") values (");
            commandText.append(backendSurveyId);
            commandText.append(",'" + DatabaseHelpers.formatDateTime(new Date()) + "'");
            commandText.append(",'" + handsetSubmissionTimestamp + "'");
            commandText.append(",'" + handsetId + "'");
            commandText.append(",'" + interviewer.getCkwSalesforceName() + "'");
            commandText.append(",'" + interviewer.getFullName() + "'");
            commandText.append(",'" + intervieweeName + "'");
            commandText.append(",'" + duplicateDetectionHash + "'");
            commandText.append(",'" + surveyLocation + "'");
            commandText.append("," + submissionSize);
            commandText.append(answerValueCommandText + ")");

            Connection connection = DatabaseHelpers.createConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();

            // Catch this to see if there is a duplicate hash 
            try {
                statement.executeUpdate(commandText.toString());
            }
            catch (SQLException e) {
                if (e.getErrorCode() == 1062) {

                    // The error is a duplicate key error so allow to be told as good
                    return HttpServletResponse.SC_CREATED;
                }
                else {
                    throw new SQLException(e.getMessage());
                }
            }
            finally {
                statement.close();
                connection.close();
            }
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

    /**
     * parses the XML for a survey response. Delegates most of its work to parseSurveySubmissionElement
     * 
     * @param xmlDocument
     *            DOM containing the submission XML
     */
    private static HashMap<String, SurveyItemResponse> parseSurveySubmission(Document xmlDocument) {
        // normalize the root node
        Element rootNode = xmlDocument.getDocumentElement();
        rootNode.normalize();
        
        HashMap<String, SurveyItemResponse> parsedSubmission = new HashMap<String, SurveyItemResponse>();

        // now parse the tree and populate surveyResponses with the raw data
        for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                parseSurveySubmissionElement((Element)childNode, parsedSubmission, null);
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
    private static void parseSurveySubmissionElement(Element submissionElement, HashMap<String, SurveyItemResponse> existingSubmission,
            SurveyAnswerGroup parentItem) {
        // question name is always the name of the start element
        String questionName = submissionElement.getNodeName();

        SurveyItemResponse surveyItemResponse = existingSubmission.get(questionName);
        if (surveyItemResponse == null) {
            surveyItemResponse = new SurveyItemResponse(questionName, parentItem);
            existingSubmission.put(questionName, surveyItemResponse);
        }

        // walk the child nodes, and either populate with a text-answer, or recurse for the multiple-answer case
        for (Node childNode = submissionElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            switch (childNode.getNodeType()) {
                case Node.ELEMENT_NODE:
                    if (!(surveyItemResponse instanceof SurveyAnswerGroup)) {
                        surveyItemResponse = new SurveyAnswerGroup(questionName, parentItem);
                        existingSubmission.put(questionName, surveyItemResponse);
                    }
                    parseSurveySubmissionElement((Element)childNode, existingSubmission, (SurveyAnswerGroup)surveyItemResponse);
                    break;

                case Node.TEXT_NODE:
                    surveyItemResponse.addAnswerText(childNode.getNodeValue());
                    break;

                default:
                    // don't care about other types of nodes
                    break;
            }
        }
    }

}
