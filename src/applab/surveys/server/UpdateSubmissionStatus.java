package applab.surveys.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import applab.server.ApplabServlet;
import applab.server.ServletRequestContext;
import applab.surveys.CustomerCareStatus;
import applab.surveys.SubmissionStatus;

/**
 * Called by ReviewSubmissions to update the status of survey submissions based on staff review
 * 
 */
public class UpdateSubmissionStatus extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    public void doApplabPost(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws IOException, ClassNotFoundException, SQLException, ServletException {

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
       
        updateSubmissionStatus(submissionId, dataTeamStatus.getDisplayName(), customerCareStatus.getDisplayName(), customerCareReview, dataTeamReview);

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
