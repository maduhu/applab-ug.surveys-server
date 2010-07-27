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

package applab.surveys.server;

import java.rmi.RemoteException;
import java.util.ArrayList;

import javax.xml.rpc.ServiceException;
import com.sforce.soap.enterprise.*;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;
import com.sforce.soap.enterprise.sobject.*;


public class SalesforceProxy {
    private SoapBindingStub binding;

    private SalesforceProxy() throws ServiceException {
        // only called from SalesforceProxy.login()
        this.binding = (SoapBindingStub)new SforceServiceLocator().getSoap();
    }

    public static SalesforceProxy login() throws ServiceException, InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException {
        SalesforceProxy proxy = new SalesforceProxy();
        LoginResult loginResult = proxy.binding.login(ApplabConfiguration.getSalesforceUsername(), ApplabConfiguration.getSalesforcePassword()
                + ApplabConfiguration.getSalesforceToken());

        proxy.binding._setProperty(SoapBindingStub.ENDPOINT_ADDRESS_PROPERTY, loginResult.getServerUrl());

        SessionHeader sessionHeader = new SessionHeader(loginResult.getSessionId());
        proxy.binding.setHeader(new SforceServiceLocator().getServiceName().getNamespaceURI(), "SessionHeader", sessionHeader);
        return proxy;
    }

    public boolean surveyIdExists(String survey_id) throws Exception {
        QueryResult query = this.binding.query("Select Name from Survey__c where Name='" + survey_id + "'");
        return (query.getSize() == 1);
    }

    public String getSurveyName(String survey_id) throws Exception {
        QueryResult query = this.binding.query("Select Survey_Name__c from Survey__c where Name='" + survey_id + "'");
        if (query.getSize() == 1) {
            Survey__c survey = (Survey__c)query.getRecords(0);
            return survey.getSurvey_Name__c();
        }
        return "Not Available";
    }

    public ArrayList<String> getPublishedSurveys() throws Exception {
        QueryResult query = this.binding.query("select Name from Survey__c where Survey_Status__c = 'Published'");
        ArrayList<String> surveyNames = new ArrayList<String>();
        for (int i = 0; i < query.getSize(); i++) {
            Survey__c survey = (Survey__c)query.getRecords(i);
            surveyNames.add(survey.getName());
        }
        return surveyNames;
    }

    public void logout() throws UnexpectedErrorFault, RemoteException {
        this.binding.logout();
    }
}