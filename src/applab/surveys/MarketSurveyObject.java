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

public class MarketSurveyObject {

    private String marketName;
    private String groupName;
    private String surveyName;

    public MarketSurveyObject(String groupName, String marketName) {
        this.groupName = groupName;
        this.marketName = marketName;
    }

    public MarketSurveyObject(String groupName, String marketName, String surveyName) {
        this.groupName = groupName;
        this.marketName = marketName;
        this.surveyName = surveyName;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getSurveyName() {
        return surveyName;
    }

    public void setSurveyName(String surveyName) {
        this.surveyName = surveyName;
    }

    public String getKey() {
        return groupName + marketName;
    }

}
