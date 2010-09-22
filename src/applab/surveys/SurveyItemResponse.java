package applab.surveys;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * in memory representation of a survey item response. Usually this just contains a text answer, but in the case of
 * repeat questions, it can contain a collection of responses as the answer.
 * 
 * Single answer example:
 * 
 * <q1>My answer</q1>
 * 
 * Repeat answer example:
 * 
 * <q3> <q4>Answer 4</q4><q5>Answer 5</q5> </q3> <q3> <q4>Answer 4 #2</q4><q5>Answer 5 #2</q5> </q3>
 */
public class SurveyItemResponse {

    private String questionName;

    // We use two fields here for performance, so that we don't need to allocate an array
    // for simple text answers
    private String singletonAnswer;
    private ArrayList<String> multipleAnswers;

    // Will be non-null if this is the child of a repeat question
    private SurveyAnswerGroup parent;

    public SurveyItemResponse(String questionName, SurveyAnswerGroup parent) {
        
        if (questionName == null) {
            questionName = "";
        }
        this.questionName = questionName;
        this.parent = parent;

        if (this.parent != null) {    
            this.parent.addChild(this);
        }
    }

    public String getQuestionName() {
        return this.questionName;
    }

    public void addAnswerText(String answerText) {
        if (answerText != null) {
            
            // See if we need to promote to a list
            if (this.singletonAnswer != null) {

                // we should only get to this case when we have a valid parent group
                assert (this.parent != null) : "we should only get multiple answers when contained in a parent group";

                this.multipleAnswers = new ArrayList<String>();
                this.multipleAnswers.add(this.singletonAnswer);
                this.singletonAnswer = null;
            }

            if (this.multipleAnswers != null) {
                this.multipleAnswers.add(answerText);
            }
            else {
                this.singletonAnswer = answerText;
            }
        }
    }

    /**
     * The encoding for a single answer is simply the text.
     * 
     * The encoding for multiple answers is, for example. [child:q6][1]answer\n[2]answer
    */
    public String getEncodedAnswer(HashMap<String, String> attachments) {
        StringBuilder encodedAnswer = new StringBuilder();
        if (this.parent != null) {
            encodedAnswer.append("[child:");
            encodedAnswer.append(this.parent.getQuestionName());
            encodedAnswer.append("]");
        }
        if (this.multipleAnswers != null) {
            int prefix = 1;
            for (String answerText : this.multipleAnswers) {
                if (prefix > 1) {
                    encodedAnswer.append("\n");
                }
                encodedAnswer.append("[");
                encodedAnswer.append(prefix);
                encodedAnswer.append("]");
                encodedAnswer.append(resolveAnswerText(answerText, attachments));
                prefix++;
            }
        }
        else if (this.singletonAnswer != null) {
            encodedAnswer.append(resolveAnswerText(this.singletonAnswer, attachments));
        }

        return encodedAnswer.toString();
    }

    /**
     * Helper function to turn an attachment reference into the correct path if necessary
     */
     private final String resolveAnswerText(String answerText, HashMap<String, String> attachments) {
        String resolvedAnswer = answerText;

        // See if the element is referencing an attachment
        if (attachments != null) {
            String attachmentPath = attachments.get(resolvedAnswer);
            if (attachmentPath != null) {
                resolvedAnswer = attachmentPath;
            }
        }
        return resolvedAnswer;
     }
}