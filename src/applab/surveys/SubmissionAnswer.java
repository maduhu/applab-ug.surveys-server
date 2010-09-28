package applab.surveys;

import java.util.HashMap;

public class SubmissionAnswer {
    
    private String questionName;
    private String answerText;
    private String answerKey;
    
    // What instance of the question is this i.e. how many times have we seen this answer before.
    private int instance;
    
    // The parent of this answer. Can be null if no parent.
    private SubmissionAnswer parent;
    
    public SubmissionAnswer (String questionName, int instance, String answerText, SubmissionAnswer parent) {
        this.questionName = questionName;
        this.instance = instance;
        this.answerText = answerText;
        this.parent = parent;
        this.answerKey = createAnswerKey(questionName, instance);
    }

    // Accessor methods
    public String getQuestionName() {
        return this.questionName;
    }
    
    public void setQuestionName(String questionName) {
        this.questionName = questionName;
    }

    /**
     * Return the answer text for this answer. Will return the attachment reference if needed.
     * 
     * @param attachments - HashMap containg the attachment details
     * 
     * @return - The answer text or attachment
     */
    public String getAnswerText(HashMap<String, String> attachments) {
        String resolvedAnswer = this.answerText;

        // See if the element is referencing an attachment
        if (attachments != null) {
            String attachmentPath = attachments.get(resolvedAnswer);
            if (attachmentPath != null) {
                resolvedAnswer = attachmentPath;
            }
        }
        return resolvedAnswer;
    }
    
    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
    
    public int getInstance() {
        return this.instance;
    }
    
    public void setInstance(int instance) {
        this.instance = instance;
    }
    
    public SubmissionAnswer getParent() {
        return this.parent;
    }
    
    public void setParent(SubmissionAnswer parent) {
        this.parent = parent;
    }
    
    public boolean hasParent() {
        if (this.parent == null) {
            return false;
        }
        return true;
    }

    /**
     * Create a unique key to be used in the hashmap that stores the answers. Format of the key is
     * questionName:AnswerInstance.
     * 
     * @param questionName
     *            - Name of the question
     * @param answerInstance
     *            - the instance of this answer
     * @return
     */
    public String createAnswerKey(String questionName, int answerInstance) {

        StringBuilder answerKey = new StringBuilder();
        answerKey.append(questionName);
        answerKey.append(":");
        answerKey.append(Integer.toString(answerInstance));
        return answerKey.toString();
    }
    
    public String getKey() {
        return this.answerKey;
    }
}