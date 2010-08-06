package applab.surveys.server;

import javax.servlet.http.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Called by ReviewSubmissions to update the status of survey submissions based on staff review
 * 
 */
public class UpdateSubmissionStatus extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    public void doApplabPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ClassNotFoundException, SQLException {
        // parse our parameters to get the relevant set of submissions to update
        Enumeration<?> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            // submission updates are encoded in the form of submission[id]=[new status]
            String parameterName = (String)names.nextElement();
            String submissionParameterPrefix = "submission";
            if (parameterName.startsWith(submissionParameterPrefix)) {
                int submissionId = Integer.parseInt(parameterName.substring(submissionParameterPrefix.length()));
                String newSurveyStatus = request.getParameter(parameterName);
                updateSubmissionStatus(submissionId, newSurveyStatus);
            }
        }
        String surveyIdParameter = request.getParameter("surveyid");
        response.sendRedirect(ApplabConfiguration.getHostUrl() + "getSubmissions?status=none&surveyId=" + surveyIdParameter);
    }

    static void updateSubmissionStatus(int submissionId, String surveyStatus) throws ClassNotFoundException, SQLException {
        Connection connection = DatabaseHelpers.createConnection(DatabaseId.Surveys);
        Statement statement = connection.createStatement();
        String sqlQuery = "UPDATE zebrasurveysubmissions set survey_status='" + surveyStatus + "' WHERE id=" + submissionId;
        statement.executeUpdate(sqlQuery);
        statement.close();
        connection.close();
    }
}
