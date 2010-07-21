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
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.rpc.ServiceException;
import com.sforce.soap.enterprise.*;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;
import com.sforce.soap.enterprise.sobject.*;

public class sfConnect {
	
	static applabConfig applab=new applabConfig();	
	private static SoapBindingStub binding;
	
	public static void login() throws ServiceException
	{
		binding=(SoapBindingStub)new SforceServiceLocator().getSoap();
		try
		{
			LoginResult loginResult=binding.login(applab.getSalesForceUsername(), applab.getSalesForcePassword()+applab.getSalesForceToken());
			
			binding._setProperty(SoapBindingStub.ENDPOINT_ADDRESS_PROPERTY, loginResult.getServerUrl());
			
			SessionHeader sh=new SessionHeader();
			sh.setSessionId(loginResult.getSessionId());
			binding.setHeader(new SforceServiceLocator().getServiceName().getNamespaceURI(), "SessionHeader", sh);			
		}
		catch(RemoteException e)
		{
			e.printStackTrace();
		}
	}
	
	public static boolean surveyIdExists(String survey_id) throws Exception
	{
		QueryResult query=binding.query("Select Name from Survey__c where Name='"+survey_id+"'");
		if(query.getSize()==1)
		{
			return true;
		}
		return false;
	}
	
	public static String getSurveyName(String survey_id) throws Exception
	{
		QueryResult query=binding.query("Select Survey_Name__c from Survey__c where Name='"+survey_id+"'");
		if(query.getSize()==1)
		{
			Survey__c s=(Survey__c)query.getRecords(0);
			return s.getSurvey_Name__c();
		}
		return "Not Avaliable";
	}
	
	public static ArrayList<String> getPublishedSurveys() throws Exception
	{
		QueryResult query=binding.query("select Name from Survey__c where Survey_Status__c = 'Published'");
		if(query.getSize()>0)
		{
			ArrayList<String> pub_list=new ArrayList<String>();
			for(int i=0;i<query.getSize();i++)
			{
				Survey__c s=(Survey__c)	query.getRecords(i);
				pub_list.add(s.getName());
			}
			return pub_list;
		}
		return null;
	}
	
	public static void logout() throws Exception
	{
		binding.logout();
	}
}
