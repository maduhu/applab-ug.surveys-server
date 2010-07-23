import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.*;

import configuration.applabConfig;

import java.util.*;
import java.util.Map.Entry;

/**
 * Server method that receives a survey submission and inserts the data into our database
 * 
 * Input: survey submission as XML (provided in POST body)
 * 
 * Output: Empty body, status code distinguishes success from failure
 * 
 */
public class ProcessSubmission extends HttpServlet {

    private static final long serialVersionUID = 1L;
    final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        // pessimistic default
        int httpStatusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        response.setHeader("Location", request.getRequestURI());
        try {
            // we are only expecting multi-part content
            if (!ServletFileUpload.isMultipartContent(request)) {
                httpStatusCode = HttpServletResponse.SC_BAD_REQUEST;
                return;
            }
            ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());
            List<?> fileList = servletFileUpload.parseRequest(request);
            HashMap<String, SurveyItemResponse> surveyResponses = null;

            // store the paths to the attachments in attachment path
            HashMap<String, String> attachmentPaths = new HashMap<String, String>();
            Iterator<?> it = fileList.iterator();
            while (it.hasNext()) {
                FileItem fileItem = (FileItem)it.next();
                String contentType = fileItem.getContentType().toLowerCase(Locale.ENGLISH);

                // survey answer content
                if (contentType == "text/xml") {
                    File temp = File.createTempFile("xmlFile", ".xml");
                    fileItem.write(temp);
                    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document xmlDocument = builder.parse(temp);
                    surveyResponses = parseSurveySubmission(xmlDocument);
                }
                // attachments (TODO: open this further?)
                else if (contentType == "image/jpeg" || contentType == "image/gif" || contentType == "image/png"
                        || contentType == "image/bmp" || contentType == "video/3gp" || contentType == "video/mp4"
                        || contentType == "video/3gpp" || contentType == "audio/3gp" || contentType == "audio/mp4"
                        || contentType == "audio/m4a" || contentType == "audio/3gpp") {
                    saveAttachment(fileItem, attachmentPaths);
                }
            }

            // now that we've processed all of the data, insert the contents into our database
            // and construct the HTTP response
            httpStatusCode = storeSurveySubmission(surveyResponses, attachmentPaths);
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        finally {
            response.setStatus(httpStatusCode);
        }
    }

    private int storeSurveySubmission(HashMap<String, SurveyItemResponse> surveyResponses, HashMap<String, String> attachmentReferences) throws Exception {
        int surveyId = Integer.parseInt(surveyResponses.get("survey_id").getEncodedAnswer(attachmentReferences));
        if (configuration.DbConnect.verifySurveyID(surveyId)) {
            // The following permanent fields should not be included in creating a hex string
            String handsetSubmissionTimestamp = surveyResponses.remove("handset_submit_time").getEncodedAnswer(attachmentReferences);
            String surveyLocation = surveyResponses.get("location").getEncodedAnswer(attachmentReferences);

            // create hex string
            String hashSource = "";
            for (SurveyItemResponse responseValue : surveyResponses.values()) {
                hashSource += responseValue.getEncodedAnswer(attachmentReferences);
            }
            String duplicateDetectionHash = configuration.md5.getMD5Hash(hashSource);

            // extract the permanent fields
            // TODO: in 2.8 we'll be passing the IMEI as an HTTP header
            String handsetId = surveyResponses.remove("handset_id").getEncodedAnswer(attachmentReferences);

            // even though the "question name" is interviewer_id, that was a typo and it actually stores the
            // interviewee's name
            String intervieweeName = surveyResponses.remove("interviewer_id").getEncodedAnswer(attachmentReferences);
            surveyResponses.remove("survey_id");

            String answerColumnsCommandText = "";
            String answerValueCommandText = "";
            for (Entry<String, SurveyItemResponse> surveyAnswer : surveyResponses.entrySet()) {
                // the question name is used as our column names for survey answers
                String answerColumn = surveyAnswer.getKey();
                // verify that these questions have been created
                if (configuration.DbConnect.verifySurveyField(answerColumn, surveyId)) {

                    answerColumnsCommandText += "," + surveyAnswer.getKey();
                    answerValueCommandText += ",'" + surveyAnswer.getValue().getEncodedAnswer(attachmentReferences) + "'";
                }
            }

            // make sure we have valid questions
            if (answerColumnsCommandText.length() > 0) {
                String currentTime = applabConfig.getDateTime();
                StringBuilder commandText = new StringBuilder();
                commandText.append("insert into zebrasurveysubmissions ");
                commandText.append("survey_id,server_entry_time,handset_submit_time,handset_id,interviewee_name,result_hash,location");
                commandText.append(answerColumnsCommandText);
                commandText.append(") values (");
                commandText.append(surveyId);
                commandText.append(",'" + currentTime + "'");
                commandText.append(",'" + handsetSubmissionTimestamp + "'");
                commandText.append(",'" + handsetId + "'");
                commandText.append(",'" + intervieweeName + "'");
                commandText.append(",'" + duplicateDetectionHash + "'");
                commandText.append(",'" + surveyLocation + "'");
                commandText.append(answerValueCommandText + ")");
                if (configuration.DbConnect.postSubmission(commandText.toString())) {
                    return HttpServletResponse.SC_CREATED;
                }
                else {
                    return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                }
            }
        }

        return HttpServletResponse.SC_NOT_FOUND;
    }

    private void saveAttachment(FileItem attachment, HashMap<String, String> attachmentReferences) throws Exception {
        // grab everything after the forward slash and convert to file extension (e.g. image/gif -> .gif)
        // use everything before the forward slash and use it for our directory (e.g. image/gif -> survey_images)
        String contentType = attachment.getContentType();
        
        int separatorIndex = contentType.lastIndexOf("/");
        assert (separatorIndex > 0 && separatorIndex < contentType.length()) : "callers should only pass valid content types";

        String directoryName = "/survey_" + contentType.substring(0, separatorIndex) + "s/";
        String fileExtension = "." + contentType.substring(separatorIndex + 1);
        // for certain extensions, we can make substitutions here if it proves necessary (e.g. jpeg->jpg)

        String attachmentReference = directoryName + new configuration.images().generateImageName() + fileExtension;

        String fullPath = this.getServletContext().getRealPath(attachmentReference);
        attachment.write(new File(fullPath));
        // TODO: we should store the full public path here, not the relative one. See
        // what the above full path gets you
        attachmentReferences.put(attachment.getName(), attachmentReference);
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
            switch (childNode.getNodeType()) {
                case Node.ELEMENT_NODE:
                    parseSurveySubmissionElement((Element)childNode, parsedSubmission, null);
                    break;

                default:
                    // don't care about other types of nodes
                    break;
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

    /**
     * in memory representation of a survey item response. Usually this just contains a text answer, but in the case of
     * repeat questions, it can contain a collection of responses as the answer.
     * 
     * Single answer example:
     * 
     * <q1>My answer</q1>
     * 
     * Repeat answer example:
     * 
     * <q3> <q4>Answer 4</q4><q5>Answer 5</q5> </q3> <q3> <q4>Answer 4 #2</q4><q5>Answer 5 #2</q5> </q3>
     */
    private static class SurveyItemResponse {
        private String questionName;

        // we use two fields here for performance, so that we don't need to allocate an array
        // for simple text answers
        private String singletonAnswer;
        private ArrayList<String> multipleAnswers;

        // will be non-null if this is the child of a repeat question
        private SurveyAnswerGroup parent;

        public SurveyItemResponse(String questionName, SurveyAnswerGroup parent) {
            if (questionName == null) {
                questionName = "";
            }
            this.questionName = questionName;
            this.parent = parent;

            if (this.parent != null) {
                this.parent.addChild(this);
            }
        }

        public String getQuestionName() {
            return this.questionName;
        }

        public void addAnswerText(String answerText) {
            if (answerText != null) {
                // see if we need to promote to a list
                if (this.singletonAnswer != null) {
                    // we should only get to this case when we have a valid parent group
                    assert (this.parent != null) : "we should only get multiple answers when contained in a parent group";

                    this.multipleAnswers = new ArrayList<String>();
                    this.multipleAnswers.add(this.singletonAnswer);
                    this.singletonAnswer = null;
                }

                if (this.multipleAnswers != null) {
                    this.multipleAnswers.add(answerText);
                }
                else {
                    this.singletonAnswer = answerText;
                }
            }
        }

        /**
         * The encoding for a single answer is simply the text.
         * 
         * The encoding for multiple answers is, for example. [child:q6][1]answer\n[2]answer
         */
        public String getEncodedAnswer(HashMap<String, String> attachments) {
            StringBuilder encodedAnswer = new StringBuilder();
            if (this.parent != null) {
                encodedAnswer.append("[child:");
                encodedAnswer.append(this.parent.getQuestionName());
                encodedAnswer.append("]");
            }
            if (this.multipleAnswers != null) {
                int prefix = 1;
                for (String answerText : this.multipleAnswers) {
                    if (prefix > 1) {
                        encodedAnswer.append("\n");
                    }
                    encodedAnswer.append("[");
                    encodedAnswer.append(prefix);
                    encodedAnswer.append("]");
                    encodedAnswer.append(resolveAnswerText(answerText, attachments));
                    prefix++;
                }
            }
            else if (this.singletonAnswer != null) {
                encodedAnswer.append(resolveAnswerText(this.singletonAnswer, attachments));
            }

            return encodedAnswer.toString();
        }

        /**
         * Helper function to turn an attachment reference into the correct path if necessary
         */
        private final String resolveAnswerText(String answerText, HashMap<String, String> attachments) {
            String resolvedAnswer = answerText;
            // see if the element is referencing an attachment
            if (attachments != null) {
                String attachmentPath = attachments.get(resolvedAnswer);
                if (attachmentPath != null) {
                    resolvedAnswer = attachmentPath;
                }
            }
            return resolvedAnswer;
        }
    }

    /**
     * used for the case of multiple-response groups, this does not have any user-provided text, but has child
     * responses.
     */
    private static class SurveyAnswerGroup extends SurveyItemResponse {
        HashMap<String, SurveyItemResponse> childResponses;

        public SurveyAnswerGroup(String questionName, SurveyAnswerGroup parent) {
            super(questionName, parent);
            this.childResponses = new HashMap<String, SurveyItemResponse>();
        }

        public void addChild(SurveyItemResponse child) {
            this.childResponses.put(child.getQuestionName(), child);
        }

        /**
         * The encoding for a submission group is a string like: 3 responses (q8, q9, q10)
         */
        @Override
        public String getEncodedAnswer(HashMap<String, String> attachments) {
            StringBuilder encodedAnswer = new StringBuilder();
            encodedAnswer.append(childResponses.size());
            encodedAnswer.append(" response");
            if (childResponses.size() != 1) {
                encodedAnswer.append("s");
            }
            encodedAnswer.append(" (");
            boolean firstChild = true;
            for (String childQuestionName : this.childResponses.keySet()) {
                if (firstChild) {
                    firstChild = false;
                }
                else {
                    encodedAnswer.append(", ");
                }
                encodedAnswer.append(childQuestionName);
            }
            encodedAnswer.append(")");
            return encodedAnswer.toString();
        }

        @Override
        public void addAnswerText(String answerText) {
            assert false : "We should never get answer text for a submission group";
        }
    }
}
