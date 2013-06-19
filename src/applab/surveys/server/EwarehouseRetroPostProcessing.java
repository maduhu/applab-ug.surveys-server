package applab.surveys.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import applab.server.ApplabServlet;
import applab.server.ServletRequestContext;
import applab.surveys.DeferredSurveyProcessingXmlGenerator;
import applab.surveys.ProcessedSubmission;
import applab.surveys.Survey;

public class EwarehouseRetroPostProcessing extends ApplabServlet {
	
	protected void doApplabGet(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {
		
		String salesforceId = request.getParameter("salesforceId");
		Survey survey = new Survey(salesforceId);
		survey.loadSurvey(true);
		doPostProcessingForPreviouslyApprovedSubmissions(survey, response);
		response.getWriter().write("Post Processing Complete!!");
	}
	
	public static void doPostProcessingForPreviouslyApprovedSubmissions(Survey survey, HttpServletResponse response) throws ClassNotFoundException, SQLException, ParserConfigurationException, ServiceException, TransformerException, IOException, SAXException {

		ResultSet submissionsResultSet = getAllApprovedSubmissionsById(survey.getPrimaryKey());
		while (submissionsResultSet.next()) {
			int submissionId = submissionsResultSet.getInt("submissionId");
			ResultSet resultSet = SurveyDatabaseHelpers.getSubmissionDetails(submissionId);
			while (resultSet.next()) {
                ProcessedSubmission submission = new ProcessedSubmission(
                		resultSet.getString("handsetId"),
                		resultSet.getString("interviewerName"),
                		resultSet.getString("location"),
                		resultSet.getString("submissionLocation")
                );
                submission.setBackendSurveyIdFromRootNode(String.valueOf(survey.getPrimaryKey()));
                submission.setSize(resultSet.getInt("submissionSize"));
                submission.setHandsetSubmitTime(resultSet.getDate("handsetTime"));
                submission.setSubmissionStartTime(resultSet.getDate("surveyStartTime"));
                submission.setSurvey(survey);
                String[] returnValues = DeferredSurveyProcessingXmlGenerator.processEwarehouseSurveys(resultSet, survey,  submission);
                
			}
		}
	}
	
	public static ResultSet getAllApprovedSubmissionsById(int surveyId) throws ClassNotFoundException, SQLException {
		
        Connection connection = SurveyDatabaseHelpers.getReaderConnection();

        // Build the SQL for the query.
        StringBuilder commandText = new StringBuilder();
        commandText.append("SELECT s.id AS submissionId, ");
        commandText.append("s.survey_id AS surveyId, ");
        commandText.append("s.survey_status AS surveyStatus ");
        commandText.append("FROM zebrasurveysubmissions s ");
        commandText.append("WHERE s.survey_id = ? ");
        commandText.append("AND s.survey_status = ? ");
        
        // Prepare the statement
        PreparedStatement query = connection.prepareStatement(commandText.toString());

        // Pass the variables to the prepared statement
        query.setInt(1, surveyId);
        query.setString(2, "Approved");

        // Execute the query
        return query.executeQuery(); 
	}

}
