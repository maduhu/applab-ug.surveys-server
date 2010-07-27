package applab.surveys.server;

import java.io.*;

import javax.servlet.http.*;
import java.sql.*;

import org.w3c.dom.*;

/**
 * Executes a select query against our databases using a SQL account with read-only access
 * 
 */
public class Select extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final static String NAMESPACE = "http://schemas.applab.org/2010/07";
    private final static String SELECT_RESPONSE_ELEMENT_NAME = "SelectResponse";
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            SelectRequest parsedRequest = parseRequest(XmlHelpers.parseXml(request.getReader()));
            String selectCommandText = parsedRequest.getSelectText();
            if (selectCommandText.length() == 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Expect a request of the form <SelectRequest>SQL command text</SelectRequest>");
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
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
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
        catch (Exception exception) {
            exception.printStackTrace();
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    // parse a select request and get out the string
    // if the string is empty, caller can respond with 400: Bad Request
    private SelectRequest parseRequest(Document requestXml) {
        assert (requestXml != null);

        DatabaseId targetDatabase = DatabaseId.Surveys;
        StringBuilder selectContent = new StringBuilder();
        Element rootNode = requestXml.getDocumentElement();
        if (rootNode.getNamespaceURI() == NAMESPACE && rootNode.getLocalName() == SELECT_RESPONSE_ELEMENT_NAME) {
            for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
                switch (childNode.getNodeType()) {
                    case Node.TEXT_NODE:
                        selectContent.append(childNode.getNodeValue());
                        break;

                    case Node.ATTRIBUTE_NODE:
                        if (childNode.getNodeName() == TARGET_ATTRIBUTE_NAME) {
                            targetDatabase = DatabaseId.valueOf(childNode.getNodeValue());
                        }
                        break;

                    default:
                        // don't care about other types of nodes
                        break;
                }
            }
        }
        return new SelectRequest(targetDatabase, selectContent.toString());
    }

    class SelectRequest {
        DatabaseId targetDatabase;
        String selectText;

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
