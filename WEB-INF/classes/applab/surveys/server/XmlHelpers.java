package applab.surveys.server;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Helper methods that can be used for manipulating XML documents
 * 
 */
public class XmlHelpers {
    static DocumentBuilderFactory documentBuilderFactory = CreateDocumentBuilderFactory();
    static TransformerFactory transformerFactory = TransformerFactory.newInstance();

    private static DocumentBuilderFactory CreateDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // need to set this to true so that getLocalName and getNamespaceURI function correctly
        factory.setNamespaceAware(true);
        return factory;
    }
    
    /**
     * Convert the given string into an XML DOM
     */
    public static Document parseXml(String xmlString) throws SAXException, IOException, ParserConfigurationException {
        return parseXml(new StringReader(xmlString));
    }

    /**
     * Convert the given reader into an XML DOM
     */
    public static Document parseXml(Reader reader) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        InputSource inputSource = new InputSource(reader);
        return documentBuilder.parse(inputSource);
    }

    /**
     * Return the contents of the given XML DOM as a string
     * @throws TransformerException 
     */
    public static String exportAsString(Document xmlDocument) throws TransformerException {
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StreamResult result = new StreamResult(new StringWriter());
        transformer.transform(new DOMSource(xmlDocument), result);
        return result.getWriter().toString();
    }
}