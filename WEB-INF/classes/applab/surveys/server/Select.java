package applab.surveys.server;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;

import javax.servlet.http.*;
import java.sql.*;

import org.w3c.dom.*;

import configuration.DbConnect;

/**
 * Executes a select query against our databases using a SQL account with read-only access
 * 
 */
public class Select extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final static String NAMESPACE = "http://schemas.applab.org/2010/07";
    private final static String SELECT_RESPONSE_NODE = "SelectResponse";

    // given a post body like:
    // <?xml version="1.0"?>
    // <SelectRequest xmlns="http://schemas.applab.org/2010/07">
    // SELECT * from ycppquiz.OktopusSearchLog
    // </SelectRequest>
    // 
    // returns a response like:
    // <?xml version="1.0"?>
    // <SelectResponse xmlns="http://schemas.applab.org/2010/07">
    // <row><col>data</col><col>data2</col></row>
    // <row><col>data</col><col>data2</col></row>
    // </SelectResponse>
    //
    // Security Concern: DoS due to expensive selects.
    // Possible mitigations: HTTP auth, HTTPS, closed query space
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String selectCommandText = parseRequest(null);
            if (selectCommandText.length() == 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                        "Expect a request of the form <SelectRequest>SQL command text</SelectRequest>");
                return;
            }
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/xml");
            PrintWriter responseStream = response.getWriter();

            responseStream.println("<?xml version=\"1.0\"?>");
            responseStream.println("<SelectRequest xmlns=\"http://schemas.applab.org/2010/07\">");
            
            Connection databaseConnection = DbConnect.createReaderConnection();
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
            responseStream.print("</SelectRequest>");
            responseStream.close();
            selectStatement.close();
            databaseConnection.close();
        }
        catch (Exception exception) {
            exception.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // parse a select request and get out the string
    // if the string is empty, caller can respond with 400: Bad Request
    private String parseRequest(Document requestXml) {
        assert (requestXml != null);

        StringBuilder selectContent = new StringBuilder();
        Element rootNode = requestXml.getDocumentElement();
        if (rootNode.getNamespaceURI() == NAMESPACE && rootNode.getLocalName() == SELECT_RESPONSE_NODE) {
            for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
                if (childNode.getNodeType() == Node.TEXT_NODE) {
                    selectContent.append(childNode.getNodeValue());
                }
                else {
                    // don't care about other types of nodes
                }
            }
        }
        return selectContent.toString();
    }
}
