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
import java.util.HashMap;
import java.util.List;

import javax.xml.rpc.ServiceException;

import org.apache.log4j.Logger;

import applab.surveys.*;
import com.sforce.soap.enterprise.*;
import com.sforce.soap.enterprise.fault.*;
import com.sforce.soap.enterprise.sobject.*;

import applab.server.SalesforceProxy;

public class SurveysSalesforceProxy extends SalesforceProxy {
    
    private static Logger logger = Logger.getLogger(SalesforceProxy.class);
    
    public SurveysSalesforceProxy() throws ServiceException, InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException {
        super();
    }

    public boolean surveyIdExists(String survey_id) throws Exception {
        QueryResult query = getBinding().query("SELECT Name FROM Survey__c WHERE Name='" + survey_id + "'");
        return (query.getSize() == 1);
    }

    public String getSurveyName(String survey_id) throws InvalidSObjectFault, MalformedQueryFault, InvalidFieldFault, InvalidIdFault,
            UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException {
        QueryResult query = getBinding().query("SELECT Survey_Name__c FROM Survey__c WHERE Name='" + survey_id + "'");
        if (query.getSize() == 1) {
            Survey__c survey = (Survey__c)query.getRecords(0);
            return survey.getSurvey_Name__c();
        }
        return "Not Available";
    }

    public ArrayList<Survey> getPublishedSurveys(String imei) throws InvalidSObjectFault, MalformedQueryFault, InvalidFieldFault,
            InvalidIdFault, UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException {

        ArrayList<Survey> surveys = new ArrayList<Survey>();
        String[] groupIds = getGroupIds(imei);
        if (!"".equalsIgnoreCase(groupIds[0])) {
            StringBuilder queryText = new StringBuilder();
            queryText.append("SELECT ");
            queryText.append("Name, ");
            queryText.append("Survey_Name__c ");
            queryText.append("FROM ");
            queryText.append("Survey__c ");
            queryText.append("WHERE ");

            // Allow staff members to download draft surveys
            if ("true".equals(groupIds[1])) {
                queryText.append("Survey_Status__c != 'Completed' ");
            }
            else {
                queryText.append("Survey_Status__c = 'Published' ");
            }
            queryText.append("AND Start_Date__c < TOMORROW ");
            queryText.append("AND End_Date__c > YESTERDAY ");
            queryText.append("AND Id IN  ");
            queryText.append("(SELECT Survey__c FROM Survey_Group_Association__c WHERE Group__c IN ( ");
            queryText.append(groupIds[0]);
            queryText.append(")) ");
            queryText.append("ORDER BY ");
            queryText.append("Survey_Name__c ASC");
            queryText.append(" ");
            queryText.append(" ");
            queryText.append(" ");
            queryText.append(" ");
            queryText.append(" ");
            QueryResult query = getBinding().query(queryText.toString());
            for (int i = 0; i < query.getSize(); i++) {
                Survey__c survey = (Survey__c)query.getRecords(i);
                surveys.add(new Survey(survey.getName(), survey.getSurvey_Name__c()));
            }
        }
        return surveys;
    }

    public HashMap<String, MarketSurveyObject> getCommoditiesByMarketDay(String marketDay) throws InvalidSObjectFault, MalformedQueryFault,
            InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault,
            InvalidQueryLocatorFault, RemoteException {

        HashMap<String, MarketSurveyObject> marketSurveys = new HashMap<String, MarketSurveyObject>();
        StringBuilder marketsQueryText = new StringBuilder();
        marketsQueryText.append("SELECT m.Group__c, m.Name ");
        marketsQueryText.append("FROM Markets__c m ");
        marketsQueryText.append(" WHERE m.Market_Day__c = '");
        marketsQueryText.append(marketDay);
        marketsQueryText.append("'");
        QueryResult marketsQuery = getBinding().query(marketsQueryText.toString());

        String groupNames = "";
        for (int i = 0; i < marketsQuery.getSize(); i++) {
            Markets__c market = (Markets__c)marketsQuery.getRecords(i);
            marketSurveys.put(market.getGroup__c(), new MarketSurveyObject(market.getGroup__c(), market.getName()));
            logger.warn("Market Loaded : " + market.getName());
            // Check if we need to put the separator
            if (i != 0) {
                groupNames += ", ";
            }
            groupNames += "'" + market.getGroup__c() + "'";
        }

        StringBuilder queryText = new StringBuilder();
        queryText.append("SELECT sg.Name, sg.Group__c, sg.Survey__r.Name, sg.Group__r.Name ");
        queryText.append("FROM Survey_Group_Association__c sg ");
        queryText.append("WHERE sg.Group__c IN (");
        queryText.append(groupNames);
        queryText.append(")");
        QueryResult query = getBinding().query(queryText.toString());

        for (int i = 0; i < query.getSize(); i++) {
            Survey_Group_Association__c surveyGroupAssociation = (Survey_Group_Association__c)query.getRecords(i);
            marketSurveys.get(surveyGroupAssociation.getGroup__c()).setSurveyName(surveyGroupAssociation.getSurvey__r().getName());
        }
        return marketSurveys;
      //  return removeBlankMarketSurveys(marketSurveys);
    }

    public boolean updateCommodities(String marketDay, ArrayList<Commodity> commodities) throws InvalidSObjectFault, MalformedQueryFault,
            InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException {

        Commodities__c[] salesforceComodities = new Commodities__c[commodities.size()];
        StringBuilder queryText = new StringBuilder();
        queryText.append("SELECT c.Id, c.Name, c.Markets__r.Name, ");
        queryText.append("c.Lowest_Retail_Price__c, ");
        queryText.append("c.Highest_Retail_Price__c, ");
        queryText.append("c.Lowest_WholeSale_Price__c, ");
        queryText.append("c.Highest_WholeSale_Price__c ");
        queryText.append("FROM Commodities__c c  ");
        queryText.append("WHERE c.Markets__r.Market_Day__c = '");
        queryText.append(marketDay);
        queryText.append("' ");
        QueryResult query = getBinding().query(queryText.toString());
        for (int i = 0; i < query.getSize(); i++) {
            Commodities__c salesforceCommodity = (Commodities__c)query.getRecords(i);
            Commodity commodity = getByNameAndMarketName(salesforceCommodity.getName(), salesforceCommodity.getMarket__r().getName(),
                    commodities);
            // TODO: Add flagging logic in case the difference between previous and current price is too large
            salesforceCommodity.setLowest_Retail_Price__c(commodity.getLowRetailPrice());
            salesforceCommodity.setHighest_Retail_Price__c(commodity.getHighRetailPrice());
            salesforceCommodity.setLowest_Wholesale_Price__c(commodity.getLowWholesalePrice());
            salesforceCommodity.setHighest_Wholesale_Price__c(commodity.getHighWholesalePrice());
            salesforceComodities[i] = salesforceCommodity;
        }
        try {
            getBinding().update(salesforceComodities);
            return true;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private Commodity getByNameAndMarketName(String name, String marketName, ArrayList<Commodity> commodities) {
        for (Commodity commodity : commodities) {
            if (commodity.getMarketName().equals(marketName) && commodity.getName().toLowerCase().equals(name.toLowerCase())) {
                return commodity;
            }
        }
        return null;
    }

}