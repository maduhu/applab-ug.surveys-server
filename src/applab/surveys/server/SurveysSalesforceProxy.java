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
import java.util.Map;

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

    /**
     * Loads MarketDay Objects from Salesforce
     * 
     * @param marketDay
     * @return
     * @throws InvalidSObjectFault
     * @throws MalformedQueryFault
     * @throws InvalidFieldFault
     * @throws InvalidIdFault
     * @throws UnexpectedErrorFault
     * @throws InvalidQueryLocatorFault
     * @throws RemoteException
     */
    public List<MarketSurveyObject> getCommoditiesByMarketDay(String marketDay) throws InvalidSObjectFault, MalformedQueryFault,
            InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault,
            InvalidQueryLocatorFault, RemoteException {

        Map<String, MarketSurveyObject> marketSurveys = new HashMap<String, MarketSurveyObject>();
        StringBuilder marketsQueryText = new StringBuilder();
        marketsQueryText.append("SELECT m.Group__c, m.Name, m.Subcounty__r.Name, m.District__r.Name, m.District__r.Region__c ");
        marketsQueryText.append("FROM Markets__c m ");
        marketsQueryText.append(" WHERE m.Market_Day__c = '");
        marketsQueryText.append(marketDay);
        marketsQueryText.append("'");
        QueryResult marketsQuery = getBinding().query(marketsQueryText.toString());

        String groupNames = "";
        for (int i = 0, j = 0; i < marketsQuery.getSize(); i++) {
            Markets__c market = (Markets__c)marketsQuery.getRecords(i);
            // Check if there is a group associated to Market
            // Only markets with a group should be processed
            if (market.getGroup__c() != null && !market.getGroup__c().isEmpty()) {
                marketSurveys.put(market.getGroup__c(), new MarketSurveyObject(market.getGroup__c(), market.getId(), market.getDistrict__r().getRegion__c(), market.getDistrict__r().getName(), market.getSubcounty__r().getName(), market.getName()));
                logger.warn("Market Loaded : " + market.getName());
                // Check if we need to put the separator
                if (j != 0) {
                    groupNames += ", ";
                }
                groupNames += "'" + market.getGroup__c() + "'";
                j++;
            }
        }

        StringBuilder personGroupQueryText = new StringBuilder();
        personGroupQueryText.append("SELECT pg.Person__r.Name, pg.Person__c, pg.Group__c ");
        personGroupQueryText.append("FROM Person_Group_Association__c pg ");
        personGroupQueryText.append("WHERE pg.Group__c IN (");
        personGroupQueryText.append(groupNames);
        personGroupQueryText.append(")");
        QueryResult personGroupQuery = getBinding().query(personGroupQueryText.toString());

        for (int i = 0; i < personGroupQuery.getSize(); i++) {
            Person_Group_Association__c personGroupAssociation = (Person_Group_Association__c)personGroupQuery.getRecords(i);
            MarketSurveyObject marketSurvey = marketSurveys.get(personGroupAssociation.getGroup__c());
            if (marketSurvey != null) {
                marketSurvey.getPersonIds().add(personGroupAssociation.getPerson__r().getName());
            }
        }

        String personNames = "";
        for (int i = 0; i < personGroupQuery.getSize(); i++) {
            Person_Group_Association__c personGroupAssoc = (Person_Group_Association__c)personGroupQuery.getRecords(i);
            if (i != 0) {
                personNames += ", ";
            }
            personNames += "'" + personGroupAssoc.getPerson__c() + "'";
        }

        StringBuilder ckwQueryText = new StringBuilder();
        ckwQueryText.append("SELECT ckw.Person__r.Name, ckw.Person__c, ckw.Name ");
        ckwQueryText.append("FROM CKW__c ckw ");
        ckwQueryText.append("WHERE ckw.Person__c IN (");
        ckwQueryText.append(personNames);
        ckwQueryText.append(")");
        QueryResult ckwQuery = getBinding().query(ckwQueryText.toString());

        for (int i = 0; i < ckwQuery.getSize(); i++) {
            CKW__c ckw = (CKW__c)ckwQuery.getRecords(i);
            for (MarketSurveyObject marketSurvey : marketSurveys.values()) {
                if (marketSurvey.getPersonIds().contains(ckw.getPerson__r().getName())) {
                    marketSurvey.getCkwIds().add(ckw.getName());
                    logger.warn("CKW-ID: " + ckw.getName());
                }
            }
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
            logger.warn("Loaded Survey : " + surveyGroupAssociation.getSurvey__r().getName() + " and Market attached : "
                    + marketSurveys.get(surveyGroupAssociation.getGroup__c()).getMarketName());
        }
        List<MarketSurveyObject> marketSurveysList = new ArrayList<MarketSurveyObject>(marketSurveys.values());
        return marketSurveysList;
    }

    /**
     * Updates market commodity prices with computed values
     * 
     * @param marketDay
     * @param currentCommodityPrices2
     * @return
     * @throws InvalidSObjectFault
     * @throws MalformedQueryFault
     * @throws InvalidFieldFault
     * @throws InvalidIdFault
     * @throws UnexpectedErrorFault
     * @throws InvalidQueryLocatorFault
     * @throws RemoteException
     */
    public boolean updateCommodities(String marketDay, List<Commodity> currentCommodityPrices2) throws InvalidSObjectFault, MalformedQueryFault,
            InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException {

        List<Commodities__c> updateCommodities = new ArrayList<Commodities__c>();
        StringBuilder queryText = new StringBuilder();
        queryText.append("SELECT c.Id, c.Name, c.Market__r.Name, ");
        queryText.append("c.Lowest_Retail_Price__c, ");
        queryText.append("c.Highest_Retail_Price__c, ");
        queryText.append("c.Lowest_Wholesale_Price__c, ");
        queryText.append("c.Highest_Wholesale_Price__c ");
        queryText.append("FROM Commodities__c c  ");
        queryText.append("WHERE c.Market__r.Market_Day__c = '");
        queryText.append(marketDay);
        queryText.append("' ");
        QueryResult query = getBinding().query(queryText.toString());
        logger.warn("Commodities sent for update: " + currentCommodityPrices2.size());
        logger.warn("Commodites loade from Salesforce: " + query.getSize());
        for (int i = 0; i < query.getSize(); i++) {
            Commodities__c salesforceCommodity = (Commodities__c)query.getRecords(i);
            Commodity commodity = getByNameAndMarketName(salesforceCommodity.getName(), salesforceCommodity.getMarket__r().getName(),
                    currentCommodityPrices2);
            if (commodity != null) {
                logger.warn("Commodity to be updated: " + commodity.getName() + " : " + commodity.getMarketName());
                // TODO: Add flagging logic in case the difference between previous and current price is too large
                salesforceCommodity.setLowest_Retail_Price__c(commodity.getLowRetailPrice());
                salesforceCommodity.setHighest_Retail_Price__c(commodity.getHighRetailPrice());
                salesforceCommodity.setLowest_Wholesale_Price__c(commodity.getLowWholesalePrice());
                salesforceCommodity.setHighest_Wholesale_Price__c(commodity.getHighWholesalePrice());
                updateCommodities.add(salesforceCommodity);
            }
        }
        try {
            Commodities__c[] salesforceCommodities = new Commodities__c[updateCommodities.size()];
            getBinding().update(updateCommodities.toArray(salesforceCommodities));
            return true;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private Commodity getByNameAndMarketName(String name, String marketName, List<Commodity> currentCommodityPrices) {
        logger.warn("Check for: " + name + " " + marketName);
        for (Commodity commodity : currentCommodityPrices) {
            if (commodity.getMarketName().toLowerCase().equals(marketName.toLowerCase())
                    && commodity.getName().toLowerCase().equals(name.toLowerCase())) {
                return commodity;
            }
        }
        return null;
    }

}