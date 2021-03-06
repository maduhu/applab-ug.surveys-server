package applab.surveys;

import java.util.*;

import applab.server.MathsHelper;

public class Submission {
	private HashMap<String, Answer> answers;
	private ArrayList<String> answerOrder;
	private int id;
	private int surveyId;
	private String interviewerId;
	private String interviewerName;
	private String serverSubmissionTime;
	private String handsetSubmissionTime;
	private String phoneNumber;
	private String location;

	private SubmissionStatus status;
	private CustomerCareStatus customerCareStatus;
	private Survey parentSurvey;
	private String customerCareReview;
	private String dataTeamReview;
	private String submissionLocation;
	private double interviewerDistance;
	
	// This constructor is used when loading a single submission details
	public Submission() {
		this.answers = new HashMap<String, Answer>();
		this.answerOrder = new ArrayList<String>();
		this.interviewerDistance = -1;
	}

	// This constructor is required when generating many submissions as they
	// require linkage to a survey
	public Submission(Survey parentSurvey) {
		if (parentSurvey == null) {
			throw new IllegalArgumentException(
					"A non-null survey reference is required to create a Submission");
		}
		this.parentSurvey = parentSurvey;
		this.answers = new HashMap<String, Answer>();
		this.status = SubmissionStatus.NotReviewed;
		this.answerOrder = new ArrayList<String>();
		this.interviewerDistance = -1;
	}

	/**
	 * return the survey associated with this submission
	 */
	public Survey getSurvey() {
		return this.parentSurvey;
	}

	public ArrayList<String> getAnswerOrder() {
		if (this.answerOrder.size() > 0) {
			return this.answerOrder;
		}
		return new ArrayList<String>();
	}

	public HashMap<String, Answer> getAnswers() {
		return this.answers;
	}

	public Answer getAnswer(String questionName) {
		return this.answers.get(questionName);
	}

	public void addAnswer(String name, Answer answer) {
		if (answer == null) {
			throw new IllegalArgumentException("answer cannot be null");
		}
		this.answers.put(name, answer);
		this.answerOrder.add(name);
	}

	public String getServerSubmissionTime() {
		if (this.serverSubmissionTime != null) {
			return this.serverSubmissionTime;
		}
		return "";
	}

	public void setServerSubmissionTime(Date serverSubmissionTime) {
		this.serverSubmissionTime = serverSubmissionTime.toString();
	}

	public String getHandsetSubmissionTime() {
		if (this.handsetSubmissionTime != null) {
			return this.handsetSubmissionTime;
		}
		return "";
	}

	public void setHandsetSubmissionTime(Date handsetSubmissionTime) {
		this.handsetSubmissionTime = handsetSubmissionTime.toString();
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getSurveyId() {
		return this.surveyId;
	}

	public void setSurveyId(int id) {
		this.surveyId = id;
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

	public String getPhoneNumber() {
		return this.phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getLocation() {
		return this.location == null ? "" : this.location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public SubmissionStatus getStatus() {
		return this.status;
	}

	public void setStatus(SubmissionStatus status) {
		this.status = status;
	}

	public CustomerCareStatus getCustomerCareStatus() {
		return this.customerCareStatus;
	}

	public void setCustomerCareStatus(CustomerCareStatus status) {
		this.customerCareStatus = status;
	}

	public void setCustomerCareReview(String customerCareReview) {
		this.customerCareReview = customerCareReview;
	}

	public String getCustomerCareReview() {
		if (this.customerCareReview == null) {
			return "";
		}
		else {
			return this.customerCareReview;
		}
	}

	public void setDataTeamReview(String dataTeamReview) {
		this.dataTeamReview = dataTeamReview;
	}

	public String getDataTeamReview() {
		if (this.dataTeamReview == null) {
			return "";
		}
		else {
			return this.dataTeamReview;
		}
	}

	public void setSubmissionLocation(String submissionLocation) {
		this.submissionLocation = submissionLocation;
	}

	public String getSubmissionLocation() {
		if (this.submissionLocation == null) {
			return "";
		}
		else {
			return this.submissionLocation;
		}
	}
	
	public double getInterviewerDistance() {
	    try{
	    if(this.interviewerDistance == -1) {
	        if(!getSubmissionLocation().isEmpty() && !getLocation().isEmpty()) {
	            double[] parsedLocation = parseLocation(getLocation());
	            double[] parsedSubmissionLocation = parseLocation(getSubmissionLocation());	            
	            this.interviewerDistance = MathsHelper.calcDistance(parsedLocation[0], parsedLocation[1], parsedSubmissionLocation[0], parsedSubmissionLocation[1]);
	        }
	    }	   
	    //TODO: Change this to a more appropriate option other than the default -1 to show previously not saved values
	    return this.interviewerDistance;
	    }
	    catch(Exception ex) {
	        ex.printStackTrace();	        
	        return 0;
	    }
	}
	
	/**
	 * Splits submission location and interviewer location that are stored in a whitespace delimited string
	 */
	private double[] parseLocation(String location)
	        throws NumberFormatException {
	   String[] locationFragments = location.split(" ");
	   double[] parsedlocation = new double[4];
	  
	   for(int index = 0; index < 4; index++) {
	       parsedlocation[index] =  Double.parseDouble(locationFragments[index]);
	   }
       return parsedlocation;
	}
}
