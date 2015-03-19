package applab.surveys.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import applab.server.ApplabServlet;
import applab.server.ServletRequestContext;
import applab.server.XmlHelpers;
import applab.surveys.DeferredSurveyProcessingXmlGenerator;
import applab.surveys.ProcessedSubmission;
import applab.surveys.Survey;
import applab.surveys.server.SurveyDatabaseHelpers;

public class LoadDataServlet  extends ApplabServlet {

	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("finally")
	@Override
    protected void doApplabPost(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {

		response.setHeader("Location", request.getRequestURI());
		String queryin = request.getParameter("query");
		String idin = request.getParameter("id");
		log("--------------processing stated----------------");
		log("--------------" + idin + "----------------");
		log("--------------" + queryin + "----------------");
	   // JDBC driver name and database URL
	   String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
	   String DB_URL = "jdbc:mysql://localhost/zebra";

	   //  Database credentials
	   String USER = "root";
	   String PASS = "ckw@pps";
		   
	   Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection(DB_URL,USER,PASS);;//SurveyDatabaseHelpers.getWriterConnection();
		//PreparedStatement submissionStatement = conn.prepareStatement("select * from fhi_monitoring_cco_north");
		PreparedStatement submissionStatement = conn.prepareStatement(queryin);
		ResultSet approvedSubmissions = submissionStatement.executeQuery();//getResultSet();
		
		Survey survey = null;//new Survey(salesforceId);
//		String survey_id = "20140121949";
		String survey_id = idin;
        Boolean first = false;
        
		while(approvedSubmissions.next()){
			//approvedSubmissions.next();
			try{
				//ProcessedSubmission submission_ = new ProcessedSubmission(approvedSubmissions.getString("imei"), approvedSubmissions.getString("interviewee_id"), approvedSubmissions.getString("location"), approvedSubmissions.getString("location"));
				//submission.parseSurveySubmission(); //no need//approvedSubmissions.getString("surveyId");
				log("salesforce survey id is " + survey_id);
				//submission_.setBackendSurveyIdFromRootNode(survey_id);
				if(survey == null){
					survey = new Survey(survey_id);
			        survey.loadSurvey(true);
					log("survey loaded");
				}
				Integer id = approvedSubmissions.getInt("Id");
				log("survey id is " + id);
				ResultSet submissionResultSet = SurveyDatabaseHelpers.getSubmissionDetails(id);
            	submissionResultSet.next();
				
            	// Do the Post Processing
                ProcessedSubmission submission = new ProcessedSubmission(
                        submissionResultSet.getString("handsetId"),
                        submissionResultSet.getString("interviewerName"),
                        submissionResultSet.getString("location"),
                        submissionResultSet.getString("submissionLocation")
                );
                String id_ = String.valueOf(id);
				log("survey primary key " + id_);
				submission.setBackendSurveyIdFromRootNode(String.valueOf(survey.getPrimaryKey()));
                submission.setSize(submissionResultSet.getInt("submissionSize"));
                submission.setHandsetSubmitTime(submissionResultSet.getDate("handsetTime"));
                submission.setSubmissionStartTime(submissionResultSet.getDate("surveyStartTime"));
                //submission.setDuplicateHash(submission.getImei()+ "-" + id_);
                submission.setSurvey(survey);
                submission.setDuplicateHash(submission.getImei() + id_);
                
                //submission.setBackendSurveyIdFromRootNode(survey_id);
                log("set id is " + submission.getSalesforceId());
                
    			log("invoking post-processing");
    			
    			
                //String[] returnValues = DeferredSurveyProcessingXmlGenerator.postProcessSurvey(submissionResultSet, survey,  submission);
    			StringBuilder xml = new StringBuilder();
    			HashMap<String, String> attributes = new HashMap<String, String>();
    			xml.append(XmlHelpers.generateStartElement("answers", attributes));
    			do {
    				String binding = submissionResultSet.getString("questionName");
    				String answerText = (submissionResultSet.getString("answer") != null ? submissionResultSet
    						.getString("answer") : "null");
    				String instance = submissionResultSet.getString("position");
    				String questionNumber = submissionResultSet
    						.getString("questionNumber");
    				if(survey.getBackEndSurveyXml() == null) log("survey.getBackEndSurveyXml is NULL");
    				String questionType = survey.getBackEndSurveyXml().getQuestions()
    						.get(binding).getType().toString();
    				String parentBinding = (submissionResultSet
    						.getString("parentBinding") != null ? submissionResultSet
    						.getString("parentBinding") : "null");
    				String parentInstance = (submissionResultSet
    						.getString("parentPosition") != null ? submissionResultSet
    						.getString("parentPosition") : "0");

    				xml.append(XmlHelpers.generateStartElement("answer", attributes));

    				xml.append(XmlHelpers.generateStartElement("binding", attributes));
    				xml.append(XmlHelpers.escapeText(binding));
    				xml.append(XmlHelpers.generateEndElement("binding"));

    				xml.append(XmlHelpers
    						.generateStartElement("answerText", attributes));
    				xml.append(XmlHelpers.escapeText(answerText));
    				xml.append(XmlHelpers.generateEndElement("answerText"));

    				xml.append(XmlHelpers.generateStartElement("instance", attributes));
    				xml.append(XmlHelpers.escapeText(instance));
    				xml.append(XmlHelpers.generateEndElement("instance"));

    				xml.append(XmlHelpers.generateStartElement("questionNumber",
    						attributes));
    				xml.append(XmlHelpers.escapeText(questionNumber));
    				xml.append(XmlHelpers.generateEndElement("questionNumber"));

    				xml.append(XmlHelpers.generateStartElement("questionType",
    						attributes));
    				xml.append(XmlHelpers.escapeText(questionType));
    				xml.append(XmlHelpers.generateEndElement("questionType"));

    				xml.append(XmlHelpers.generateStartElement("parentBinding",
    						attributes));
    				xml.append(XmlHelpers.escapeText(parentBinding));
    				xml.append(XmlHelpers.generateEndElement("parentBinding"));

    				xml.append(XmlHelpers.generateStartElement("parentInstance",
    						attributes));
    				xml.append(XmlHelpers.escapeText(parentInstance));
    				xml.append(XmlHelpers.generateEndElement("parentInstance"));

    				xml.append(XmlHelpers.generateEndElement("answer"));

    			} while (submissionResultSet.next());

    			//skip the first, was used in testing
//    			if(!first){
//    				log("skipping first, already processed");
//    				first = true;
//    				continue;
//    			}
    			xml.append(XmlHelpers.generateEndElement("answers"));
    			String[] returnValues = DeferredSurveyProcessingXmlGenerator.saveToSalesforce(xml.toString(), submission);
                
                log(submission.getImei() + " submitted a survey with the following result : " + returnValues[1]);
                
                PreparedStatement logStatement = conn.prepareStatement(
                		"INSERT INTO fhi_processed_submissions(survey_id,submission_id,salesforce_response, created) values(?,?,?,now())");
                logStatement.setInt(1, approvedSubmissions.getInt("surveyId"));
                logStatement.setInt(2, id);
                if(returnValues != null && returnValues.length > 1){
                	logStatement.setString(3, returnValues[1]);
                } else {
                	logStatement.setString(3, "no result");
                	//log("FHI, " + id + ", no result");
                }
        		logStatement.execute();//getResultSet();
			}
			catch(Exception e){
				e.printStackTrace();
				log("FHI ERROR: " + e.getMessage());
			}
			finally{
				//log("FHI, " + id + ", no result");
			}
		}
		log("processing ended");
	}
}
