<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page session="true" import="java.util.*, applab.surveys.*, applab.server.*"%>
<% SubmissionStatistics stats = (SubmissionStatistics) session.getAttribute("survey.statistics");
   String baseUrl = (String)session.getAttribute("survey.baseUrl");
   String moreDetailsUrl = baseUrl + "getDetailedSubmission";
   String filterSubmissionsUrl = baseUrl + "getSubmissions";
   String exportToCsvUrl = baseUrl + "downloadFile";
   String startDate = (String) session.getAttribute("survey.startDate");
   String endDate   = (String) session.getAttribute("survey.endDate");
   String status       = (String) session.getAttribute("survey.status");
   String salesforceId = (String) session.getAttribute("survey.salesforceId");  
   Boolean showDraft  =  (Boolean) session.getAttribute("survey.showDraft"); 
   String approvalFailureResponse = (String) session.getAttribute("survey.failureMessage");
   String baseSearchUrl = "https://na5.salesforce.com/_ui/common/search/client/ui/UnifiedSearchResults?str=";
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<link rel="stylesheet" type="text/css" href="css/SubmissionReports.css" />
<link rel="stylesheet" type="text/css" href="css/jquery-ui-1.8.16.custom.css" />

<script type='text/JavaScript' src='js/utils.js'></script>
<script type='text/JavaScript' src='js/jquery-1.6.2.min.js'></script>
<script type='text/JavaScript' src='js/jquery-ui-1.8.16.custom.min.js'></script>
<script type='text/JavaScript' src='js/datepicker.js'></script>
<script type="text/javascript">
	var j$ = jQuery.noConflict();
		j$(document).ready(function() {
			 j$('#refineSubmissionStartDate').datetimepicker({  
			      duration: '',  
			      showTime: true, 
			      showSecond: true,
			      constrainInput: false,  
			      stepMinutes: 1,  
		 	      stepHours: 1,  
		 	      altTimeField: '',  
		 	      time24h: false,
		 	      timeFormat: 'hh:mm:ss',
		  	      dateFormat: 'yy-m-d'
		     });
			 j$('#refineSubmissionEndDate').datetimepicker({  
			      duration: '',  
			      showTime: true, 
			      showSecond: true,
			      constrainInput: false,  
			      stepMinutes: 1,  
		 	      stepHours: 1,  
		 	      altTimeField: '',  
		 	      time24h: false,
		 	      timeFormat: 'hh:mm:ss',
		  	      dateFormat: 'yy-m-d'
		     });
	});
</script>
<script type="text/javascript">
    var response = '<%=approvalFailureResponse%>';
    if(response != null && response != 'null' && response != undefined && response != ''){
        alert("APPROVAL FAILED: "+response);
    }
</script>
</head>
<body>
	<div class="row">
		<div>Filter By:</div>
		<div>
			<form id="filterSubmissions" method="post" action="<%= filterSubmissionsUrl %>">
				<input type="hidden" name="surveyId"  value="<%= salesforceId %>"/>
				<div>
					Survey:
				</div>
				<div>
					Person/CKW:
				</div>
				<div>
					Status:
					<select name="status">
						<option value="null">All Statuses</option>
						<%for (SubmissionStatus surveyStatus : SubmissionStatus.values()) { %>
							<option value="<%= surveyStatus.getHtmlParameterValue() %>"
								<% if (status == surveyStatus.getHtmlParameterValue()) { %>
									selected="true"
								<%	 }
							%>
							>
								<%= surveyStatus.getDisplayName() %>
							</option>
						<% } %>
					</select>				
				</div>
				<div>
					Date From:
					<input class="dateInput" id="refineSubmissionStartDate" type="text" name="startDate" value="<%= startDate %>">
				</div>
				<div>
					Date To:
					<input class="dateInput" id="refineSubmissionEndDate" type="text" name="endDate" value="<%= endDate %>">
					<button onclick="clearField('refineSubmissionStartDate');clearField('refineSubmissionEndDate');return false;">Clear Dates</button>
				</div>
				<div>
					Show Drafts:
					<input class="" type="checkbox" name="showDraft"
					/>
				</div>
				<input type="submit" name="Submit"    value="Refine Search"/>
			</form>
			<div id="exportToCsv">
				<% if (stats != null) {%>
					<form action="<%= exportToCsvUrl %>" method="post">
						<input type="submit" name="Submit"    value="Export as CSV"/>
						<input type="hidden" name="downloadType" value="Csv"/>
						<input type="hidden" name="downloadTarget" value="SubmissionCsv"/>
						<input type="hidden" name="surveyId"  value="<%= stats.getSurvey().getPrimaryKey() %>"/>
						<input type="hidden" name="surveySalesforceId"  value="<%= salesforceId %>"/>
						<input type="hidden" name="startDate" value="<%= startDate %>"/>
						<input type="hidden" name="endDate"   value="<%= endDate %>"/>
						<input type="hidden" name="status"    value="<%= status %>"/>				
						<input type="hidden" name="showDraft"    value="<%= showDraft %>"/>
					</form>
				<% } %>
			</div>
		</div>	
	</div>
	<div class="row">
	<% if (stats != null ) {%>
		<table>
			<tr>
				<th>Survey Status</th>
				<th>Number of Submissions</th>
			</tr>
			<%for (SubmissionStatus surveyStatus : SubmissionStatus.values()) { %>
				<tr>
					<td><%= surveyStatus.getDisplayName() %></td>
					<td> <%= stats.getNumberOfSubmissions(surveyStatus)  %></td>
				</tr>
			<% } %>
		</table>
	<% } %>
	</div>
	<div class="row">
	<table class="results">
		<tr>
			<th>Survey ID</th>
			<th>Submission ID</th>
			<th>Survey Start Time</th>
			<th>Survey End Time</th>
			<th>Server Entry Time</th>
			<th>ID</th>
			<th>Name</th>
			<th>Customer Care Review</th>
			<th>Data Team Review</th>
			<th>Submission Distance(kms)</th>
			<th>More Details</th>
		</tr>
		<% if (stats != null ) {%>
			<% for (Integer submissionId : stats.getSurvey().getSubmissionOrder()) {
				Submission submission = stats.getSurvey().getSubmissions(false).get(submissionId);
			%>
			<tr>
				<td>
					<%= submission.getSurvey().getPrimaryKey() %>
				</td>
				<td>
					<%= submission.getId() %>
				</td>
				<td>
					<%= submission.getSurveyStartTime() %>
				</td>
				<td>
					<%= submission.getHandsetSubmissionTime() %>
				</td>
				<td>
					<%= submission.getServerSubmissionTime() %>
				</td>
				<td>
				<a href= <%= baseSearchUrl + submission.getInterviewerId() %> > <%= submission.getInterviewerId() %></a>
				</td>
				<td>
					<%= submission.getInterviewerName() %>
				</td>
				<td>
					<%= submission.getCustomerCareStatus().getDisplayName() %>
				</td>
				<td>
					<%= submission.getStatus().toString() %>
				</td>
				<td>
				    <% if(submission.getInterviewerDistance() >= 0) {%>
				    	<%= submission.getInterviewerDistance() %>
				    <% }
				       else { %>
				        <%= "Unknown" %>
				    <% } %>
				</td>
				<td>
					<form action="<%= moreDetailsUrl %>" method="post">
						<input type="hidden" name="submissionId" value="<%=submission.getId()%>"/>
						<input type="hidden" name="surveyId" value="<%=submission.getSurvey().getPrimaryKey()%>"/>
						<input type="hidden" name="surveySalesforceId"  value="<%= salesforceId %>"/>
						<input type="hidden" name="startDate" value="<%= startDate %>"/>
						<input type="hidden" name="endDate"   value="<%= endDate %>"/>
						<input type="hidden" name="status"    value="<%= status %>"/>
						<input type="hidden" name="showDraft" value="<%= showDraft %>"/>
						<input type="submit" value="More Details"/>
					</form>
				</td>
			</tr>
			<% } 
		} 
		else {
			%>
			<tr>
				<td colspan="9">
					There are no Submissions for your search criteria. Please try again.
				</td>
			</tr>
		<% } %>
	</table>
	</div>
</body>
</html>