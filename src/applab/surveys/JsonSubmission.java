package applab.surveys;

import java.util.ArrayList;

public class JsonSubmission {

    private ArrayList<JsonAnswer> answers;

    public JsonSubmission() {
        this.answers = new ArrayList<JsonAnswer>();
    }

    public void addAnswer(JsonAnswer jsonAnswer) {
        this.answers.add(jsonAnswer);
    }

    public ArrayList<JsonAnswer> getAnswers() {
        return this.answers;
    }
}
