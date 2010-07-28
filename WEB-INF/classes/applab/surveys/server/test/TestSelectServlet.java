package applab.surveys.server.test;

import junit.framework.Assert;

import org.junit.Test;
import org.w3c.dom.Document;

import applab.surveys.server.DatabaseId;
import applab.surveys.server.Select;
import applab.surveys.server.XmlHelpers;

public class TestSelectServlet {

    @Test
    public void testSearchQuery() {
        // send request for search data
        StringBuilder searchQuery = new StringBuilder();
        searchQuery.append("SELECT searches.interviewer_id, count(*) ");
        searchQuery.append("FROM ycppquiz.OktopusSearchLog searches ");
        searchQuery.append("WHERE searches.server_entry_time >= '");
        searchQuery.append("");
        searchQuery.append("' and searches.server_entry_time < '");
        searchQuery.append("'");
        searchQuery.append("GROUP BY searches.interviewer_id");
        Document parsedQuery = parseSelectRequest(searchQuery.toString(), DatabaseId.Search);
        Assert.assertEquals(Select.getQueryFromRequest(parsedQuery), searchQuery.toString());
        Assert.assertEquals(Select.getTargetFromRequest(parsedQuery), DatabaseId.Search);
    }

    @Test
    public void testSurveyQuery() {
        // send request for survey data
        StringBuilder surveyQuery = new StringBuilder();
        surveyQuery.append("SELECT submissions.interviewer_id, surveys.survey_id, ");
        surveyQuery.append("sum(case when submissions.survey_status='Approved' then 1 else 0 end) as Approved, ");
        surveyQuery.append("sum(case when submissions.survey_status='Rejected' then 1 else 0 end) as Rejected, ");
        surveyQuery.append("sum(case when submissions.survey_status='Pending' then 1 else 0 end) as Pending, ");
        surveyQuery.append("sum(case when submissions.survey_status='NotReviewed' then 1 else 0 end) as NotReviewed, ");
        surveyQuery.append("sum(case when submissions.survey_status='Duplicates' then 1 else 0 end) as Duplicates, ");
        surveyQuery.append("FROM zebra.zebrasurveysubmissions submissions inner join zebra.zebrasurvey surveys ");
        surveyQuery.append("ON submissions.survey_id = surveys.id ");
        surveyQuery.append("WHERE submissions.server_entry_time > '");
        surveyQuery.append("");
        surveyQuery.append("' and submissions.server_entry_time < '");
        surveyQuery.append("'");
        surveyQuery.append("GROUP BY submissions.interviewer_id, submissions.survey_id");

        Document parsedQuery = parseSelectRequest(surveyQuery.toString(), DatabaseId.Surveys);
        Assert.assertEquals(Select.getQueryFromRequest(parsedQuery), surveyQuery.toString());
        Assert.assertEquals(Select.getTargetFromRequest(parsedQuery), DatabaseId.Surveys);
    }

    private Document parseSelectRequest(String queryString, DatabaseId targetDatabase) {
        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append("<?xml version=\"1.0\"?>");
        requestBuilder.append("<SelectRequest xmlns=\"http://schemas.applab.org/2010/07\" target=\"");
        if (targetDatabase == DatabaseId.Search) {
            requestBuilder.append("Search");
        }
        else {
            requestBuilder.append("Surveys");
        }
        requestBuilder.append("\">");

        // replace < and > with entities
        queryString = queryString.replaceAll("<", "&lt;");
        queryString = queryString.replaceAll(">", "&gt;");
        requestBuilder.append(queryString);
        requestBuilder.append("</SelectRequest>");

        try {
            return XmlHelpers.parseXml(requestBuilder.toString());
        }
        catch (Exception e) {
            Assert.fail(e.toString());
            return null;
        }
    }
}
