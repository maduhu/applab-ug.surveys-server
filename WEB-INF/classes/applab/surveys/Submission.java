package applab.surveys;

import java.util.*;

public class Submission {
    private ArrayList<Answer> answers;
    private int id;
    private String interviewerId;
    private String interviewerName;
    private SubmissionStatus status;
    private Survey parentSurvey;

    public Submission(Survey parentSurvey) {
        if (parentSurvey == null) {
            throw new IllegalArgumentException("A non-null survey reference is required to create a Submission");
        }
        this.parentSurvey = parentSurvey;
        this.answers = new ArrayList<Answer>();
        this.status = SubmissionStatus.NotReviewed;
    }

    /**
     * return the survey associated with this submission
     */
    public Survey getSurvey() {
        return this.parentSurvey;        
    }

    public Collection<Answer> getAnswers() {
        return this.answers;
    }
    
    public void addAnswer(Answer answer) {
        if (answer == null) {
            throw new IllegalArgumentException("answer cannot be null");
        }
        this.answers.add(answer);
    }
    
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInterviewerId() {
        return this.interviewerId;
    }

    public void setInterviewerId(String interviewerId) {
        this.interviewerId = interviewerId;
    }

    public String getInterviewerName() {
        return this.interviewerName;
    }

    public void setInterviewerName(String interviewerName) {
        this.interviewerName = interviewerName;
    }

    public SubmissionStatus getStatus() {
        return this.status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }
}
