package applab.surveys.server;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

import org.xml.sax.SAXException;

import applab.server.ApplabServlet;
import applab.server.DatabaseHelpers;
import applab.server.ServletRequestContext;
import applab.surveys.SubmissionStatistics;
import applab.surveys.SubmissionStatus;

/**
 * Display an administrative page that displays the submissions for a particular survey, including overall statistics on
 * how many submissions are in each stage of review.
 * 
 */
public class ReviewSubmissions extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public void doApplabGet(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws ClassNotFoundException, SQLException,
            IOException, ServiceException, ParseException, ServletException, SAXException, ParserConfigurationException {
        getSubmissions(request, response, context);
    }
    
    @Override
    public void doApplabPost(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws ClassNotFoundException, SQLException,
            IOException, ServiceException, ParseException, ServletException, SAXException, ParserConfigurationException {
        getSubmissions(request, response, context);
    }
    
    private void getSubmissions(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws ClassNotFoundException, SQLException,
            IOException, ServiceException, ParseException, ServletException, SAXException, ParserConfigurationException {

        // Can create the session here as it is the access point of the session (No login at the moment)
        HttpSession session = request.getSession(true);
        if (session == null) {
//TODO - Make a generic error page.
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "FAIL!!!");
        }
        else {

            // Extract the request data
            String salesforceSurveyId = request.getParameter("surveyId");

            boolean showDraft = false;
            if ("on".equals(request.getParameter("showDraft"))) {
                showDraft = true;
            }
            
            // Extract the optional parameters
            java.sql.Date startDate = null;
            java.sql.Date endDate = null;
            try {
                startDate =  DatabaseHelpers.getSqlDateFromString(request.getParameter("startDate"), 0);
            }
            catch (Exception e) {

                // Do nothing and leave the dates blank
            }
            try {
                endDate   = DatabaseHelpers.getSqlDateFromString(request.getParameter("endDate"), 0);
            }
            catch (Exception e) {

                // Do nothing and leave the dates blank
            }
           
            java.util.Date now = new java.util.Date();
            // If we only have one of the dates provided then set the other one to now
            if (startDate != null && endDate == null) {
                endDate = new java.sql.Date(now.getTime());    
            }
            
            if (startDate == null && endDate != null) {
                startDate = new java.sql.Date(now.getTime());    
            }
            
            String statusParameter = request.getParameter("status") == null && request.getParameter("status") == "null" ? null: request.getParameter("status");

            SubmissionStatistics statistics = SubmissionStatistics.getStatistics(salesforceSurveyId, startDate, endDate);
            SubmissionStatus statusFilter = null;
            if (statistics != null) {
        
                // We have some statistics so lets load the submission
                statusFilter = SubmissionStatus.parseHtmlParameter(statusParameter);
                statistics.getSurvey().loadSubmissions(statusFilter, startDate, endDate, true, salesforceSurveyId, showDraft);

                // Bind the submission object
                session.setAttribute("survey.statistics", statistics);
                session.setAttribute("survey.surveyId", String.valueOf(statistics.getSurvey().getPrimaryKey()));
            }

            session.setAttribute("survey.salesforceId", salesforceSurveyId);
            if (startDate != null) {
                session.setAttribute("survey.startDate", startDate.toString() + " 00:00:00");
            }
            else {
                session.setAttribute("survey.startDate", "Enter Date");
            }
            if (endDate != null) {
                session.setAttribute("survey.endDate", endDate.toString() + " 23:59:59");
            }
            else {
                session.setAttribute("survey.endDate", "Enter Date");
            }
            if (statusFilter != null) {
                session.setAttribute("survey.status", statusFilter.getHtmlParameterValue());
            }
            else {
               session.setAttribute("survey.status", "none");
            }
            
            session.setAttribute("survey.showDraft", showDraft);
                
            // Play the jsp page to display the details
            String url = "/jsp/ReviewSubmissions.jsp";
            ServletContext sc = getServletContext();
            RequestDispatcher rd = sc.getRequestDispatcher(url);
            rd.forward(request, response);
        }
    }
}
