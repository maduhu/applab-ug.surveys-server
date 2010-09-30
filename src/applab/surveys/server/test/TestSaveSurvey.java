package applab.surveys.server.test;


import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

import applab.surveys.server.SurveyDatabaseHelpers;

public class TestSaveSurvey extends TestCase {

    @Test
    public void testSaveNewSubmission() throws Exception {
        
        String XML = "<xf:xforms xmlns:xf=\"http://www.w3.org/2002/xforms\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><xf:model><xf:instance id=\"new_form1\"><new_form1 name=\"New Form1\" id=\"2010090056\" formKey=\"new_form1\"><q1/><location/><q3/><q4/><q5><q6/><q7/></q5><q9/><q8/></new_form1></xf:instance><xf:bind id=\"q1\" nodeset=\"/new_form1/q1\" type=\"xsd:string\"/><xf:bind id=\"location\" nodeset=\"/new_form1/location\" format=\"gps\" type=\"xsd:string\"/><xf:bind id=\"q3\" nodeset=\"/new_form1/q3\" type=\"xsd:string\"/><xf:bind id=\"q4\" nodeset=\"/new_form1/q4\" type=\"xsd:string\"/><xf:bind id=\"q5\" nodeset=\"/new_form1/q5\"/><xf:bind id=\"q9\" nodeset=\"/new_form1/q9\" type=\"xsd:string\"/><xf:bind id=\"q8\" nodeset=\"/new_form1/q8\" format=\"image\" type=\"xsd:base64Binary\"/></xf:model><xf:group id=\"1\"><xf:label>Page1</xf:label><xf:input bind=\"q1\"><xf:label>Name's</xf:label></xf:input><xf:input bind=\"location\"><xf:label>Location</xf:label></xf:input><xf:input bind=\"q3\"><xf:label>Age</xf:label></xf:input><xf:select1 bind=\"q4\"><xf:label>gender</xf:label><xf:item id=\"male\"><xf:label>Male</xf:label><xf:value>male</xf:value></xf:item><xf:item id=\"female\"><xf:label>Female</xf:label><xf:value>female</xf:value></xf:item></xf:select1><xf:group id=\"q5\"><xf:label>SiblingInfo</xf:label><xf:repeat bind=\"q5\"><xf:input ref=\"q6\" type=\"xsd:string\"><xf:label>Age</xf:label></xf:input><xf:input ref=\"q7\" type=\"xsd:string\"><xf:label>Name</xf:label></xf:input></xf:repeat></xf:group><xf:select bind=\"q9\"><xf:label>Do you have</xf:label><xf:item id=\"2\"><xf:label>Bike</xf:label><xf:value>2</xf:value></xf:item><xf:item id=\"4\"><xf:label>Car</xf:label><xf:value>4</xf:value></xf:item><xf:item id=\"8\"><xf:label>HoverCraft</xf:label><xf:value>8</xf:value></xf:item><xf:item id=\"16\"><xf:label>SpaceShip</xf:label><xf:value>16</xf:value></xf:item></xf:select><xf:input bind=\"q8\"><xf:label>Photo of you</xf:label></xf:input></xf:group></xf:xforms>";
        String surveyId = "20983749";
        String surveyName = "TestFormLocal2";
        String creationDate = "2010/09/30 00:00:00";
        boolean result = SurveyDatabaseHelpers.saveXform(surveyId, XML, surveyName, creationDate);
        Assert.assertTrue(result);
    }
    
    @Test
    public void testUpdateSubmission() throws Exception {
        
        String XML = "<xf:xforms xmlns:xf=\"http://www.w3.org/2002/xforms\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><xf:model><xf:instance id=\"new_form1\"><new_form1 name=\"New Form1\" id=\"2010090056\" formKey=\"new_form1\"><q1/><location/><q3/><q4/><q5><q6/><q7/></q5><q9/><q8/></new_form1></xf:instance><xf:bind id=\"q1\" nodeset=\"/new_form1/q1\" type=\"xsd:string\"/><xf:bind id=\"location\" nodeset=\"/new_form1/location\" format=\"gps\" type=\"xsd:string\"/><xf:bind id=\"q3\" nodeset=\"/new_form1/q3\" type=\"xsd:string\"/><xf:bind id=\"q4\" nodeset=\"/new_form1/q4\" type=\"xsd:string\"/><xf:bind id=\"q5\" nodeset=\"/new_form1/q5\"/><xf:bind id=\"q9\" nodeset=\"/new_form1/q9\" type=\"xsd:string\"/><xf:bind id=\"q8\" nodeset=\"/new_form1/q8\" format=\"image\" type=\"xsd:base64Binary\"/></xf:model><xf:group id=\"1\"><xf:label>Page1</xf:label><xf:input bind=\"q1\"><xf:label>Name's</xf:label></xf:input><xf:input bind=\"location\"><xf:label>Location</xf:label></xf:input><xf:input bind=\"q3\"><xf:label>Age</xf:label></xf:input><xf:select1 bind=\"q4\"><xf:label>Sex</xf:label><xf:item id=\"male\"><xf:label>Male</xf:label><xf:value>male</xf:value></xf:item><xf:item id=\"female\"><xf:label>Female</xf:label><xf:value>female</xf:value></xf:item></xf:select1><xf:group id=\"q5\"><xf:label>SiblingInfo</xf:label><xf:repeat bind=\"q5\"><xf:input ref=\"q6\" type=\"xsd:string\"><xf:label>Age</xf:label></xf:input><xf:input ref=\"q7\" type=\"xsd:string\"><xf:label>Name</xf:label></xf:input></xf:repeat></xf:group><xf:select bind=\"q9\"><xf:label>Do you have</xf:label><xf:item id=\"2\"><xf:label>Bike</xf:label><xf:value>2</xf:value></xf:item><xf:item id=\"4\"><xf:label>Car</xf:label><xf:value>4</xf:value></xf:item><xf:item id=\"8\"><xf:label>HoverCraft</xf:label><xf:value>8</xf:value></xf:item><xf:item id=\"16\"><xf:label>SpaceShip</xf:label><xf:value>16</xf:value></xf:item></xf:select><xf:input bind=\"q8\"><xf:label>Photo of you</xf:label></xf:input></xf:group></xf:xforms>";
        String surveyId = "20983749";
        String surveyName = "TestFormLocal2";
        boolean result = SurveyDatabaseHelpers.saveXform(surveyId, surveyName, XML);
        Assert.assertTrue(result);
    }
}
