<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page session="true" import="java.util.*, applab.surveys.*, applab.server.*"%>
<% SubmissionStatistics stats = (SubmissionStatistics) session.getAttribute("survey.statistics");
   String moreDetailsUrl = ApplabConfiguration.getHostUrl() + "getDetailedSubmission";
   String filterSubmissionsUrl = ApplabConfiguration.getHostUrl() + "getSubmissions";
   String exportToCsvUrl = ApplabConfiguration.getHostUrl() + "downloadFile";
   String startDate = (String) session.getAttribute("survey.startDate");
   String endDate   = (String) session.getAttribute("survey.endDate");
   String status       = (String) session.getAttribute("survey.status");
   String salesforceId = (String) session.getAttribute("survey.salesforceId");
   Boolean showDraft  =  (Boolean) session.getAttribute("survey.showDraft");
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link rel="stylesheet" type="text/css" href="css/SubmissionReports.css" />
<script type='text/JavaScript' src='js/dateWidget.js'></script>
<script type='text/JavaScript' src='js/utils.js'></script>
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
					CKW:
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
					<img alt="" src="images/DateSelect.gif" onclick='changeTimeString(0);scwShow(refineSubmissionStartDate, event);return false;'>
				</div>
				<div>
					Date To:
					<input class="dateInput" id="refineSubmissionEndDate" type="text" name="endDate" value="<%= endDate %>">
					<img class="datePic" alt="" src="images/DateSelect.gif" onclick='changeTimeString(1);scwShow(refineSubmissionEndDate, event);return false;'>
					<button onclick="clearField('refineSubmissionStartDate');clearField('refineSubmissionEndDate');return false;">Clear Dates</button>
				</div>
				<div>
					Show Drafts:
					<input class="" type="checkbox" name="showDraft"
					<% if (showDraft) {%>
						checked="true"
					<% } %>
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
			<th>Server Entry Time</th>
			<th>Handset Submission Time</th>
			<th>CKW ID</th>
			<th>CKW Name</th>
			<th>Customer Care Review</th>
			<th>Data Team Review</th>
			<th>More Details</th>
		</tr>
		<% if (stats != null ) {%>
			<% for (Integer submissionId : stats.getSurvey().getSubmissionOrder()) {
				Submission submission = stats.getSurvey().getSubmissions().get(submissionId);
			%>
			<tr>
				<td>
					<%= submission.getSurvey().getPrimaryKey() %>
				</td>
				<td>
					<%= submission.getId() %>
				</td>
				<td>
					<%= submission.getServerSubmissionTime() %>
				</td>
				<td>
					<%= submission.getHandsetSubmissionTime() %>
				</td>
				<td>
					<%= submission.getInterviewerId() %>
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
					<form action="<%= moreDetailsUrl %>" method="post">
						<input type="hidden" name="submissionId" value="<%=submission.getId()%>"/>
						<input type="hidden" name="surveyId" value="<%=submission.getSurvey().getPrimaryKey()%>"/>
						<input type="hidden" name="surveySalesforceId"  value="<%= salesforceId %>"/>
						<input type="hidden" name="startDate" value="<%= startDate %>"/>
						<input type="hidden" name="endDate"   value="<%= endDate %>"/>
						<input type="hidden" name="status"    value="<%= status %>"/>
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