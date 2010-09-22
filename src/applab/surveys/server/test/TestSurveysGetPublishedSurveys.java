/**
 * 
 */
package applab.surveys.server.test;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import applab.server.SalesforceProxy;
import applab.surveys.Survey;
import applab.surveys.server.SurveysSalesforceProxy;

import com.sforce.soap.enterprise.SaveResult;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.enterprise.sobject.*;

public class TestSurveysGetPublishedSurveys extends TestCase {

    // Object Ids to delete (put any ids you create in here and they'll be deleted in the tearDown)
    private ArrayList<String> createdObjects = new ArrayList<String>();

    // Binding
    private SoapBindingStub binding = null;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        binding = SalesforceProxy.createBinding();

        try {
            // Create handset
            Phone__c phone = new Phone__c();
            phone.setSerial_Number__c("MyTestSerialNumber"); // This is required too
            phone.setIMEI__c("MyTestPhoneImei");
            SaveResult[] phoneSaveResult = binding.create(new Phone__c[] { phone });

            if (!phoneSaveResult[0].isSuccess()) {
                throw new Exception("Test Failed: Failed to save Phone!");
            }
            else {
                createdObjects.add(phoneSaveResult[0].getId());
            }

            // Create Person and link to hand set
            Person__c person = new Person__c();
            person.setFirst_Name__c("MyTestFirstName");
            person.setLast_Name__c("MyTestLastName");
            person.setHandset__c(phoneSaveResult[0].getId());
            SaveResult[] personSaveResult = binding.create(new Person__c[] { person });

            if (!personSaveResult[0].isSuccess()) {
                throw new Exception("Test Failed: Failed to save Person!");
            }
            else {
                createdObjects.add(personSaveResult[0].getId());
            }

            // Create group
            Group__c group = new Group__c();
            group.setName("MyTestGroup");
            SaveResult[] groupSaveResult = binding.create(new Group__c[] { group });

            if (!groupSaveResult[0].isSuccess()) {
                throw new Exception("Test Failed: Failed to save Group!");
            }
            else {
                createdObjects.add(groupSaveResult[0].getId());
            }

            // Create Person Group Association
            Person_Group_Association__c personGroupAssociation = new Person_Group_Association__c();
            personGroupAssociation.setGroup__c(groupSaveResult[0].getId());
            personGroupAssociation.setPerson__c(personSaveResult[0].getId());
            SaveResult[] pgaSaveResult = binding.create(new Person_Group_Association__c[] { personGroupAssociation });

            if (!pgaSaveResult[0].isSuccess()) {
                throw new Exception("Test Failed: Failed to save Person Group Association!");
            }
            else {
                createdObjects.add(pgaSaveResult[0].getId());
            }

            // Create survey, publish it and add it to group
            Survey__c survey = new Survey__c();
            survey.setSurvey_Name__c("MyTestSurvey");
            survey.setSurvey_Status__c("Published");
            SaveResult[] surveySaveResult = binding.create(new Survey__c[] { survey });

            if (!surveySaveResult[0].isSuccess()) {
                throw new Exception("Test Failed: Failed to save Survey!");
            }
            else {
                createdObjects.add(surveySaveResult[0].getId());
            }

            Survey_Group_Association__c surveyGroupAssociation = new Survey_Group_Association__c();
            surveyGroupAssociation.setSurvey__c(surveySaveResult[0].getId());
            surveyGroupAssociation.setGroup__c(groupSaveResult[0].getId());
            SaveResult[] sgaSaveResult = binding.create(new Survey_Group_Association__c[] { surveyGroupAssociation });

            if (!sgaSaveResult[0].isSuccess()) {
                throw new Exception("Test Failed: Failed to save Survey Group Association!");
            }
            else {
                createdObjects.add(sgaSaveResult[0].getId());
            }

            // Create second group and survey (Added to test bug with multiple groups/surveys)
            // Create group2
            Group__c group2 = new Group__c();
            group2.setName("MyTestGroup2");
            SaveResult[] groupSaveResult2 = binding.create(new Group__c[] { group2 });

            if (!groupSaveResult2[0].isSuccess()) {
                throw new Exception("Test Failed: Failed to save Group2!");
            }
            else {
                createdObjects.add(groupSaveResult2[0].getId());
            }

            // Create Person Group Association 2
            Person_Group_Association__c personGroupAssociation2 = new Person_Group_Association__c();
            personGroupAssociation2.setGroup__c(groupSaveResult2[0].getId());
            personGroupAssociation2.setPerson__c(personSaveResult[0].getId());
            SaveResult[] pgaSaveResult2 = binding.create(new Person_Group_Association__c[] { personGroupAssociation2 });

            if (!pgaSaveResult2[0].isSuccess()) {
                throw new Exception("Test Failed: Failed to save Person Group Association 2!");
            }
            else {
                createdObjects.add(pgaSaveResult2[0].getId());
            }

            // Create survey, publish it and add it to group
            Survey__c survey2 = new Survey__c();
            survey2.setSurvey_Name__c("MyTestSurvey2");
            survey2.setSurvey_Status__c("Published");
            SaveResult[] surveySaveResult2 = binding.create(new Survey__c[] { survey2 });

            if (!surveySaveResult2[0].isSuccess()) {
                throw new Exception("Test Failed: Failed to save Survey 2!");
            }
            else {
                createdObjects.add(surveySaveResult2[0].getId());
            }

            Survey_Group_Association__c surveyGroupAssociation2 = new Survey_Group_Association__c();
            surveyGroupAssociation2.setSurvey__c(surveySaveResult2[0].getId());
            surveyGroupAssociation2.setGroup__c(groupSaveResult2[0].getId());
            SaveResult[] sgaSaveResult2 = binding.create(new Survey_Group_Association__c[] { surveyGroupAssociation2 });

            if (!sgaSaveResult2[0].isSuccess()) {
                throw new Exception("Test Failed: Failed to save Survey Group Association2!");
            }
            else {
                createdObjects.add(sgaSaveResult2[0].getId());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            tearDown();
            Assert.fail(e.toString());
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clean up
        if (createdObjects.size() > 0) {
            binding.delete(createdObjects.toArray(new String[0]));
        }
    }

    /**
     * Test getPublishedSurveys(String imei)
     * 
     * @throws Exception
     */
    @Test
    public void testGetPublishedSurveys() throws Exception {
        SurveysSalesforceProxy salesforceProxy = new SurveysSalesforceProxy();
        ArrayList<Survey> publishedSurveys = salesforceProxy.getPublishedSurveys("MyTestPhoneImei");

        // There should only be one
        Assert.assertEquals(2, publishedSurveys.size());

        // Check the survey names (shouldn't be the same)
        Assert.assertFalse(publishedSurveys.get(0).getName() == publishedSurveys.get(1).getName());

        // Check the survey names
        ArrayList<String> names = new ArrayList<String>();
        names.add(publishedSurveys.get(0).getName());
        names.add(publishedSurveys.get(1).getName());
        Assert.assertTrue(names.contains("MyTestSurvey"));
        Assert.assertTrue(names.contains("MyTestSurvey2"));
    }
}
