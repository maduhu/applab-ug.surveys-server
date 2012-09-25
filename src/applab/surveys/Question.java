package applab.surveys;

import java.util.*;

/**
 * Object that represents a question in a survey form.
 * 
 * There are currently two supported types of questions: 1) Input - used for free-entry questions, such as text,
 * numbers, and dates
 * 
 * 2) Select - used for multiple-choice questions, either with only one choice (select1) or multiple choices (select)
 * 
 */
public class Question {
    private String binding;
    private String rawValue;
    private String displayValue;
    private HashMap<String, String> choices;
    private HashMap<Integer, String> bindingIndexMap;
    private QuestionType type;
    private Integer totalInstances;
    private Integer numberOfSelects;
    private Integer questionNumber;

    public Question(String binding, String value, QuestionType type, int questionNumber) {
        this.binding = binding;
        this.rawValue = value;
        this.choices = new HashMap<String, String>();this.bindingIndexMap = new HashMap<Integer, String>();
        this.totalInstances = 0;
        this.numberOfSelects = 0;
        this.type = type;
        this.questionNumber = questionNumber;

        // Using the question type parse any information we may need
        int index = -1;
        switch (type) {
            case Select1:
                index = parseRawValue();
                if (index > -1) {
                    buildChoiceIndex(index);
                }
                break;
            case Select:
                index = parseRawValue();
                if (index > -1) {
                    buildChoiceIndex(index);
                }
                break;
            default:
                this.displayValue = this.rawValue;
                break;
        }
    }

    public String getBinding() {
        return this.binding;
    }

    public QuestionType getType() {
        return this.type;
    }

    public String getDisplayValue() {
        return this.displayValue;
    }
    
    public int getNumberOfSelects() {
        return this.numberOfSelects;
    }
    
    // We can't be certain that the questions were saved correctly (user input error)
    // So parse the text just in case
    public String parseDisplayValue() {
        int index = this.rawValue.indexOf(" - ");
        if (index == -1) {
            return this.displayValue;
        }
        return this.displayValue.substring(0, index);
       
    }
    public String getRawValue() {
        return this.rawValue;
    }

    public String getChoice(String choiceIndex) {
        String choiceValue = this.choices.get(choiceIndex);
        // for convenience, map null to the empty string
        if (choiceValue == null) {
            choiceValue = "";
        }
        return choiceValue;
    }

    public String getValueAtIndex(Integer index) {
    	return this.bindingIndexMap.get(index);
    }

    public Integer getTotalInstances() {
        return this.totalInstances;
    }
    
    public void setTotalInstances(Integer newTotal) {
        this.totalInstances = newTotal;
    }
    
    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(Integer questionNumber) {
        this.questionNumber = questionNumber;
    }

    // From the raw text value of the question get the display value
    private int parseRawValue() {
        int index = this.rawValue.indexOf(" -- ");
        if (index == -1) {
            this.displayValue = this.rawValue;
        }
        else {
            this.displayValue = this.rawValue.substring(0, index);
        }
        return index;
    }

    private void buildChoiceIndex(int index) {

        String encodedChoices = this.rawValue.substring(index + 4);
        int choiceCount = 0;
        int binding = 0;
        while (encodedChoices.length() > 0) {
            // choices are of the form <choice text>:<choice index>;
            int colonIndex = encodedChoices.indexOf(':');binding++;
            if (colonIndex == -1) {
                // if we don't find a colon, bail
                break;
            }
            int semicolonIndex = encodedChoices.indexOf(';', colonIndex + 1);
            // if we don't find a semi-colon, assume we're on the last entry
            if (semicolonIndex == -1) {
                semicolonIndex = encodedChoices.length() - 1;
            }
            String choiceValue = encodedChoices.substring(0, colonIndex);
            // there are cases where the answers are prefixed by "--- " to work around a pre-1.6 android display issue.
            // We strip these for any processing purposes
            if (choiceValue.startsWith("---")) {
                choiceValue = choiceValue.substring(3).trim();
            }
            
            // While by convention our data collection team uses integers as the option names,
            // the tool and schema are based on string representations and we cannot always expect an integer 
            String choiceIndex = encodedChoices.substring(colonIndex + 1, semicolonIndex);this.bindingIndexMap.put(binding, choiceIndex);
            this.choices.put(choiceIndex, choiceValue);
            choiceCount++;
            encodedChoices = encodedChoices.substring(semicolonIndex + 1);
        }
        this.numberOfSelects = choiceCount;
    }
}
