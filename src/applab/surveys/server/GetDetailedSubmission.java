package applab.surveys.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;

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
import applab.surveys.Answer;
import applab.surveys.CustomerCareStatus;
import applab.surveys.Question;
import applab.surveys.Submission;
import applab.surveys.SubmissionStatus;
import applab.surveys.Survey;


public class GetDetailedSubmission extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public void doApplabPost(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws ClassNotFoundException, SQLException,
            IOException, ServiceException, ParseException, ServletException, SAXException, ParserConfigurationException {

        // At the moment we are not really using the session except to pass objects to the jsp
        // as we are already logged in via salesforce. This isn't massively secure and needs
        // to be improved. TODO - sort this out!!
        HttpSession session = request.getSession(true);
        if (session == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Session");
        }
        
        String surveyId = request.getParameter("surveyId");
        String surveySalesforceId = request.getParameter("surveySalesforceId");
        String submissionId = request.getParameter("submissionId");
        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");
        String status = request.getParameter("status");
        String showDraft = request.getParameter("showDraft");

        // Load the survey
        Survey survey = new Survey(surveySalesforceId);
        survey.loadSurvey(surveySalesforceId, true);

        // Get the submission details
        ResultSet submissionData = getDetailedSubmissionData(Integer.valueOf(submissionId));
        
        if (DatabaseHelpers.getRowCount(submissionData) == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Submission ID does not exist: " + submissionId);
        }
        else {

            // Create the detailed submission data.
            Submission detailedSubmission = createSubmission(submissionData, survey);
            detailedSubmission.setId(Integer.valueOf(submissionId));

            // Bind the submission object
            session.setAttribute("survey.detailedSubmission", detailedSubmission);
            session.setAttribute("survey.surveySalesforceId", surveySalesforceId);
            session.setAttribute("survey.surveyId", surveyId);
            session.setAttribute("survey.startDate", startDate);
            session.setAttribute("survey.endDate", endDate);
            session.setAttribute("survey.status", status);
            session.setAttribute("survey.showDraft", showDraft);

            // Play the jsp page to display the details
            String url = "/jsp/SubmissionDetails.jsp";
            ServletContext servletContext = getServletContext();
            RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher(url);
            requestDispatcher.forward(request, response);
        }
    }

    /**
     * Get the data from the DB
     * 
     * @param SubmissionId - The id in the database that corresponds to the submission required.
     */
    private ResultSet getDetailedSubmissionData(int submissionId) throws SQLException, ClassNotFoundException {
        
        Connection connection = SurveyDatabaseHelpers.getReaderConnection();
        
        // Build the SQL for the query.
        StringBuilder commandText = new StringBuilder();
        commandText.append("SELECT a.question_number AS questionNumber, ");
        commandText.append("a.question_name AS questionName, ");
        commandText.append("a.position AS position, ");
        commandText.append("a.answer AS answer, ");
        commandText.append("s.server_entry_time AS surveyTime, ");
        commandText.append("s.handset_submit_time AS handsetTime, ");
        commandText.append("s.survey_id AS surveyId, ");
        commandText.append("s.interviewer_id AS interviewerId, ");
        commandText.append("s.interviewer_name AS interviewerName, ");
        commandText.append("s.mobile_number AS mobileNumber, ");
        commandText.append("s.survey_status AS surveyStatus, ");
        commandText.append("s.customer_care_status AS customerCareStatus, ");
        commandText.append("s.location AS location ");
        commandText.append("FROM answers a, zebrasurveysubmissions s, ");
        commandText.append("WHERE a.submission_id = ?, ");
        commandText.append("AND a.submission_id = s.id, ");
        commandText.append("ORDER BY questionNumber, position");
        
        // Prepare the statement
        PreparedStatement query = connection.prepareStatement(commandText.toString());
       
        // Pass the variables to the prepared statement
        query.setInt(1, submissionId);
        
        // Execute the query
        return query.executeQuery();
    }
    
    private Submission createSubmission(ResultSet resultSet, Survey survey)
            throws SQLException, ClassNotFoundException, SAXException, IOException, ParserConfigurationException {

        // Get the questions from the survey
        HashMap<String, Question> questions = survey.getBackEndSurveyXml().getQuestions();
        Submission submission = new Submission(survey);

        submission.setHandsetSubmissionTime(resultSet.getDate("handsetTime"));
        submission.setServerSubmissionTime(resultSet.getDate("surveyTime"));
        submission.setInterviewerName(resultSet.getString("interviewerName"));
        submission.setInterviewerId(resultSet.getString("interviewerId"));
        submission.setPhoneNumber(resultSet.getString("mobileNumber"));
        submission.setLocation(resultSet.getString("location"));
        
        // Deal with the statuses
        SubmissionStatus dataTeamStatus = SubmissionStatus.parseDisplayName(resultSet.getString("surveyStatus"));
        submission.setStatus(dataTeamStatus);
        CustomerCareStatus customerCareStatus = CustomerCareStatus.parseDisplayName(resultSet.getString("customerCareStatus"));
        submission.setCustomerCareStatus(customerCareStatus);
        
        // Loop through the result set and add the answers.
        do {
            
            int position = resultSet.getInt("position");
            String questionName = resultSet.getString("questionName");

            // The addition here is that the DB is zero ordered but the display 
            // should start at 1
            String name = questionName + "_" + Integer.toString(position + 1);
            submission.addAnswer(name, Answer.create(questions.get(questionName), resultSet.getString("answer"), position));
        }
        while (resultSet.next());
        
        return submission;
    }
}
