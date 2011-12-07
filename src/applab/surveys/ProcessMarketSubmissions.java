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
import java.util.Set;
import java.util.regex.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;
import org.xml.sax.SAXException;

import com.sforce.soap.enterprise.fault.InvalidFieldFault;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.InvalidQueryLocatorFault;
import com.sforce.soap.enterprise.fault.InvalidSObjectFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.MalformedQueryFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;

import applab.server.DatabaseHelpers;
import applab.server.DateHelpers;
import applab.surveys.server.SurveyDatabaseHelpers;
import applab.surveys.server.SurveysSalesforceProxy;

public class ProcessMarketSubmissions {

    private final String HIGH_WHOLESALE_PRICE = "high_wholesale_price_[a-z]*";
    private final String LOW_WHOLESALE_PRICE = "low_wholesale_price_[a-z]*";
    private final String HIGH_RETAIL_PRICE = "high_retail_price_[a-z]*";
    private final String LOW_RETAIL_PRICE = "low_retail_price_[a-z]*";
    private final String WHOLESALE_UNIT_OF_MEASUREMENT_WEIGHT = "weight_unit_measurement_[a-z]*_wholesale";
    private final String RETAIL_UNIT_OF_MEASUREMENT_WEIGHT = "weight_unit_measurement_[a-z]*_retail";
    private HashMap<String, Pattern> bindingPatterns;
    private HashMap<String, MarketSurveyObject> marketSurveys;
    private ArrayList<Survey> surveys;
    private String marketDay;
    private ArrayList<Commodity> previousCommodityPrices;
    private ArrayList<Commodity> currentCommodityPrices;
    private SurveysSalesforceProxy surveysSalesForceProxy;

    public ProcessMarketSubmissions() throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException {
        surveysSalesForceProxy = new SurveysSalesforceProxy();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        marketDay = DateHelpers.getDayOfWeek(calendar.getTime());
    }

    /**
     * Orchestrates processing of market price submissions
     * 
     * @throws Exception
     */
    public boolean performSubmissionProcessing() throws Exception {

        this.previousCommodityPrices = null;
        this.bindingPatterns = compilePatterns();
        marketSurveys = surveysSalesForceProxy.getCommoditiesByMarketDay(marketDay);
        if (marketSurveys.isEmpty()) {
            throw new Exception("No surveys for the give day" + marketDay);
        }

        Set<String> salesforceIds = new HashSet<String>();
        for (String groupName : marketSurveys.keySet()) {
            salesforceIds.add(marketSurveys.get(groupName).getSurveyName());
        }
        this.surveys = loadSurveys(salesforceIds);
        this.currentCommodityPrices = processCommodityPrices(surveys);
        boolean hasSavedCommodityPrices = saveCommodityPrices(currentCommodityPrices);
        return hasSavedCommodityPrices;
    }

    /**
     * Loads previous day's market surveys including submissions to be processed
     * 
     * @param salesforceSurveyIds
     * @return
     * @throws Exception
     */
    public ArrayList<Survey> loadSurveys(Set<String> salesforceSurveyIds) throws Exception {

        ArrayList<Survey> surveys = new ArrayList<Survey>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -4);
        java.sql.Date startDate = new java.sql.Date(calendar.getTimeInMillis());
        java.sql.Date endDate = new java.sql.Date(calendar.getTimeInMillis());

        for (String salesforceSurveyId : salesforceSurveyIds) {
            try {
                Survey survey = new Survey(salesforceSurveyId);
                survey.loadSubmissions(SubmissionStatus.Approved, startDate, endDate, false, salesforceSurveyId, false, true);
                surveys.add(survey);
            }
            catch (Exception ex) {
                throw new Exception("salesforceId" + salesforceSurveyId + ex.getMessage() + ex.getClass().getName());
            }
        }
        return surveys;
    }

    public boolean saveCommodityPrices(ArrayList<Commodity> commodities) throws InvalidSObjectFault, MalformedQueryFault,
            InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException {
        boolean savedToBackEnd = false;
        boolean savedToSalesForce = false;
        try {
            savedToBackEnd = saveCommodityPricesToBackendDataBase(commodities);
            savedToSalesForce = saveCommodityPricesToSalesForce(commodities);            
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return savedToBackEnd && savedToSalesForce;
    }

    public boolean saveCommodityPricesToSalesForce(ArrayList<Commodity> commodities) throws InvalidSObjectFault, MalformedQueryFault,
            InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault, InvalidQueryLocatorFault, RemoteException {
        return surveysSalesForceProxy.updateCommodities(marketDay, commodities);
    }

    public boolean saveCommodityPricesToBackendDataBase(ArrayList<Commodity> commodities)
            throws ClassNotFoundException, SQLException {
        // Create the connection to the database
        Connection connection = SurveyDatabaseHelpers.getWriterConnection();
        connection.setAutoCommit(false);

        StringBuilder commandText = new StringBuilder();
        commandText.append("INSERT INTO commodityprices ");
        commandText.append("(name, market_name, ");
        commandText.append("(high_wholesale_price, low_wholesale_price, high_retail_price, low_retail_price, ");
        commandText.append("date_created");
        commandText.append(") values (?, ?, ?, ?, ?, ?, CURDATE())");
        PreparedStatement submissionStatement = connection.prepareStatement(commandText.toString());

        for (Commodity commodity : commodities) {
            submissionStatement.setString(0, commodity.getName());
            submissionStatement.setString(1, commodity.getMarketName());
            submissionStatement.setDouble(2, commodity.getHighWholesalePrice());
            submissionStatement.setDouble(3, commodity.getLowWholesalePrice());
            submissionStatement.setDouble(4, commodity.getHighRetailPrice());
            submissionStatement.setDouble(5, commodity.getHighRetailPrice());
            submissionStatement.addBatch();
        }
        try {
            submissionStatement.executeBatch();
        }
        catch (SQLException e) {
            e.printStackTrace();
            connection.rollback();
            connection.setAutoCommit(true);
            submissionStatement.close();
            return false;
        }

        connection.commit();
        connection.setAutoCommit(true);
        submissionStatement.close();
        return true;
    }

    public ArrayList<Commodity> processCommodityPrices(ArrayList<Survey> surveys) throws SAXException, IOException,
            ParserConfigurationException, ClassNotFoundException, SQLException, ParseException {

        ArrayList<Commodity> allcommodityPrices = new ArrayList<Commodity>();
        ParsedSurveyXml parsedSurveyXml = null;
        for (Survey survey : surveys) {
            parsedSurveyXml = survey.getBackEndSurveyXml();
            HashSet<String> commodityNames = new HashSet<String>();
            ArrayList<Commodity> commodityPrices = new ArrayList<Commodity>();
            for (String questionBinding : parsedSurveyXml.getQuestionOrder()) {

                if (isCommodityPriceRelatedBinding(questionBinding)) {
                    
                    Question question = parsedSurveyXml.getQuestions().get(questionBinding);
                    String commodityName = getCommodityName(questionBinding);
                    Commodity commodity = null;

                    if (!commodityNames.contains(commodityName)) {
                        commodity = new Commodity(commodityName);
                        commodityNames.add(commodityName);
                        commodityPrices.add(commodity);
                        commodity.getQuestions().add(question);
                        commodity.setMarketName(marketSurveys.get(survey.getSalesforceId()).getMarketName());
                    }
                    else {
                        commodity = getCommoditybyName(commodityPrices, commodityName);
                    }

                }
            }
            commodityPrices = setCommodityPrices(survey.getSubmissions(false).values(), commodityPrices);
            if (commodityPrices != null) {
                allcommodityPrices.addAll(commodityPrices);
            }
        }
        return allcommodityPrices;
    }

    public ArrayList<Commodity> setCommodityPrices(Collection<Submission> surveySubmissions, ArrayList<Commodity> commodities)
            throws ClassNotFoundException, SQLException, ParseException {

        Submission[] submissions = new Submission[surveySubmissions.size()];
        submissions = surveySubmissions.toArray(submissions);
        if (submissions.length == 0) {
            return null;
        }
        else if (submissions.length == 2) {
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
                for (Question question : commodity.getQuestions()) {
                    Answer answer1 = submissions[0].getAnswer(question.getBinding());
                    Answer answer2 = submissions[1].getAnswer(question.getBinding());
                    // Check if the question was answered. Its possible that a particular commodity is not in the
                    // market.
                    if (answer1 != null && answer2 != null) {
                        // Check if answers to both submissions are to weight related questions
                        if (answer1.getParentQuestion().getBinding().endsWith("wholesale")
                                && answer2.getParentQuestion().getBinding().endsWith("wholesale")) {
                            wholesaleWeightUnit1 = Double.valueOf(answer1.getRawAnswerText());
                            wholesaleWeightUnit2 = Double.valueOf(answer2.getRawAnswerText());
                        }
                        else if (answer1.getParentQuestion().getBinding().endsWith("retail")
                                && answer2.getParentQuestion().getBinding().endsWith("retail")) {
                            retailWeightUnit1 = Double.valueOf(answer1.getRawAnswerText());
                            retailWeightUnit2 = Double.valueOf(answer2.getRawAnswerText());
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
                    }
                }
                double[] highWholesaleValues = comparePrices(highWholesalePrice1, wholesaleWeightUnit1, highWholesalePrice2,
                        wholesaleWeightUnit2, true);
                double[] lowWholesaleValues = comparePrices(lowWholesalePrice1, wholesaleWeightUnit1, lowWholesalePrice2,
                        wholesaleWeightUnit2, false);
                double[] highRetailValues = comparePrices(highRetailPrice1, retailWeightUnit1, highRetailPrice2, retailWeightUnit2, true);
                double[] lowRetailValues = comparePrices(lowRetailPrice1, retailWeightUnit1, lowRetailPrice2, retailWeightUnit2, false);

                commodity.setRawHighWholesalePrice(highWholesaleValues[0]);
                commodity.setWeightOfWholesaleUnitOfMeasure(highWholesaleValues[1]);
                commodity.setRawLowWholesalePrice(lowWholesaleValues[0]);
                commodity.setRawHighRetailPrice(highRetailValues[0]);
                commodity.setWeightOfRetailUnitOfMeasure(highRetailValues[1]);
                commodity.setRawLowRetailPrice(lowRetailValues[0]);

            }
        }
        else if (submissions.length == 1) {
            if (this.previousCommodityPrices == null) {
                this.previousCommodityPrices = getPreviousCommodityPrices();
            }

            for (Commodity commodity : commodities) {
                Commodity previousCommodity = getCommodityByNameAndMarketName(commodity.getName(), commodity.getMarketName(),
                        this.previousCommodityPrices);
                if (previousCommodity == null) {
                    previousCommodity = getPreviousCommodityPrice(commodity.getName(), commodity.getMarketName());
                }
                // Check if there are any previous commodity prices for this commodity.
                // Returns null if this is the first entry, in which case no comparison is required.
                if (previousCommodity == null) {
                    for (Question question : commodity.getQuestions()) {
                        Answer answer = submissions[0].getAnswer(question.getBinding());
                        // Check if the question was answered. Its possible that a particular commodity is not in the
                        // market.
                        if (answer != null) {
                            // Check if answer is to weight related questions
                            if (answer.getParentQuestion().getBinding().endsWith("wholesale")) {
                                commodity.setWeightOfWholesaleUnitOfMeasure(Double.valueOf(answer.getRawAnswerText()));
                            }
                            else if (answer.getParentQuestion().getBinding().endsWith("retail")) {
                                commodity.setWeightOfRetailUnitOfMeasure(Double.valueOf(answer.getRawAnswerText()));
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
                        // Check if the question was answered. Its possible that a particular commodity is not in the
                        // market.
                        if (answer != null) {
                            // Check if answers to both submissions are to weight related questions
                            if (answer.getParentQuestion().getBinding().endsWith("wholesale")) {
                                wholesaleWeightUnit1 = Double.valueOf(answer.getRawAnswerText());
                            }
                            else if (answer.getParentQuestion().getBinding().endsWith("retail")) {
                                retailWeightUnit1 = Double.valueOf(answer.getRawAnswerText());
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
                        }
                    }
                    double highWholesaleValue = comparePrices(highWholesalePrice1, wholesaleWeightUnit1,
                            previousCommodity.getHighWholesalePrice(), true);
                    double lowWholesaleValue = comparePrices(lowWholesalePrice1, wholesaleWeightUnit1,
                            previousCommodity.getLowWholesalePrice(), false);
                    double highRetailValue = comparePrices(highRetailPrice1, retailWeightUnit1, previousCommodity.getHighRetailPrice(),
                            true);
                    double lowRetailValue = comparePrices(lowRetailPrice1, retailWeightUnit1, previousCommodity.getLowRetailPrice(), false);

                    commodity.setHighWholesalePrice(highWholesaleValue);
                    commodity.setLowWholesalePrice(lowWholesaleValue);
                    commodity.setHighRetailPrice(highRetailValue);
                    commodity.setLowRetailPrice(lowRetailValue);
                }
            }

        }
        return commodities;
    }

    public double[] comparePrices(double price1, double weightOfUnitOfMeasure1, double price2, double weightOfUnitOfMeasure2,
                                  boolean getHigher) {
        double normalisedPrice1 = price1 / weightOfUnitOfMeasure1;
        double normalisedPrice2 = price2 / weightOfUnitOfMeasure2;
        double[] result = new double[2];

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
        return result;
    }

    public double comparePrices(double price1, double weightOfUnitOfMeasure1, double normalisedPrice2, boolean getHigher) {
        double normalisedPrice1 = price1 / weightOfUnitOfMeasure1;
        double result = 0;

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
        return result;
    }

    public Commodity getCommoditybyName(ArrayList<Commodity> commodities, String name) {
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
        commandText.append("SELECT c.id AS Id,");
        commandText.append("c.name AS Name,");
        commandText.append("c.market_name AS marketName,");
        commandText.append("c.high_wholesale_price AS highWholesalePrice, ");
        commandText.append("c.low_wholesale_price AS lowWholesalePrice, ");
        commandText.append("c.high_retail_price AS highRetailPrice, ");
        commandText.append("c.low_retail_price AS lowRetailPrice, ");
        commandText.append("c.date_created AS dateCreated");
        commandText.append("FROM commodityprices c ");
        commandText.append("WHERE c.date_created = DATE(DATE_SUB(CURDATE(), INTERVAL 1 WEEK))");
        PreparedStatement preparedStatement = connection.prepareStatement(commandText.toString());
        ResultSet resultSet = preparedStatement.executeQuery();
        ArrayList<Commodity> commodities = new ArrayList<Commodity>();

        while (resultSet.next()) {
            Commodity commodity = new Commodity();
            commodity.setId(resultSet.getInt("Id"));
            commodity.setName(resultSet.getString("Name"));
            commodity.setLastUpdateDate(DatabaseHelpers.getJavaDateFromString(resultSet.getString("dateCreated"), 0));
            commodity.setLowRetailPrice(resultSet.getDouble("lowRetailPrice"));
            commodity.setLowWholesalePrice(resultSet.getDouble("lowWholesalePrice"));
            commodity.setHighRetailPrice(resultSet.getDouble("highRetailPrice"));
            commodity.setHighWholesalePrice(resultSet.getDouble("highWholesalePrice"));
            commodity.setMarketName(resultSet.getString("marketName"));
            commodities.add(commodity);
        }
        return commodities;
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
        commandText.append("c.date_created AS dateCreated");
        commandText.append("FROM commodityprices c ");
        commandText.append("WHERE c.market_name = ? ");
        commandText.append("AND c.name = ? ");
        commandText.append("ORDER BY c.date_created DESC");
        PreparedStatement preparedStatement = connection.prepareStatement(commandText.toString());
        preparedStatement.setString(0, marketName);
        preparedStatement.setString(1, commodityName);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            Commodity commodity = new Commodity();
            commodity.setId(resultSet.getInt("Id"));
            commodity.setName(resultSet.getString("Name"));
            commodity.setLastUpdateDate(DatabaseHelpers.getJavaDateFromString(resultSet.getString("dateCreated"), 0));
            commodity.setLowRetailPrice(resultSet.getDouble("lowRetailPrice"));
            commodity.setLowWholesalePrice(resultSet.getDouble("lowWholesalePrice"));
            commodity.setHighRetailPrice(resultSet.getDouble("highRetailPrice"));
            commodity.setHighWholesalePrice(resultSet.getDouble("highWholesalePrice"));
            commodity.setMarketName(resultSet.getString("marketName"));
            return commodity;
        }
        return null;
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
                    return splitBinding[splitBinding.length - 1];
                }
                else {
                    return splitBinding[splitBinding.length - 2];
                }
            }

        }
        return null;
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
