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

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;
import org.xml.sax.*;

public class surveyList extends HttpServlet {
	
	
	protected void doGet(HttpServletRequest request ,HttpServletResponse response)
	{
		try
		{
			String survey_id=request.getParameter("surveyid");
			if(!survey_id.equals(""))
			{
				//get the survey name
				String survey_name=configuration.DbConnect.getSurveyName(survey_id);
				// get this survey from zebrasurvey
				String zebra_survey_id=configuration.DbConnect.getZebraSurveyId(survey_id);
				String xform_data=configuration.DbConnect.getXformData(survey_id);
				if(!xform_data.equals("") || !xform_data.equals(null))
				{
					//replace strings
					String replaceStr="<xf:xforms xmlns:xf=\"http://www.w3.org/2002/xforms\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">";
					String addStr="<xf:xforms xmlns:xf=\"http://www.w3.org/2002/xforms\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:jr=\"http://openrosa.org/javarosa\" xmlns:ev=\"http://www.w3.org/2001/xml-events\"><xf:head><xf:title>"+survey_name+"</xf:title></xf:head>";
					String parsedStr=xform_data.replaceFirst(replaceStr, addStr);				
					System.out.println(parsedStr);
					DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
					DocumentBuilder db=dbf.newDocumentBuilder();
					InputSource is=new InputSource();
					is.setCharacterStream(new StringReader(parsedStr));
					Document doc=db.parse(is);
					Node xform=doc.getFirstChild();
					Node handset_id=doc.createElement("new_form1");
					xform.appendChild(handset_id);
					Transformer transformer=TransformerFactory.newInstance().newTransformer();
					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
					StreamResult result=new StreamResult(new StringWriter());
					DOMSource source=new DOMSource(doc);
					transformer.transform(source, result);
					String xmlString=result.getWriter().toString();
					System.out.println(xmlString);
					//Node handset_id=doc.createElement("new_form1");
					
					/*
					NodeList node_1=doc.getElementsByTagName("xf:xforms");
					
					for(int i=0;i<node_1.getLength();i++)
					{
						Element element_1=(Element)node_1.item(i);
						NodeList node_2=element_1.getElementsByTagName("xf:model");
						for(int j=0;j<node_2.getLength();j++)
						{
							Element element_2=(Element)node_2.item(j);
							NodeList node_3=element_2.getElementsByTagName("xf:instance");
							for(int x=0;x<node_3.getLength();x++)
							{
								Element element_3=(Element)node_3.item(x);
								NodeList node_4=element_3.getElementsByTagName("new_form1");
								for(int y=0;y<node_4.getLength();y++)
								{
									Element element_4=(Element)node_4.item(y);
									//Node handset_id=node_4.
									System.out.println(element_4.getNodeName());
									//NodeList node_5=element_4.getChildNodes();
									//System.out.println(node_5.getLength());
								}								
							}
						}
					}
					/*
					String str_split[]=parsedStr.split("<xf:");
					for(int i=0;i<str_split.length;i++)
					{
						if(str_split[i].contains("format=\"image\""))
						{
							parsedStr=this.replaceString(str_split[i], str_split, parsedStr,"image");							
						}
						else if(str_split[i].contains("format=\"video\""))
						{
							parsedStr=this.replaceString(str_split[i], str_split, parsedStr,"video");
						}
						else if(str_split[i].contains("format=\"audio\""))
						{
							parsedStr=this.replaceString(str_split[i], str_split, parsedStr,"audio");
						}
					}
					parsedStr=parsedStr.replaceAll("bind=", "ref=");
					String add_xform_data_param1="formKey=\"new_form1\">" +
							"\n<handset_id/>" +
							"\n<handset_submit_time/>" +
							"\n<survey_id>"+zebra_survey_id+"</survey_id>" +
							"\n<interviewer_id/>"+
							"\n<location/>";
					String replace_xform_str_1="formKey=\"new_form1\">";
					String parsed_xform_str2=parsedStr.replaceFirst(replace_xform_str_1, add_xform_data_param1);
					
					String add_xform_data_param2="</xf:instance>" +
							"\n\t<xf:bind nodeset=\"/new_form1/handset_id\" type=\"string\" jr:preload=\"property\" jr:preloadParams=\"deviceid\"/>" +
							"\n\t<xf:bind nodeset=\"/new_form1/handset_submit_time\" type=\"dateTime\" jr:preload=\"timestamp\" jr:preloadParams=\"end\"/>"+
							"\n\t<xf:bind nodeset=\"/new_form1/interviewer_id\" type=\"string\" required=\"true()\"/>"+							
							"\n\t<xf:bind nodeset=\"/new_form1/location\" type=\"geopoint\" />";
					String replace_xform_str_2="</xf:instance>";
					String parsed_xform_str3=parsed_xform_str2.replaceFirst(replace_xform_str_2, add_xform_data_param2);
					
					String add_xform_data_param3="</xf:label>" +
							"\n\t<xf:input ref=\"interviewer_id\">" +
							"\n\t\t<xf:label>Interviewee Name or ID</xf:label>" +
							"\n\t</xf:input>";
					
					String replace_xform_str3="</xf:label>";
					
					String parsed_xform_str4=parsed_xform_str3.replaceFirst(replace_xform_str3, add_xform_data_param3);
					
					//check for gps field
					
					String replace_xform_str_gps="type=\"xsd:string\" format=\"gps\"";
					String add_xform_data_param4="type=\"geopoint\"";
					String parsed_xform_str5_=parsed_xform_str4.replaceAll(replace_xform_str_gps, add_xform_data_param4);
					
					String replace_xform_str_gps_="format=\"gps\" type=\"xsd:string\"";
					String add_xform_data_param4_="type=\"geopoint\"";
					String parsed_xform_str5=parsed_xform_str5_.replaceAll(replace_xform_str_gps_, add_xform_data_param4_);
					
					
					
					//check for image field					
					
					String replace_xform_str_img="type=\"xsd:base64Binary\" format=\"image\"";
					String add_xform_data_param5="type=\"base64Binary\"";
					String parsed_xform_str6=parsed_xform_str5.replaceAll(replace_xform_str_img, add_xform_data_param5);
									
					
					//check for video fields
					String replace_xform_str_video="type=\"xsd:base64Binary\" format=\"video\"";
					String add_xform_data_param6="type=\"base64Binary\"";
					String parsed_xform_str7=parsed_xform_str6.replaceAll(replace_xform_str_video, add_xform_data_param6);
					
					//check for audio fields
					String replace_xform_str_audio="type=\"xsd:base64Binary\" format=\"audio\"";
					String add_xform_data_param7="type=\"base64Binary\"";
					String parsed_xform_str8=parsed_xform_str7.replaceAll(replace_xform_str_audio, add_xform_data_param7);
					
					String replace_xform_str_grp="</xf:group>"+
													"\n</xf:xforms>";
					String add_xform_str_grp="<xf:input ref=\"location\">"+ 
												"\n\t<xf:label>Enter the location</xf:label>"+ 
												"\n\t</xf:input>"+
												"\n\t</xf:group>"+
												"\n\t</xf:xforms>";
					String parsed_xform_str9=parsed_xform_str8.replaceAll(replace_xform_str_grp, add_xform_str_grp);
					
					System.out.println(parsed_xform_str9);
					
					//response.getWriter().write(parsed_xform_str9);
					 * 
					 */
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	protected String replaceString(String search_str,String str_split[],String parsed_str,String type)
	{
		String lookup="bind id=\"";
		String lookup_2="\" nodeset=\"";
		int get_str_position=search_str.indexOf(lookup_2);
		String new_str=search_str.substring(lookup.length(),get_str_position);
		String lookup_replace_str_1="input bind=\""+new_str+"\"";
		for(int j=0;j<str_split.length;j++)
		{
			if(str_split[j].contains(lookup_replace_str_1))
			{
				//add upload to input
				String add_str_upload="";
				String add_str_upload2="upload";
				if(type.equals("image"))
				{
					add_str_upload="upload mediatype=\"image/*\" size=\"2345\"";
				}
				else if(type.equals("video"))
				{
					add_str_upload="upload mediatype=\"video/*\"";
				}
				else if(type.equals("audio"))
				{
					add_str_upload="upload mediatype=\"audio/*\"";
				}
				String replace_str_input="input";
				String new_str_upload=str_split[j].replaceAll(replace_str_input, add_str_upload);									
				String new_str_upload2=str_split[j+1].replaceAll(replace_str_input, add_str_upload2);									
				parsed_str=parsed_str.replaceAll(str_split[j], new_str_upload);
				parsed_str=parsed_str.replaceAll(str_split[j+1], new_str_upload2);
			}
		}
		return parsed_str;
	}
	
	public String getCharacterDataFromElement(Element element)
	{
		Node child=element.getFirstChild();
		if(child instanceof CharacterData)
		{
			CharacterData cd=(CharacterData)child;
			return cd.getData();
		}
		return "?";
	}
}
