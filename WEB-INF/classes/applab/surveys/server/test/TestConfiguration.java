package applab.surveys.server.test;

import static org.junit.Assert.*;

import org.junit.Test;

import applab.server.ApplabConfiguration;
import applab.server.DatabaseId;

public class TestConfiguration {
    @Test
    public void testApplabConfiguration() {
        for (DatabaseId databaseId : DatabaseId.values()) {
            assertNotNull(ApplabConfiguration.getDatabaseUrl(databaseId));
            assertNotNull(ApplabConfiguration.getDatabaseUsername(databaseId));
            assertNotNull(ApplabConfiguration.getDatabasePassword(databaseId));
        }
        assertNotNull(ApplabConfiguration.getSalesforceUsername());
        assertNotNull(ApplabConfiguration.getSalesforcePassword());
        assertNotNull(ApplabConfiguration.getSalesforceToken());
    }
}
