package applab.surveys;

/**
 * Maps to the different xforms type values
 *
 */
public enum QuestionType {
    /**
     * Used for free-entry questions, such as text, numbers, and dates
     */
    Input,
    
    /**
     * Used for multiple-choice questions, and allow for multiple selections in the response
     */
    Select,
    
    /**
     * Same as Select, but limited to one response
     */
    Select1,
    
    /**
     * Indicates that this is a repeat Node
     */
    Repeat
}
