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
    private String name;
    private String rawValue;
    private String displayValue;
    private HashMap<String, String> choices;
    private QuestionType type;

    public Question(String name, String value) {
        this.name = name;
        this.rawValue = value;
        this.choices = new HashMap<String, String>();

        // default to input type
        this.type = QuestionType.Input;
        this.displayValue = this.rawValue;

        // TODO, ZBR-89: we need to store type in the questions table so that we can better track Select vs. Input
        // in our answer parsing (we'd then pass QuestionType in the ctor). Right now we ad-hoc based on question value
        // contents,
        // since they are of the form <displayValue> - <choice1>:1;<choice2>:2;...
        if (this.rawValue.contains(":1;")) {
            // look for "? - ", and fall back on " - " if necessary
            int separatorIndex = this.rawValue.indexOf("? - ");
            if (separatorIndex > -1) {
                // move past the question mark, which is part of the displayValue
                separatorIndex++;
            }
            else {
                separatorIndex = this.rawValue.indexOf(" - ");
            }

            // if both searches failed, then treat this as a raw value
            if (separatorIndex > -1) {
                this.displayValue = this.rawValue.substring(0, separatorIndex);

                // for the choices, move past the " - ";
                String encodedChoices = this.rawValue.substring(separatorIndex + 3);
                while (encodedChoices.length() > 0) {
                    // choices are of the form <choice text>:<choice index>;
                    int colonIndex = encodedChoices.indexOf(':');
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
                    String choiceIndex = encodedChoices.substring(colonIndex + 1, semicolonIndex);
                    this.choices.put(choiceIndex, choiceValue);
                    encodedChoices = encodedChoices.substring(semicolonIndex + 1);
                }

                if (this.choices.size() > 0) {
                    // again, we don't know the type, so right now we treat everything as multi-choice
                    this.type = QuestionType.Select;
                }
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public QuestionType getType() {
        return this.type;
    }

    public String getDisplayValue() {
        return this.displayValue;
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
}
