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

import java.rmi.RemoteException;
import java.util.ArrayList;

import javax.xml.rpc.ServiceException;

import applab.surveys.*;

import com.sforce.soap.enterprise.*;
import com.sforce.soap.enterprise.fault.*;
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

    public String getSurveyName(String survey_id) throws InvalidSObjectFault, MalformedQueryFault, InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException {
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
    
    /** 
     * Given a handset id (IMEI), lookup the associated interviewer information from Salesforce
     */
    public Interviewer lookupInterviewer(String imei) throws InvalidSObjectFault, MalformedQueryFault, InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException {
        StringBuilder commandText = new StringBuilder();
        commandText.append("select Name, First_Name__c, Last_Name__c from CKW__c");
        commandText.append("where Phone__r.IMEI__c = '" + imei + "'");
        QueryResult query = this.binding.query(commandText.toString());
        if (query.getSize() != 1) {
            throw new RemoteException("We should have exactly one CKW with IMEI='" + imei + "', but we found " + query.getSize());
        }
        CKW__c ckw = (CKW__c)query.getRecords(0);
        return new Interviewer(ckw.getFirst_Name__c(), ckw.getLast_Name__c(), ckw.getId());
    }

    public void logout() throws UnexpectedErrorFault, RemoteException {
        this.binding.logout();
    }
}