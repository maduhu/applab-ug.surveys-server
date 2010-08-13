package applab.surveys.server.test;

import java.io.*;
import java.net.*;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

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
            // make a query to our Select servlet
            URL url;
            try {
                url = new URL(ApplabConfiguration.getHostUrl() + "select");
                URLConnection connection = url.openConnection();
                // since we're doing a POST, need to configure a few settings
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "text/xml");
                DataOutputStream requestStream = new DataOutputStream(connection.getOutputStream());

                requestStream.writeBytes("<?xml version=\"1.0\"?>");
                requestStream.writeBytes("<SelectRequest xmlns=\"http://schemas.applab.org/2010/07\" target=\"");
                requestStream.writeBytes(database.toString());
                requestStream.writeBytes("\">");
                requestStream.writeBytes(commandText);
                requestStream.writeBytes("</SelectRequest>");                
                requestStream.close();

                Reader responseBody = new InputStreamReader(connection.getInputStream());
                return new RemoteResultSet(XmlHelpers.parseXml(responseBody));
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