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

import java.util.ArrayList;
import java.util.List;

public class MarketSurveyObject {

    private String marketId;
    private String marketName;
    private String groupId;
    private String subcountyName;
    private String surveyName;
    private String districtName;
    private String regionName;
    private List<Commodity> commodities;
    private List<String> personIds;
    private List<String> ckwIds;

    public MarketSurveyObject(String groupId, String marketId, String regionName, String districtName, String subcountyName, String marketName) {
        this.marketId = marketId;
        this.groupId = groupId;
        this.marketName = marketName;
        this.districtName = districtName;
        this.subcountyName = subcountyName;
        this.regionName = regionName;
        this.commodities = new ArrayList<Commodity>();
        this.personIds = new ArrayList<String>();
        this.setCkwIds(new ArrayList<String>());
        
    }

    public MarketSurveyObject(String groupId, String marketId, String regionName, String districtName, String subcountyName, String marketName, String surveyName) {
        this.marketId = marketId;
        this.groupId = groupId;
        this.marketName = marketName;
        this.surveyName = surveyName;
        this.regionName = regionName;
        this.districtName = districtName;
        this.subcountyName = subcountyName;
        this.commodities = new ArrayList<Commodity>();
        this.personIds = new ArrayList<String>();
        this.setCkwIds(new ArrayList<String>());
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public String getSurveyName() {
        return surveyName;
    }

    public void setSurveyName(String surveyName) {
        this.surveyName = surveyName;
    }

    public List<Commodity> getCommodities() {
        return commodities;
    }

    public void setCommodities(List<Commodity> commodities) {
        this.commodities = commodities;
    }

    public List<String> getPersonIds() {
        return personIds;
    }

    public void setPersonIds(List<String> personIds) {
        this.personIds = personIds;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }
    
    public static List<MarketSurveyObject> getBySurveyName(String surveyName, List<MarketSurveyObject> marketSurveys) {
        List<MarketSurveyObject> fitleredMarketSurveys = new ArrayList<MarketSurveyObject>();
        for (MarketSurveyObject marketSurvey : marketSurveys) {
            if (marketSurvey.getSurveyName().equals(surveyName)) {
                fitleredMarketSurveys.add(marketSurvey);
            }
        }
        return fitleredMarketSurveys;
    }

    public List<String> getCkwIds() {
        return ckwIds;
    }

    public void setCkwIds(List<String> ckwIds) {
        this.ckwIds = ckwIds;
    }

    public String getSubcountyName() {
        return subcountyName;
    }

    public void setSubcountyName(String subcountyName) {
        this.subcountyName = subcountyName;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }   
    
}
