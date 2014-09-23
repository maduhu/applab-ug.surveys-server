package applab.surveys.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import applab.server.ApplabServlet;
import applab.server.ServletRequestContext;
import applab.surveys.CustomerCareStatus;
import applab.surveys.DeferredSurveyProcessingXmlGenerator;
import applab.surveys.ProcessedSubmission;
import applab.surveys.SubmissionStatus;
import applab.surveys.Survey;

/**
 * Called by ReviewSubmissions to update the status of survey submissions based on staff review
 * 
 */
public class UpdateSubmissionStatus extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    public void doApplabPost(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws IOException, ClassNotFoundException, SQLException, ServletException, ServiceException, ParserConfigurationException, TransformerException, SAXException {

        HttpSession session = request.getSession(true);
        if (session == null) {
//TODO - Make a generic error page.
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "FAIL!!!");
        }

        int submissionId = Integer.valueOf(request.getParameter("submissionId"));
        SubmissionStatus dataTeamStatus = SubmissionStatus.parseHtmlParameter(request.getParameter("dtStatus"));
        CustomerCareStatus customerCareStatus = CustomerCareStatus.parseHtmlParameter(request.getParameter("ccStatus"));
        
        String customerCareReview = request.getParameter("ccReview") + (request.getParameter("addCcReview") == "" ? "" : "\n" + request.getParameter("addCcReview"));
        String dataTeamReview = request.getParameter("dtReview") + (request.getParameter("addDtReview") == "" ? "" : "\n" + request.getParameter("addDtReview"));
        String salesforceId = request.getParameter("surveyId");
        Survey survey = new Survey(salesforceId);
        survey.loadSurvey(true);
        if (survey.isPostProcessingDeferred()) {

	        // Check if this survey has deferred post processing and the data team status is Approved, was not previously Approved.
	        if (dataTeamStatus.getDisplayName().equalsIgnoreCase("Approved")) {
	        	Connection connection = SurveyDatabaseHelpers.getReaderConnection();
	        	StringBuilder commandText = new StringBuilder();
	            commandText.append("SELECT s.survey_status AS surveyStatus ");
	            commandText.append("FROM zebrasurveysubmissions s  ");
	            commandText.append("WHERE s.id = ? ");
	            PreparedStatement query = connection.prepareStatement(commandText.toString());
	            query.setInt(1, submissionId);
	            ResultSet resultSet = query.executeQuery();
	            resultSet.next();
	            String previousDataTeamReview = resultSet.getString("surveyStatus");
	            if (!previousDataTeamReview.equalsIgnoreCase("Approved")) {
	            	ResultSet submissionResultSet = SurveyDatabaseHelpers.getSubmissionDetails(submissionId);
	            	submissionResultSet.next();

	            	// Do the Post Processing
	                ProcessedSubmission submission = new ProcessedSubmission(
	                        submissionResultSet.getString("handsetId"),
	                        submissionResultSet.getString("interviewerName"),
	                        submissionResultSet.getString("location"),
	                        submissionResultSet.getString("submissionLocation")
	                );
	                submission.setBackendSurveyIdFromRootNode(String.valueOf(survey.getPrimaryKey()));
	                submission.setSize(submissionResultSet.getInt("submissionSize"));
	                submission.setHandsetSubmitTime(submissionResultSet.getDate("handsetTime"));
	                submission.setSubmissionStartTime(submissionResultSet.getDate("surveyStartTime"));
	                submission.setSurvey(survey);
	                String[] returnValues = DeferredSurveyProcessingXmlGenerator.postProcessSurvey(submissionResultSet, survey,  submission);
	                log(submission.getImei() + " submitted a survey with the following result : " + returnValues[1]);
	                if (returnValues[0].equalsIgnoreCase("1")) {
	                	updateSubmissionStatus(submissionId, dataTeamStatus.getDisplayName(), customerCareStatus.getDisplayName(), customerCareReview, dataTeamReview);
	                }
	                else if(returnValues[0].equalsIgnoreCase("0")) {
	                	session.setAttribute("survey.failureMessage", returnValues[1]);
	                }
	            }
	        }
	        else {
	        	updateSubmissionStatus(submissionId, dataTeamStatus.getDisplayName(), customerCareStatus.getDisplayName(), customerCareReview, dataTeamReview);
	        }
        }
        else {
        	updateSubmissionStatus(submissionId, dataTeamStatus.getDisplayName(), customerCareStatus.getDisplayName(), customerCareReview, dataTeamReview);
        }

        // Redirect to the reviewsubmissionServlet.
        String url = "/getSubmissions";
        RequestDispatcher requestDispatcher = request.getRequestDispatcher(url);
        requestDispatcher.forward(request, response);
        
    }

    static void updateSubmissionStatus(int submissionId, String surveyStatus, String customerCareStatus, String customerCareReview, String dataTeamReview) throws ClassNotFoundException, SQLException {
        Connection connection = SurveyDatabaseHelpers.getWriterConnection();
        String query = "UPDATE zebrasurveysubmissions SET survey_status = ?, customer_care_status = ?, customer_care_review = ?, data_team_review = ? WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, surveyStatus);
        preparedStatement.setString(2, customerCareStatus);
        preparedStatement.setString(3, customerCareReview);
        preparedStatement.setString(4, dataTeamReview);
        preparedStatement.setInt(5, submissionId);
        preparedStatement.executeUpdate();
        connection.close();
    }
}
