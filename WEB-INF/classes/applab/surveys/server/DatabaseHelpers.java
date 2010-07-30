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

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Helper methods for interacting with our survey and search databases
 * 
 */
public class DatabaseHelpers {

    final static String JDBC_DRIVER = "com.mysql.jdbc.Driver";

    // helper function to create a connection to our local database
    public static Connection createConnection(DatabaseId targetDatabase) throws ClassNotFoundException, SQLException {
        // make sure the JDBC driver is loaded into memory
        Class.forName(JDBC_DRIVER);
        return DriverManager.getConnection(ApplabConfiguration.getDatabaseUrl(targetDatabase), 
                ApplabConfiguration.getDatabaseUsername(targetDatabase),
                ApplabConfiguration.getDatabasePassword(targetDatabase));
    }

    // like createConnection, except it uses a read-only account
    public static Connection createReaderConnection(DatabaseId targetDatabase) throws ClassNotFoundException, SQLException {
        // we don't have deployment setup correctly, so use the read-write account for now
        // make sure the JDBC driver is loaded into memory
        Class.forName(JDBC_DRIVER);
        return DriverManager.getConnection(ApplabConfiguration.getDatabaseUrl(targetDatabase), 
                ApplabConfiguration.getReaderUsername(targetDatabase), 
                ApplabConfiguration.getReaderPassword(targetDatabase));
    }

    /**
     * Helper method to ensure consistent date formatting in our database
     */
    public static String formatDateTime(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.format(date);
    }

    public static boolean verifySurveyID(int surveyPrimaryKey) {
        try {
            Connection connection = createReaderConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            String sqlQuery = "SELECT id from zebrasurvey where id=" + surveyPrimaryKey;
            ResultSet resultSet = statement.executeQuery(sqlQuery);
            while (resultSet.next()) {
                int surveyID = Integer.parseInt(resultSet.getString("id"));
                statement.close();
                connection.close();
                if (surveyID == surveyPrimaryKey) {
                    return true;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getSurveyName(String surveyId) {
        try {
            Connection connection = createReaderConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            String sqlQuery = "SELECT survey_name from zebrasurvey where survey_id='" + surveyId + "'";
            ResultSet resultSet = statement.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String surveyName = resultSet.getString("survey_name");
                statement.close();
                connection.close();
                return surveyName;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getZebraSurveyId(String salesforceSurveyId) {
        try {
            Connection connection = createReaderConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            String sqlQuery = "SELECT id from zebrasurvey where survey_id='" + salesforceSurveyId + "'";
            ResultSet resultSet = statement.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String databaseId = resultSet.getString("id");
                statement.close();
                connection.close();
                return databaseId;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getXformData(String surveyId) {
        try {
            Connection connection = createReaderConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            String sqlQuery = "SELECT xform from zebrasurvey where survey_id='" + surveyId + "'";
            ResultSet resultSet = statement.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String xformData = resultSet.getString("xform");
                statement.close();
                connection.close();
                return xformData;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean verifySurveyField(String xform_param_var, int surveyId) {
        try {
            Connection connection = createReaderConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            String sqlQuery = "SELECT xform_param_var from zebrasurveyquestions where xform_param_var='" + xform_param_var
                    + "' and survey_id=" + surveyId;
            ResultSet resultSet = statement.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String xform_param_var_ = resultSet.getString("xform_param_var");
                if (xform_param_var_.trim().equals(xform_param_var)) {
                    statement.close();
                    connection.close();
                    return true;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean saveXform(String surveyId, String surveyName, String xformData) {
        try {
            Connection connection = createConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            String sqlQuery = "UPDATE zebrasurvey set survey_name='" + surveyName + "',xform='" + xformData + "' where survey_id='"
                    + surveyId + "'";
            statement.executeUpdate(sqlQuery);
            statement.close();
            connection.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean saveXform(String surveyId, String xform_data, String surveyName, String creationDate) {
        try {
            Connection connection = createConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            String sqlQuery = "INSERT into zebrasurvey (survey_name,survey_id,created_at,xform) values ('" + surveyName + "','" + surveyId
                    + "','" + creationDate + "','" + xform_data + "')";

            statement.executeUpdate(sqlQuery);
            statement.close();
            connection.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean saveZebraSurveyQuestions(int surveyId, String xform_param_name, String xform_param_var) {
        try {
            Connection connection = createConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            String sqlQuery = "INSERT into zebrasurveyquestions (survey_id,xform_param_name,xform_param_var,xform_param_options) values ("
                    + surveyId + ",'" + xform_param_name + "','" + xform_param_var + "','')";
            statement.executeUpdate(sqlQuery);
            statement.close();
            connection.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteSurveyFromSurveyQuestions(int surveyId) {
        try {
            Connection connection = createConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            String sqlQuery = "DELETE from zebrasurveyquestions where survey_id=" + surveyId;
            statement.executeUpdate(sqlQuery);
            statement.close();
            connection.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Hashtable<String, String> getZebraSurveyQuestions(int surveyId) {
        Hashtable<String, String> questions = new Hashtable<String, String>();
        try {
            Connection connection = createReaderConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            String sqlQuery = "select xform_param_name,xform_param_var from zebrasurveyquestions where survey_id=" + surveyId;
            ResultSet resultSet = statement.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String xform_param_var = resultSet.getString("xform_param_var").trim();
                String xform_param_name = resultSet.getString("xform_param_name").trim();
                questions.put(xform_param_var, xform_param_name);
            }
            statement.close();
            connection.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return questions;
    }

    public static boolean updateSurveyQuestion(String xform_param_var, String xform_param_name, int surveyId) {
        try {
            Connection connection = createConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            String sqlQuery = "UPDATE zebrasurveyquestions set xform_param_name='" + xform_param_name + "' where survey_id="
                    + surveyId + " and xform_param_var='" + xform_param_var + "'";
            statement.executeUpdate(sqlQuery);
            statement.close();
            connection.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean surveyQuestionHasSubmissions(int surveyId, String parameter) {
        try {
            Connection connection = createReaderConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            String sqlQuery = "SELECT " + parameter + " from zebrasurveysubmissions where survey_id=" + surveyId;
            ResultSet resultSet = statement.executeQuery(sqlQuery);
            try {
                while (resultSet.next()) {
                    if (resultSet.getString(parameter) != "null") {
                        return true;
                    }
                }
            }
            finally {
                statement.close();
                connection.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteSurveyQuestion(int surveyId, String parameter) {
        try {
            Connection connection = createConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
            String sqlQuery = "DELETE from zebrasurveyquestions where xform_param_var='" + parameter + "' and survey_id=" + surveyId;
            statement.executeUpdate(sqlQuery);
            statement.close();
            connection.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public HashMap<String, String> getHandsetIdsWithNullInterviewers() throws Exception {
        Connection connection = createReaderConnection(DatabaseId.Surveys);
        Statement statement = connection.createStatement();

        String sqlQuery = "SELECT id,handset_id from zebrasurveysubmsissions where interviewer_id = ''";
        ResultSet queryResults = statement.executeQuery(sqlQuery);
        // in the order of interviewer_name,interviewer_id,handset_id
        HashMap<String, String> handsetList = new HashMap<String, String>();
        while (queryResults.next()) {
            handsetList.put(queryResults.getString("id"), queryResults.getString("handset_id"));
        }
        statement.close();
        connection.close();
        return handsetList;
    }

    public void setInterviewerNameAndId(int submissionId, String interviewerName, String interviewerId) throws ClassNotFoundException,
            SQLException {
        Connection connection = createConnection(DatabaseId.Surveys);
        Statement statement = connection.createStatement();
        String sqlQuery = "UPDATE zebrasurveysubmissions set interviewer_name='" + interviewerName + "', interviewer_id='" + interviewerId
                + "' where id=" + submissionId;
        statement.executeUpdate(sqlQuery);
        statement.close();
        connection.close();
    }

    public String getSurveyStatus(int submissionId) throws SQLException, ClassNotFoundException {
        Connection connection = createReaderConnection(DatabaseId.Surveys);
        Statement statement = connection.createStatement();
        String sqlQuery = "SELECT survey_status from zebrasurveysubmissions where id=" + submissionId;
        ResultSet queryResults = statement.executeQuery(sqlQuery);
        String surveyStatus = "";
        while (queryResults.next()) {
            surveyStatus = queryResults.getString("survey_status");
        }
        statement.close();
        connection.close();
        return surveyStatus;
    }
}