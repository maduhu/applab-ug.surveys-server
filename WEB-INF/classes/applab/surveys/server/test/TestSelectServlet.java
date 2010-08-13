package applab.surveys.server.test;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;
import org.w3c.dom.Document;

import applab.server.*;
import applab.surveys.server.*;

public class TestSelectServlet {

    private static Date GetFirstDayOfMonth() {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_MONTH, 1);
        return now.getTime();
    }

    @Test
    public void testRemoteQuery() {
        // leverage our RemoteSqlImplementation to test our Select implementation
        SqlImplementation.Instance remoteSqlImplementation = new RemoteSqlImplementation().createInstance();
        try {
            ResultSet resultSet = remoteSqlImplementation.executeQuery(DatabaseId.Search, xmlEncode(createSearchQuery()));
            if (resultSet.next()) {
                Assert.assertNotNull(resultSet.getString(1));
                Assert.assertEquals(resultSet.getString(1), resultSet.getString("interviewer_id"));
                Assert.assertNotNull(resultSet.getString(2));
                Assert.assertEquals(resultSet.getInt(2), resultSet.getInt("totalSearches"));
            }

            resultSet = remoteSqlImplementation.executeQuery(DatabaseId.Surveys, xmlEncode(createSurveyQuery()));
            if (resultSet.next()) {
                Assert.assertNotNull(resultSet.getString(1));
                Assert.assertEquals(resultSet.getString(1), resultSet.getString("interviewer_id"));
                Assert.assertNotNull(resultSet.getString(2));
                Assert.assertEquals(resultSet.getString(2), resultSet.getString("survey_id"));
                Assert.assertNotNull(resultSet.getString(3));
                Assert.assertEquals(resultSet.getInt(3), resultSet.getInt("Approved"));
                Assert.assertNotNull(resultSet.getString(4));
                Assert.assertEquals(resultSet.getInt(4), resultSet.getInt("Rejected"));
                Assert.assertNotNull(resultSet.getString(5));
                Assert.assertEquals(resultSet.getInt(5), resultSet.getInt("Pending"));
                Assert.assertNotNull(resultSet.getString(6));
                Assert.assertEquals(resultSet.getInt(6), resultSet.getInt("NotReviewed"));
                Assert.assertNotNull(resultSet.getString(7));
                Assert.assertEquals(resultSet.getInt(7), resultSet.getInt("Duplicates"));
            }
        }
        catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    private static String createSearchQuery() {
        StringBuilder searchQuery = new StringBuilder();
        searchQuery.append("SELECT searches.interviewer_id, count(*) AS totalSearches ");
        searchQuery.append("FROM ycppquiz.OktopusSearchLog searches ");
        searchQuery.append("WHERE searches.server_entry_time >= '");
        searchQuery.append(DatabaseHelpers.formatDateTime(GetFirstDayOfMonth()));
        searchQuery.append("' and searches.server_entry_time < '");
        searchQuery.append(DatabaseHelpers.formatDateTime(new Date()));
        searchQuery.append("' GROUP BY searches.interviewer_id");
        return searchQuery.toString();
    }

    private static String createSurveyQuery() {
        StringBuilder surveyQuery = new StringBuilder();
        surveyQuery.append("SELECT submissions.interviewer_id, surveys.survey_id, ");
        surveyQuery.append("sum(case when submissions.survey_status='Approved' then 1 else 0 end) as Approved, ");
        surveyQuery.append("sum(case when submissions.survey_status='Rejected' then 1 else 0 end) as Rejected, ");
        surveyQuery.append("sum(case when submissions.survey_status='Pending' then 1 else 0 end) as Pending, ");
        surveyQuery.append("sum(case when submissions.survey_status='NotReviewed' then 1 else 0 end) as NotReviewed, ");
        surveyQuery.append("sum(case when submissions.survey_status='Duplicate' then 1 else 0 end) as Duplicates ");
        surveyQuery.append("FROM zebra.zebrasurveysubmissions submissions inner join zebra.zebrasurvey surveys ");
        surveyQuery.append("ON submissions.survey_id = surveys.id ");
        surveyQuery.append("WHERE submissions.server_entry_time > '");
        surveyQuery.append(DatabaseHelpers.formatDateTime(GetFirstDayOfMonth()));
        surveyQuery.append("' and submissions.server_entry_time < '");
        surveyQuery.append(DatabaseHelpers.formatDateTime(new Date()));
        surveyQuery.append("' GROUP BY submissions.interviewer_id, submissions.survey_id");
        return surveyQuery.toString();
    }

    @Test
    public void testSearchQueryParsing() {
        testQueryParsing(DatabaseId.Search, createSearchQuery());
    }

    @Test
    public void testSurveyQueryParsing() {
        testQueryParsing(DatabaseId.Surveys, createSurveyQuery());
    }

    private void testQueryParsing(DatabaseId database, String queryString) {
        Document parsedQuery = parseSelectRequest(queryString, database);
        Assert.assertEquals(Select.getQueryFromRequest(parsedQuery), queryString);
        Assert.assertEquals(Select.getTargetFromRequest(parsedQuery), database);
    }

    private Document parseSelectRequest(String queryString, DatabaseId targetDatabase) {
        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append("<?xml version=\"1.0\"?>");
        requestBuilder.append("<SelectRequest xmlns=\"http://schemas.applab.org/2010/07\" target=\"");
        requestBuilder.append(targetDatabase.toString());
        requestBuilder.append("\">");

        requestBuilder.append(xmlEncode(queryString));
        requestBuilder.append("</SelectRequest>");

        try {
            return XmlHelpers.parseXml(requestBuilder.toString());
        }
        catch (Exception e) {
            Assert.fail(e.toString());
            return null;
        }
    }

    private static String xmlEncode(String source) {
        // replace < and > with entities
        source = source.replaceAll("<", "&lt;");
        return source.replaceAll(">", "&gt;");
    }
}
