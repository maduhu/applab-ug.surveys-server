package applab.surveys;

import java.util.HashMap;

public class SubmissionAnswer {

    private Question question;
    private String questionName;
    private String answerText;

    // What instance of the question is this i.e. how many times have we seen this answer before.
    private int instance;

    // The parent of this answer. Can be null if no parent.
    private SubmissionAnswer parent;
    private Boolean isValid;

    public SubmissionAnswer(Question question, String questionName, int instance, String answerText, SubmissionAnswer parent) {
        this.question = question;
        this.questionName = questionName;
        this.instance = instance;
        this.answerText = answerText;
        this.parent = parent;
        this.isValid = true;
    }

    // Accessor methods
    public String getQuestionName() {
        if (this.question == null) {
            return this.questionName;
        }
        return this.question.getBinding();
    }

    public Question getQuestion() {
        return this.question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }
    /**
     * Return the answer text for this answer. Will return the attachment reference if needed.
     * 
     * @param attachments
     *            - HashMap containg the attachment details
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

    public Boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
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
        if (this.question == null) {
            return createAnswerKey(this.questionName, this.instance);
        }
        return createAnswerKey(this.question.getBinding(), this.instance);
    }
}