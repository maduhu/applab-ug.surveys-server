import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.enterprise.sobject.CKW__c;
import applab.server.ApplabServlet;
import applab.server.DatabaseHelpers;
import applab.server.DatabaseId;
import applab.server.SalesforceProxy;
import applab.server.ServletRequestContext;
import applab.server.SqlImplementation;

/**
 * This servelet is used to update both search and survey logs with the interviewee info, until we roll out the new code
 * New code for search logs in salesforce, so this file will no longer be necessary
 * New code for surveys will resolve this info on insert, so this file will no longer be necessary
 *
 */
public class GetInterviewerName extends ApplabServlet {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;

    public void doApplabGet(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {
        String command = "Select distinct handset_id from OktopusSearchLog where (interviewer_id = '' OR interviewer_id is null) and handset_id != ''";
        SqlImplementation.Instance implementation = SqlImplementation.getInstance();
        ResultSet resultSet = implementation.executeQuery(DatabaseId.Search, command);
        
        // concatenate
        if(resultSet.first()) {
            String handsetIdString = "'" + resultSet.getString("handset_id") + "'";
            while(resultSet.next()) {
                handsetIdString += ", '" + resultSet.getString("handset_id") + "'";
            }
            
            if((!handsetIdString.isEmpty()) && (handsetIdString.length() > 0)) {
                SoapBindingStub binding = SalesforceProxy.createBinding();
                QueryResult ckwQuery = binding.query("SELECT Name, Person__r.First_Name__c, Person__r.Last_Name__c, Person__r.Handset__r.IMEI__c from CKW__c where Person__r.Handset__r.IMEI__c IN ("+ handsetIdString +")");
                Connection connection = DatabaseHelpers.createConnection(DatabaseId.Search);
                Statement statement = connection.createStatement();
                for (int counter = 0; counter < ckwQuery.getSize(); counter++) {
                    CKW__c ckw = (CKW__c)ckwQuery.getRecords(counter);
                    command = "Update OktopusSearchLog set interviewer_id = '" + ckw.getName() + "', " +
                    		"interviewer_name = '" + ckw.getPerson__r().getFirst_Name__c() + " " + 
                    		ckw.getPerson__r().getLast_Name__c() + "' where handset_id = '" + 
                    		ckw.getPerson__r().getHandset__r().getIMEI__c() + "' and (interviewer_id = '' OR interviewer_id is null)";
                    statement.executeUpdate(command);
                }
            }
        }
        
        // Repeat for surveys
        command = "Select distinct handset_id from zebrasurveysubmissions where (interviewer_id = '' OR interviewer_id is null) and handset_id != ''";
        implementation = SqlImplementation.getInstance();
        resultSet = implementation.executeQuery(DatabaseId.Surveys, command);
        
        // concatenate
        if(resultSet.first()) {
            String handsetIdString = "'" + resultSet.getString("handset_id") + "'";
            while(resultSet.next()) {
                handsetIdString += ", '" + resultSet.getString("handset_id") + "'";
            }
            
            if((!handsetIdString.isEmpty()) && (handsetIdString.length() > 0)) {
                SoapBindingStub binding = SalesforceProxy.createBinding();
                QueryResult ckwQuery = binding.query("SELECT Name, Person__r.First_Name__c, Person__r.Last_Name__c, Person__r.Handset__r.IMEI__c from CKW__c where Person__r.Handset__r.IMEI__c IN ("+ handsetIdString +")");
                Connection connection = DatabaseHelpers.createConnection(DatabaseId.Surveys);
                Statement statement = connection.createStatement();
                for (int counter = 0; counter < ckwQuery.getSize(); counter++) {
                    CKW__c ckw = (CKW__c)ckwQuery.getRecords(counter);
                    command = "Update zebrasurveysubmissions set interviewer_id = '" + ckw.getName() + "', " +
                    		"interviewer_name = '" + ckw.getPerson__r().getFirst_Name__c() + " " + 
                    		ckw.getPerson__r().getLast_Name__c() + "' where handset_id = '" + 
                    		ckw.getPerson__r().getHandset__r().getIMEI__c() + "' and (interviewer_id = '' OR interviewer_id is null)";
                    statement.executeUpdate(command);
                }
            }
        }
    }
}
