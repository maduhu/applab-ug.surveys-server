package applab.surveys;

/**
 * in memory representation of the answer to a question in a survey submission. 
 * Usually this just contains a text answer, but in the case of repeat questions, 
 * it can contain a collection of responses as the answer.
 * 
 * Single answer example:
 * 
 * <q1>My answer</q1>
 * 
 * Repeat answer example:
 * 
 * <q3> <q4>Answer 4</q4><q5>Answer 5</q5> </q3> <q3> <q4>Answer 4 #2</q4><q5>Answer 5 #2</q5> </q3>
 */
public abstract class Answer {
    private Question question;
    private String rawAnswerText;
    private int instance;

    Answer(Question question, String rawAnswerText, int instance) {
        assert (question != null) : "internal callers must ensure question is non-null";
        assert (rawAnswerText != null) : "internal callers must ensure question is non-null";
        this.question = question;
        this.rawAnswerText = rawAnswerText;
        this.instance = instance;
    }

    public Question getParentQuestion() {
        return this.question;
    }

    public String getRawAnswerText() {
        return this.rawAnswerText;
    }
    
    public int getInstance() {
        return this.instance;
    }

    // resolves the raw answer into a friendly name that can be used for review
    public abstract String getFriendlyAnswerText();

    // create an answer based on the raw answer text and question
    public static Answer create(Question question, String rawAnswerText, int instance) {
        if (question == null) {
            throw new IllegalArgumentException("question must be non-null");
        }
        
        if (rawAnswerText == null) {
            rawAnswerText = "";
        }

        switch (question.getType()) {
            case Input:
                return new InputAnswer(question, rawAnswerText, instance);
            case Select:
            case Select1:
                return new SelectAnswer(question, rawAnswerText, instance);
        }
        throw new IllegalArgumentException("Unsupported QuestionType: " + question.getType());
    }

    /**
     * Represents an answer to an <input> question, which is used for free-entry questions, such as text, numbers, and
     * dates
     * 
     */
    private static class InputAnswer extends Answer {
        public InputAnswer(Question question, String rawAnswerText, int instance) {
            super(question, rawAnswerText, instance);
        }

        // for input questions, the raw answer text is the same as the friendly version
        @Override
        public String getFriendlyAnswerText() {
            return this.getRawAnswerText();
        }
    }

    // response to a multiple-choice answer, may contain multiple responses. rawAnswerText contains indices to the
    // question choices
    private static class SelectAnswer extends Answer {
        private String friendlyAnswerText;

        public SelectAnswer(Question question, String rawAnswerText, int instance) {
            super(question, rawAnswerText, instance);
        }

        @Override
        public String getFriendlyAnswerText() {
            if (this.friendlyAnswerText == null) {
                // the rawAnswerText will contain a space-delimited list of numbers (e.g. "1 3")
                StringBuilder parsedAnswerText = new StringBuilder();
                int startIndex = 0;
                String rawAnswer = getRawAnswerText();
                while (startIndex < rawAnswer.length()) {
                    int endIndex = rawAnswer.indexOf(' ', startIndex);
                    if (endIndex == -1) {
                        endIndex = rawAnswer.length();
                    }
                    String choiceIndex = rawAnswer.substring(startIndex, endIndex);
                    if (startIndex > 0) {
                        parsedAnswerText.append(" ");
                    }
                    parsedAnswerText.append(this.getParentQuestion().getChoice(choiceIndex));
                    startIndex = endIndex + 1;
                }
                this.friendlyAnswerText = parsedAnswerText.toString();
            }

            return this.friendlyAnswerText;
        }
    }
}
