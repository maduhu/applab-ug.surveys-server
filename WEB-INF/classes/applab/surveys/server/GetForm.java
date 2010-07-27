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
import java.util.*;

import org.w3c.dom.*;

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
public class GetForm extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            String surveyId = request.getParameter("surveyid");
            if (!surveyId.equals("")) {
                // get the survey name
                String surveyName = DatabaseHelpers.getSurveyName(surveyId);
                
                // get this survey from zebrasurvey
                String zebra_survey_id = DatabaseHelpers.getZebraSurveyId(surveyId);
                String xformData = DatabaseHelpers.getXformData(surveyId);
                if (xformData != null && xformData.length() > 0) {
                    // fixup Purcforms generated data with a start tag that includes the survey name
                    String designerGeneratedFormBeginning = "<xf:xforms xmlns:xf=\"http://www.w3.org/2002/xforms\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">";
                    String targetFormBeginning = "<xf:xforms xmlns:xf=\"http://www.w3.org/2002/xforms\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:jr=\"http://openrosa.org/javarosa\" xmlns:ev=\"http://www.w3.org/2001/xml-events\"><xf:head><xf:title>"
                            + surveyName + "</xf:title></xf:head>";
                    xformData = xformData.replaceFirst(designerGeneratedFormBeginning, targetFormBeginning);
                    
                    Document doc = XmlHelpers.parseXml(xformData);

                    NodeList xf_bind_nodes = doc.getElementsByTagName("xf:bind");

                    ArrayList<String> upload_element = new ArrayList<String>();
                    Hashtable<String, String> map = new Hashtable<String, String>();

                    for (int j = 0; j < xf_bind_nodes.getLength(); j++) {
                        Element xf_bind_element = (Element)xf_bind_nodes.item(j);
                        if (xf_bind_element.hasAttribute("format")) {
                            String attr_value = xf_bind_element.getAttribute("format");
                            if (attr_value.equalsIgnoreCase("gps")) {
                                xf_bind_element.setAttribute("type", "geopoint");
                            }
                            else if (attr_value.equalsIgnoreCase("image")) {
                                xf_bind_element.setAttribute("type", "binary");
                                upload_element.add(xf_bind_element.getAttribute("id"));
                                map.put(xf_bind_element.getAttribute("id"), "image");
                            }
                            else if (attr_value.equalsIgnoreCase("video")) {
                                xf_bind_element.setAttribute("type", "binary");
                                upload_element.add(xf_bind_element.getAttribute("id"));
                                map.put(xf_bind_element.getAttribute("id"), "video");
                            }
                            else if (attr_value.equalsIgnoreCase("audio")) {
                                xf_bind_element.setAttribute("type", "binary");
                                upload_element.add(xf_bind_element.getAttribute("id"));
                                map.put(xf_bind_element.getAttribute("id"), "audio");
                            }
                        }
                    }

                    Enumeration<String> keys = map.keys();
                    while (keys.hasMoreElements()) {
                        String key = keys.nextElement();
                        String value = map.get(key);
                        NodeList xf_input_nodes = doc.getElementsByTagName("xf:input");
                        for (int j = 0; j < xf_input_nodes.getLength(); j++) {
                            Element xf_input_element = (Element)xf_input_nodes.item(j);
                            String bind_attr_value = xf_input_element.getAttribute("bind");
                            if (key.equalsIgnoreCase(bind_attr_value)) {
                                if (value.equalsIgnoreCase("image")) {
                                    xf_input_element.setAttribute("mediatype", "image/*");
                                    xf_input_element.setAttribute("size", "2345");
                                }
                                else if (value.equalsIgnoreCase("video")) {
                                    xf_input_element.setAttribute("mediatype", "video/*");
                                }
                                else if (value.equalsIgnoreCase("audio")) {
                                    xf_input_element.setAttribute("mediatype", "audio/*");
                                }
                                doc.renameNode(xf_input_element, "", "xf:upload");
                            }
                        }
                    }

                    NodeList node_1 = doc.getElementsByTagName("xf:xforms");
                    for (int i = 0; i < node_1.getLength(); i++) {
                        Element element_1 = (Element)node_1.item(i);

                        NodeList node_2 = element_1.getElementsByTagName("xf:model");
                        for (int j = 0; j < node_2.getLength(); j++) {
                            Element element_2 = (Element)node_2.item(j);

                            Node bind_handset_id = doc.createElement("xf:bind");
                            NamedNodeMap handset_id_attr = bind_handset_id.getAttributes();

                            Attr nodeset_handset_id = doc.createAttribute("nodeset");
                            nodeset_handset_id.setValue("/new_form1/handset_id");
                            handset_id_attr.setNamedItem(nodeset_handset_id);

                            Attr type_handset_id = doc.createAttribute("type");
                            type_handset_id.setValue("string");
                            handset_id_attr.setNamedItem(type_handset_id);

                            Attr jr_handset_id = doc.createAttribute("jr:preload");
                            jr_handset_id.setValue("property");
                            handset_id_attr.setNamedItem(jr_handset_id);

                            Attr jr_preload_handset_id = doc.createAttribute("jr:preloadParams");
                            jr_preload_handset_id.setValue("deviceid");
                            handset_id_attr.setNamedItem(jr_preload_handset_id);

                            Attr id_handset_id = doc.createAttribute("id");
                            id_handset_id.setValue("handset_id");
                            handset_id_attr.setNamedItem(id_handset_id);

                            element_2.appendChild(bind_handset_id);

                            Node bind_handset_submit_time = doc.createElement("xf:bind");
                            NamedNodeMap handset_submit_time_attr = bind_handset_submit_time.getAttributes();

                            Attr nodeset_handset_submit_time = doc.createAttribute("nodeset");
                            nodeset_handset_submit_time.setValue("/new_form1/handset_submit_time");
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

                            element_2.appendChild(bind_handset_submit_time);

                            NodeList node_3 = element_2.getElementsByTagName("xf:instance");
                            for (int x = 0; x < node_3.getLength(); x++) {
                                Element element_3 = (Element)node_3.item(x);
                                NodeList node_4 = element_3.getElementsByTagName("new_form1");
                                for (int y = 0; y < node_4.getLength(); y++) {
                                    Element element_4 = (Element)node_4.item(y);
                                    Node handset_id = doc.createElement("handset_id");
                                    Node handset_submit_time = doc.createElement("handset_submit_time");
                                    Node interviewee_name = doc.createElement("interviewee_name");
                                    Node location = doc.createElement("location");
                                    Node xform_survey_id = doc.createElement("survey_id");
                                    xform_survey_id.setTextContent(zebra_survey_id);
                                    element_4.appendChild(handset_id);
                                    element_4.appendChild(handset_submit_time);
                                    element_4.appendChild(interviewee_name);
                                    element_4.appendChild(location);
                                    element_4.appendChild(xform_survey_id);
                                }
                            }
                        }
                    }

                    String xmlString = XmlHelpers.exportAsString(doc);
                    System.out.println(xmlString);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
