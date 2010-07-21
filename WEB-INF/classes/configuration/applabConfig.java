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
package configuration;
import java.util.*;
import java.text.*;
import java.io.*;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

public class applabConfig {

	Hashtable<String,String> keyvalues;
	
	public applabConfig()
	{
		try
		{
			keyvalues=new Hashtable<String,String>();
			
			File temp=new File("../webapps/zebra/WEB-INF/classes/configuration/application.xml");			
			DocumentBuilder builder=DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc=builder.parse(temp);
			doc.getDocumentElement().normalize();
			NodeList nodeList=doc.getElementsByTagName("appsettings");
			//get the first node which is the appsettings node
			Node app_node=nodeList.item(0);
			//convert the node into an element
			Element app_element=(Element)app_node;
			
			//zebra username
			NodeList zbr_user_list=app_element.getElementsByTagName("zebra-username");
			Element zbr_user_element=(Element)zbr_user_list.item(0);
			NodeList zbr_user_list_level1=zbr_user_element.getChildNodes();
			keyvalues.put("zebra-username", ((Node)zbr_user_list_level1.item(0)).getNodeValue());
			
			//zebra password
			NodeList zbr_pwd_list=app_element.getElementsByTagName("zebra-password");
			Element zbr_pwd_element=(Element)zbr_pwd_list.item(0);
			NodeList zbr_pwd_list_level1=zbr_pwd_element.getChildNodes();
			keyvalues.put("zebra-password", ((Node)zbr_pwd_list_level1.item(0)).getNodeValue());
			
			//zebra ip url
			NodeList zbr_url_list=app_element.getElementsByTagName("zebra-url");
			Element zbr_url_element=(Element)zbr_url_list.item(0);
			NodeList zbr_url_list_level1=zbr_url_element.getChildNodes();
			keyvalues.put("zebra-url", ((Node)zbr_url_list_level1.item(0)).getNodeValue());		
			
			//salesforce username
			NodeList sf_username_list=app_element.getElementsByTagName("salesforce-username");
			Element sf_username_element=(Element)sf_username_list.item(0);
			NodeList sf_username_list_level1=sf_username_element.getChildNodes();
			keyvalues.put("salesforce-username", ((Node)sf_username_list_level1.item(0)).getNodeValue());
			
			//salesforce password
			NodeList sf_pwd_list=app_element.getElementsByTagName("salesforce-password");
			Element sf_pwd_element=(Element)sf_pwd_list.item(0);
			NodeList sf_pwd_list_level1=sf_pwd_element.getChildNodes();
			keyvalues.put("salesforce-password", ((Node)sf_pwd_list_level1.item(0)).getNodeValue());
			
			//salesforce token
			NodeList sf_token_list=app_element.getElementsByTagName("salesforce-token");
			Element sf_token_element=(Element)sf_token_list.item(0);
			NodeList sf_token_list_level1=sf_token_element.getChildNodes();
			keyvalues.put("salesforce-token", ((Node)sf_token_list_level1.item(0)).getNodeValue());
		
		}
		catch(Exception e)
		{
			e.printStackTrace();			
		}
	}
	
	public static String getDateTime()
	{
		DateFormat dateFormat=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date=new Date();
		return dateFormat.format(date);
	}
	
	public String getZebraUsername()
	{
		return keyvalues.get("zebra-username");
	}
	
	public String getZebraPassword()
	{
		return keyvalues.get("zebra-password");
	}
	
	public String getZebraUrl()
	{
		return keyvalues.get("zebra-url");
	}
	
	public String getSalesForceUsername()
	{
		return keyvalues.get("salesforce-username");
	}
	
	public String getSalesForcePassword()
	{
		return keyvalues.get("salesforce-password");
	}
	
	public String getSalesForceToken()
	{
		return keyvalues.get("salesforce-token");
	}
}
