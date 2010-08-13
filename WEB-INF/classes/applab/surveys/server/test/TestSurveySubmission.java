package applab.surveys.server.test;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import applab.surveys.server.ProcessSubmission;
import junit.framework.Assert;
import junit.framework.TestCase;


public class TestSurveySubmission extends TestCase {
    @Test public void testSingleAnswerParsing() { 
        // process submission and assert bits about the results
    } 

    @Test public void testRepeatAnswerParsing() { 
        // process submission and assert bits about the results
    } 

    @Test public void testAttachments() throws IOException {
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
}
