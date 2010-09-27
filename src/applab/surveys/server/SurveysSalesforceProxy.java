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

import applab.Farmer;
import applab.surveys.*;

import com.sforce.soap.enterprise.*;
import com.sforce.soap.enterprise.fault.*;
import com.sforce.soap.enterprise.sobject.*;

import applab.server.SalesforceProxy;

public class SurveysSalesforceProxy extends SalesforceProxy {
    public SurveysSalesforceProxy() throws ServiceException, InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException {
        super();
    }

    public boolean surveyIdExists(String survey_id) throws Exception {
        QueryResult query = getBinding().query("Select Name from Survey__c where Name='" + survey_id + "'");
        return (query.getSize() == 1);
    }

    public String getSurveyName(String survey_id) throws InvalidSObjectFault, MalformedQueryFault, InvalidFieldFault, InvalidIdFault,
            UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException {
        QueryResult query = getBinding().query("Select Survey_Name__c from Survey__c where Name='" + survey_id + "'");
        if (query.getSize() == 1) {
            Survey__c survey = (Survey__c)query.getRecords(0);
            return survey.getSurvey_Name__c();
        }
        return "Not Available";
    }

    public ArrayList<Survey> getPublishedSurveys(String imei) throws InvalidSObjectFault, MalformedQueryFault, InvalidFieldFault,
            InvalidIdFault, UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException {

        // Apparently we have to split these queries up otherwise we get too many nested queries and SF chokes on the
        // joins
        QueryResult groupQuery = getBinding().query("Select Group__c from Person_Group_Association__c where Person__c IN " +
                                "(Select Id from Person__c where Handset__r.IMEI__c = '" + imei + "')");

        ArrayList<Survey> surveys = new ArrayList<Survey>();

        if (groupQuery.getSize() > 0) {
            String groupIds = "";
            for (int i = 0; i < groupQuery.getSize(); i++) {
                Person_Group_Association__c group = (Person_Group_Association__c)groupQuery.getRecords(i);

                // Check if we need to put the separator
                if (i != 0) {
                    groupIds += ", ";
                }

                groupIds += "'" + group.getGroup__c() + "'";
            }

            QueryResult query = getBinding().query(
                    "Select Name, Survey_Name__c from Survey__c where Survey_Status__c = 'Published' and Id in " +
                            "(Select Survey__c from Survey_Group_Association__c where Group__c IN (" + groupIds + "))");

            for (int i = 0; i < query.getSize(); i++) {
                Survey__c survey = (Survey__c)query.getRecords(i);
                surveys.add(new Survey(survey.getName(), survey.getSurvey_Name__c()));
            }
        }
        return surveys;
    }
}