package applab.surveys.server.test;

import java.io.*;

import javax.servlet.http.*;
import javax.xml.parsers.ParserConfigurationException;

import java.sql.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import applab.server.*;

/**
 * TEST-ONLY: method used by our tests to perform remote updates against the test-database so that developers don't all
 * need to have a local instance of MySQL with the relevant data included
 * 
 */
public class Update extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    private final static String NAMESPACE = "http://schemas.applab.org/2010/07";
    private final static String UPDATE_REQUEST_ELEMENT_NAME = "UpdateRequest";
    private final static String TARGET_ATTRIBUTE_NAME = "target";

    // given a post body like:
    // <?xml version="1.0"?>
    // <UpdateRequest xmlns="http://schemas.applab.org/2010/07" target="Search"> <!-- or "Surveys" -->
    // SELECT * from ycppquiz.OktopusSearchLog
    // </UpdateRequest>
    // 
    // returns a response like: 
    // <?xml version="1.0"?>
    // <UpdateResponse xmlns="http://schemas.applab.org/2010/07">
    // <number of rows updated>
    // </UpdateResponse>
    
    protected void doApplabPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ClassNotFoundException, SQLException, SAXException, ParserConfigurationException {
        Document requestXml = XmlHelpers.parseXml(request.getReader());
        UpdateRequest parsedRequest = parseRequest(requestXml);
        String updateCommandText = parsedRequest.getUpdateText();
        if (updateCommandText.length() == 0) {
            response
                    .sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "Expect a request of the form <UpdateRequest xmlns=\"http://schemas.applab.org/2010/07\">SQL command text</UpdateRequest>");
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/xml");
        PrintWriter responseStream = response.getWriter();

        Connection databaseConnection = DatabaseHelpers.createConnection(parsedRequest.getDatabaseId());
        Statement updateStatement = databaseConnection.createStatement();
        int numberOfRowsUpdated = updateStatement.executeUpdate(updateCommandText);
        responseStream.println("<?xml version=\"1.0\"?>");
        responseStream.print("<UpdateResponse xmlns=\"http://schemas.applab.org/2010/07\">");
        responseStream.print(numberOfRowsUpdated);
        responseStream.print("</UpdateResponse>");
        responseStream.close();
        updateStatement.close();
        databaseConnection.close();
    }

    // parse a select request and get out the string
    // if the string is empty, caller can respond with 400: Bad Request
    private static UpdateRequest parseRequest(Document requestXml) {
        assert (requestXml != null);

        DatabaseId targetDatabase = DatabaseId.Surveys;
        StringBuilder updateContent = new StringBuilder();
        Element rootNode = requestXml.getDocumentElement();
        if (NAMESPACE.equals(rootNode.getNamespaceURI()) && UPDATE_REQUEST_ELEMENT_NAME.equals(rootNode.getLocalName())) {
            String targetDatabaseValue = rootNode.getAttribute(TARGET_ATTRIBUTE_NAME);
            if (targetDatabaseValue.length() > 0) {
                targetDatabase = DatabaseId.valueOf(targetDatabaseValue);
            }

            // and look through the child node for the text content
            for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
                if (childNode.getNodeType() == Node.TEXT_NODE) {
                    updateContent.append(childNode.getNodeValue());
                }
            }
        }
        return new UpdateRequest(targetDatabase, updateContent.toString());
    }

    static class UpdateRequest {
        private DatabaseId targetDatabase;
        private String updateText;

        public UpdateRequest(DatabaseId targetDatabase, String updateText) {
            this.targetDatabase = targetDatabase;
            this.updateText = updateText;
        }

        public DatabaseId getDatabaseId() {
            return this.targetDatabase;
        }

        public String getUpdateText() {
            return this.updateText;
        }
    }
}
