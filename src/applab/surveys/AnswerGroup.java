package applab.surveys;

import java.util.HashMap;

/**
 * used for the case of multiple-response groups, this does not have any user-provided text, but has child
 * responses.
 * 
 * In the x-form response, the answers will appear like this
 * <q3> <q4>Answer 4</q4><q5>Answer 5</q5> </q3> 
 * <q3> <q4>Answer 4 #2</q4><q5>Answer 5 #2</q5> </q3>
 */
public class AnswerGroup extends Answer {
    // each group can only have one answer for each question that is contained within the answer group
    private HashMap<Question, Answer> childResponses;

    AnswerGroup(Question question, String rawAnswerText) {
        super(question, rawAnswerText);
        this.childResponses = new HashMap<Question, Answer>();
    }

    public void addChild(Answer child) {
        this.childResponses.put(child.getParentQuestion(), child);
    }

    /**
     * The encoding for a submission group is a string like: 3 responses (q8, q9, q10)
     */
    @Override
    public String getFriendlyAnswerText() {
        // TODO Auto-generated method stub
        return null;
    }

}
