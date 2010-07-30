package applab.surveys.server;

import java.io.*;

import javax.servlet.http.*;
import javax.xml.parsers.ParserConfigurationException;

import java.sql.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Executes a select query against our databases using a SQL account with read-only access
 * 
 */
public class Select extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    private final static String NAMESPACE = "http://schemas.applab.org/2010/07";
    private final static String SELECT_REQUEST_ELEMENT_NAME = "SelectRequest";
    private final static String TARGET_ATTRIBUTE_NAME = "target";

    // given a post body like:
    // <?xml version="1.0"?>
    // <SelectRequest xmlns="http://schemas.applab.org/2010/07" target="Search"> <!-- or "Surveys" -->
    // SELECT * from ycppquiz.OktopusSearchLog
    // </SelectRequest>
    // 
    // returns a response like:
    // <?xml version="1.0"?>
    // <SelectResponse xmlns="http://schemas.applab.org/2010/07">
    // <row><column1Name>data</column1Name><column2Name>data2</column2Name></row>
    // <row><column1Name>data</column1Name><column2Name>data2</column2Name></row>
    // </SelectResponse>
    //
    // Security Concern: DoS due to expensive selects.
    // Possible mitigations: HTTP auth, HTTPS, closed query space
    @Override
    protected void doApplabPost(HttpServletRequest request, HttpServletResponse response) throws IOException, SAXException, ParserConfigurationException, ClassNotFoundException, SQLException {
        Document requestXml = XmlHelpers.parseXml(request.getReader());
        SelectRequest parsedRequest = parseRequest(requestXml);
        String selectCommandText = parsedRequest.getSelectText();
        if (selectCommandText.length() == 0) {
            response
                    .sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "Expect a request of the form <SelectRequest xmlns=\"http://schemas.applab.org/2010/07\">SQL command text</SelectRequest>");
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/xml");
        PrintWriter responseStream = response.getWriter();

        responseStream.println("<?xml version=\"1.0\"?>");
        responseStream.println("<SelectResponse xmlns=\"http://schemas.applab.org/2010/07\">");

        Connection databaseConnection = DatabaseHelpers.createReaderConnection(parsedRequest.getDatabaseId());
        Statement selectStatement = databaseConnection.createStatement();
        ResultSet resultSet = selectStatement.executeQuery(selectCommandText);
        ResultSetMetaData columnMetadata = resultSet.getMetaData();
        int columnCount = columnMetadata.getColumnCount();
        while (resultSet.next()) {
            responseStream.print("<row>");

            // JDBC references are 1-based, not 0-based
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                String columnName = columnMetadata.getColumnName(columnIndex);
                responseStream.print("<" + columnName + ">");
                responseStream.print(resultSet.getString(columnIndex));
                responseStream.print("</" + columnName + ">");
            }
            responseStream.println("</row>");
        }
        responseStream.print("</SelectResponse>");
        responseStream.close();
        selectStatement.close();
        databaseConnection.close();
    }

    // Test methods
    // TODO: how do we hide these from production?
    public static String getQueryFromRequest(Document requestXml) {
        return parseRequest(requestXml).getSelectText();
    }

    public static DatabaseId getTargetFromRequest(Document requestXml) {
        return parseRequest(requestXml).getDatabaseId();
    }

    // parse a select request and get out the string
    // if the string is empty, caller can respond with 400: Bad Request
    private static SelectRequest parseRequest(Document requestXml) {
        assert (requestXml != null);

        DatabaseId targetDatabase = DatabaseId.Surveys;
        StringBuilder selectContent = new StringBuilder();
        Element rootNode = requestXml.getDocumentElement();
        if (NAMESPACE.equals(rootNode.getNamespaceURI()) && SELECT_REQUEST_ELEMENT_NAME.equals(rootNode.getLocalName())) {
            String targetDatabaseValue = rootNode.getAttribute(TARGET_ATTRIBUTE_NAME);
            if (targetDatabaseValue.length() > 0) {
                targetDatabase = DatabaseId.valueOf(targetDatabaseValue);
            }

            // and look through the child node for the text content
            for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
                if (childNode.getNodeType() == Node.TEXT_NODE) {
                    selectContent.append(childNode.getNodeValue());
                }
            }
        }
        return new SelectRequest(targetDatabase, selectContent.toString());
    }

    static class SelectRequest {
        private DatabaseId targetDatabase;
        private String selectText;

        public SelectRequest(DatabaseId targetDatabase, String selectText) {
            this.targetDatabase = targetDatabase;
            this.selectText = selectText;
        }

        public DatabaseId getDatabaseId() {
            return this.targetDatabase;
        }

        public String getSelectText() {
            return this.selectText;
        }
    }
}
