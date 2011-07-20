package applab.surveys;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import applab.server.Base64;
import applab.server.XmlHelpers;

/**
 * This class provides the methods required to parse a purcform and generate a questions HashMap along with the question
 * order array.
 * 
 */

public class ParsedSurveyXml {

    private String xml;
    private ArrayList<String> questionOrder;
    private HashMap<String, Question> questions;
    private HashMap<String, TranslationMap> languageMap;
    private FormType formType;

    private int questionNumberCounter;

    public ParsedSurveyXml(String xml) {
        this.xml = xml;
        this.questionOrder = null;
        this.questions = null;
        this.languageMap = null;
        this.formType = null;
        this.questionNumberCounter = 1;
    }

    public ArrayList<String> getQuestionOrder() throws SAXException, IOException, ParserConfigurationException {
        if (this.questionOrder == null) {
            parseQuestions();
        }
        return this.questionOrder;
    }

    public HashMap<String, Question> getQuestions() throws SAXException, IOException, ParserConfigurationException {
        if (this.questions == null) {
            parseQuestions();
        }
        return this.questions;
    }

    /**
     * Determine if a survey contains a question
     * 
     * @param questionName
     *            - the question we are looking for.
     * @return - boolean
     */
    public boolean hasQuestion(String questionName) throws SAXException, IOException, ParserConfigurationException {
        if (this.questions == null) {
            parseQuestions();
        }
        if (this.questions.containsKey(questionName)) {
            return true;
        }
        return false;
    }

    public FormType getFormType() {
        return this.formType;
    }

    /**
     * Extract the translated question name from the language map
     * 
     * @param languageCode
     *            - The language that we are looking for
     * @param question
     *            - the question we want the text xlated for
     * @return
     */
    public String getXlation(String languageCode, Question question) {

        String xlatedName = "";

        switch (this.formType) {
            case JavaRosa:
                if (this.languageMap.containsKey(languageCode)
                        && this.languageMap.get(languageCode).getLanguageMap().containsKey(question.getDisplayValue())) {
                    xlatedName = this.languageMap.get(languageCode).getLanguageMap().get(question.getDisplayValue());
                }
                break;
            default:
                xlatedName = question.parseDisplayValue();
                break;
        }
        return xlatedName;
    }

    /**
     * 
     * @param languageCode
     * @param answer
     * @return
     */
    public String getXlation(String languageCode, String answer) {

        String xlatedName = "";

        switch (this.formType) {
            case JavaRosa:
                if (this.languageMap.containsKey(languageCode) && this.languageMap.get(languageCode).getLanguageMap().containsKey(answer)) {
                    xlatedName = this.languageMap.get(languageCode).getLanguageMap().get(answer);
                }
                break;
            default:
                xlatedName = answer;
                break;
        }
        return xlatedName;
    }

    /**
     * 
     * @param input
     *            - original string
     * @return - a gzipped base64 encoded string of the input
     */
    public String compressXml(String input) throws IOException {
        return Base64.encodeBytes(input.getBytes(), Base64.GZIP);
    }

    /**
     * 
     * @param input
     *            gzipped, base64 encoded string.
     * @return - the uncompressed original string
     */
    public String decompressXml(String input) throws IOException {
        return new String(Base64.decode(input));
    }

    /**
     * Check the parent node to decide which type of format the survey is in
     */
    public void parseQuestions() throws SAXException, IOException, ParserConfigurationException {

        this.questions = new HashMap<String, Question>();
        this.questionOrder = new ArrayList<String>();
        Document xmlDocument = XmlHelpers.parseXml(this.xml);
        xmlDocument.normalizeDocument();
        Element rootNode = xmlDocument.getDocumentElement();

        // Decide which format the survey is saved in
        this.formType = getFormType(rootNode);

        switch (this.formType) {
            case JavaRosa:
                parseJavaRosaForm(rootNode);
                break;
            case Xform:
                parseXform(rootNode);
                break;
        }
    }

    private void parseJavaRosaForm(Element rootNode) throws SAXException, IOException, ParserConfigurationException {

        for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if (childNode.getLocalName().equals("head")) {
                    parseJavaRosaHead((Element)childNode);
                }
                if (childNode.getLocalName().equals("body")) {
                    parseJavaRosaBody((Element)childNode);
                }
            }
        }
    }

    private void parseJavaRosaHead(Element rootNode) throws SAXException, IOException, ParserConfigurationException {

        for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {

                // Model Tag - this contains the xlations
                if (childNode.getLocalName().equals("model")) {
                    parseXlations((Element)childNode);
                }
            }
        }
    }

    private void parseJavaRosaBody(Element rootNode) throws SAXException, IOException, ParserConfigurationException {

        for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                // Group tag
                if (childNode.getLocalName().equals("group")) {
                    parseGroup((Element)childNode);
                }
            }
        }
    }

    /**
     * 
     * @param rootNode
     */
    private void parseXlations(Element rootNode) {

        this.languageMap = new HashMap<String, TranslationMap>();
        for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if (childNode.getLocalName().equals("itext")) {
                    parseItext((Element)childNode);
                }
            }
        }
    }

    private void parseItext(Element itextNode) {
        for (Node childNode = itextNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if (childNode.getLocalName().equals("translation")) {
                    parseLanguage((Element)childNode);
                }
            }
        }

    }

    private void parseLanguage(Element languageNode) {
        String languageCode = languageNode.getAttribute("lang");
        if (this.languageMap != null) {
            TranslationMap translationMap = null;
            if (!this.languageMap.containsKey(languageCode)) {
                translationMap = new TranslationMap(languageCode);
            }
            else {
                translationMap = this.languageMap.get(languageCode);
            }
            for (Node childNode = languageNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (childNode.getLocalName().equals("text")) {
                        Element child = (Element)childNode;
                        translationMap.addTranslation(child.getAttribute("id"), parseXlation(child));
                    }
                }
            }
            this.languageMap.put(languageCode, translationMap);
        }
    }

    private String parseXlation(Element xlationNode) {
        String xlation = "";
        for (Node childNode = xlationNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if (childNode.getLocalName().equals("value")) {
                    xlation = getCharacterDataFromElement((Element)childNode);
                }
            }
        }
        return xlation;
    }

    /**
     * Parse the form to generate the question hash map and the questionOrder array.
     * 
     * The xml has changed from <xf:<elementType>> to just <<elementType>>. This is why there is the contains method
     * used and some slightly dodgy bits here and there especially with the select and select1 nodes. TODO - clean out
     * the old way when enough time has passed that the old forms are not in use 2.11 maybe. This removes the xf: prefix
     * 
     * @param rootNode
     *            - The node containing the XML
     */
    private void parseXform(Element rootNode) throws SAXException, IOException, ParserConfigurationException {

        for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {

                // Group tag
                if (childNode.getLocalName().equals("group")) {
                    parseGroup((Element)childNode);
                }
            }
        }
    }

    private void parseGroup(Element groupElement) {

        for (Node childNode = groupElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {

                // Input tag
                if (childNode.getLocalName().equals("input") || childNode.getLocalName().equals("upload")) {
                    parseInput((Element)childNode);
                }
                else if (childNode.getLocalName().contains("select")) {
                    parseSelect((Element)childNode);
                }
                else if (childNode.getLocalName().equals("group")) {
                    parseSubgroup((Element)childNode);
                }
            }
        }
    }

    private void parseSubgroup(Element groupElement) {

        String binding = groupElement.getAttribute("id");
        String questionName = "";

        // Need to add this here so that the question is in the right place.
        this.questionOrder.add(binding);
        Integer questionNumber = this.questionNumberCounter;
        this.questionNumberCounter++;
        for (Node childNode = groupElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue; // only care about element nodes
            }
            if (null != childNode.getLocalName()) {
                if (childNode.getLocalName().equals("label")) {
                    questionName = getQuestionData((Element)childNode);
                }
                else if (childNode.getLocalName().equals("repeat")) {
                    for (Node repeatChild = childNode.getFirstChild(); repeatChild != null; repeatChild = repeatChild.getNextSibling()) {
                        if (null != repeatChild.getLocalName()) {
                            if (repeatChild.getLocalName().equals("input") || childNode.getLocalName().equals("upload")) {
                                parseInput((Element)repeatChild);
                            }

                            // This will catch the single selects as well
                            else if (repeatChild.getLocalName().contains("select")) {
                                parseSelect((Element)repeatChild);
                            }
                        }
                    }
                }
            }
        }
        this.questions.put(binding, new Question(binding, questionName, QuestionType.Repeat, questionNumber));
    }

    /**
     * Parse an input tag, which is used for free-entry questions, such as text, numbers, and dates.
     * 
     */
    private void parseInput(Element inputElement) {

        String questionBinding = getFormElementName(inputElement);
        Question question = null;
        for (Node childNode = inputElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if (childNode.getLocalName().equals("label")) {
                    String binding = getFormElementName(inputElement);
                    question = new Question(binding, getQuestionData((Element)childNode), QuestionType.Input,this.questionNumberCounter);
                }
            }
        }
        if (question != null) {
            this.questions.put(questionBinding, question);
            this.questionNumberCounter++;
            this.questionOrder.add(questionBinding);
        }
    }

    /**
     * Parse a select (select) tag, which is used for multiple-choice questions, either with only one choice (select1)
     * or multiple choices (select).
     * 
     */
    private void parseSelect(Element selectElement) {

        String questionBinding = getFormElementName(selectElement);
        String questionText = "";
        String values = "";

        for (Node childNode = selectElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if (childNode.getLocalName().equals("label")) {
                    questionText = getQuestionData((Element)childNode);
                }
                else if (childNode.getLocalName().equals("item")) {

                    // Items are of the form <item><label>Choice Label</label><value>Choice value</value></item>
                    Element itemElement = (Element)childNode;

                    String value = "";
                    String option = "";
                    for (Node itemChild = itemElement.getFirstChild(); itemChild != null; itemChild = itemChild.getNextSibling()) {
                        if (null != itemChild.getLocalName()) {
                            if (itemChild.getLocalName().equals("label")) {
                                option = getQuestionData((Element)itemChild);
                            }
                            else if (itemChild.getLocalName().equals("value")) {
                                value = getCharacterDataFromElement((Element)itemChild);
                            }
                        }
                    }

                    values += option + ":" + value + ";";
                }
            }
        }
        String rawText = questionText + " -- " + values;

        // Create the question with the required type
        if (selectElement.getLocalName().equals("select1")) {
            this.questions.put(questionBinding, new Question(questionBinding, rawText, QuestionType.Select1, this.questionNumberCounter));
        }
        else {
            this.questions.put(questionBinding, new Question(questionBinding, rawText, QuestionType.Select, this.questionNumberCounter));
        }
        this.questionNumberCounter++;
        this.questionOrder.add(questionBinding);
    }

    private String getFormElementName(Element formElement) {

        // Bind attributes take top priority if they exist
        String xformName = formElement.getAttribute("bind");

        // Otherwise look for a ref parameter
        if (xformName == null || xformName.length() == 0) {
            xformName = formElement.getAttribute("ref");
        }
        return xformName;
    }

    private String getQuestionData(Element element) {

        String questionName = "";
        switch (this.formType) {
            case JavaRosa:
                questionName = setLanguageKey(element);
                break;
            case Xform:
                questionName = getCharacterDataFromElement(element);
                break;
        }
        return questionName;
    }

    /**
     * Parse the xfrom element to get required data
     * 
     * @param element
     *            - To be parsed
     * @return - String that represents the required data
     */
    private String getCharacterDataFromElement(Element element) {
        Node child = element.getFirstChild();
        if (child instanceof CharacterData) {
            return ((CharacterData)child).getData();
        }
        return "?";
    }

    /**
     * Parse the JavaRosa label Node to get the key for the translation
     * 
     * @param languageNode
     */
    private String setLanguageKey(Element labelNode) {

        // Parse the name from the ref value
        String refString = labelNode.getAttribute("ref");
        int startIndex = refString.indexOf("('") + 2;
        int endIndex = refString.length() - 2;
        return refString.substring(startIndex, endIndex);
    }

    private FormType getFormType(Element rootNode) {

        FormType formType = null;

        // Decide which format the survey is saved in
        String rootNodeName = rootNode.getLocalName();
        if (rootNodeName.equals("xform")) {
            formType = FormType.Xform;
        }
        else if (rootNodeName.equals("html")) {
            formType = FormType.JavaRosa;
        }
        return formType;
    }
}
