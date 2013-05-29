package applab.surveys.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import applab.surveys.ProcessedSubmission;

public class EwarehouseSurveyProcessing {

	private static final Map<String, String> questionTypeMap;
	static {
		questionTypeMap = new HashMap<String, String>();
		questionTypeMap.put("q1", "Input");
		questionTypeMap.put("q12", "Input");
		questionTypeMap.put("q26", "Select1");
		questionTypeMap.put("q28", "Select1");
		questionTypeMap.put("q31", "Select1");
		questionTypeMap.put("q32", "Select1");
		questionTypeMap.put("q33", "Select1");
		questionTypeMap.put("q34", "Select1");
		questionTypeMap.put("q35", "Select1");
		questionTypeMap.put("q36", "Select1");
		questionTypeMap.put("q37", "Select1");
		questionTypeMap.put("q38", "Select1");
		questionTypeMap.put("q39", "Select1");
		questionTypeMap.put("q40", "Select1");
		questionTypeMap.put("q29", "Select");
		questionTypeMap.put("q75", "Select");
	}

	/**
	 * 
	 * @param submissionResultSet
	 *            - The ResultSet that contains the survey answers
	 * 
	 * @param document
	 *            - The Xml document to be created
	 * @return 
	 *            - The modified Xml Document
	 * @throws SQLException
	 */
	public static Document processEwarehouseSurveys(
			ResultSet submissionResultSet, Document document)
			throws SQLException {

		Element rootElement = document.createElement("answers");
		document.appendChild(rootElement);
		while (submissionResultSet.next()) {
			String binding = submissionResultSet.getString("questionName");
			String answerText = submissionResultSet.getString("answer");
			String instance = submissionResultSet.getString("position");
			String questionNumber = submissionResultSet
					.getString("questionNumber");
			String questionType = (questionTypeMap.containsKey(binding)) ? questionTypeMap
					.get(binding) : "Input";
			String parentBinding = submissionResultSet
					.getString("parentBinding");
			String parentInstance = submissionResultSet
					.getString("parentPosition");
			Element answerElement = document.createElement("answer");
			rootElement.appendChild(answerElement);
			Element bindingElement = document.createElement("binding");
			bindingElement.appendChild(document.createTextNode(binding));
			answerElement.appendChild(bindingElement);
			Element answerTextElement = document.createElement("answerText");
			answerTextElement.appendChild(document.createTextNode(answerText));
			answerElement.appendChild(answerTextElement);
			Element instanceElement = document.createElement("instance");
			instanceElement.appendChild(document.createTextNode(instance));
			answerElement.appendChild(instanceElement);
			Element questionNumberElement = document
					.createElement("questionNumber");
			questionNumberElement.appendChild(document
					.createTextNode(questionNumber));
			answerElement.appendChild(questionNumberElement);
			Element questionTypeElement = document
					.createElement("questionType");
			questionTypeElement.appendChild(document
					.createTextNode(questionType));
			answerElement.appendChild(questionTypeElement);
			Element parentBindingElement = document
					.createElement("parentBinding");
			parentBindingElement.appendChild(document
					.createTextNode(parentBinding));
			answerElement.appendChild(parentBindingElement);
			Element parentInstanceElement = document
					.createElement("parentInstance");
			parentInstanceElement.appendChild(document
					.createTextNode(parentInstance));
			answerElement.appendChild(parentInstanceElement);
			rootElement.appendChild(answerElement);
		}
		return document;
	}

}
