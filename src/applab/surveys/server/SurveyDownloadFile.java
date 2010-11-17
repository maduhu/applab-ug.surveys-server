package applab.surveys.server;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

import org.xml.sax.SAXException;

import applab.server.ApplabServlet;
import applab.server.DatabaseHelpers;
import applab.server.FileHelpers;
import applab.server.ServletRequestContext;
import applab.surveys.SubmissionStatistics;
import applab.surveys.SubmissionStatus;

public class SurveyDownloadFile extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public void doApplabPost(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws ClassNotFoundException, SQLException,
            IOException, ServiceException, ParseException, ServletException, SAXException, ParserConfigurationException {
       
        DownloadType downloadType = DownloadType.valueOf(request.getParameter("downloadType"));
        DownloadTarget downloadTarget = DownloadTarget.valueOf(request.getParameter("downloadTarget"));
        
        switch (downloadType) {
        case Csv:
            response.setContentType("text/csv");
            break;
        default:
            response.setContentType("text/html");
        }

        String filePathName     = "";
        String fileDownloadName = "";
        String outputString = null;

        switch (downloadTarget) {
            case SubmissionCsv:

                // Extract the request data
                String salesforceSurveyId = request.getParameter("surveySalesforceId");

                // Extract the optional parameters
                java.sql.Date startDate = null;
                java.sql.Date endDate = null;
                String startDateDescription = "NoStartDate";
                String endDateDescription = "NoEndDate";
                String statusDescription = "NoStatus";
                boolean showDraft = false;
                if ("on".equals(request.getParameter("showDraft"))) {
                    showDraft = true;
                }
                try {
                    startDate =  DatabaseHelpers.getSqlDateFromString(request.getParameter("startDate"), 0);
                    startDateDescription = startDate.toString();
                }
                catch (Exception e) {

                    // Do nothing and leave the dates blank
                }
                try {
                    endDate   = DatabaseHelpers.getSqlDateFromString(request.getParameter("endDate"), 0);
                    endDateDescription = endDate.toString();
                }
                catch (Exception e) {

                    // Do nothing and leave the dates blank
                }
           
                java.util.Date now = new java.util.Date();
                
                // If we only have one of the dates provided then set the other one to now
                if (startDate != null && endDate == null) {
                    endDate = new java.sql.Date(now.getTime());    
                    endDateDescription = endDate.toString();
                }
            
                if (startDate == null && endDate != null) {
                    startDate = new java.sql.Date(now.getTime());    
                    startDateDescription = startDate.toString();
                }

                SubmissionStatus status = request.getParameter("status") == null && request.getParameter("status") == "null" ? null: SubmissionStatus.parseHtmlParameter(request.getParameter("status"));
                if (status != null) {
                    statusDescription =  status.toString();
                }
                SubmissionStatistics statistics = SubmissionStatistics.getStatistics(salesforceSurveyId, startDate, endDate);

                if (statistics == null) {
// TODO - ErrorPage                    
                }
                else {

                    // We have some statistics so lets load the submission
                    statistics.getSurvey().loadSubmissions(status, startDate, endDate, false, salesforceSurveyId, showDraft);
                    outputString = statistics.getSurvey().generateCsv();
                    fileDownloadName = "SubmissionCSV-" + startDateDescription + "-" + endDateDescription + "-" + statusDescription + ".csv";
                }
                break;
            default:
                break;
                // TODO - Error
        }
        
        // Set the content-disposition
        response.setHeader("Content-Disposition", "attachment; filename=" + fileDownloadName);


        // Check to see if this is a file or a string
        if (outputString == null) {
            File file = new File(filePathName);

            // Read the file into a byte array
            byte[] inputData = FileHelpers.readFile(file);

            // Set up the reader and the output stream
            ServletOutputStream output = response.getOutputStream();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputData);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(output);

            // Set the required header
            int length = inputData.length;
            response.setContentLength(length);

            byte buffer[] = new byte[(1024 *1024) * 2];
            int bytesRead;
            while (-1 != (bytesRead = byteArrayInputStream.read(buffer, 0, buffer.length))) {
                    bufferedOutputStream.write(buffer, 0, bytesRead);
            }
            output.flush();
            output.close();
            byteArrayInputStream.close();
            bufferedOutputStream.close();
        }
        else {
            
            // The output is a string so just write it out
            response.setContentLength(outputString.length());
            response.getWriter().write(outputString);
        }
    }
    
    private enum DownloadType {
        Csv,
        Pdf;
    }
    
    private enum DownloadTarget {
        SubmissionCsv;
    }
}
