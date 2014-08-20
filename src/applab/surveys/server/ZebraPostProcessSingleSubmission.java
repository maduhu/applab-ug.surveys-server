package applab.surveys.server;

import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import applab.server.ApplabServlet;
import applab.server.ServletRequestContext;
import applab.surveys.DeferredSurveyProcessingXmlGenerator;
import applab.surveys.ProcessedSubmission;
import applab.surveys.Survey;

public class ZebraPostProcessSingleSubmission extends ApplabServlet {

	protected void doApplabGet(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {

		String submissionId = request.getParameter("submissionId");
		String salesforceId = request.getParameter("salesforceId");
		log("we should have submissionId "+ submissionId + " and salesforceId " + salesforceId);
		Survey survey = new Survey(salesforceId);
		survey.loadSurvey(true);
		ResultSet resultSet = SurveyDatabaseHelpers.getSubmissionDetails(Integer.parseInt(submissionId));
		
		if (resultSet.next()) {
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
            log(submission.getImei() + " submitted a survey " + submission.getSurvey().getSalesforceId() + " with the result: " + returnValues[1]);
            response.getWriter().write(submission.getImei() + " submitted a survey " + submission.getSurvey().getSalesforceId() + " with the result: " + returnValues[1]);
		}
	}
}
