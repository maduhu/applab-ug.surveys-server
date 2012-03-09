package applab.surveys;

/**
 * in memory representation of the answer to a question in a survey submission.
 * Usually this just contains a text answer, but in the case of repeat
 * questions, it can contain a collection of responses as the answer.
 * 
 * Single answer example:
 * 
 * <q1>My answer</q1>
 * 
 * Repeat answer example:
 * 
 * <q3> <q4>Answer 4</q4><q5>Answer 5</q5> </q3> <q3> <q4>Answer 4
 * #2</q4><q5>Answer 5 #2</q5> </q3>
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
	public abstract String getFriendlyAnswerText(boolean isCsv, Survey survey);

	// create an answer based on the raw answer text and question
	public static Answer create(Question question, String rawAnswerText,
			int instance) {
		if (question == null) {

			/**
			 * Removed Section : CKW-1727 Some survey submissions fail when you
			 * try to view details and export to CSV throw new
			 * IllegalArgumentException("question must be non-null");
			 */
			return null;
		} else {
			if (rawAnswerText == null) {
				rawAnswerText = "";
			}

			switch (question.getType()) {
			case Input:
			case Repeat:
				return new InputAnswer(question, rawAnswerText, instance);
			case Select:
			case Select1:
				return new SelectAnswer(question, rawAnswerText, instance);
			}
			throw new IllegalArgumentException("Unsupported QuestionType: "
					+ question.getType());
		}
	}

	/**
	 * Represents an answer to an <input> question, which is used for free-entry
	 * questions, such as text, numbers, and dates
	 * 
	 */
	private static class InputAnswer extends Answer {
		public InputAnswer(Question question, String rawAnswerText, int instance) {
			super(question, rawAnswerText, instance);
		}

		// for input questions, the raw answer text is the same as the friendly
		// version
		@Override
		public String getFriendlyAnswerText(boolean isCsv, Survey survey) {
			return this.getRawAnswerText();
		}
	}

	// response to a multiple-choice answer, may contain multiple responses.
	// rawAnswerText contains indices to the
	// question choices
	private static class SelectAnswer extends Answer {
		private String friendlyAnswerText;

		public SelectAnswer(Question question, String rawAnswerText,
				int instance) {
			super(question, rawAnswerText, instance);
		}

		@Override
		// isCsv is because the customer care team and the data collection team
		// want the display
		// slightly different.
		public String getFriendlyAnswerText(boolean isCsv, Survey survey) {
			if (this.friendlyAnswerText == null) {

				// The rawAnswerText will contain a space-delimited list of
				// numbers (e.g. "1 3")
				StringBuilder parsedAnswerText = new StringBuilder();
				int startIndex = 0;
				String rawAnswer = getRawAnswerText();
				while (startIndex < rawAnswer.length()) {
					int endIndex = rawAnswer.indexOf(' ', startIndex);
					if (endIndex == -1) {
						endIndex = rawAnswer.length();
					}
					String choiceIndex = rawAnswer.substring(startIndex,
							endIndex);
					parsedAnswerText.append(" ");
					if (isCsv) {
						parsedAnswerText.append(choiceIndex);
					} else {

						// May need to xlate the answer
						String answer = survey.getBackEndSurveyXml()
								.getXlation(
										"en",
										this.getParentQuestion().getChoice(
												choiceIndex));
						parsedAnswerText.append(answer);
					}
					parsedAnswerText.append(" ");
					startIndex = endIndex + 1;
				}
				this.friendlyAnswerText = parsedAnswerText.toString();
			}

			return this.friendlyAnswerText;
		}
	}
}
