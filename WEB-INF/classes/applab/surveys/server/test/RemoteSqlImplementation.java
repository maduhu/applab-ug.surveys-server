package applab.surveys.server.test;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import applab.net.*;
import applab.server.*;

public class RemoteSqlImplementation extends SqlImplementation {

    @Override
    public Instance createInstance() {
        return new RemoteSqlInstance();
    }

    private class RemoteSqlInstance extends Instance {

        @Override
        public void dispose() {
            // nothing to do
        }

        @Override
        public ResultSet executeQuery(DatabaseId database, String commandText) throws SQLException {
            try {
                // make a query to our Select servlet
                HttpPost postRequest = new HttpPost(ApplabConfiguration.getHostUrl() + "select");
                postRequest.appendToBody("<?xml version=\"1.0\"?>");
                postRequest.appendToBody("<SelectRequest xmlns=\"http://schemas.applab.org/2010/07\" target=\"");
                postRequest.appendToBody(database.toString());
                postRequest.appendToBody("\">");
                postRequest.appendToBody(commandText);
                postRequest.appendToBody("</SelectRequest>");                

                return new RemoteResultSet(XmlHelpers.parseXml(postRequest.getResponse().getBodyReader()));
            }
            catch (SAXException e) {
                throw new SQLException("executeQuery error: " + e.getMessage());
            }
            catch (IOException e) {
                throw new SQLException("executeQuery error: " + e.getMessage());
            }
            catch (ParserConfigurationException e) {
                throw new SQLException("executeQuery error: " + e.getMessage());
            }
        }
    }
}