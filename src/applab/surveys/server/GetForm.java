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

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import applab.server.ApplabServlet;
import applab.server.DatabaseTable;
import applab.server.SelectCommand;
import applab.server.ServletRequestContext;
import applab.server.XmlHelpers;

/**
 * Server method that returns the XML for a survey
 * 
 * We require a transform here since the survey designer saves data in a different format than the mobile client
 * expects. We hope to push this transform to the designer->save path over time.
 * 
 * Input: survey id (provided in the surveyid URL parameter)
 * 
 * Output: xform data to be used by a mobile client
 * 
 */
public class GetForm extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    public void doApplabGet(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {
        String salesforceSurveyId = request.getParameter("surveyid");

        String surveyFormXml = getSurveyForm(salesforceSurveyId);
        if (surveyFormXml == null) {
            // bad parameter, respond that the form was not found
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Survey ID does not exist: " + salesforceSurveyId);
        }
        else {
            log(surveyFormXml);
            response.getWriter().write(surveyFormXml);
        }
    }

    /**
     * Given the salesforce Id of a survey, fetch the XML for that survey
     * @throws ClassNotFoundException 
     * @throws SQLException 
     * 
     */
    private static String getSurveyForm(String salesforceSurveyId) throws TransformerException, SAXException, IOException,
            ParserConfigurationException, SQLException, ClassNotFoundException {
        if (salesforceSurveyId == null || salesforceSurveyId.isEmpty()) {
            return null;
        }
        String formData;
        String surveyId;
        
        SelectCommand selectCommand = new SelectCommand(DatabaseTable.Survey);
        try {
            selectCommand.addField("xform");
            selectCommand.addField("survey_name");
            selectCommand.addField("id");
            selectCommand.whereEquals("survey_id", "'" + salesforceSurveyId + "'");
            ResultSet resultSet = selectCommand.execute();
            if (resultSet.next()) {
                formData = resultSet.getString("xform");
                surveyId = resultSet.getString("id");
            }
            else {
                return null;
            }
        }
        finally {
            selectCommand.dispose();
        }

        if (formData == null || formData.isEmpty()) {
            return null;
        }

        Document doc = XmlHelpers.parseXml(formData);
        doc.normalizeDocument();
        Element rootNode = doc.getDocumentElement();
        for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE && childNode.getLocalName().equals("head")) {
                return insertNodes((Element) childNode, surveyId, doc);
            }
        }
        
        // If we dont add anything in return the form as is
        return XmlHelpers.exportAsString(doc);
    }

    private static String insertNodes(Element headElement, String surveyId, Document doc)
            throws TransformerException {
    
        for (Node childNode = headElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE && childNode.getLocalName().equals("model")) {
                Element modelElement = (Element) childNode;
                String formName = "";
                for (Node childModelNode = modelElement.getFirstChild(); childModelNode != null; childModelNode = childModelNode.getNextSibling()) {
                    if (childModelNode.getNodeType() == Node.ELEMENT_NODE && childModelNode.getLocalName().equals("instance")) {
                        Element instanceElement = (Element) childModelNode;
                        formName = instanceElement.getAttribute("id");
                        for (Node childInstanceNode = instanceElement.getFirstChild(); childInstanceNode != null; childInstanceNode = childInstanceNode.getNextSibling()) {
                             if (childInstanceNode.getNodeType() == Node.ELEMENT_NODE && childInstanceNode.getLocalName().equals(formName)) {
                                 Element formElement = (Element) childInstanceNode; 
                                 //remove style attribute if present, and also any empty xmlns attributes
                                 for (int i = 0; i < formElement.getChildNodes().getLength(); i++) {
                                     Node dataNode = formElement.getChildNodes().item(i);
                                     if (dataNode.getNodeType() == Node.ELEMENT_NODE && ((Element)dataNode).hasAttribute("style")) {
                                           ((Element)dataNode).removeAttribute("style");
                                     }
                                     if (dataNode.getNodeType() == Node.ELEMENT_NODE && ((Element)dataNode).hasAttribute("xmlns")) {
                                         if (((Element)dataNode).getAttribute("xmlns").equals("")) {
                                             ((Element)dataNode).removeAttribute("xmlns");
                                         }
                                     }
                                  }
                                 // Create the new nodes for the instance model
                                 Node handsetSubmitTime = doc.createElement("handset_submit_time");
                                 Node formSurveyId = doc.createElement("survey_id");
                                 formSurveyId.setTextContent(surveyId);
                                 formElement.appendChild(handsetSubmitTime);
                                 formElement.appendChild(formSurveyId);
                             }
                        }
                    }
                    //sometimes the xmlns attribute shows up on the bind nodes, and this is not necessary
                    else if (childModelNode.getNodeType() == Node.ELEMENT_NODE && childModelNode.getLocalName().equals("bind")) {
                        if (((Element)childModelNode).hasAttribute("xmlns")) {
                            ((Element)childModelNode).removeAttribute("xmlns");
                        }
                    }
                }
                Node bindHandsetSubmitTime = doc.createElement("bind");
                NamedNodeMap handsetSubmitTimeAttr = bindHandsetSubmitTime.getAttributes();

                Attr nodesetHandsetSubmitTime = doc.createAttribute("nodeset");
                nodesetHandsetSubmitTime.setValue("/" + formName + "/handset_submit_time");
                handsetSubmitTimeAttr.setNamedItem(nodesetHandsetSubmitTime);

                Attr typeHandsetSubmitTime = doc.createAttribute("type");
                typeHandsetSubmitTime.setValue("dateTime");
                handsetSubmitTimeAttr.setNamedItem(typeHandsetSubmitTime);

                Attr jrHandsetSubmitTime = doc.createAttribute("jr:preload");
                jrHandsetSubmitTime.setValue("timestamp");
                handsetSubmitTimeAttr.setNamedItem(jrHandsetSubmitTime);

                Attr jrPreloadHandsetSubmitTime = doc.createAttribute("jr:preloadParams");
                jrPreloadHandsetSubmitTime.setValue("end");
                handsetSubmitTimeAttr.setNamedItem(jrPreloadHandsetSubmitTime);

                Attr idHandsetSubmitTime = doc.createAttribute("id");
                idHandsetSubmitTime.setValue("handset_submit_time");
                handsetSubmitTimeAttr.setNamedItem(idHandsetSubmitTime);

                modelElement.appendChild(bindHandsetSubmitTime);
            }
        }
        return XmlHelpers.exportAsString(doc);
    }
}
