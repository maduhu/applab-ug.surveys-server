package applab.surveys.server;

/*

 Copyright (C) 2010 Grameen Foundation
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy of
 the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 License for the specific language governing permissions and limitations under
 the License.
 */

import javax.servlet.http.*;
import org.w3c.dom.*;

import java.io.*;
import java.util.*;

public class saveXform extends HttpServlet {
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

    private void parseXformsGroup(Element groupNode, HashMap<String, String> parsedQuestions) {
        for (Node childNode = groupNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            Element element = (Element)childNode;
            NodeList nodes_2 = element.getElementsByTagName("xf:input");
            NodeList nodes_3 = element.getElementsByTagName("xf:select1");
            NodeList nodes_4 = element.getElementsByTagName("xf:select");
            if (nodes_2.getLength() > 0) {
                for (int j = 0; j < nodes_2.getLength(); j++) {
                    Element element_2 = (Element)nodes_2.item(j);
                    String parameter = element_2.getAttribute("bind");
                    NodeList nodes_2_1 = element_2.getElementsByTagName("xf:label");
                    Element element_2_1 = (Element)nodes_2_1.item(0);
                    String question = this.getCharacterDataFromElement(element_2_1);
                    parsedQuestions.put(parameter, question);
                    //paramQtn.add(parameter);
                }
            }
            if (nodes_3.getLength() > 0) {
                for (int j = 0; j < nodes_3.getLength(); j++) {
                    Element element_2 = (Element)nodes_3.item(j);
                    String parameter = element_2.getAttribute("bind");
                    NodeList nodes_3_1 = element_2.getElementsByTagName("xf:label");
                    Element element_2_1 = (Element)nodes_3_1.item(0);
                    String question = this.getCharacterDataFromElement(element_2_1);
                    NodeList nodes_3_2 = element_2.getElementsByTagName("xf:item");
                    String items = "";
                    for (int x = 0; x < nodes_3_2.getLength(); x++) {
                        Element element_2_3 = (Element)nodes_3_2.item(x);
                        NodeList nodes_3_3 = element_2_3.getElementsByTagName("xf:label");
                        NodeList nodes_3_4 = element_2_3.getElementsByTagName("xf:value");
                        Element element_2_4 = (Element)nodes_3_3.item(0);
                        Element element_2_5 = (Element)nodes_3_4.item(0);
                        String option = this.getCharacterDataFromElement(element_2_4);
                        String value = this.getCharacterDataFromElement(element_2_5);
                        if (items.length() == 0) {
                            items = option + ":" + value + ";" + items;
                        }
                        else {
                            items = items + "" + option + ":" + value + ";";
                        }
                    }
                    parsedQuestions.put(parameter, question + " - " + items);
                    //paramQtn.add(parameter);
                }
            }
            if (nodes_4.getLength() > 0) {
                for (int j = 0; j < nodes_4.getLength(); j++) {
                    Element element_2 = (Element)nodes_4.item(j);
                    String parameter = element_2.getAttribute("bind");
                    NodeList nodes_4_1 = element_2.getElementsByTagName("xf:label");
                    Element element_2_1 = (Element)nodes_4_1.item(0);
                    String question = this.getCharacterDataFromElement(element_2_1);
                    NodeList nodes_3_2 = element_2.getElementsByTagName("xf:item");
                    String items = "";
                    for (int x = 0; x < nodes_3_2.getLength(); x++) {
                        Element element_2_3 = (Element)nodes_3_2.item(x);
                        NodeList nodes_3_3 = element_2_3.getElementsByTagName("xf:label");
                        NodeList nodes_3_4 = element_2_3.getElementsByTagName("xf:value");
                        Element element_2_4 = (Element)nodes_3_3.item(0);
                        Element element_2_5 = (Element)nodes_3_4.item(0);
                        String option = this.getCharacterDataFromElement(element_2_4);
                        String value = this.getCharacterDataFromElement(element_2_5);
                        if (items.length() == 0) {
                            items = option + ":" + value + ";" + items;
                        }
                        else {
                            items = items + "" + option + ":" + value + ";";
                        }
                    }
                    parsedQuestions.put(parameter, question + " - " + items);
                    //paramQtn.add(parameter);
                }
            }
        }
    }

    private void createSurveyQuestions(int backendSurveyId, String xformData) {
        try {
            // check if survey exists in zebra
            Hashtable<String, String> zebraQuestions = DatabaseHelpers.getZebraSurveyQuestions(backendSurveyId);
            HashMap<String, String> saveQuestions = new HashMap<String, String>();
            ArrayList<String> paramQtn = new ArrayList<String>();
            Document xmlDocument = XmlHelpers.parseXml(xformData);
            xmlDocument.normalizeDocument();

            Element rootNode = xmlDocument.getDocumentElement();
            for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    // Purcforms group tag
                    if ("xf:group".equals(childNode.getNodeName())) {
                        parseXformsGroup((Element)childNode, saveQuestions);
                    }
                }
            }

            Enumeration<String> oldKeys = zebraQuestions.keys();
            while (oldKeys.hasMoreElements()) {
                String key = oldKeys.nextElement();
                int cursor = 0;
                for (int i = 0; i < paramQtn.size(); i++) {
                    if (key.equals(paramQtn.get(i))) {
                        cursor++;
                    }
                }
                if (cursor == 0) {
                    // this parameter was deleted during the design
                    if (!DatabaseHelpers.surveyQuestionHasSubmissions(backendSurveyId, key)) {
                        DatabaseHelpers.deleteSurveyQuestion(backendSurveyId, key);
                    }
                }
            }
            for (String key : saveQuestions.keySet()) {
                if (DatabaseHelpers.verifySurveyField(key, backendSurveyId)) {
                    if (!DatabaseHelpers.surveyQuestionHasSubmissions(backendSurveyId, key)) {
                        // compare the questions
                        if (!saveQuestions.get(key).equals(zebraQuestions.get(key))) {
                            // update the question only
                            DatabaseHelpers.updateSurveyQuestion(key, saveQuestions.get(key), backendSurveyId);
                        }
                    }
                }
                else {
                    // does not exist.
                    DatabaseHelpers.saveZebraSurveyQuestions(backendSurveyId, saveQuestions.get(key), key);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getCharacterDataFromElement(Element element) {
        Node child = element.getFirstChild();
        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData)child;
            return cd.getData();
        }
        return "?";
    }

}
