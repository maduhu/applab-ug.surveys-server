package applab.surveys.server;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.w3c.dom.Document;

import applab.server.ApplabServlet;
import applab.server.ServletRequestContext;
import applab.server.XmlHelpers;
import applab.surveys.ProcessedSubmission;

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
    private static final String RESPONSE = "message";

    @Override
    protected void doApplabPost(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {

        response.setContentType("text/html");
        response.setHeader("Location", request.getRequestURI());

        // We are only expecting multi-part content
        if (!ServletFileUpload.isMultipartContent(request)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "request must be MIME encoded");
            return;
        }
        ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());
        List<?> fileList = servletFileUpload.parseRequest(request);

        // Create the new submission object
        ProcessedSubmission submission = new ProcessedSubmission(
                context.getHandsetId(),
                request.getHeader("x-applab-interviewee-id"),
                request.getHeader("x-applab-survey-location"),
                context.getSubmissionLocation()
        );

        // Store the paths to the attachments in attachment path
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
                submission.checkIntervieweeId(fileItem.getName());

                Document xmlDocument = XmlHelpers.parseXml(fileItem.getString());
                submission.setXml(xmlDocument);
                submission.parseSurveySubmission();

                // Log the XML input
                log(submission.getImei() + " Has submitted from - " + fileItem.getString());
            }

            // Attachments (TODO: open this further? This just needs to kept inline with ODK. Expand this when they do)
            else if (contentType == "image/jpeg" || contentType == "image/gif" || contentType == "image/png" || contentType == "image/bmp"
                    || contentType == "video/3gp" || contentType == "video/mp4" || contentType == "video/3gpp"
                    || contentType == "audio/3gp" || contentType == "audio/mp4" || contentType == "audio/m4a"
                    || contentType == "audio/3gpp") {log(contentType);
                String attachmentReference = submission.createAttachmentReference(fileItem.getContentType());log(fileItem.getContentType());
                submission.saveAttachment(fileItem, context, this.getServletContext().getRealPath(attachmentReference), attachmentReference);log("full path="+context.getFullPath(attachmentReference.substring(1)));
                
                // Check the url of the saved attachment to ensure it's valid, else return an error
        		URL url = new URL(context.getFullPath(attachmentReference.substring(1)));
        		HttpURLConnection   conn = (HttpURLConnection)url.openConnection();
        		if(conn.getResponseCode()!= 200) {
        			log("Attachment Url messed up");
        			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Attachment Url messed up, please resubmit");
        		}
            }
        }
        submission.setSize(totalSize);

        if (!submission.checkSalesforceId()) {
            log("Cannot submit this survey for handset " + submission.getImei() + " because the backend survey id " + submission.getBackendSurveyId()
                + " does not exist on our system");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The survey does not exist");
            return;
        }

        if (!submission.loadSurvey()) {

            // Someone has deleted the form from salesforce. Should never be done. Fail silently so the
            // submission is off the phone. Then find who deleted the form and lightly kill them.
            log("Handset with IMEI: " + submission.getImei() + " submitted survey id " + submission.getSalesforceId()
                    + ". It no longer exists in SF. Find out what happened");
            response.setStatus(HttpServletResponse.SC_CREATED);
            return;
        }

        int httpResponseCode = -1;


        // Validate the survey against the questions. This will remove any answers that are not in the survey.
        String[] validateResponse = submission.validateSubmission();
        if (validateResponse[0].equals("1")) {

            // Check that the submission is not already in salesforce
            if (SurveyDatabaseHelpers.isSubmissionAlreadyInDb(submission)) {
            	log("submission for IMEI " + submission.getImei() + " already exists, will be ignored");
            	httpResponseCode = HttpServletResponse.SC_CREATED;
            	response.setStatus(httpResponseCode);
            	return;
            }
            Boolean stopSaveToBackend = false;

            // Decide where this survey should be saved to
            if ((submission.getSurvey().getSaveToSalesforce() || !submission.getSurvey().getPostProcessingMethod().equalsIgnoreCase("NONE"))
            		&& !submission.getSurvey().isPostProcessingDeferred()) {

                // We may want to pass data to salesforce and then save the actual submission to backend.
                stopSaveToBackend = submission.getSurvey().getStopSaveToBackend();

                // Save to Salesforce
                String[] returnValues = submission.saveToSalesforce();
                httpResponseCode = Integer.valueOf(returnValues[0]);
                response.setHeader(RESPONSE, returnValues[1]);
                log("Handset with IMEI: " + submission.getImei() + " submitted a survey with the following result " + returnValues[1]);
            }

            // Forward the submission to an external server
            if (!submission.getSurvey().getExportUrl().equals("NONE") && (httpResponseCode == -1 || httpResponseCode == 201)) {
                String[] exportResults = submission.exportSubmissionToExternalServer();
                log("Handset with IMEI: " + submission.getImei() + " exported submission to " + submission.getSurvey().getExportUrl() + " " + exportResults[1]);
                httpResponseCode = Integer.parseInt(exportResults[0]);
                log(String.valueOf(httpResponseCode));
            }

            // Save to our backend - TODO - What do we do about a failure here, having already sent the submission to the other client
            if (!stopSaveToBackend && (httpResponseCode == -1 || httpResponseCode == 201)) {
                httpResponseCode = submission.storeSurveySubmission();
            }
        }
        else {
            log("Handset with IMEI: " + submission.getImei() + " failed to validate submission because " + validateResponse[1]);
            httpResponseCode = HttpServletResponse.SC_BAD_REQUEST;
        }
        response.setStatus(httpResponseCode);
    }
}
