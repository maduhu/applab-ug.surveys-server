/*

Copyright (C) 2010 Grameen Foundation
Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
 */
package applab.surveys.server;

import java.util.*;
import java.text.*;
import java.io.*;

public class ApplabConfiguration {

    static ApplabConfiguration singletonValue = new ApplabConfiguration();

    Properties configurationProperties;

    ApplabConfiguration() {
        this.configurationProperties = new Properties();

        try {
            FileInputStream configurationFile = new FileInputStream("../../../application.xml");
            configurationProperties.loadFromXML(configurationFile);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getSurveysUsername() {
        return singletonValue.configurationProperties.getProperty("database-username");
    }

    public static String getSurveysPassword() {
        return singletonValue.configurationProperties.getProperty("database-password");
    }

    public static String getReaderUsername() {
        return singletonValue.configurationProperties.getProperty("database-reader-username");
    }

    public static String getReaderPassword() {
        return singletonValue.configurationProperties.getProperty("database-reader-password");
    }

    public static String getHostUrl() {
        return singletonValue.configurationProperties.getProperty("host-url");
    }

    public static String getSalesforceUsername() {
        return singletonValue.configurationProperties.getProperty("salesforce-username");
    }

    public static String getSalesforcePassword() {
        return singletonValue.configurationProperties.getProperty("salesforce-password");
    }

    public static String getSalesforceToken() {
        return singletonValue.configurationProperties.getProperty("salesforce-token");
    }

    public static String getSearchUsername() {
        return singletonValue.configurationProperties.getProperty("search-username");
    }

    public static String getSearchPassword() {
        return singletonValue.configurationProperties.getProperty("search-password");
    }
}
