package applab.surveys.server.test;

import static org.junit.Assert.*;

import org.junit.Test;

import applab.surveys.server.ApplabConfiguration;

public class TestConfiguration {
    @Test public void testApplabConfiguration() { 
        assertNotNull(ApplabConfiguration.getSurveysUsername());
        assertNotNull(ApplabConfiguration.getSurveysPassword());
        assertNotNull(ApplabConfiguration.getSearchUsername());
        assertNotNull(ApplabConfiguration.getSearchPassword());
        assertNotNull(ApplabConfiguration.getSalesforceUsername());
        assertNotNull(ApplabConfiguration.getSalesforcePassword());
        assertNotNull(ApplabConfiguration.getSalesforceToken());
    } 
}
