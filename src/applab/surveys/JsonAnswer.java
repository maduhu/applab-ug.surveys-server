package applab.surveys;

public class JsonAnswer {

    private String answer;
    private String binding;
    private String type;
    private String parentBinding;
    private String parentInstance;
    private String instance;
    private String questionNumber;
    private String questionText;

    public JsonAnswer(String answer, String binding, String type, String parentBinding, String instance, String questionNumber, String questionText, String parentInstance) {
        this.answer = answer;
        this.binding = binding;
        this.type = type;
        this.parentBinding = parentBinding;
        this.instance = instance;
        this.questionNumber = questionNumber;
        this.questionText = questionText;
        this.parentInstance = parentInstance;
    }
}
