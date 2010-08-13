/*
 * Copyright (C) 2010 Grameen Foundation
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package applab.surveys.server;

import javax.servlet.http.*;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import applab.server.XmlHelpers;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * Called from our survey designer to save an x-form (http://www.w3.org/TR/xforms/) into
 * our backend database.
 * 
 * We store the raw XML as a blob in the zebrasurvey table, and shred the questions 
 * into rows on the zebrasurveyquestions table.
 * 
 * XForms notes:
 * Namespace = http://www.w3.org/2002/xforms
 *
 */
public class SaveDesignerForm extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String survey_id = request.getParameter("formId");

        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        reader.close();
        String xform_data1 = sb.toString();
        String xform_data = xform_data1.replaceAll("\'", "\\'");
        System.out.println(xform_data.replaceAll("'", "\\'"));
        // get the survey name
        try {
            SalesforceProxy salesforceProxy = SalesforceProxy.login();
            String surveyName = salesforceProxy.getSurveyName(survey_id);
            // check if id exists in zebrasurvey
            String databaseId = DatabaseHelpers.getZebraSurveyId(survey_id);
            if (databaseId != null) {
                DatabaseHelpers.saveXform(survey_id, surveyName, xform_data);
                // on saving check with zebrasurveyquestions
                // configuration.DbConnect.deleteSurveyFromSurveyQuestions(zebra_survey_id);
                this.createSurveyQuestions(Integer.parseInt(databaseId), xform_data);
            }
            else if (salesforceProxy.surveyIdExists(survey_id)) {
                DatabaseHelpers.saveXform(survey_id, xform_data, surveyName, DatabaseHelpers.formatDateTime(new Date()));
                // on saving
                int zebra_survey_id = Integer.parseInt(DatabaseHelpers.getZebraSurveyId(survey_id));
                this.createSurveyQuestions(zebra_survey_id, xform_data);
            }
            salesforceProxy.logout();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * All xforms elements have a comment schema for identifying their "name" (parent location for the content).
     */    
    private String getFormElementName(Element formElement) {
        // bind attributes take top priority if they exist 
        String xformName = formElement.getAttribute("bind");
        
        // otherwise look for a ref parameter
        if (xformName == null || xformName.length() == 0) {
            xformName = formElement.getAttribute("ref");
        }
        
        return xformName;
    }
    
    /**
     * parse an x-forms group. Groups may have a label, a ref, both, or neither.
     */
    private void parseXformsGroup(Element groupElement, HashMap<String, String> parsedQuestions) {
        for (Node childNode = groupElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                // Purcforms input tag
                if ("xf:input".equals(childNode.getNodeName())) {
                    parseXformsInput((Element)childNode, parsedQuestions);
                }
                else if ("xf:select".equals(childNode.getNodeName()) || "xf:select1".equals(childNode.getNodeName())) {
                    parseXformsSelect((Element)childNode, parsedQuestions);
                }
                else if ("xf:group".equals(childNode.getNodeName())) {
                    parseXformsSubgroup((Element)childNode, parsedQuestions);
                }
            }
        }
    }

    /**
     * parse an x-forms group. Groups may have a label, a ref, both, or neither.
     * 
     * It can also contain <repeat> tags. A <repeat> is given the same visual hints in the UI as a <group>. 
     * 
     * The caption used for the banner header is taken from the <repeat>'s <label> (<group><label>xx</label><repeat>...</repeat></group>)
     * 
     *  <group>s and <repeat>s may be nested within each other arbitrarily deep. 
     */
    private void parseXformsSubgroup(Element groupElement, HashMap<String, String> parsedQuestions) {
        String parameter = groupElement.getAttribute("id");
        String question = "";
        for (Node childNode = groupElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue; // only care about element nodes
            }

            if ("xf:label".equals(childNode.getNodeName())) {
                question = getCharacterDataFromElement((Element)childNode);
                parsedQuestions.put(parameter, question);
            }
            else if ("xf:repeat".equals(childNode.getNodeName())) {
                for (Node repeatChild = childNode.getFirstChild(); repeatChild != null; repeatChild = repeatChild.getNextSibling()) {
                    if ("xf:input".equals(repeatChild.getNodeName())) {
                        parseXformsInput((Element)repeatChild, parsedQuestions);
                    }
                    else if ("xf:select".equals(repeatChild.getNodeName()) || "xf:select1".equals(repeatChild.getNodeName())) {
                        parseXformsSelect((Element)repeatChild, parsedQuestions);
                    }
                }
            }
        }
    }

    /**
     * Parse an xforms input (x:input) tag, which is used for free-entry questions, such as text, numbers, and dates. 
     * 
     * Format is like: <input ref="name"><label>What is your name?</label></input>
     */
    private void parseXformsInput(Element inputElement, HashMap<String, String> parsedQuestions) {
        String questionName = getFormElementName(inputElement);
        String questionValue = "";
        for (Node childNode = inputElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if ("xf:label".equals(childNode.getNodeName())) {
                    questionValue = getCharacterDataFromElement((Element)childNode);
                }
            }
        }
        parsedQuestions.put(questionName, questionValue);
    }

    /**
     * Parse an xforms select (x:select) tag, which is used for multiple-choice questions, either with
     * only one choice (xf:select1) or multiple choices (xf:select). 
     * 
     * Format is like: <select ref="symptoms"><label>What are your symptoms?</label><item><label/><value/></item>...</select>
     * 
     * @param isTopLevel Used to distinguish between top-level input or repeat group input
     */
    private void parseXformsSelect(Element selectElement, HashMap<String, String> parsedQuestions) {
        String parameter = getFormElementName(selectElement);
        String question = "";
        String values = "";

        for (Node childNode = selectElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if ("xf:label".equals(childNode.getNodeName())) {
                    question = getCharacterDataFromElement((Element)childNode);
                }
                else if ("xf:item".equals(childNode.getNodeName())) {
                    // items are of the form <item><label>Choice Label</label><value>Choice value</value></item>
                    Element itemElement = (Element)childNode;
                    String value = itemElement.getAttribute("id");

                    String option = "";
                    for (Node itemChild = itemElement.getFirstChild(); itemChild != null; itemChild = itemChild.getNextSibling()) {
                        if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                            if ("xf:label".equals(childNode.getNodeName())) {
                                option = getCharacterDataFromElement((Element)childNode);
                            }
                            else if ("xf:value".equals(childNode.getNodeName())) {
                                value = getCharacterDataFromElement((Element)childNode);
                            }
                        }
                    }

                    values += option + ":" + value + ";";
                }
            }
        }

        parsedQuestions.put(parameter, question + " - " + values);
    }

    private void createSurveyQuestions(int backendSurveyId, String xformData) throws SAXException, IOException,
            ParserConfigurationException {
        // check if survey exists in zebra
        Hashtable<String, String> backendQuestions = DatabaseHelpers.getZebraSurveyQuestions(backendSurveyId);
        HashMap<String, String> parsedQuestions = new HashMap<String, String>();

        // first parse the x-form
        Document xmlDocument = XmlHelpers.parseXml(xformData);
        xmlDocument.normalizeDocument();
        Element rootNode = xmlDocument.getDocumentElement();
        for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                // Purcforms group tag
                if ("xf:group".equals(childNode.getNodeName())) {
                    parseXformsGroup((Element)childNode, parsedQuestions);
                }
            }
        }

        // then check for deleted questions
        for (String key : backendQuestions.keySet()) {
            if (!parsedQuestions.containsKey(key)) {
                // this parameter was deleted during the design
                if (!DatabaseHelpers.surveyQuestionHasSubmissions(backendSurveyId, key)) {
                    DatabaseHelpers.deleteSurveyQuestion(backendSurveyId, key);
                }
            }
        }

        // and finally deal with add/update of questions
        for (Entry<String, String> question : parsedQuestions.entrySet()) {
            if (DatabaseHelpers.verifySurveyField(question.getKey(), backendSurveyId)) {
                if (!DatabaseHelpers.surveyQuestionHasSubmissions(backendSurveyId, question.getKey())) {
                    // compare the question values
                    if (!question.getValue().equals(backendQuestions.get(question.getKey()))) {
                        // update the question only
                        DatabaseHelpers.updateSurveyQuestion(question.getKey(), question.getValue(), backendSurveyId);
                    }
                }
            }
            else {
                // does not exist.
                DatabaseHelpers.saveZebraSurveyQuestions(backendSurveyId, question.getValue(), question.getKey());
            }
        }
    }

    public String getCharacterDataFromElement(Element element) {
        Node child = element.getFirstChild();
        if (child instanceof CharacterData) {
            return ((CharacterData)child).getData();
        }
        return "?";
    }
}
