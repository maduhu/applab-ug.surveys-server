package applab.surveys.server.test;

import org.junit.*;

import applab.server.SqlImplementation;

/**
 * abstract base class that sets up our configuration to access our databases remotely
 *
 */
public abstract class ServerTest {
    @BeforeClass protected void setupRemoteDatabaseAccess() { 
        SqlImplementation.setCurrent(new RemoteSqlImplementation());
    } 
}
