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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.rpc.ServiceException;

import org.apache.log4j.Logger;

import applab.surveys.*;

import com.sforce.soap.enterprise.*;
import com.sforce.soap.enterprise.fault.*;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.soap.schemas._class.ImportBackendServerKeywords.ImportBackendServerKeywordsBindingStub;
import com.sforce.soap.schemas._class.ImportBackendServerKeywords.ImportBackendServerKeywordsServiceLocator;
import com.sforce.soap.schemas._class.ImportBackendServerKeywords.MenuItemAdapter;

import applab.server.ApplabConfiguration;
import applab.server.SalesforceProxy;
import applab.server.WebAppId;

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
        marketsQueryText.append("SELECT m.Group__c, m.Name, m.Subcounty__r.Display_Name__c, m.District__r.Name, m.District__r.Region__c ");
        marketsQueryText.append("FROM Markets__c m ");
        marketsQueryText.append(" WHERE m.Market_Day__c = '");
        marketsQueryText.append(marketDay);
        marketsQueryText.append("'");
        marketsQueryText.append(" AND m.Subcounty__c != NULL");
        marketsQueryText.append(" AND m.District__c != NULL");
        marketsQueryText.append(" AND m.Group__c != NULL");
        QueryResult marketsQuery = getBinding().query(marketsQueryText.toString());

        String groupNames = "";
        for (int i = 0, j = 0; i < marketsQuery.getSize(); i++) {
            Markets__c market = (Markets__c)marketsQuery.getRecords(i);
            // Check if there is a group associated to Market
            // Only markets with a group should be processed
            if (market.getGroup__c() != null && !market.getGroup__c().isEmpty()) {
                marketSurveys.put(market.getGroup__c(), new MarketSurveyObject(market.getGroup__c(), market.getId(), market.getDistrict__r().getRegion__c(), market.getDistrict__r().getName(), market.getSubcounty__r().getDisplay_Name__c(), market.getName()));
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
        queryText.append(" AND sg.Survey__r.End_Date__c >= TODAY ");
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
    public boolean updateCommodities(String marketDay, List<Commodity> currentCommodityPrices2, String CATEGORY, String BASE_KEYWORD, String ATTRIBUTION, String MENU_NAME) throws InvalidSObjectFault, MalformedQueryFault,
            InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException {

        List<Commodity> updateCommodities = new ArrayList<Commodity>();
        logger.warn("commodity count is " + currentCommodityPrices2.size());
        List<String> allMarkets = new ArrayList<String>();
        String marketsString = "";
        //get unique markets
        for(Commodity commodity: currentCommodityPrices2){
        	if(!allMarkets.contains(commodity.getMarketName())){
        		logger.warn("market added " + commodity.getMarketName());
        		allMarkets.add(commodity.getMarketName());
        		if(marketsString.length() == 0){
        			marketsString = "'" + commodity.getMarketName() + "'";
        		}
        		else{
        			marketsString += ", '" + commodity.getMarketName() + "'";
        		}
        	}
        }
        //remove last comma (,)
        marketsString = marketsString.trim();
        if(marketsString.endsWith(",")){
        	if (marketsString.length() > 0 && marketsString.charAt(marketsString.length()-1)==',') {
        		marketsString = marketsString.substring(0, marketsString.length()-1);
        	}
        }
        logger.warn("markets with updatable commodities are " + allMarkets.size() + " " + marketsString);
        if(allMarkets.size() == 0){
        	//however, this shouldn't happen. commodities should already have the markets from submissions
        	return false;
        }

        //get all child menu items for current markets. these should be corresponding commodities
        StringBuilder queryText = new StringBuilder();
        queryText.append("SELECT ");
		queryText.append("	Id, Label__c, Parent_Item__r.Id, Parent_Item__r.Label__c ");
		queryText.append("FROM ");
		queryText.append("	Menu_Item__c ");
		queryText.append("WHERE ");
		queryText.append("Parent_Item__r.Label__c in (");
		queryText.append(marketsString);
		queryText.append(") ");
		queryText.append("AND Is_Active__c = true ");
		logger.warn(queryText.toString());
		QueryResult query = getBinding().query(queryText.toString());
		if(query.getSize() == 0){
			logger.warn("There are no updatable keywords under these markets " + query.getSize());
			//return false;//proceeding will result in creation of new keywords
		}
	      for (int i = 0; i < query.getSize(); i++) {
	    	  Menu_Item__c salesforceCommodityMenuItem = (Menu_Item__c)query.getRecords(i);
	    	  Commodity commodity = getByNameAndMarketName(salesforceCommodityMenuItem.getLabel__c(), salesforceCommodityMenuItem.getParent_Item__r().getLabel__c(),
	              currentCommodityPrices2);
	    	  if (commodity != null) {
	    		  logger.warn("Commodity to be updated: " + commodity.getName() + " : " + commodity.getMarketName());
	    		  // TODO: Add flagging logic in case the difference between previous and current price is too large
	    		  //no need for this since check was made earlier to normalise the prices
	    		  updateCommodities.add(commodity);
	    	  }
	      }

		if(updateCommodities.size() == 0){
			logger.warn("There are no updatable keywords relating these markets " + query.getSize());
			//return false;//proceeding will result in creation of new keywords
		}
    	List<MenuItemAdapter> menuItemAdapeters = generateItemAdapters(currentCommodityPrices2, CATEGORY, BASE_KEYWORD, ATTRIBUTION);
        try {
    		logger.warn("Creating keywords exporter");
        	ImportBackendServerKeywordsBindingStub keywordsExporter = setupSalesforceAuthentication();
    		logger.warn("sending keywords to salesforce...");
        	boolean passed = keywordsExporter.importBackendKeywords(menuItemAdapeters.toArray(new MenuItemAdapter[menuItemAdapeters.size()]), MENU_NAME);
        	logger.warn("updating keywords successful? : " + passed);
        	return passed;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex);
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
        
    /*
     *	creates MenuItemAdapters for the given market prices passed
     *	@param keywords		a list of market price items to be save to salesforce
     *	@return 			List of MenuItemAdapters
     */
     private List<MenuItemAdapter> generateItemAdapters(List<Commodity> keywords, String CATEGORY, String BASE_KEYWORD, String ATTRIBUTION){
     	int updatableKeyWordsCount = 0;
 		logger.warn("MIS: Generating itemAdapters");        
         List<MenuItemAdapter> adapters = new ArrayList<MenuItemAdapter>();
         for (Integer i = 0; i < keywords.size(); i++) {
         	updatableKeyWordsCount++;  
             // split keywords breabcrumb to build menu paths for adapters
         	CommodityPriceKeyword priceKeyword = new CommodityPriceKeyword(keywords.get(i), ATTRIBUTION, BASE_KEYWORD, 64, CATEGORY);
         	String[] rawTokens = priceKeyword.getKeyword().trim().replaceAll("\\s+", " ").split(" ");
         	String[] tokens = removeUnderscore(priceKeyword.getKeyword().trim().replaceAll("\\s+", " ").split(" "));
 
             // current and previous paths
             String previousPath = "";
             String currentPath = "";
             // loop over all the tokens and build adapters
             for (Integer j = 0; j < tokens.length; j++) {
                 previousPath = currentPath;
                 currentPath = buildAdapterMenuPath(rawTokens, j);
 
                 // Make sure that there is no 'similar' adapter already loaded
                 if (!existsInAdaptersList(adapters, currentPath)) {
                     MenuItemAdapter adapter = new MenuItemAdapter();
                     adapter.setMenuPath(currentPath);
                     adapter.setIsActive(true);
                     adapter.setLastModifiedDate(Calendar.getInstance());
                     adapter.setLabel(tokens[j]);
                     adapter.setIsProcessed(false);
 
                     // Fill in content, attribution et al if its the end point item
                     if (j == tokens.length - 1) {
                         adapter.setContent(priceKeyword.getContent());
                         adapter.setAttribution(ATTRIBUTION);
                     }
                     adapter.setPreviousItemPath(previousPath);
                     adapters.add(adapter);
                     logger.warn(currentPath);
                 }
             }
         }
         logger.warn("MIS: adding adapter completed with updatable keywords = " + updatableKeyWordsCount);
		 logger.warn("MIS: adding adapter completed with " + adapters.size());
         return adapters;
     }
     
     /**
      *	replaces underscores with spaces
      *	@param tokens	string containing inderscores
      *	@return			string without underscores
      */
     private String[] removeUnderscore(String[] tokens) {
          if (tokens.length > 0) {
              for (Integer x = 0; x < tokens.length; x++) {
                  String token = tokens[x].trim().replaceAll("_", " ");
                  tokens[x] = token;
              }
          }
          return tokens;
      }
      
      /**
       *	builds a menu path
       *	@param tokens	strings elements in the menu path
       *	@param level	the last level in the menu path
       *	@return			menu path
       */
     private String buildAdapterMenuPath(String[] tokens, Integer level) {
           String path = "";
           for (Integer x = 0; x < tokens.length && x <= level; x++) {
               String y = tokens[x];
               path = path + ' ' + y;
           }
           return path.trim();
       }
       
       /**
        *	Checks if the keyword already exists in list of adapters
        *	@param adapters		list of adapters
        *	@param path			string path or keyword
        *	@return				true if exists, false otherwise
        */
     private boolean existsInAdaptersList(List<MenuItemAdapter> adapters, String path) {
            for (MenuItemAdapter adapter : adapters){
                if (adapter.getMenuPath().equals(path)){
                    return true;
                }
            }
            return false;
        }
        
     /**
      * 
      * Creates a login session and assigns it to localBindingStub object to give a handle to logout when required
      * 
      * @return		proxy instance upon which we can call and pass keywords heading for update
      * @throws Exception	here we enforce a catch
      * @throws LoginFault	there can be failure to login, this exception should be expected
      * @throws RemoteException	remote errors arising
      */
     private ImportBackendServerKeywordsBindingStub setupSalesforceAuthentication() throws Exception, LoginFault, RemoteException {
        	
 		ImportBackendServerKeywordsServiceLocator getImportServiceLocator = new ImportBackendServerKeywordsServiceLocator();
		ImportBackendServerKeywordsBindingStub serviceStub = (ImportBackendServerKeywordsBindingStub) getImportServiceLocator.getImportBackendServerKeywords();
		
		// Use soap api to login and get session info
		SforceServiceLocator soapServiceLocator = new SforceServiceLocator();
		soapServiceLocator.setSoapEndpointAddress((String) ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceAddress", ""));
		SoapBindingStub localbinding = (SoapBindingStub) soapServiceLocator.getSoap();
		LoginResult loginResult = localbinding.login((String) ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceUsername", ""),
		            							(String) ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforcePassword", "") +
		                             			(String) ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceToken", ""));

		SessionHeader sessionHeader = new SessionHeader(loginResult.getSessionId());
		serviceStub.setHeader("http://soap.sforce.com/schemas/class/ImportBackendServerKeywords","SessionHeader", sessionHeader);
		
		return serviceStub;
	}
}