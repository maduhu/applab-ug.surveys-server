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

    static String getDatabaseUrl(DatabaseId targetDatabase) {
        String SURVEY_DATABASE_URL = "jdbc:mysql://localhost:3306/zebra";
        String SEARCH_DATABASE_URL = "jdbc:mysql://localhost:3306/ycppquiz";

        switch (targetDatabase) {
            case Search:
                return SEARCH_DATABASE_URL;

            case Surveys:
                return SURVEY_DATABASE_URL;
        }

        return "";
    }

    static String getDatabaseUsername(DatabaseId targetDatabase) {
        switch (targetDatabase) {
            case Search:
                return ApplabConfiguration.getSearchUsername();

            case Surveys:
                return ApplabConfiguration.getSurveysUsername();
        }

        return "";
    }

    static String getDatabasePassword(DatabaseId targetDatabase) {
        switch (targetDatabase) {
            case Search:
                return ApplabConfiguration.getSearchPassword();

            case Surveys:
                return ApplabConfiguration.getSurveysPassword();
        }

        return "";
    }

    // helper function to create a connection to our local database
    public static Connection createConnection(DatabaseId targetDatabase) throws ClassNotFoundException, SQLException {
        // make sure the JDBC driver is loaded into memory
        Class.forName(JDBC_DRIVER);
        return DriverManager.getConnection(getDatabaseUrl(targetDatabase), getDatabaseUsername(targetDatabase),
                getDatabasePassword(targetDatabase));
    }

    // like createConnection, except it uses a read-only account
    public static Connection createReaderConnection(DatabaseId targetDatabase) throws ClassNotFoundException, SQLException {
// we don't have deployment setup correctly, so use the read-write account for now
        return createConnection(targetDatabase);
        // make sure the JDBC driver is loaded into memory
//        Class.forName(JDBC_DRIVER);
//        return DriverManager.getConnection(getDatabaseUrl(targetDatabase), ApplabConfiguration.getReaderUsername(), ApplabConfiguration
//                .getReaderPassword());
    }

    /**
     * Helper method to ensure consistent date formatting in our database
     */
    public static String formatDateTime(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.format(date);
    }

    public static boolean verifySurveyID(int survey_id) {
        try {
            Connection con = createReaderConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "SELECT id from zebrasurvey where id=" + survey_id;
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            while (resultSet.next()) {
                int surveyID = Integer.parseInt(resultSet.getString("id"));
                stmt.close();
                con.close();
                if (surveyID == survey_id) {
                    return true;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean zebraSurveyIdExists(String survey_id) {
        try {
            Connection con = createReaderConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "SELECT survey_id from zebrasurvey where survey_id='" + survey_id + "'";
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String surveyID = resultSet.getString("survey_id");
                stmt.close();
                con.close();
                if (surveyID.equals(survey_id)) {
                    return true;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getSurveyName(String survey_id) {
        try {
            Connection con = createReaderConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "SELECT survey_name from zebrasurvey where survey_id='" + survey_id + "'";
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String survey_name = resultSet.getString("survey_name");
                stmt.close();
                con.close();
                return survey_name;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getZebraSurveyId(String salesforce_survey_id) {
        try {
            Connection con = createReaderConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "SELECT id from zebrasurvey where survey_id='" + salesforce_survey_id + "'";
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String xform_data = resultSet.getString("id");
                stmt.close();
                con.close();
                return xform_data;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Hashtable<String, String> getSurveyQuestionParameter(int zebra_survey_id) {
        Hashtable<String, String> questionAnswer = new Hashtable<String, String>();
        try {
            Connection con = createReaderConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "SELECT xform_param_name,xform_param_var from zebrasurveyquestions where survey_id=" + zebra_survey_id;
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            while (resultSet.next()) {
                questionAnswer.put(resultSet.getString("xform_param_name"), resultSet.getString("xform_param_var"));
            }
            stmt.close();
            con.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return questionAnswer;
    }

    public static Hashtable<Integer, String[]> getAnswer(String parameter, int zebra_survey_id, String status) {
        Hashtable<Integer, String[]> answer = new Hashtable<Integer, String[]>();
        String split[] = parameter.split(",");
        try {
            Connection con = createReaderConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "SELECT " + parameter + " from zebrasurveysubmissions where survey_id=" + zebra_survey_id
                    + " and survey_status='" + status + "'";
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            int cursor = 0;
            while (resultSet.next()) {
                String values[] = new String[split.length];
                for (int i = 0; i < values.length; i++) {
                    values[i] = resultSet.getString(split[i]);
                }
                answer.put(cursor, values);
                cursor++;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return answer;
    }

    public static String getXformData(String survey_id) {
        try {
            Connection con = createReaderConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "SELECT xform from zebrasurvey where survey_id='" + survey_id + "'";
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String xform_data = resultSet.getString("xform");
                stmt.close();
                con.close();
                return xform_data;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updateSurveySubmissionStatus(int survey_submission_id, String survey_status) {
        try {
            Connection con = createConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "UPDATE zebrasurveysubmissions set survey_status='" + survey_status + "' where id=" + survey_submission_id;
            stmt.executeUpdate(sqlQuery);
            stmt.close();
            con.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean verifySurveyField(String xform_param_var, int surveyID) {
        try {
            Connection con = createReaderConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "SELECT xform_param_var from zebrasurveyquestions where xform_param_var='" + xform_param_var
                    + "' and survey_id=" + surveyID;
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String xform_param_var_ = resultSet.getString("xform_param_var");
                if (xform_param_var_.trim().equals(xform_param_var)) {
                    stmt.close();
                    con.close();
                    return true;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * returns true if the submission was successful
     */
    public static boolean postSubmission(String sqlQuery) {
        try {
            Connection connection = createConnection(DatabaseId.Surveys);
            Statement statement = connection.createStatement();
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

    public static boolean saveXform(String survey_id, String survey_name, String xform_data) {
        try {
            Connection con = createConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "UPDATE zebrasurvey set survey_name='" + survey_name + "',xform='" + xform_data + "' where survey_id='"
                    + survey_id + "'";
            stmt.executeUpdate(sqlQuery);
            stmt.close();
            con.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean saveXform(String survey_id, String xform_data, String surveyName, String creationDate) {
        try {
            Connection con = createConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "INSERT into zebrasurvey (survey_name,survey_id,created_at,xform) values ('" + surveyName + "','" + survey_id
                    + "','" + creationDate + "','" + xform_data + "')";

            stmt.executeUpdate(sqlQuery);
            stmt.close();
            con.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean saveZebraSurveyQuestions(int survey_id, String xform_param_name, String xform_param_var) {
        try {
            Connection con = createConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "INSERT into zebrasurveyquestions (survey_id,xform_param_name,xform_param_var,xform_param_options) values ("
                    + survey_id + ",'" + xform_param_name + "','" + xform_param_var + "','')";
            stmt.executeUpdate(sqlQuery);
            stmt.close();
            con.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteSurveyFromSurveyQuestions(int survey_id) {
        try {
            Connection con = createConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "DELETE from zebrasurveyquestions where survey_id=" + survey_id;
            stmt.executeUpdate(sqlQuery);
            stmt.close();
            con.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getNumberOfSubmissions(int zebra_survey_id, String survey_status) {
        try {
            Connection con = createReaderConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "SELECT count(survey_status) as status from zebrasurveysubmissions where survey_status='" + survey_status
                    + "' and survey_id=" + zebra_survey_id;
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            while (resultSet.next()) {
                int status = Integer.parseInt(resultSet.getString("status"));
                stmt.close();
                con.close();
                return status;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Hashtable<String, String> getZebraSurveyQuestions(int zebra_survey_id) {
        Hashtable<String, String> questions = new Hashtable<String, String>();
        try {
            Connection con = createReaderConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "select xform_param_name,xform_param_var from zebrasurveyquestions where survey_id=" + zebra_survey_id;
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String xform_param_var = resultSet.getString("xform_param_var").trim();
                String xform_param_name = resultSet.getString("xform_param_name").trim();
                questions.put(xform_param_var, xform_param_name);
            }
            stmt.close();
            con.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return questions;
    }

    public static String updateSurveyQuestion(String xform_param_var, String xform_param_name, int zebra_survey_id) {
        try {
            Connection con = createConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "UPDATE zebrasurveyquestions set xform_param_name='" + xform_param_name + "' where survey_id="
                    + zebra_survey_id + " and xform_param_var='" + xform_param_var + "'";
            stmt.executeUpdate(sqlQuery);
            stmt.close();
            con.close();
            return "Update Successful";
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "?";
    }

    public static boolean surveyQuestionHasSubmissions(int zebra_survey_id, String parameter) {
        try {
            Connection con = createReaderConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "SELECT " + parameter + " from zebrasurveysubmissions where survey_id=" + zebra_survey_id;
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            try {
                while (resultSet.next()) {
                    if (resultSet.getString(parameter) != "null") {
                        return true;
                    }
                }
            }
            finally {
                stmt.close();
                con.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteSurveyQuestion(int zebra_survey_id, String parameter) {
        try {
            Connection con = createConnection(DatabaseId.Surveys);
            Statement stmt = con.createStatement();
            String sqlQuery = "DELETE from zebrasurveyquestions where xform_param_var='" + parameter + "' and survey_id=" + zebra_survey_id;
            stmt.executeUpdate(sqlQuery);
            stmt.close();
            con.close();
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

    public String setInterviewerNameAndId(int submissionId, String interviewerName, String interviewerId) throws ClassNotFoundException,
            SQLException {
        Connection connection = createConnection(DatabaseId.Surveys);
        Statement statement = connection.createStatement();
        String sqlQuery = "UPDATE zebrasurveysubmissions set interviewer_name='" + interviewerName + "', interviewer_id='" + interviewerId
                + "' where id=" + submissionId;
        statement.executeUpdate(sqlQuery);
        statement.close();
        connection.close();
        return "Success";
    }

    public String getSurveyStatus(int submissionId) throws SQLException, ClassNotFoundException {
        Connection connection = createReaderConnection(DatabaseId.Surveys);
        Statement statement = connection.createStatement();
        String sqlQuery = "SELECT survey_status from zebrasurveysubmissions where id=" + submissionId;
        ResultSet queryResults = statement.executeQuery(sqlQuery);
        String survey_status = "";
        while (queryResults.next()) {
            survey_status = queryResults.getString("survey_status");
        }
        statement.close();
        connection.close();
        return survey_status;
    }
}