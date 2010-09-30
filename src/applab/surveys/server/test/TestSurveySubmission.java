package applab.surveys.server.test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;
import org.w3c.dom.Document;

import applab.server.XmlHelpers;
import applab.surveys.SubmissionAnswer;
import applab.surveys.server.ProcessSubmission;

public class TestSurveySubmission extends TestCase {
    @Test
    public void testSingleAnswerParsing() {
        // process submission and assert bits about the results
    }

    @Test
    public void testRepeatAnswerParsing() {
        // process submission and assert bits about the results
    }

    @Test
    public void testAttachments() throws IOException {
        // TODO: implement a stub context/FileItem to test the full ProcessSubmission.saveAttachment code directly

        // this should create a reference of the form /survey_images/<generated_id>.jpeg
        String attachmentReference = ProcessSubmission.createAttachmentReference("image/jpeg");
        Assert.assertTrue(attachmentReference.endsWith(".jpeg"));
        Assert.assertTrue(attachmentReference.startsWith("/survey_images/"));

        File targetFile = new File(attachmentReference);

        // create the directory if necessary
        File parentDirectory = targetFile.getParentFile();
        if (parentDirectory != null) {
            parentDirectory.mkdirs();
        }
        Assert.assertTrue(targetFile.createNewFile());
        targetFile.deleteOnExit();
        parentDirectory.deleteOnExit();
    }
    
    @Test
    public void testGetFarmerId() {
        String fileName = "Sample Form_[UA0001]_2010-23-34_00-00-00.xml";
        String farmerId = ProcessSubmission.getFarmerId(fileName);
        Assert.assertTrue(farmerId.equals("UA0001"));
    }
    /*
     * @Test public void testDuplicateSubmissions() throws Exception {
     * 
     * // Create the submission HashMap<String, SurveyItemResponse> answers = new HashMap<String, SurveyItemResponse>();
     * HashMap<String, SurveyItemResponse> answers2 = new HashMap<String, SurveyItemResponse>(); HashMap<String, String>
     * attachments = new HashMap<String, String>();
     * 
     * // Add the basic details SurveyItemResponse surveyId = new SurveyItemResponse("survey_id", null);
     * surveyId.addAnswerText("1"); answers.put("survey_id", surveyId); answers2.put("survey_id", surveyId);
     * 
     * SurveyItemResponse location = new SurveyItemResponse("location", null);
     * location.addAnswerText("Where I Say It Is"); answers.put("location", location); answers2.put("location",
     * location);
     * 
     * SurveyItemResponse handsetSubmitTime= new SurveyItemResponse("handset_submit_time", null);
     * handsetSubmitTime.addAnswerText(DatabaseHelpers.formatDateTime(new Date())); answers.put("handset_submit_time",
     * handsetSubmitTime); answers2.put("handset_submit_time", handsetSubmitTime);
     * 
     * SurveyItemResponse handsetId= new SurveyItemResponse("handset_id", null);
     * handsetId.addAnswerText("000000000000000"); answers.put("handset_id", handsetId); answers2.put("handset_id",
     * handsetId);
     * 
     * SurveyItemResponse interviewerId = new SurveyItemResponse("interviewer_id", null);
     * interviewerId.addAnswerText("Simon Jones"); answers.put("interviewer_id", interviewerId);
     * answers2.put("interviewer_id", interviewerId);
     * 
     * SurveyItemResponse submissionSize = new SurveyItemResponse("submission_size", null);
     * submissionSize.addAnswerText("407"); answers.put("submission_size", submissionSize);
     * answers2.put("submission_size", submissionSize);
     * 
     * 
     * // Add the answers SurveyItemResponse q1 = new SurveyItemResponse("q1", null); q1.addAnswerText("MyAnswer1");
     * answers.put("q1", q1); answers2.put("q1", q1); SurveyItemResponse q2 = new SurveyItemResponse("q2", null);
     * q2.addAnswerText("MyAnswer2"); answers.put("q2", q2); answers2.put("q2", q2); SurveyItemResponse q3 = new
     * SurveyItemResponse("q3", null); q3.addAnswerText("MyAnswer3"); answers.put("q3", q3); answers2.put("q3", q3);
     * SurveyItemResponse q4 = new SurveyItemResponse("q4", null); q4.addAnswerText("MyAnswer4"); answers.put("q4", q4);
     * answers2.put("q4", q4);
     * 
     * // Submit the survey the first time. Should work int firstResult =
     * ProcessSubmission.storeSurveySubmission(answers, attachments, "359444022449943");
     * 
     * // Submit the survey the second time. Should get an error int secondResult =
     * ProcessSubmission.storeSurveySubmission(answers2, attachments, "359444022449943");
     * 
     * // Conduct the tests Assert.assertEquals(201, firstResult); Assert.assertEquals(400, secondResult);
     * 
     * }
     */

    @Test
    public void testSubmissionParsingAndStorage() throws Exception {

        HashMap<String, SubmissionAnswer> answers = new HashMap<String, SubmissionAnswer>();
        HashMap<String, String> attachments = new HashMap<String, String>();

        String submissionXML = "<new_form1 formKey=\"new_form1\" id=\"2010090056\" name=\"New Form1\"><q1>Owen</q1><q2>25</q2><q3>Here</q3><q4>1 8</q4><q5><q6>David</q6><q7><q9>11</q9><q10>222</q10></q7><q7><q9>1</q9><q10>2</q10></q7></q5><q5><q6>Mr Trewileger</q6><q7><q9>3</q9><q10>4</q10></q7></q5><q5><q6>Jim</q6><q7><q9>5</q9><q10>6</q10></q7></q5><handset_submit_time>2010-09-23T15:42:27.825</handset_submit_time><location>27486201472</location><survey_id>1</survey_id></new_form1>";
        Document xmlDocument = XmlHelpers.parseXml(submissionXML);

        answers = ProcessSubmission.parseSurveySubmissionPublic(xmlDocument);

        // add the size as a response item to the responses, so that it gets saved to the db
        long size = submissionXML.length();

        int result = ProcessSubmission.storeSurveySubmission(answers, attachments, "359444022449943", size, "");
        Assert.assertEquals(201, result);

    }
}
