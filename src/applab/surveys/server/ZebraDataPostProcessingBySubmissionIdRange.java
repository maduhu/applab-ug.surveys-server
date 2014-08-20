package applab.surveys.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import applab.server.ApplabServlet;
import applab.server.ServletRequestContext;
import applab.surveys.DeferredSurveyProcessingXmlGenerator;
import applab.surveys.ProcessedSubmission;
import applab.surveys.Survey;

public class ZebraDataPostProcessingBySubmissionIdRange extends ApplabServlet {

	protected void doApplabGet(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {
		
		String salesforceId = request.getParameter("salesforceId");
		String submissionStatus = request.getParameter("status");
		String submissionIdRange = request.getParameter("submissionId");
		int submissionIds = Integer.valueOf(submissionIdRange);
		Survey survey = new Survey(salesforceId);
		survey.loadSurvey(true);
		ResultSet submissionsResultSet = getAllSubmissionsById(survey.getPrimaryKey(), submissionStatus, submissionIds);
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
                String[] returnValues = DeferredSurveyProcessingXmlGenerator.postProcessSurvey(resultSet, survey,  submission);
                log("Handset with IMEI: " + submission.getImei() + " submission id" + submissionId + " submitted a survey with the following result " + returnValues[1]);
			}
		}
		log("post processing complete!!");
		response.getWriter().print("post processing complete!");
	}
	
	private static ResultSet getAllSubmissionsById(int surveyId, String submissionStatus, int submissionId) throws ClassNotFoundException, SQLException {
		
        Connection connection = SurveyDatabaseHelpers.getReaderConnection();

        // Build the SQL for the query.
        StringBuilder commandText = new StringBuilder();
        commandText.append("SELECT s.id AS submissionId, ");
        commandText.append("s.survey_id AS surveyId, ");
        commandText.append("s.result_hash AS resultHash, ");
        commandText.append("s.survey_status AS surveyStatus ");
        commandText.append("FROM zebrasurveysubmissions s ");
        commandText.append("WHERE s.survey_id = ? ");
        commandText.append("AND s.id >= ? ");
        if (submissionStatus.equalsIgnoreCase("Approved") || submissionStatus.equalsIgnoreCase("Pending")) {
        	commandText.append("AND s.survey_status = ? ");
        }

        // Prepare the statement
        PreparedStatement query = connection.prepareStatement(commandText.toString());

        // Pass the variables to the prepared statement
        query.setInt(1, surveyId);
        query.setInt(2, submissionId);
        if (submissionStatus.equalsIgnoreCase("Approved") || submissionStatus.equalsIgnoreCase("Pending")) {
        	query.setString(3, submissionStatus);
        }

        // Execute the query
        return query.executeQuery(); 
	}

}
