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

import java.util.*;

/**
 * Represents a Commodity Price saved in the backend database
 * */

public class Commodity implements Cloneable {

    private int id;
    private String marketName;
    private String subcountyName;
    private String districtName;
    private String regionName;
    private String name;
    private String wholesaleUnitOfMeasure;
    private String retailUnitOfMeasure;    
    private Date lastUpdateDate;
    private double weightOfWholesaleUnitOfMeasure;
    private double weightOfRetailUnitOfMeasure;
    private double rawLowRetailPrice;
    private double rawHighRetailPrice;
    private double rawLowWholesalePrice;
    private double rawHighWholesalePrice;
    private double lowRetailPrice;
    private double highRetailPrice;
    private double lowWholesalePrice;
    private double highWholesalePrice;
    private ArrayList<Question> questions;

    public Commodity() {
        questions = new ArrayList<Question>();
        lowRetailPrice = 0;
        highRetailPrice = 0;
        lowWholesalePrice = 0;
        highWholesalePrice = 0;
        weightOfWholesaleUnitOfMeasure = 0;
        weightOfRetailUnitOfMeasure = 0;
    }

    public Commodity(String name) {
        this();
        this.name = name;
    }

    public Commodity(String name, String marketName, String subcountyName) {
        this();
        this.name = name;
        this.marketName = marketName;
        this.subcountyName = subcountyName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public double getLowRetailPrice() {
        if (lowRetailPrice == 0) {
            if (rawLowRetailPrice != 0 && weightOfRetailUnitOfMeasure != 0) {
                lowRetailPrice = rawLowRetailPrice / weightOfRetailUnitOfMeasure;
            }
        }
        return lowRetailPrice;
    }

    public void setLowRetailPrice(double lowRetailPrice) {
        this.lowRetailPrice = lowRetailPrice;
    }

    public double getHighRetailPrice() {
        if (highRetailPrice == 0) {
            if (rawHighRetailPrice != 0 && weightOfRetailUnitOfMeasure != 0) {
                highRetailPrice = rawHighRetailPrice / weightOfRetailUnitOfMeasure;
            }
        }
        return highRetailPrice;
    }

    public void setHighRetailPrice(double highRetailPrice) {
        this.highRetailPrice = highRetailPrice;
    }

    public double getLowWholesalePrice() {
        if (lowWholesalePrice == 0) {
            if (rawLowWholesalePrice != 0 && weightOfWholesaleUnitOfMeasure != 0) {
                lowWholesalePrice = rawLowWholesalePrice / weightOfWholesaleUnitOfMeasure;
            }
        }
        return lowWholesalePrice;
    }

    public void setLowWholesalePrice(double lowWholesalePrice) {
        this.lowWholesalePrice = lowWholesalePrice;
    }

    public double getHighWholesalePrice() {
        if (highWholesalePrice == 0) {
            if (rawHighWholesalePrice != 0 && weightOfWholesaleUnitOfMeasure != 0) {
                highWholesalePrice = rawHighWholesalePrice / weightOfWholesaleUnitOfMeasure;
            }
        }
        return highWholesalePrice;
    }

    public void setHighWholesalePrice(double highWholesalePrice) {
        this.highWholesalePrice = highWholesalePrice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.toLowerCase();
    }
    
    public String getSubcountyName() {
        return subcountyName;
    }

    public void setSubcountyName(String subcountyName) {
        this.subcountyName = subcountyName;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getWholesaleUnitOfMeasure() {
        return wholesaleUnitOfMeasure;
    }

    public void setWholesaleUnitOfMeasure(String wholesaleUnitOfMeasure) {
        this.wholesaleUnitOfMeasure = wholesaleUnitOfMeasure;
    }

    public String getRetailUnitOfMeasure() {
        return retailUnitOfMeasure;
    }

    public void setRetailUnitOfMeasure(String retailUnitOfMeasure) {
        this.retailUnitOfMeasure = retailUnitOfMeasure;
    }

    public double getWeightOfWholesaleUnitOfMeasure() {
        return weightOfWholesaleUnitOfMeasure;
    }

    public void setWeightOfWholesaleUnitOfMeasure(double weightOfWholesaleUnitOfMeasure) {
        this.weightOfWholesaleUnitOfMeasure = weightOfWholesaleUnitOfMeasure;
    }

    public double getWeightOfRetailUnitOfMeasure() {
        return weightOfRetailUnitOfMeasure;
    }

    public void setWeightOfRetailUnitOfMeasure(double weightOfRetailUnitOfMeasure) {
        this.weightOfRetailUnitOfMeasure = weightOfRetailUnitOfMeasure;
    }

    public double getRawLowRetailPrice() {
        return rawLowRetailPrice;
    }

    public void setRawLowRetailPrice(double rawLowRetailPrice) {
        this.rawLowRetailPrice = rawLowRetailPrice;
    }

    public double getRawHighRetailPrice() {
        return rawHighRetailPrice;
    }

    public void setRawHighRetailPrice(double rawHighRetailPrice) {
        this.rawHighRetailPrice = rawHighRetailPrice;
    }

    public double getRawLowWholesalePrice() {
        return rawLowWholesalePrice;
    }

    public void setRawLowWholesalePrice(double rawLowWholesalePrice) {
        this.rawLowWholesalePrice = rawLowWholesalePrice;
    }

    public double getRawHighWholesalePrice() {
        return rawHighWholesalePrice;
    }

    public void setRawHighWholesalePrice(double rawHighWholesalePrice) {
        this.rawHighWholesalePrice = rawHighWholesalePrice;
    }

    public ArrayList<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(ArrayList<Question> questions) {
        this.questions = questions;
    }
    
    /**
     * Makes a shallow copy of the Commodity in case the same survey is used for multiple markets
     */
    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError("Failed to clone commodity " + this.name);
        }
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

}
