/*
 * Copyright (C) 2011 Grameen Foundation
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

package applab.surveys;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import applab.server.DatabaseHelpers;
import applab.surveys.server.SurveyDatabaseHelpers;
import applab.surveys.server.SurveysSalesforceProxy;

import com.sforce.soap.enterprise.fault.InvalidFieldFault;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.InvalidQueryLocatorFault;
import com.sforce.soap.enterprise.fault.InvalidSObjectFault;
import com.sforce.soap.enterprise.fault.MalformedQueryFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;

public class ProcessMarketSubmissions {

    private final String HIGH_WHOLESALE_PRICE = "high_wholesale_price_[a-z_]*";
    private final String LOW_WHOLESALE_PRICE = "low_wholesale_price_[a-z_]*";
    private final String HIGH_RETAIL_PRICE = "high_retail_price_[a-z_]*";
    private final String LOW_RETAIL_PRICE = "low_retail_price_[a-z_]*";
    private final String WHOLESALE_UNIT_OF_MEASUREMENT_WEIGHT = "weight_unit_measurement_[a-z]*_wholesale";
    private final String RETAIL_UNIT_OF_MEASUREMENT_WEIGHT = "weight_unit_measurement_[a-z]*_retail";
    private final String MARKET_NAME_BINDING = "market_name";
    private final String ATTRIBUTION = "NAADS Survey";
    private final String MENU_NAME = "CKW Search";
    private final String CATEGORY = "Market_Information";
    private final String BASE_KEYWORD = "Subcounty_Market_Prices";
    private HashMap<String, Pattern> bindingPatterns;
    private List<MarketSurveyObject> marketSurveys;
    private ArrayList<Survey> surveys;
    private String marketDay;
    private ArrayList<Commodity> previousCommodityPrices;
    private List<Commodity> currentCommodityPrices;
    private SurveysSalesforceProxy surveysSalesforceProxy;
    private static Logger logger;
    private int daysBacktoProcess;
    private int daysToProcess;
    private Calendar calendar;
    private ArrayList<String> numbers;
    private int categoryId;

    /**
     * Constructor explicitly declaring dependencies passed as parameters
     * 
     * @param logger
     * @param calendar
     * @param daysBacktoProcess
     * @param daysToProcess
     * @param surveysSalesforceProxy
     */
    public ProcessMarketSubmissions(Calendar calendar, int daysBacktoProcess, int daysToProcess,
            String marketDay, SurveysSalesforceProxy surveysSalesforceProxy) {
        this.calendar = calendar;
        this.marketDay = marketDay;
        this.daysBacktoProcess = daysBacktoProcess;
        this.daysToProcess = daysToProcess;
        ProcessMarketSubmissions.logger = Logger.getLogger(ProcessMarketSubmissions.class);
        this.surveysSalesforceProxy = surveysSalesforceProxy;
        this.categoryId = 64;
    }

    /**
     * Orchestrates processing of market price submissions
     * 
     * @throws Exception
     */
    public boolean performSubmissionProcessing() throws Exception {

        this.previousCommodityPrices = null;
        this.bindingPatterns = compilePatterns();
        this.numbers = createNumberPatterns();
        marketSurveys = surveysSalesforceProxy.getCommoditiesByMarketDay(marketDay);
        if (marketSurveys.isEmpty()) {
            logger.warn("No surveys for the given day: " + marketDay);
            return true;
        }
        logger.warn("Market Survey objects loaded: " + marketSurveys.size());
        Set<String> salesforceIds = new HashSet<String>();
        for (MarketSurveyObject marketSurvey : marketSurveys) {
            salesforceIds.add(marketSurvey.getSurveyName());
        }
        this.surveys = loadSurveys(salesforceIds);
        boolean hasSavedCommodityPrices = true;
        for (Survey survey : this.surveys) {
            this.currentCommodityPrices = processCommodityPrices(survey);
            logger.warn("Finished processing commodity prices for a survey");
            hasSavedCommodityPrices &= saveCommodityPrices(currentCommodityPrices);
        }
        return hasSavedCommodityPrices;
    }

    /**
     * Loads previous day's market surveys including submissions to be processed
     * 
     * @param salesforceSurveyIds
     * @param calendar
     *            : represents the set "current" date
     * @param startDay
     *            : days before calendar day when to start processing (expressed in negatives)
     * @param daysToProcess
     *            : number of days for which to load submissions
     * @return
     * @throws Exception
     */
    public ArrayList<Survey> loadSurveys(Set<String> salesforceSurveyIds) throws Exception {

        ArrayList<Survey> surveys = new ArrayList<Survey>();
        calendar.add(Calendar.DATE, daysBacktoProcess);
        java.sql.Date endDate = new java.sql.Date(calendar.getTimeInMillis());
        logger.warn("Submission ends:: " + endDate.toString());
        calendar.add(Calendar.DATE, -daysToProcess);
        java.sql.Date startDate = new java.sql.Date(calendar.getTimeInMillis());
        logger.warn("Submission begins:: " + startDate.toString());

        for (String salesforceSurveyId : salesforceSurveyIds) {
            try {
                if(salesforceSurveyId == null){
                	logger.warn("survey ID was null skipping...");
                	continue;
                }
                Survey survey = new Survey(salesforceSurveyId);
                survey.loadSubmissions(SubmissionStatus.NotReviewed, startDate, endDate, false, salesforceSurveyId, false);
                surveys.add(survey);
                logger.warn("Loaded Survey: " + survey.getName() + " Submission Count: " + survey.getSubmissions(false).size());
            }
            catch (Exception ex) {
                throw new Exception("salesforceId" + salesforceSurveyId + ex.getMessage() + ex.getClass().getName());
            }
        }
        return surveys;
    }

    public boolean saveCommodityPrices(List<Commodity> commodities) throws InvalidSObjectFault, MalformedQueryFault,
            InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException {
        boolean savedToBackEnd = false;
        boolean savedToSearch = false;
        boolean savedToSalesForce = false;
        if (commodities != null && commodities.size() != 0) {
            try {
                savedToBackEnd = saveCommodityPricesToBackendDatabase(commodities);
                savedToSearch = saveCommodityPricesToSearchDatabase(commodities);
                savedToSalesForce = saveCommodityPricesToSalesForce(commodities);
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
            return savedToBackEnd && savedToSalesForce && savedToSearch;
        }
        return true;
    }

    public boolean saveCommodityPricesToSalesForce(List<Commodity> commodities) throws InvalidSObjectFault, MalformedQueryFault,
            InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException {
        return surveysSalesforceProxy.updateCommodities(marketDay, commodities, CATEGORY, BASE_KEYWORD, ATTRIBUTION, MENU_NAME);
    }

    public boolean saveCommodityPricesToBackendDatabase(List<Commodity> commodities)
            throws ClassNotFoundException, SQLException {
        // Create the connection to the database
        Connection connection = SurveyDatabaseHelpers.getWriterConnection();
        connection.setAutoCommit(false);
        StringBuilder commandText = new StringBuilder();
        commandText.append("INSERT INTO commodityprices ");
        commandText.append("(name, market_name, ");
        commandText.append(" high_wholesale_price, low_wholesale_price, high_retail_price, low_retail_price, ");
        commandText.append(" date_created");
        commandText.append(") values (?, ?, ?, ?, ?, ?, CURDATE())");
        PreparedStatement submissionStatement = connection.prepareStatement(commandText.toString());

        for (Commodity commodity : commodities) {
            submissionStatement.setString(1, commodity.getName());
            submissionStatement.setString(2, commodity.getMarketName());
            submissionStatement.setDouble(3, commodity.getHighWholesalePrice());
            submissionStatement.setDouble(4, commodity.getLowWholesalePrice());
            submissionStatement.setDouble(5, commodity.getHighRetailPrice());
            submissionStatement.setDouble(6, commodity.getHighRetailPrice());
            submissionStatement.addBatch();
            logger.warn("My SQL insert statement: " + submissionStatement.toString());
        }
        try {
            submissionStatement.executeBatch();
            logger.warn("Executed batch save");
        }
        catch (SQLException e) {
            e.printStackTrace();
            connection.rollback();
            connection.setAutoCommit(true);
            submissionStatement.close();
            logger.warn("Failed to commit");
            return false;
        }
        connection.commit();
        logger.warn("Commited changes");
        connection.setAutoCommit(true);
        submissionStatement.close();
        return true;
    }

    public boolean saveCommodityPricesToSearchDatabase(List<Commodity> commodities) throws ClassNotFoundException, SQLException {
        HashMap<String, Integer> previousKeywords = getPreviousMarketPriceKeywords();
        List<CommodityPriceKeyword> updateCommodityPriceKeywords = new ArrayList<CommodityPriceKeyword>();
        List<CommodityPriceKeyword> insertCommodityPriceKeywords = new ArrayList<CommodityPriceKeyword>();
        for (Commodity commodity : commodities) {
            CommodityPriceKeyword priceKeyword = new CommodityPriceKeyword(commodity, ATTRIBUTION, BASE_KEYWORD, categoryId, CATEGORY);
            // Sort commodity price keywords depending on whether they are updates or new keywords
            if (previousKeywords.containsKey(priceKeyword.getKeyword())) {
                priceKeyword.setId(previousKeywords.get(priceKeyword.getKeyword()));
                updateCommodityPriceKeywords.add(priceKeyword);
            }
            else {
                insertCommodityPriceKeywords.add(priceKeyword);
            }
        }
        logger.warn("Keywords for update: " + updateCommodityPriceKeywords.size());
        logger.warn("Keywords for insert: " + insertCommodityPriceKeywords.size());
        return updateCommodityPricesInSearchDatabase(updateCommodityPriceKeywords)
                && insertCommodityPricesToSearchDatabase(insertCommodityPriceKeywords);
    }

    public boolean updateCommodityPricesInSearchDatabase(List<CommodityPriceKeyword> commodityPriceKeywords) throws ClassNotFoundException,
            SQLException {

        // Create the connection to the database
        Connection connection = SurveyDatabaseHelpers.getSearchWriterConnection();
        connection.setAutoCommit(false);
        StringBuilder commandText = new StringBuilder();
        commandText.append("UPDATE keyword ");
        commandText.append("SET content = ?, ");
        commandText.append("updated = NOW() ");
        commandText.append("WHERE Id = ? ");
        PreparedStatement submissionStatement = connection.prepareStatement(commandText.toString());
        for (CommodityPriceKeyword commodityPriceKeyword : commodityPriceKeywords) {
            logger.warn("Generated pre-update keyword: " + commodityPriceKeyword.getKeyword());
            submissionStatement.setString(1, commodityPriceKeyword.getContent());
            submissionStatement.setInt(2, commodityPriceKeyword.getId());
            submissionStatement.addBatch();
            logger.warn("My SQL update statement: " + submissionStatement.toString());
        }
        try {
            submissionStatement.executeBatch();
            logger.warn("Executed batch update");
        }
        catch (SQLException e) {
            e.printStackTrace();
            connection.rollback();
            connection.setAutoCommit(true);
            submissionStatement.close();
            logger.warn("Failed to update");
            return false;
        }
        connection.commit();
        logger.warn("Commited changes");
        connection.setAutoCommit(true);
        submissionStatement.close();
        return true;
    }

    public boolean insertCommodityPricesToSearchDatabase(List<CommodityPriceKeyword> commodityPriceKeywords) throws ClassNotFoundException,
            SQLException {

        // Create the connection to the database
        Connection connection = SurveyDatabaseHelpers.getSearchWriterConnection();
        connection.setAutoCommit(false);
        StringBuilder commandText = new StringBuilder();
        commandText.append("INSERT INTO keyword ");
        commandText.append("(keyword, categoryid, ");
        commandText.append(" content, otrigger, isdeleted, weight, quizAction_quizId, quizAction_action, ");
        commandText.append(" attribution, updated, createdate");
        commandText.append(") values (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), CURDATE())");
        PreparedStatement submissionStatement = connection.prepareStatement(commandText.toString());

        for (CommodityPriceKeyword commodityPriceKeyword : commodityPriceKeywords) {
            logger.warn("Generated pre-commit keyword: " + commodityPriceKeyword.getBaseKeyword() + commodityPriceKeyword.getContent());
            submissionStatement.setString(1, commodityPriceKeyword.getKeyword());
            submissionStatement.setInt(2, commodityPriceKeyword.getCategoryId());
            submissionStatement.setString(3, commodityPriceKeyword.getContent());
            submissionStatement.setInt(4, 0);
            submissionStatement.setByte(5, (byte)0);
            submissionStatement.setDouble(6, (byte)0);
            submissionStatement.setInt(7, 0);
            submissionStatement.setString(8, "");
            submissionStatement.setString(9, commodityPriceKeyword.getAttribution());
            submissionStatement.addBatch();
            logger.warn("My SQL insert statement: " + submissionStatement.toString());
        }
        try {
            submissionStatement.executeBatch();
            logger.warn("Executed batch save");
        }
        catch (SQLException e) {
            e.printStackTrace();
            connection.rollback();
            connection.setAutoCommit(true);
            submissionStatement.close();
            logger.warn("Failed to commit");
            return false;
        }
        connection.commit();
        logger.warn("Commited changes");
        connection.setAutoCommit(true);
        submissionStatement.close();
        return true;
    }

    public List<Commodity> processCommodityPrices(Survey survey) throws SAXException, IOException,
            ParserConfigurationException, ClassNotFoundException, SQLException, ParseException {
    	
        ParsedSurveyXml parsedSurveyXml = null;
        parsedSurveyXml = survey.getBackEndSurveyXml();
        HashSet<String> commodityNames = new HashSet<String>();
        List<Commodity> commodityPrices = new ArrayList<Commodity>();
        List<MarketSurveyObject> relatedMarketSurveys = MarketSurveyObject.getBySurveyName(survey.getSalesforceId(), marketSurveys);
        logger.warn("Processing Survey: " + survey.getSalesforceId() + " Market survey objects " + relatedMarketSurveys.size());
        // Get top most related market survey object only since the others have same structure
        MarketSurveyObject topMarketSurvey = relatedMarketSurveys.get(0);
        logger.warn("Questions to be processed: " + parsedSurveyXml.getQuestionOrder().size());
        for (String questionBinding : parsedSurveyXml.getQuestionOrder()) {
            if (isCommodityPriceRelatedBinding(questionBinding)) {
                //logger.warn("Question Binding: " + questionBinding);
                Question question = parsedSurveyXml.getQuestions().get(questionBinding);
                String commodityName = getCommodityName(questionBinding);
                String units = "";
                //extract units from name
                if(commodityName.contains("/")){
                	String tempCommodityName = commodityName;
                	commodityName = tempCommodityName.split("/")[0];
                	units = tempCommodityName.split("/")[1];
                }
                Commodity commodity = null;

                if (!commodityNames.contains(commodityName)) {
                    commodity = new Commodity(commodityName, units);
                    commodityNames.add(commodityName);
                    commodityPrices.add(commodity);
                    commodity.getQuestions().add(question);
                    Question markenNameQuestion = parsedSurveyXml.getQuestions().get(MARKET_NAME_BINDING);
                    commodity.getQuestions().add(markenNameQuestion);
                    topMarketSurvey.getCommodities().add(commodity);
                }
                else {
                    commodity = getCommoditybyName(commodityPrices, commodityName);
                    commodity.getQuestions().add(question);
                }
            }
        }
        logger.warn("Commodity Prices for one Market Survey: " + commodityPrices.size());
//        commodityPrices = cloneCommodities(commodityPrices, relatedMarketSurveys);
        logger.warn("Commodity Prices for" + relatedMarketSurveys.size() + " Market Surveys: " + commodityPrices.size());
        commodityPrices = setCommodityPrices(survey.getSubmissions(true).values(), commodityPrices, relatedMarketSurveys, survey);
        List<Commodity> marketDayCommodities = new ArrayList<Commodity>();
        if(commodityPrices != null){
	        for(Commodity marketDayCommodity: commodityPrices){
	        	for(MarketSurveyObject marketDayObject: marketSurveys){
	        		if(marketDayObject.getMarketName() != null && marketDayCommodity.getMarketName() != null 
	        				&& marketDayCommodity.getMarketName().toLowerCase().equals(marketDayObject.getMarketName().toLowerCase())){
	        			marketDayCommodities.add(marketDayCommodity);
	        			break;
	        		}
	        	}
	        }
        }
        if (marketDayCommodities != null && marketDayCommodities.size() != 0) {
            logger.warn("null check for commodity prices");
            logger.warn("commodity prices count ::" + marketDayCommodities.size());
        }
        else {
            logger.warn("Commodity prices object null or empty for: " + survey.getSalesforceId());
        }
        return marketDayCommodities;
    }

    public List<Commodity> setCommodityPrices(Collection<Submission> surveySubmissions, List<Commodity> commodities,
                                              List<MarketSurveyObject> marketSurveys, Survey currentSurvey)
            throws ClassNotFoundException, SQLException, ParseException {

        Submission[] allSubmissions = new Submission[surveySubmissions.size()];
        allSubmissions = surveySubmissions.toArray(allSubmissions);
        logger.warn("all submissions count = " + allSubmissions.length);
        for (MarketSurveyObject marketSurvey : marketSurveys) {
            Submission[] submissions = filterSubmissionsByPersonIds(allSubmissions, marketSurvey, currentSurvey);
            logger.warn("relevant submissions count = " + submissions.length);
            if (submissions.length == 0) {
                return null;
            }
            else if (submissions.length >= 2) {
                for (Commodity commodity : commodities) {
                    double wholesaleWeightUnit1 = 0;
                    double retailWeightUnit1 = 0;
                    double wholesaleWeightUnit2 = 0;
                    double retailWeightUnit2 = 0;
                    double highWholesalePrice1 = 0;
                    double highWholesalePrice2 = 0;
                    double lowWholesalePrice1 = 0;
                    double lowWholesalePrice2 = 0;
                    double highRetailPrice1 = 0;
                    double highRetailPrice2 = 0;
                    double lowRetailPrice1 = 0;
                    double lowRetailPrice2 = 0;
                    String market1 = "";
                    String market2 = "";
                    for (Question question : commodity.getQuestions()) {
                        Answer answer1 = submissions[0].getAnswer(question.getBinding() + "_1");
                        Answer answer2 = submissions[1].getAnswer(question.getBinding() + "_1");
//                        logger.warn("Question:: " + question.getBinding() + " answer1 :: " + answer1.getRawAnswerText() + " answer2 :: "
//                                + answer2.getRawAnswerText());
                        // Check if the question was answered. Its possible that a particular commodity is not in the
                        // market.
                        if (answer1 != null && answer2 != null) {
                            // Check if answers to both submissions are to weight related questions
                            if (answer1.getParentQuestion().getBinding().endsWith("wholesale")
                                    && answer2.getParentQuestion().getBinding().endsWith("wholesale")) {
                                wholesaleWeightUnit1 = extractNumberFromString(answer1.getRawAnswerText());
                                wholesaleWeightUnit2 = extractNumberFromString(answer2.getRawAnswerText());
                            }
                            else if (answer1.getParentQuestion().getBinding().endsWith("retail")
                                    && answer2.getParentQuestion().getBinding().endsWith("retail")) {
                                retailWeightUnit1 = extractNumberFromString(answer1.getRawAnswerText());
                                retailWeightUnit2 = extractNumberFromString(answer2.getRawAnswerText());
                            }
                            else if (answer1.getParentQuestion().getBinding().startsWith("high_wholesale_price_")
                                    && answer2.getParentQuestion().getBinding().startsWith("high_wholesale_price_")) {
                                highWholesalePrice1 = Double.valueOf(answer1.getRawAnswerText());
                                highWholesalePrice2 = Double.valueOf(answer2.getRawAnswerText());
                            }
                            else if (answer1.getParentQuestion().getBinding().startsWith("low_wholesale_price_")
                                    && answer2.getParentQuestion().getBinding().startsWith("low_wholesale_price_")) {
                                lowWholesalePrice1 = Double.valueOf(answer1.getRawAnswerText());
                                lowWholesalePrice2 = Double.valueOf(answer2.getRawAnswerText());
                            }
                            else if (answer1.getParentQuestion().getBinding().startsWith("high_retail_price_")
                                    && answer2.getParentQuestion().getBinding().startsWith("high_retail_price_")) {
                                highRetailPrice1 = Double.valueOf(answer1.getRawAnswerText());
                                highRetailPrice2 = Double.valueOf(answer2.getRawAnswerText());
                            }
                            else if (answer1.getParentQuestion().getBinding().startsWith("low_retail_price_")
                                    && answer2.getParentQuestion().getBinding().startsWith("low_retail_price_")) {
                                lowRetailPrice1 = Double.valueOf(answer1.getRawAnswerText());
                                lowRetailPrice2 = Double.valueOf(answer2.getRawAnswerText());
                            }
                            else if(answer1.getParentQuestion().getBinding().startsWith("market_name")
                                    && answer2.getParentQuestion().getBinding().startsWith("market_name")) {
                            	market1 = answer1.getFriendlyAnswerText(false, currentSurvey).trim();
                            	market2 = answer2.getFriendlyAnswerText(false, currentSurvey).trim();
//                        		logger.warn(market1 + ", " + market2 + " market submissions");
                            }
                        }
                    }
                    double[] highWholesaleValues = comparePrices(highWholesalePrice1, wholesaleWeightUnit1, highWholesalePrice2,
                            wholesaleWeightUnit2, true);
                    double[] lowWholesaleValues = comparePrices(lowWholesalePrice1, wholesaleWeightUnit1, lowWholesalePrice2,
                            wholesaleWeightUnit2, false);
                    double[] highRetailValues = comparePrices(highRetailPrice1, retailWeightUnit1, highRetailPrice2, retailWeightUnit2,
                            true);
                    double[] lowRetailValues = comparePrices(lowRetailPrice1, retailWeightUnit1, lowRetailPrice2, retailWeightUnit2, false);

                    commodity.setRawHighWholesalePrice(highWholesaleValues[0]);
                    commodity.setWeightOfWholesaleUnitOfMeasure(highWholesaleValues[1]);
                    commodity.setRawLowWholesalePrice(lowWholesaleValues[0]);
                    commodity.setRawHighRetailPrice(highRetailValues[0]);
                    commodity.setWeightOfRetailUnitOfMeasure(highRetailValues[1]);
                    commodity.setRawLowRetailPrice(lowRetailValues[0]);

                    commodity.setHighWholesalePrice(highWholesaleValues[0]);
                    commodity.setLowWholesalePrice(lowWholesaleValues[0]);
                    commodity.setHighRetailPrice(highRetailValues[0]);
                    commodity.setLowRetailPrice(lowRetailValues[0]);                    

            		if(market1.equals(market2)){
            			commodity = setSubcountyDistrictAndRegionByMarketName(commodity, market1);
            		}
                }
            }
            else if (submissions.length == 1) {
                if (this.previousCommodityPrices == null) {
                    this.previousCommodityPrices = getPreviousCommodityPrices();
                }

                for (Commodity commodity : commodities) {
                    Commodity previousCommodity = getCommodityByNameAndMarketName(commodity.getName(), commodity.getMarketName(),
                            this.previousCommodityPrices);
                    // Check if there are any previous commodity prices for this commodity.
                    // Returns null if this is the first entry, in which case no comparison is required.
                    if (previousCommodity == null) {
                        previousCommodity = getPreviousCommodityPrice(commodity.getName(), commodity.getMarketName());
                    }
                    if (previousCommodity == null) {
                        //logger.warn("Commodity questions count:: " + commodity.getQuestions().size());
                        String temp = "";
                        for (String k : submissions[0].getAnswers().keySet()) {
                            temp = temp + ", " + k;
                        }
                        //logger.warn("Answer keys::" + temp);
                        for (Question question : commodity.getQuestions()) {
                            Answer answer = submissions[0].getAnswer(question.getBinding() + "_1");
                            // Check if the question was answered. Its possible that a particular commodity is not in
                            // the
                            // market.
                            if (answer != null) {
                                //logger.warn("Question:: " + question.getBinding() + " answer :: " + answer.getRawAnswerText());
                                // Check if answer is to weight related questions
                                if (answer.getParentQuestion().getBinding().endsWith("wholesale")) {
                                    commodity.setWeightOfWholesaleUnitOfMeasure(extractNumberFromString(answer.getRawAnswerText()));
                                }
                                else if (answer.getParentQuestion().getBinding().endsWith("retail")) {
                                    commodity.setWeightOfRetailUnitOfMeasure(extractNumberFromString(answer.getRawAnswerText()));
                                }
                                else if (answer.getParentQuestion().getBinding().startsWith("high_wholesale_price_")) {
                                    commodity.setHighWholesalePrice(Double.valueOf(answer.getRawAnswerText()));
                                }
                                else if (answer.getParentQuestion().getBinding().startsWith("low_wholesale_price_")) {
                                    commodity.setLowWholesalePrice(Double.valueOf(answer.getRawAnswerText()));
                                }
                                else if (answer.getParentQuestion().getBinding().startsWith("high_retail_price_")) {
                                    commodity.setHighRetailPrice(Double.valueOf(answer.getRawAnswerText()));
                                }
                                else if (answer.getParentQuestion().getBinding().startsWith("low_retail_price_")) {
                                    commodity.setLowRetailPrice(Double.valueOf(answer.getRawAnswerText()));
                                }
                                else if(answer.getParentQuestion().getBinding().startsWith("market_name")) {
                                	String marketName = answer.getFriendlyAnswerText(false, currentSurvey).trim();
//                            		logger.warn(marketName + ", market submission");
                        			commodity = setSubcountyDistrictAndRegionByMarketName(commodity, marketName);
                                }
                            }
                        }
                    }

                    else {
                        double wholesaleWeightUnit1 = 0;
                        double retailWeightUnit1 = 0;
                        double highWholesalePrice1 = 0;
                        double lowWholesalePrice1 = 0;
                        double highRetailPrice1 = 0;
                        double lowRetailPrice1 = 0;
                        for (Question question : commodity.getQuestions()) {
                            Answer answer = submissions[0].getAnswer(question.getBinding());
                            // Check if the question was answered. Its possible that a particular commodity is not in
                            // the
                            // market.
                            if (answer != null) {
                                // Check if answers to both submissions are to weight related questions
                                if (answer.getParentQuestion().getBinding().endsWith("wholesale")) {
                                    wholesaleWeightUnit1 = extractNumberFromString(answer.getRawAnswerText());
                                }
                                else if (answer.getParentQuestion().getBinding().endsWith("retail")) {
                                    retailWeightUnit1 = extractNumberFromString(answer.getRawAnswerText());
                                }
                                else if (answer.getParentQuestion().getBinding().startsWith("high_wholesale_price_")) {
                                    highWholesalePrice1 = Double.valueOf(answer.getRawAnswerText());
                                }
                                else if (answer.getParentQuestion().getBinding().startsWith("low_wholesale_price_")) {
                                    lowWholesalePrice1 = Double.valueOf(answer.getRawAnswerText());
                                }
                                else if (answer.getParentQuestion().getBinding().startsWith("high_retail_price_")) {
                                    highRetailPrice1 = Double.valueOf(answer.getRawAnswerText());
                                }
                                else if (answer.getParentQuestion().getBinding().startsWith("low_retail_price_")) {
                                    lowRetailPrice1 = Double.valueOf(answer.getRawAnswerText());
                                }
                                else if(answer.getParentQuestion().getBinding().startsWith("market_name")) {
                                	String marketName = answer.getFriendlyAnswerText(false, currentSurvey).trim();
//                            		logger.warn(marketName + ", market submission");
                        			commodity = setSubcountyDistrictAndRegionByMarketName(commodity, marketName);
                                }
                            }
                        }
                        double highWholesaleValue = comparePrices(highWholesalePrice1, wholesaleWeightUnit1,
                                previousCommodity.getHighWholesalePrice(), true);
                        double lowWholesaleValue = comparePrices(lowWholesalePrice1, wholesaleWeightUnit1,
                                previousCommodity.getLowWholesalePrice(), false);
                        double highRetailValue = comparePrices(highRetailPrice1, retailWeightUnit1, previousCommodity.getHighRetailPrice(),
                                true);
                        double lowRetailValue = comparePrices(lowRetailPrice1, retailWeightUnit1, previousCommodity.getLowRetailPrice(),
                                false);

                        commodity.setHighWholesalePrice(highWholesaleValue);
                        commodity.setLowWholesalePrice(lowWholesaleValue);
                        commodity.setHighRetailPrice(highRetailValue);
                        commodity.setLowRetailPrice(lowRetailValue);
                    }
                }

            }
        }
        return commodities;
    }

    private Commodity setSubcountyDistrictAndRegionByMarketName(Commodity commodity, String marketName){
    	for(MarketSurveyObject marketObject: marketSurveys){
    		if(marketObject.getMarketName().toLowerCase().equals(marketName.toLowerCase().trim())){
    			commodity.setMarketName(marketName);
                commodity.setRegionName(marketObject.getRegionName());
                commodity.setDistrictName(marketObject.getDistrictName());
                commodity.setSubcountyName(marketObject.getSubcountyName());
            	return commodity;
    		}
    	}
    	logger.warn("could not get marketSurveyObject for " + commodity.getMarketName());
        return commodity;
    }
    
    private Submission[] filterSubmissionsByPersonIds(Submission[] allSubmissions, MarketSurveyObject marketSurvey, Survey currentSurvey) throws ClassNotFoundException, SQLException {
        if (allSubmissions.length == 0) {
            return allSubmissions;
        }
        List<Submission> relevantSubmissions = new ArrayList<Submission>();
        for (int i = 0; i < allSubmissions.length; i++) {
            logger.warn("Check for relevance PersonId:: " + allSubmissions[i].getInterviewerId());
            if (marketSurvey.getPersonIds().contains(allSubmissions[i].getInterviewerId())
                    || marketSurvey.getCkwIds().contains(allSubmissions[i].getInterviewerId())
                    || marketSurvey.getSurveyName().equals(currentSurvey.getSalesforceId())) {
                relevantSubmissions.add(allSubmissions[i]);
            }
        }
        Submission[] submissions = new Submission[relevantSubmissions.size()];
        submissions = relevantSubmissions.toArray(submissions);
        return submissions;
    }

    public double[] comparePrices(double price1, double weightOfUnitOfMeasure1, double price2, double weightOfUnitOfMeasure2,
                                  boolean getHigher) {
        double normalisedPrice1 = price1 / weightOfUnitOfMeasure1;
        double normalisedPrice2 = price2 / weightOfUnitOfMeasure2;
        double[] result = new double[2];
        if (normalisedPrice1 == 0 || normalisedPrice2 == 0) {
            if (normalisedPrice1 == 0 && normalisedPrice2 != 0) {
                result[0] = price2;
                result[1] = weightOfUnitOfMeasure2;
            }
            else {
                result[0] = price1;
                result[1] = weightOfUnitOfMeasure1;
            }
        }
        else {

            if (normalisedPrice1 > normalisedPrice2) {
                if (getHigher) {
                    result[0] = price1;
                    result[1] = weightOfUnitOfMeasure1;
                }
                else {
                    result[0] = price2;
                    result[1] = weightOfUnitOfMeasure2;
                }
            }
            else {
                if (getHigher) {
                    result[0] = price2;
                    result[1] = weightOfUnitOfMeasure2;
                }
                else {
                    result[0] = price1;
                    result[1] = weightOfUnitOfMeasure1;
                }
            }
        }
        return result;
    }

    public double comparePrices(double price1, double weightOfUnitOfMeasure1, double normalisedPrice2, boolean getHigher) {
        double result = 0;
    	if(weightOfUnitOfMeasure1 != 0){
	        double normalisedPrice1 = price1 / weightOfUnitOfMeasure1;
	
	        if (normalisedPrice1 > normalisedPrice2) {
	            if (getHigher) {
	                result = normalisedPrice1;
	            }
	            else {
	                result = normalisedPrice2;
	            }
	        }
	        else {
	            if (getHigher) {
	                result = normalisedPrice2;
	            }
	            else {
	                result = normalisedPrice1;
	            }
	        }
    	}
    	else{
	        double unnormalisedPrice = price1;
	    	
	        if (unnormalisedPrice >= normalisedPrice2 * 2 || unnormalisedPrice <= normalisedPrice2 / 2) {
	        	//outside bounds
	        	logger.warn("price was outside the acceptable range. mataining previous");
	            result = normalisedPrice2;
	        }
	        else {
	            result = unnormalisedPrice;
	        }
    	}
        return result;
    }

    public Commodity getCommoditybyName(List<Commodity> commodities, String name) {
        for (Commodity commodity : commodities) {
            if (commodity.getName().equals(name)) {
                return commodity;
            }
        }
        return null;
    }

    /**
     * Finds the previous week's prices for all relevant commodities from the backend database
     * 
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ParseException
     */
    public ArrayList<Commodity> getPreviousCommodityPrices() throws ClassNotFoundException, SQLException, ParseException {
        Connection connection = SurveyDatabaseHelpers.getReaderConnection();
        StringBuilder commandText = new StringBuilder();
        commandText.append("SELECT c.id AS Id, ");
        commandText.append("c.name AS Name, ");
        commandText.append("c.market_name AS marketName, ");
        commandText.append("c.high_wholesale_price AS highWholesalePrice, ");
        commandText.append("c.low_wholesale_price AS lowWholesalePrice, ");
        commandText.append("c.high_retail_price AS highRetailPrice, ");
        commandText.append("c.low_retail_price AS lowRetailPrice, ");
        commandText.append("c.date_created AS dateCreated ");
        commandText.append("FROM commodityprices AS c ");
        commandText.append("WHERE c.date_created = DATE(DATE_SUB(CURDATE(), INTERVAL 1 WEEK))");
        PreparedStatement preparedStatement = connection.prepareStatement(commandText.toString());
        ResultSet resultSet = preparedStatement.executeQuery();
        ArrayList<Commodity> commodities = new ArrayList<Commodity>();
        logger.warn("Loading privious Commodity Prices");
        while (resultSet.next()) {
            Commodity commodity = new Commodity();
            commodity.setId(resultSet.getInt("Id"));
            commodity.setName(resultSet.getString("Name"));
            commodity.setLastUpdateDate(DatabaseHelpers.getJavaDateFromString(resultSet.getString("dateCreated"), 3));
            commodity.setLowRetailPrice(resultSet.getDouble("lowRetailPrice"));
            commodity.setLowWholesalePrice(resultSet.getDouble("lowWholesalePrice"));
            commodity.setHighRetailPrice(resultSet.getDouble("highRetailPrice"));
            commodity.setHighWholesalePrice(resultSet.getDouble("highWholesalePrice"));
            commodity.setMarketName(resultSet.getString("marketName"));
            commodities.add(commodity);
        }
        logger.warn("Loaded previous commodities: " + commodities.size());
        return commodities;
    }

    public HashMap<String, Integer> getPreviousMarketPriceKeywords() throws ClassNotFoundException, SQLException {
        Connection connection = SurveyDatabaseHelpers.getSearchReaderConnection();
        StringBuilder commandText = new StringBuilder();
        commandText.append("SELECT k.id AS Id, ");
        commandText.append("k.keyword AS priceKeyword ");
        commandText.append("FROM keyword AS k ");
        commandText.append("WHERE k.categoryId = ");
        commandText.append(this.categoryId);
        commandText.append(" AND k.isDeleted = 0 ");
        commandText.append("AND k.keyword LIKE '");
        commandText.append(CATEGORY + " " + BASE_KEYWORD);
        commandText.append("%'");
        logger.warn("Select query " + commandText.toString());
        PreparedStatement preparedStatement = connection.prepareStatement(commandText.toString());
        ResultSet resultSet = preparedStatement.executeQuery();
        logger.warn("Loading previous keywords");
        HashMap<String, Integer> map = new HashMap<String, Integer>();

        while (resultSet.next()) {
            String keyword = resultSet.getString("priceKeyword");
            if (!map.containsKey(keyword)) {
                map.put(keyword, resultSet.getInt("Id"));
            }
        }
        logger.warn("Loaded previous commodities: " + map.size());
        return map;
    }

    /**
     * Finds the most recent commodity price entries for a given commodity in the database. This is called if there were
     * no submissions for this commodity in the previous week
     * 
     * @param commodityName
     * @param marketName
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ParseException
     */
    public Commodity getPreviousCommodityPrice(String commodityName, String marketName) throws ClassNotFoundException, SQLException,
            ParseException {
        Connection connection = SurveyDatabaseHelpers.getReaderConnection();
        StringBuilder commandText = new StringBuilder();
        commandText.append("SELECT c.id AS Id,");
        commandText.append("c.name AS Name,");
        commandText.append("c.market_name AS marketName,");
        commandText.append("c.high_wholesale_price AS highWholesalePrice, ");
        commandText.append("c.low_wholesale_price AS lowWholesalePrice, ");
        commandText.append("c.high_retail_price AS highRetailPrice, ");
        commandText.append("c.low_retail_price AS lowRetailPrice, ");
        commandText.append("c.date_created AS dateCreated ");
        commandText.append("FROM commodityprices AS c ");
        commandText.append(" WHERE c.market_name = ? ");
        commandText.append(" AND c.name = ? ");
        commandText.append(" ORDER BY c.date_created DESC");
        PreparedStatement preparedStatement = connection.prepareStatement(commandText.toString());
        preparedStatement.setString(1, marketName);
        preparedStatement.setString(2, commodityName);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            Commodity commodity = new Commodity();
            commodity.setId(resultSet.getInt("Id"));
            commodity.setName(resultSet.getString("Name"));
            commodity.setLastUpdateDate(DatabaseHelpers.getJavaDateFromString(resultSet.getString("dateCreated"), 3));
            commodity.setLowRetailPrice(resultSet.getDouble("lowRetailPrice"));
            commodity.setLowWholesalePrice(resultSet.getDouble("lowWholesalePrice"));
            commodity.setHighRetailPrice(resultSet.getDouble("highRetailPrice"));
            commodity.setHighWholesalePrice(resultSet.getDouble("highWholesalePrice"));
            commodity.setMarketName(resultSet.getString("marketName"));
            return commodity;
        }
        return null;
    }

    /**
     * Clone Commodities for Surveys used for more than one market
     * 
     * @param commodities
     * @param marketSurveys
     */
    public List<Commodity> cloneCommodities(List<Commodity> commodities, List<MarketSurveyObject> marketSurveys) {
        if (marketSurveys.size() > 1) {
            MarketSurveyObject topMarketSurvey = marketSurveys.get(0);
            for (int i = 1; i < marketSurveys.size(); i++) {
                for (Commodity commodity : topMarketSurvey.getCommodities()) {
                    MarketSurveyObject marketSurvey = marketSurveys.get(i);
                    Commodity commodityClone = (Commodity)commodity.clone();
                    commodityClone.setMarketName(marketSurvey.getMarketName());
                    marketSurvey.getCommodities().add(commodityClone);
                    commodities.add(commodityClone);
                }
            }
        }
        return commodities;
    }

    public Commodity getCommodityByNameAndMarketName(String commodityName, String marketName, ArrayList<Commodity> commodities) {
        for (Commodity commodity : commodities) {
            if (commodity.getName().equals(commodityName) && commodity.getMarketName().equals(marketName)) {
                return commodity;
            }
        }
        return null;
    }

    public boolean isCommodityPriceRelatedBinding(String binding) {

        Matcher matcher;
        for (String bindingPatternKey : bindingPatterns.keySet()) {
            matcher = bindingPatterns.get(bindingPatternKey).matcher(binding);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    public String getCommodityName(String binding) {

        Matcher matcher;
        for (String bindingPatternKey : bindingPatterns.keySet()) {
            matcher = bindingPatterns.get(bindingPatternKey).matcher(binding);
            if (matcher.matches()) {
                String[] splitBinding = binding.split("_");
                if (bindingPatternKey.startsWith("low") || bindingPatternKey.startsWith("high")) {
                	String name = binding.split("_price_")[1];
                	if(name.contains("_per_")){
                		//binding contains SI units
                		String realName = name.split("_per_")[0];
                		String units = name.split("_per_")[1];
                		return realName.replace('_', ' ') + "/" + units.replace('_', ' ');
                	}
                	else{
                		return name.replace('_', ' ');
                	}
                }
                else {
                    return splitBinding[splitBinding.length - 2];
                }
            }

        }
        return null;
    }

    public double extractNumberFromString(String text) {
        char[] characaters = text.toCharArray();
        String newText = "";
        for (char character : characaters) {
            String item = String.valueOf(character);
            if (numbers.contains(item)) {
                newText = newText + item;
            }
            else if (newText.length() > 0) {
                return Double.valueOf(newText);
            }
            else {
                return 0;
            }
        }
        if (newText.length() > 0) {
            return Double.valueOf(newText);
        }
        else {
            return 0;
        }
    }

    private ArrayList<String> createNumberPatterns() {
        ArrayList<String> numbers = new ArrayList<String>();
        String[] digits = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "." };
        for (String digit : digits) {
            numbers.add(digit);
        }
        return numbers;
    }

    /**
     * Compiles Regular Expression patterns to be used in matching relevant survey questions
     * 
     * @return
     */
    public HashMap<String, Pattern> compilePatterns() {
        HashMap<String, Pattern> patterns = new HashMap<String, Pattern>();
        patterns.put(HIGH_WHOLESALE_PRICE, Pattern.compile(HIGH_WHOLESALE_PRICE));
        patterns.put(LOW_WHOLESALE_PRICE, Pattern.compile(LOW_WHOLESALE_PRICE));
        patterns.put(HIGH_RETAIL_PRICE, Pattern.compile(HIGH_RETAIL_PRICE));
        patterns.put(LOW_RETAIL_PRICE, Pattern.compile(LOW_RETAIL_PRICE));
        patterns.put(WHOLESALE_UNIT_OF_MEASUREMENT_WEIGHT, Pattern.compile(WHOLESALE_UNIT_OF_MEASUREMENT_WEIGHT));
        patterns.put(RETAIL_UNIT_OF_MEASUREMENT_WEIGHT, Pattern.compile(RETAIL_UNIT_OF_MEASUREMENT_WEIGHT));
        return patterns;
    }

}
