package applab.surveys.server;

import applab.server.*;
import applab.surveys.*;

import javax.servlet.http.*;
import javax.xml.rpc.ServiceException;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

/**
 * Display an administrative page that displays the submissions for a particular survey, including overall statistics on
 * how many submissions are in each stage of review.
 * 
 */
public class ReviewSubmissions extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public void doApplabGet(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws ClassNotFoundException, SQLException,
            IOException, ServiceException {
        String salesforceSurveyId = request.getParameter("surveyId");
        SubmissionStatistics statistics = SubmissionStatistics.getStatistics(salesforceSurveyId);

        if (statistics == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Survey ID does not exist: " + salesforceSurveyId);
            return;
        }

        String statusParameter = request.getParameter("status");
        PrintWriter responseWriter = response.getWriter();
        responseWriter.write("<html><head>");
        responseWriter.write("<link href=\"StyleSheet.css\" rel=\"stylesheet\" type=\"text/css\"");
        responseWriter.write("</head>");
        responseWriter.write("<body>");

        // first table: overall survey statistics (how many submissions are in each state)
        responseWriter.write("<table border=1 cellspacing=0 cellpadding=4>");
        responseWriter.write("<tr><td colspan=2><b>" + statistics.getSurvey().getName() + " survey</b></td></tr>");
        responseWriter.write("<tr><td><b>Submission Status</b></td><td><b>Number of submissions</b></td></tr>");

        // have links to the detail pages (which add the status parameter
        String baseUrl = ApplabConfiguration.getHostUrl() + "getSubmissions?surveyId=" + salesforceSurveyId + "&status=";
        for (SubmissionStatus status : SubmissionStatus.values()) {
            responseWriter.write("<tr><td>");
            responseWriter.write("<a href=\"" + baseUrl + status.getHtmlParameterValue() + "\">");
            responseWriter.write(status.getDisplayName());
            responseWriter.write("</a></td>");
            responseWriter.write("<td>");
            responseWriter.write(Integer.toString(statistics.getNumberOfSubmissions(status)));
            responseWriter.write("</td></tr>");
        }
        responseWriter.write("</table>");

        // now show the answers so that the team can update their review status
        Collection<Question> questions = statistics.getSurvey().getQuestions();
        if (questions.size() > 0) {
            // columns are Status, Interviewer Id, Interviewer Name, [answers]
            int tableColumnCount = questions.size() + 3;
            // header describing the submissions filter
            SubmissionStatus statusFilter = SubmissionStatus.parseHtmlParameter(statusParameter);
            responseWriter.write("<h2>Submissions");
            if (statusFilter != null) {
                responseWriter.write(" (Status = " + statusFilter.getDisplayName() + ")");
            }
            responseWriter.write("</h2>");

            responseWriter.write("<form method=post action=\"" + ApplabConfiguration.getHostUrl() + "updateSubmissionStatus\">");
            responseWriter.write("<table border=1 cellpadding=3 cellspacing=0>");

            // include the survey id as a hidden field to use when processing the submission updates
            responseWriter.write("<tr><td class=bold colspan=" + tableColumnCount + "><input type=hidden name=surveyid value="
                    + salesforceSurveyId + "></td></tr>");
            // and the submit button at the top
            responseWriter.write("<tr><td class=bold colspan=" + tableColumnCount
                    + "><input type=submit value=Submit class=style></td></tr>");

            responseWriter.write("<tr>");
            responseWriter.write("<td class=bold2 valign=top>Survey Status</td>");
            responseWriter.write("<td class=bold2>Interviewer Id</td>");
            responseWriter.write("<td class=bold2>Interviewer Name</td>");

            // process the list of survey questions, grabbing the name to use
            // for updating answers and the value to use as a table header
            for (Question question : questions) {
                response.getWriter().write("<td class=bold2 valign=top>" + question.getDisplayValue() + "</td>");
            }
            responseWriter.write("</tr>");

            for (Submission submission : statistics.getSurvey().getSubmissions(statusFilter)) {
                responseWriter.write("<tr>");

                // first list the status dropdown. We use submissionId to name the dropdown.
                responseWriter.write("<td class=text valign=top>");
                responseWriter.write("<select name=submission" + submission.getId() + ">");
                for (SubmissionStatus status : SubmissionStatus.values()) {
                    responseWriter.write("<option value=\"" + status.getDisplayName() + "\"");
                    if (submission.getStatus() == status) {
                        responseWriter.write(" selected ");
                    }
                    responseWriter.write(">" + status.getDisplayName() + "</option>");
                }
                responseWriter.write("</select></td>");

                // then the interviewer id...
                responseWriter.write("<td class=text valign=top>" + submission.getInterviewerId() + "</td>");

                // the interviewer name
                responseWriter.write("<td class=text valign=top>" + submission.getInterviewerName() + "</td>");

                // and the answers to the questions
                for (Answer answer : submission.getAnswers()) {
                    String answerText = answer.getFriendlyAnswerText();
                    if (answerText == null || answerText.trim().length() == 0) {
                        answerText = "[No Answer]";
                    }
                    responseWriter.write("<td class=text valign=top>" + answerText + "</td>");
                }
                responseWriter.write("</tr>");
            }
            responseWriter.write("</table></form>");
        }

        responseWriter.write("</body></html>");
    }

    /**
     * represents the set of submission statistics for a given survey (based on SF id)
     * 
     */
    private static class SubmissionStatistics {
        private Survey survey;
        private HashMap<SubmissionStatus, Integer> numberOfSubmissions;

        private SubmissionStatistics(Survey survey) {
            assert (survey != null) : "internal API: caller must ensure non-null survey";
            this.survey = survey;
            this.numberOfSubmissions = new HashMap<SubmissionStatus, Integer>();
        }

        public int getNumberOfSubmissions(SubmissionStatus status) {
            return this.numberOfSubmissions.get(status);
        }

        public Survey getSurvey() {
            return this.survey;
        }

        public static SubmissionStatistics getStatistics(String salesforceSurveyId) throws ClassNotFoundException, SQLException {
            Connection connection = DatabaseHelpers.createReaderConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            SubmissionStatistics statistics = null;

            // TODO, ZBR-86: we will want to bound this query based on date, which will require an addition to the WHERE
            // clause
            StringBuilder statisticsQuery = new StringBuilder();
            statisticsQuery.append("SELECT surveys.id as SurveyPrimaryKey, ");
            statisticsQuery.append("sum(case when submissions.survey_status='Approved' then 1 else 0 end) as Approved, ");
            statisticsQuery.append("sum(case when submissions.survey_status='Rejected' then 1 else 0 end) as Rejected, ");
            statisticsQuery.append("sum(case when submissions.survey_status='Pending' then 1 else 0 end) as Pending, ");
            statisticsQuery.append("sum(case when submissions.survey_status='Not Reviewed' then 1 else 0 end) as NotReviewed, ");
            statisticsQuery.append("sum(case when submissions.survey_status='Duplicate' then 1 else 0 end) as Duplicate ");
            statisticsQuery.append("FROM zebra.zebrasurveysubmissions submissions inner join zebra.zebrasurvey surveys ");
            statisticsQuery.append("ON submissions.survey_id = surveys.id ");
            statisticsQuery.append("WHERE surveys.survey_id = '" + salesforceSurveyId + "'");
            statisticsQuery.append(" GROUP BY submissions.survey_id");

            ResultSet resultSet = statement.executeQuery(statisticsQuery.toString());

            // check if we have any results for this survey
            if (resultSet.next()) {
                // if so, grab some base information
                Survey parentSurvey = new Survey(resultSet.getInt("SurveyPrimaryKey"), salesforceSurveyId);
                statistics = new SubmissionStatistics(parentSurvey);

                // and count up our submission statistics
                int notReviewedCount = 0;
                int pendingCount = 0;
                int approvedCount = 0;
                int rejectedCount = 0;
                int duplicateCount = 0;

                do {
                    notReviewedCount += resultSet.getInt("NotReviewed");
                    pendingCount += resultSet.getInt("Pending");
                    approvedCount += resultSet.getInt("Approved");
                    rejectedCount += resultSet.getInt("Rejected");
                    duplicateCount += resultSet.getInt("Duplicate");
                } while (resultSet.next());

                statistics.numberOfSubmissions.put(SubmissionStatus.NotReviewed, notReviewedCount);
                statistics.numberOfSubmissions.put(SubmissionStatus.Pending, pendingCount);
                statistics.numberOfSubmissions.put(SubmissionStatus.Approved, approvedCount);
                statistics.numberOfSubmissions.put(SubmissionStatus.Rejected, rejectedCount);
                statistics.numberOfSubmissions.put(SubmissionStatus.Duplicate, duplicateCount);
            }

            statement.close();
            connection.close();

            return statistics;
        }
    }
}
