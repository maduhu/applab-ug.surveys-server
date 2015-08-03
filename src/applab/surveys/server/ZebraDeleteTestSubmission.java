package applab.surveys.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import applab.server.ApplabServlet;
import applab.server.ServletRequestContext;
import applab.surveys.Survey;
import applab.surveys.SurveyStatus;

public class ZebraDeleteTestSubmission extends ApplabServlet{
	
	protected void doApplabGet(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {
		
		String salesforceId = request.getParameter("salesforceId");
		int submissionId = Integer.valueOf(request.getParameter("submissionId"));
		String customerCareReview = request.getParameter("ccReview") + (request.getParameter("addCcReview") == "" ? "" : "\n" + request.getParameter("addCcReview"));
        String dataTeamReview = request.getParameter("dtReview") + (request.getParameter("addDtReview") == "" ? "" : "\n" + request.getParameter("addDtReview"));
		Survey survey = new Survey(salesforceId);
		survey.loadSurvey(true);
//		String surveyStatus = survey.getSurveyStatus().toString();
		if (customerCareReview.equalsIgnoreCase("Flagged") && dataTeamReview.equalsIgnoreCase("Rejected"))
			deleteSubmission(survey.getPrimaryKey(), submissionId, customerCareReview, dataTeamReview);

		
	}
	
	private static void deleteSubmission(int surveyId, int submissionId, String customerCareReview, String dataTeamReview) throws Exception {
        Connection connection = SurveyDatabaseHelpers.getWriterConnection();
        String query = "DELETE zebrasurveysubmissions survey_id = ?, customer_care_review = ?, data_team_review = ? WHERE id = ? AND data_team_review =? ";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, surveyId);
        preparedStatement.setString(2, customerCareReview);
        preparedStatement.setString(3, dataTeamReview);
        preparedStatement.setInt(4, submissionId);
        preparedStatement.executeUpdate();
        connection.close();
    }

}
