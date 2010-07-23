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
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import java.util.*;

public class saveXform extends HttpServlet{

	public void doPost(HttpServletRequest request,HttpServletResponse response) throws IOException
	{
		String survey_id=request.getParameter("formId");
		BufferedReader reader=new BufferedReader(new InputStreamReader(request.getInputStream()));
		StringBuilder sb=new StringBuilder();
		String line=null;
		while((line=reader.readLine())!=null)
		{
			sb.append(line+"\n");
		}
		request.getInputStream().close();
		String xform_data1=sb.toString();
        String xform_data = configuration.manipulation.replace(xform_data1, "\'", "\\'");
		System.out.println(configuration.manipulation.replace(xform_data,"'","\\'"));
		//get the survey name
		try
		{
			configuration.sfConnect.login();
			String surveyName=configuration.sfConnect.getSurveyName(survey_id);
			//check if id exists in zebrasurvey
			if(configuration.DbConnect.zebraSurveyIdExists(survey_id))
			{
				//System.out.println(xform_data);
				configuration.DbConnect.saveXform(survey_id,surveyName,xform_data);
				//on saving check with zebrasurveyquestions
				int zebra_survey_id=Integer.parseInt(configuration.DbConnect.getZebraSurveyId(survey_id));
				//configuration.DbConnect.deleteSurveyFromSurveyQuestions(zebra_survey_id);
				this.createSurveyQuestions(zebra_survey_id, xform_data);
			}
			else
			{
				if(configuration.sfConnect.surveyIdExists(survey_id))
				{
					String creation_date=configuration.applabConfig.getDateTime();
					configuration.DbConnect.saveXform(survey_id, xform_data, surveyName, creation_date);
					//on saving
					int zebra_survey_id=Integer.parseInt(configuration.DbConnect.getZebraSurveyId(survey_id));
					this.createSurveyQuestions(zebra_survey_id, xform_data);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	protected void createSurveyQuestions(int zebra_survey_id,String xform_data)
	{
		try
		{
			//check if survey exists in zebra
			Hashtable<String,String> zebraQuestions=configuration.DbConnect.getZebraSurveyQuestions(zebra_survey_id);
			Hashtable<String,String> saveQuestions=new Hashtable<String,String>();
			ArrayList<String> paramQtn=new ArrayList<String>();
			DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
			DocumentBuilder db=dbf.newDocumentBuilder();
			InputSource is=new InputSource();
			is.setCharacterStream(new StringReader(xform_data));
			Document doc=db.parse(is);
			NodeList nodes=doc.getElementsByTagName("xf:group");
			for(int i=0;i<nodes.getLength();i++)
			{
				Element element=(Element)nodes.item(i);
				NodeList nodes_2=element.getElementsByTagName("xf:input");
				NodeList nodes_3=element.getElementsByTagName("xf:select1");
				NodeList nodes_4=element.getElementsByTagName("xf:select");
				if(nodes_2.getLength()>0)
				{
					for(int j=0;j<nodes_2.getLength();j++)
					{
						Element element_2=(Element)nodes_2.item(j);
						String parameter=element_2.getAttribute("bind");						
						NodeList nodes_2_1=element_2.getElementsByTagName("xf:label");
						Element element_2_1=(Element)nodes_2_1.item(0);
						String question=this.getCharacterDataFromElement(element_2_1);
						saveQuestions.put(parameter, question);
						paramQtn.add(parameter);
					}
				}
				if(nodes_3.getLength()>0)
				{
					for(int j=0;j<nodes_3.getLength();j++)
					{
						Element element_2=(Element)nodes_3.item(j);
						String parameter=element_2.getAttribute("bind");						
						NodeList nodes_3_1=element_2.getElementsByTagName("xf:label");
						Element element_2_1=(Element)nodes_3_1.item(0);
						String question=this.getCharacterDataFromElement(element_2_1);
						NodeList nodes_3_2=element_2.getElementsByTagName("xf:item");
						String items="";
						for(int x=0;x<nodes_3_2.getLength();x++)
						{
							Element element_2_3=(Element)nodes_3_2.item(x);
							NodeList nodes_3_3=element_2_3.getElementsByTagName("xf:label");
							NodeList nodes_3_4=element_2_3.getElementsByTagName("xf:value");
							Element element_2_4=(Element)nodes_3_3.item(0);
							Element element_2_5=(Element)nodes_3_4.item(0);
							String option=this.getCharacterDataFromElement(element_2_4);
							String value=this.getCharacterDataFromElement(element_2_5);
							if(items.length()==0)
							{
								items=option+":"+value+";"+items;
							}
							else
							{
								items=items+""+option+":"+value+";";
							}
						}
						saveQuestions.put(parameter, question+" - "+items);
						paramQtn.add(parameter);
					}
				}
				if(nodes_4.getLength()>0)
				{
					for(int j=0;j<nodes_4.getLength();j++)
					{
						Element element_2=(Element)nodes_4.item(j);
						String parameter=element_2.getAttribute("bind");
						NodeList nodes_4_1=element_2.getElementsByTagName("xf:label");
						Element element_2_1=(Element)nodes_4_1.item(0);
						String question=this.getCharacterDataFromElement(element_2_1);
						NodeList nodes_3_2=element_2.getElementsByTagName("xf:item");
						String items="";
						for(int x=0;x<nodes_3_2.getLength();x++)
						{
							Element element_2_3=(Element)nodes_3_2.item(x);
							NodeList nodes_3_3=element_2_3.getElementsByTagName("xf:label");
							NodeList nodes_3_4=element_2_3.getElementsByTagName("xf:value");
							Element element_2_4=(Element)nodes_3_3.item(0);
							Element element_2_5=(Element)nodes_3_4.item(0);
							String option=this.getCharacterDataFromElement(element_2_4);
							String value=this.getCharacterDataFromElement(element_2_5);
							if(items.length()==0)
							{
								items=option+":"+value+";"+items;
							}
							else
							{
								items=items+""+option+":"+value+";";
							}
						}
						saveQuestions.put(parameter, question+" - "+items);
						paramQtn.add(parameter);
					}
				}
			}
			Enumeration<String> oldKeys=zebraQuestions.keys();
			while(oldKeys.hasMoreElements())
			{
				String key=oldKeys.nextElement();
				int cursor=0;
				for(int i=0;i<paramQtn.size();i++)
				{
					if(key.equals(paramQtn.get(i)))
					{
						cursor++;
					}
				}
				if(cursor==0)
				{
					//this parameter was deleted during the design					
					if(!configuration.DbConnect.surveyQuestionHasSubmissions(zebra_survey_id, key))
					{
						configuration.DbConnect.deleteSurveyQuestion(zebra_survey_id, key);
					}
				}
			}			
			Enumeration<String> keys=saveQuestions.keys();
			while(keys.hasMoreElements())
			{
				String key=keys.nextElement();
				if(configuration.DbConnect.verifySurveyField(key, zebra_survey_id))
				{
					if(!configuration.DbConnect.surveyQuestionHasSubmissions(zebra_survey_id, key))
					{
						//compare the questions
						if(!saveQuestions.get(key).equals(zebraQuestions.get(key)))
						{
							//update the question only
							configuration.DbConnect.updateSurveyQuestion(key, saveQuestions.get(key), zebra_survey_id);
						}
					}
				}
				else
				{
					//does not exist.
					configuration.DbConnect.saveZebraSurveyQuestions(zebra_survey_id, saveQuestions.get(key), key);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
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
