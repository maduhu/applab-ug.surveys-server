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

package configuration;

import java.sql.*;
import java.util.*;

public class DbConnect {

    final static String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    final static String DATABASE_URL = "jdbc:mysql://localhost:3306/zebra";
    static applabConfig applab = new applabConfig();

    public static boolean verifySurveyID(int survey_id) {
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "Select id from zebrasurvey where id=" + survey_id;
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "Select survey_id from zebrasurvey where survey_id='" + survey_id + "'";
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "select survey_name from zebrasurvey where survey_id='" + survey_id + "'";
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "select id from zebrasurvey where survey_id='" + salesforce_survey_id + "'";
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "select xform_param_name,xform_param_var from zebrasurveyquestions where survey_id=" + zebra_survey_id;
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
        Connection con = null;
        Statement stmt = null;
        String split[] = parameter.split(",");
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "select " + parameter + " from zebrasurveysubmissions where survey_id=" + zebra_survey_id
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "select xform from zebrasurvey where survey_id='" + survey_id + "'";
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "update zebrasurveysubmissions set survey_status='" + survey_status + "' where id=" + survey_submission_id;
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "Select xform_param_var from zebrasurveyquestions where xform_param_var='" + xform_param_var
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
            Class.forName(JDBC_DRIVER);
            Connection connection = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "update zebrasurvey set survey_name='" + survey_name + "',xform='" + xform_data + "' where survey_id='"
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "insert into zebrasurvey (survey_name,survey_id,created_at,xform) values ('" + surveyName + "','" + survey_id
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "Insert into zebrasurveyquestions (survey_id,xform_param_name,xform_param_var,xform_param_options) values ("
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "Delete from zebrasurveyquestions where survey_id=" + survey_id;
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "select count(survey_status) as status from zebrasurveysubmissions where survey_status='" + survey_status
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "select xform_param_name,xform_param_var from zebrasurveyquestions where survey_id=" + zebra_survey_id;
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String xform_param_name = resultSet.getString("xform_param_name").trim();
                String xform_param_var = resultSet.getString("xform_param_var").trim();
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "update zebrasurveyquestions set xform_param_name='" + xform_param_name + "' where survey_id="
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
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "select " + parameter + " from zebrasurveysubmissions where survey_id=" + zebra_survey_id;
            System.out.println(sqlQuery);
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            int cursor = 0;
            while (resultSet.next()) {
                try {
                    if (!resultSet.getString(parameter).equals("null")) {
                        cursor++;
                    }
                }
                catch (NullPointerException ex) {

                }
            }
            stmt.close();
            con.close();
            if (cursor > 0) {
                return true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteSurveyQuestion(int zebra_survey_id, String parameter) {
        Connection con = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(DATABASE_URL, applab.getZebraUsername(), applab.getZebraPassword());
            stmt = con.createStatement();
            String sqlQuery = "delete from zebrasurveyquestions where xform_param_var='" + parameter + "' and survey_id=" + zebra_survey_id;
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
}
