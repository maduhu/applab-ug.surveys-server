package applab.surveys;

import java.util.HashMap;

/**
 * used for the case of multiple-response groups, this does not have any user-provided text, but has child
 * responses.
 */
public class SurveyAnswerGroup extends SurveyItemResponse {
    /**
     * used for the case of multiple-response groups, this does not have any user-provided text, but has child
     * responses.
     */
    HashMap<String, SurveyItemResponse> childResponses;

    public SurveyAnswerGroup(String questionName, SurveyAnswerGroup parent) {
        super(questionName, parent);
        this.childResponses = new HashMap<String, SurveyItemResponse>();
    } 

    public void addChild(SurveyItemResponse child) {
        this.childResponses.put(child.getQuestionName(), child);
    } 

    /**
     * The encoding for a submission group is a string like: 3 responses (q8, q9, q10)
     */
    @Override
    public String getEncodedAnswer(HashMap<String, String> attachments) {
        StringBuilder encodedAnswer = new StringBuilder();
        encodedAnswer.append(childResponses.size());
        encodedAnswer.append(" response");
        if (childResponses.size() != 1) {
            encodedAnswer.append("s");
        }
        if (childResponses.size() > 0) {
            encodedAnswer.append(" (");
            boolean firstChild = true;
            for (String childQuestionName : this.childResponses.keySet()) {
                if (firstChild) {
                    firstChild = false;
                }
                else {
                    encodedAnswer.append(", ");
                }
                encodedAnswer.append(childQuestionName);
            }
            encodedAnswer.append(")");
        }
        return encodedAnswer.toString();
    }

    @Override
    public void addAnswerText(String answerText) {
        assert false : "We should never get answer text for a submission group";
    }
}
