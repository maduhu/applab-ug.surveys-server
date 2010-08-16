package applab.surveys.server;

import java.io.*;

import javax.servlet.http.*;
import javax.xml.parsers.ParserConfigurationException;

import java.sql.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import applab.server.*;

/**
 * Executes a select query against our databases using a SQL account with read-only access
 * 
 */
public class Select extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    public final static String NAMESPACE = "http://schemas.applab.org/2010/07";
    public final static String SELECT_REQUEST_ELEMENT_NAME = "SelectRequest";
    public final static String SELECT_RESPONSE_ELEMENT_NAME = "SelectResponse";
    public final static String ROW_ELEMENT_NAME = "row";
    public final static String TARGET_ATTRIBUTE_NAME = "target";

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
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Expect a request of the form <SelectRequest xmlns=\"" + NAMESPACE + "\">SQL command text</SelectRequest>");
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/xml");
        PrintWriter responseStream = response.getWriter();

        responseStream.println("<?xml version=\"1.0\"?>");
        responseStream.print("<" + SELECT_RESPONSE_ELEMENT_NAME);
        responseStream.println("xmlns=\"" + NAMESPACE + "\">");

        Connection databaseConnection = DatabaseHelpers.createReaderConnection(parsedRequest.getDatabaseId());
        Statement selectStatement = databaseConnection.createStatement();
        ResultSet resultSet = selectStatement.executeQuery(selectCommandText);
        ResultSetMetaData columnMetadata = resultSet.getMetaData();
        int columnCount = columnMetadata.getColumnCount();
        while (resultSet.next()) {
            printStartElement(responseStream, ROW_ELEMENT_NAME);

            // JDBC references are 1-based, not 0-based
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                String columnName = columnMetadata.getColumnName(columnIndex);
                printStartElement(responseStream, columnName);
                responseStream.print(resultSet.getString(columnIndex));
                printEndElement(responseStream, columnName);
            }
            printEndElement(responseStream, ROW_ELEMENT_NAME);
            responseStream.println();
        }
        printEndElement(responseStream, SELECT_RESPONSE_ELEMENT_NAME);
        responseStream.close();
        selectStatement.close();
        databaseConnection.close();
    }
    
    // used for standard start elements without any attributes 
    private static void printStartElement(PrintWriter stream, String elementName) {
        stream.print("<" + elementName + ">");
    }

    private static void printEndElement(PrintWriter stream, String elementName) {
        stream.print("</" + elementName + ">");
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
        String selectContent = "";
        Element rootNode = requestXml.getDocumentElement();
        if (NAMESPACE.equals(rootNode.getNamespaceURI()) && SELECT_REQUEST_ELEMENT_NAME.equals(rootNode.getLocalName())) {
            // see if a specific database has been requested
            String targetDatabaseValue = rootNode.getAttribute(TARGET_ATTRIBUTE_NAME);
            if (targetDatabaseValue.length() > 0) {
                targetDatabase = DatabaseId.valueOf(targetDatabaseValue);
            }

            // and get the XML content for our SELECT statement
            selectContent = XmlHelpers.getContent(rootNode);
        }
        return new SelectRequest(targetDatabase, selectContent);
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
