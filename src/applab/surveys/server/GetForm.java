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
import org.w3c.dom.NodeList;
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
        String xformData;
        String zebra_survey_id;
        
        SelectCommand selectCommand = new SelectCommand(DatabaseTable.Survey);
        try {
            selectCommand.addField("xform");
            selectCommand.addField("survey_name");
            selectCommand.addField("id");
            selectCommand.whereEquals("survey_id", "'" + salesforceSurveyId + "'");
            ResultSet resultSet = selectCommand.execute();
            if (resultSet.next()) {
                xformData = resultSet.getString("xform");
                zebra_survey_id = resultSet.getString("id");
            }
            else {
                return null;
            }
        }
        finally {
            selectCommand.dispose();
        }

        if (xformData == null || xformData.isEmpty()) {
            return null;
        }

        Document doc = XmlHelpers.parseXml(xformData);
        
        NodeList node_1 = doc.getElementsByTagName("h:html");
        for (int i = 0; i < node_1.getLength(); i++) {
            Element element_1 = (Element)node_1.item(i);

            NodeList node_2 = element_1.getElementsByTagName("h:head");
            for (int j = 0; j < node_2.getLength(); j++) {
                Element element_2 = (Element)node_2.item(j);
                NodeList node_3 = element_2.getElementsByTagName("model");
                String prefix = "";
                if (node_3.getLength() == 0) {
                    node_3 = element_2.getElementsByTagName("xf:model");
                    prefix = "xf:";
                }
                for (int k = 0; k < node_3.getLength(); k++) {
                    Element element_3 = (Element)node_3.item(k);

                    NodeList node_4 = element_3.getElementsByTagName(prefix + "instance");
                
                    String formName = "";
                
                    for (int x = 0; x < node_4.getLength(); x++) {
                        Element element_4 = (Element)node_4.item(x);
                        formName = element_4.getAttribute("id");
                        NodeList node_5 = element_4.getElementsByTagName(formName);
                        for (int y = 0; y < node_5.getLength(); y++) {
                            Element element_5 = (Element)node_5.item(y);
                            Node handset_submit_time = doc.createElement("handset_submit_time");
                            Node xform_survey_id = doc.createElement("survey_id");
                            xform_survey_id.setTextContent(zebra_survey_id);
                            element_5.appendChild(handset_submit_time);
                            element_5.appendChild(xform_survey_id);
                        }
                    }
                
                    Node bind_handset_submit_time = doc.createElement("bind");
                    NamedNodeMap handset_submit_time_attr = bind_handset_submit_time.getAttributes();

                    Attr nodeset_handset_submit_time = doc.createAttribute("nodeset");
                    nodeset_handset_submit_time.setValue("/" + formName + "/handset_submit_time");
                    handset_submit_time_attr.setNamedItem(nodeset_handset_submit_time);

                    Attr type_handset_submit_time = doc.createAttribute("type");
                    type_handset_submit_time.setValue("dateTime");
                    handset_submit_time_attr.setNamedItem(type_handset_submit_time);

                    Attr jr_handset_submit_time = doc.createAttribute("jr:preload");
                    jr_handset_submit_time.setValue("timestamp");
                    handset_submit_time_attr.setNamedItem(jr_handset_submit_time);

                    Attr jr_preload_handset_submit_time = doc.createAttribute("jr:preloadParams");
                    jr_preload_handset_submit_time.setValue("end");
                    handset_submit_time_attr.setNamedItem(jr_preload_handset_submit_time);

                    Attr id_handset_submit_time = doc.createAttribute("id");
                    id_handset_submit_time.setValue("handset_submit_time");
                    handset_submit_time_attr.setNamedItem(id_handset_submit_time);

                    element_3.appendChild(bind_handset_submit_time);
                }
            }
        }
        return XmlHelpers.exportAsString(doc);
    }
}
