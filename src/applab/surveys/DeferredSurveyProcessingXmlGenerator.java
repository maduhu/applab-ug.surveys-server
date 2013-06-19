package applab.surveys;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;

import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.enterprise.SessionHeader;
import com.sforce.soap.enterprise.SforceServiceLocator;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;
import com.sforce.soap.schemas._class.ProcessSurveySubmission.ProcessSurveySubmissionBindingStub;
import com.sforce.soap.schemas._class.ProcessSurveySubmission.ProcessSurveySubmissionServiceLocator;
import com.sforce.soap.schemas._class.ProcessSurveySubmission.SurveySubmission;

import applab.Location;
import applab.server.ApplabConfiguration;
import applab.server.WebAppId;
import applab.server.XmlHelpers;
import applab.surveys.ProcessedSubmission;
import applab.surveys.Survey;

/**
 * Class to create submission answer XML to be passed to the
 * ProcessSurveySubmission web service
 * 
 */

public class DeferredSurveyProcessingXmlGenerator {

	/**
	 * 
	 * @param submissionResultSet
	 *            - The ResultSet that contains the survey answers
	 * 
	 * @param survey
	 *            - The survey to which this submission belongs
	 * 
	 * @param pSubmission
	 *            - The processed submission object
	 * @return - The salesforce web service return array
	 * @throws SQLException
	 * @throws TransformerException
	 * @throws ServiceException
	 * @throws ClassNotFoundException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public static String[] processEwarehouseSurveys(
			ResultSet submissionResultSet, Survey survey,
			ProcessedSubmission pSubmission) throws SQLException,
			TransformerException, ClassNotFoundException, ServiceException,
			SAXException, IOException, ParserConfigurationException {

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

		xml.append(XmlHelpers.generateEndElement("answers"));
		return saveToSalesforce(xml.toString(), pSubmission);
	}

	public static String[] saveToSalesforce(String xmlString,
			ProcessedSubmission pSubmission) throws InvalidIdFault,
			UnexpectedErrorFault, LoginFault, RemoteException,
			ClassNotFoundException, SQLException, ServiceException {

		ProcessSurveySubmissionServiceLocator serviceLocator = new ProcessSurveySubmissionServiceLocator();
		ProcessSurveySubmissionBindingStub serviceStub = (ProcessSurveySubmissionBindingStub) serviceLocator
				.getProcessSurveySubmission();

		// Use soap api to login and get session info
		SforceServiceLocator soapServiceLocator = new SforceServiceLocator();
		soapServiceLocator.setSoapEndpointAddress((String) ApplabConfiguration
				.getConfigParameter(WebAppId.global, "salesforceAddress", ""));
		SoapBindingStub binding = (SoapBindingStub) soapServiceLocator
				.getSoap();
		LoginResult loginResult = binding.login(
				(String) ApplabConfiguration.getConfigParameter(
						WebAppId.global, "salesforceUsername", ""),
				(String) ApplabConfiguration.getConfigParameter(
						WebAppId.global, "salesforcePassword", "")
						+ (String) ApplabConfiguration.getConfigParameter(
								WebAppId.global, "salesforceToken", ""));
		SessionHeader sessionHeader = new SessionHeader(
				loginResult.getSessionId());

		// Share the session info with our webservice
		serviceStub.setHeader(
				"http://soap.sforce.com/schemas/class/ProcessSurveySubmission",
				"SessionHeader", sessionHeader);

		// Create and populate the webservice
		SurveySubmission surveySubmission = new SurveySubmission();
		surveySubmission.setJson("none");
		surveySubmission.setXml(xmlString);
		surveySubmission.setImei(pSubmission.getImei());
		surveySubmission.setFarmerId(pSubmission.getIntervieweeId());
		surveySubmission.setSurveyId(pSubmission.getSalesforceId());
		surveySubmission.setResultHash(pSubmission.getDuplicateHash());
		surveySubmission.setSurveySize(String.valueOf(pSubmission.getSize()));
		surveySubmission.setHandsetSubmitTime(String.valueOf(pSubmission
				.getHandsetSubmissionTime().getTime()));
		surveySubmission.setSubmissionStartTime(String.valueOf(pSubmission
				.getSubmissionStartTime().getTime()));

		// Generate the interview location
		Location locationObject = null;

		locationObject = Location.parseLocation(pSubmission
				.getInterviewLocation());
		surveySubmission.setInterviewLatitude(locationObject.latitude
				.toString());
		surveySubmission.setInterviewLongitude(locationObject.longitude
				.toString());
		surveySubmission.setInterviewAltitude(locationObject.altitude
				.toString());
		surveySubmission.setInterviewAccuracy(locationObject.accuracy
				.toString());
		surveySubmission.setInterviewGPSTimestamp(String
				.valueOf(locationObject.timestamp));
		locationObject = null;

		// Generate the submission location
		locationObject = Location.parseLocation(pSubmission
				.getSubmissionLocation());
		surveySubmission.setSubmissionLatitude(locationObject.latitude
				.toString());
		surveySubmission.setSubmissionLongitude(locationObject.longitude
				.toString());
		surveySubmission.setSubmissionAltitude(locationObject.altitude
				.toString());
		surveySubmission.setSubmissionAccuracy(locationObject.accuracy
				.toString());
		surveySubmission.setSubmissionGPSTimestamp(String
				.valueOf(locationObject.timestamp));

		// Send the submission to salesforce
		SurveySubmission resultSurveySubmission = serviceStub
				.processSurveySubmission(surveySubmission);

		// Check the result and return the correct response code
		String returnCode = String.valueOf(HttpServletResponse.SC_CREATED);
		if (!resultSurveySubmission.getSuccess()) {
			returnCode = String.valueOf(HttpServletResponse.SC_BAD_REQUEST);
		}
		String[] returnValues = { returnCode,
				resultSurveySubmission.getErrorMessage() };
		return returnValues;
	}
}
