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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;

import applab.server.DatabaseHelpers;
import applab.server.DatabaseTable;
import applab.server.SelectCommand;
import applab.server.WebAppId;

/**
 * Helper methods for interacting with our survey and search databases
 * 
 */
public class SurveyDatabaseHelpers {

    final static String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static Connection readerConnection;
    static Connection writerConnection;
    static Connection searchReaderConnection;
    static Connection searchWriterConnection;

    public static Connection getReaderConnection() throws ClassNotFoundException, SQLException {

        if (readerConnection == null || readerConnection.isClosed()) {
            readerConnection = DatabaseHelpers.createReaderConnection(WebAppId.zebra);
        }
        return readerConnection;
    }

    public static Connection getWriterConnection() throws ClassNotFoundException, SQLException {

        if (writerConnection == null || writerConnection.isClosed()) {
            writerConnection = DatabaseHelpers.createConnection(WebAppId.zebra);
        }
        return writerConnection;
    }

    public static Connection getSearchReaderConnection() throws ClassNotFoundException, SQLException {

        if (searchReaderConnection == null || searchReaderConnection.isClosed()) {
            searchReaderConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/ycppquiz", "ycppquiz", "ycppquiz");
        }
        return searchReaderConnection;
    }

    public static Connection getSearchWriterConnection() throws ClassNotFoundException, SQLException {

        if (searchWriterConnection == null || searchWriterConnection.isClosed()) {
            Class.forName(JDBC_DRIVER);
            searchWriterConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/ycppquiz", "ycppquiz", "ycppquiz");
        }
        return searchWriterConnection;
    }

    public static String verifySurveyID(int surveyPrimaryKey) {
        try {
            Connection connection = getReaderConnection();
            Statement statement = connection.createStatement();
            String sqlQuery = "SELECT id, survey_id from zebrasurvey where id=" + surveyPrimaryKey;
            ResultSet resultSet = statement.executeQuery(sqlQuery);
            while (resultSet.next()) {
                int surveyID = Integer.parseInt(resultSet.getString("id"));
                String salesforceId = resultSet.getString("survey_id");
                statement.close();
                if (surveyID == surveyPrimaryKey) {
                    return salesforceId;
                }
            }
            statement.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getSurveyName(String surveyId) {
        try {
            Connection connection = getReaderConnection();
            Statement statement = connection.createStatement();
            String sqlQuery = "SELECT survey_name from zebrasurvey where survey_id='" + surveyId + "'";
            ResultSet resultSet = statement.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String surveyName = resultSet.getString("survey_name");
                statement.close();
                return surveyName;
            }
            statement.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getZebraSurveyId(String salesforceSurveyId) {
        try {
            Connection connection = getReaderConnection();
            Statement statement = connection.createStatement();
            String sqlQuery = "SELECT id from zebrasurvey where survey_id='" + salesforceSurveyId + "'";
            ResultSet resultSet = statement.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String databaseId = resultSet.getString("id");
                statement.close();
                return databaseId;
            }
            statement.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getXformData(String surveyId) throws SQLException, ClassNotFoundException {
        SelectCommand selectCommand = new SelectCommand(DatabaseTable.Survey);
        try {
            selectCommand.addField("xform");
            selectCommand.whereEquals("survey_id", "'" + surveyId + "'");
            ResultSet resultSet = selectCommand.execute();
            if (resultSet.next()) {
                return resultSet.getString("xform");
            }
            else {
                return null;
            }
        }
        finally {
            selectCommand.dispose();
        }
    }

    public static boolean saveXform(String surveyId, String surveyName, String xformData) {
        try {
            Connection connection = getWriterConnection();
            String query = "UPDATE zebrasurvey set survey_name = ?, xform = ? where survey_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, surveyName);
            statement.setString(2, xformData);
            statement.setString(3, surveyId);
            statement.executeUpdate();
            statement.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean saveXform(String surveyId, String xform_data, String surveyName, String creationDate)
            throws ClassNotFoundException, SQLException, ParseException {
        try {
            Connection connection = getWriterConnection();
            String query = "INSERT into zebrasurvey (survey_name, survey_id, created_at, xform) values (?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, surveyName);
            statement.setString(2, surveyId);
            java.sql.Date creation = DatabaseHelpers.getSqlDateFromString(creationDate, 0);
            statement.setDate(3, creation);
            statement.setString(4, xform_data);
            statement.execute();
            statement.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}