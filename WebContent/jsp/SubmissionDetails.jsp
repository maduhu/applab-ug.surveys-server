<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page session="true" import="java.util.*, applab.surveys.*, applab.server.*"%>
<% Submission submission = (Submission) session.getAttribute("survey.detailedSubmission");
   String baseUrl = (String)session.getAttribute("survey.baseUrl");
   String changeStatusUrl = baseUrl + "updateSubmissionStatus";
   String surveySubmissionUrl = baseUrl + "getSubmissions";
   String surveySalesforceId  = (String) session.getAttribute("survey.surveySalesforceId");
   String surveyId  = (String)session.getAttribute("survey.surveyId");
   String startDate = (String)session.getAttribute("survey.startDate");
   String endDate   = (String)session.getAttribute("survey.endDate");
   String status    = (String)session.getAttribute("survey.status");
   String showDraft = (String)session.getAttribute("survey.showDraft");
%>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
		<link rel="stylesheet" type="text/css" href="css/SubmissionReports.css" />
	</head>
	<body>
	<div class="row">
		<form action="<%=surveySubmissionUrl %>" method="post">
			<input type="hidden" name="surveyId" value="<%=surveySalesforceId %>"/>
			<input type="hidden" name="startDate" value="<%=startDate %>"/>
			<input type="hidden" name="endDate" value="<%=endDate %>"/>
			<input type="hidden" name="status" value="<%=status %>"/>
			<input type="hidden" name="showDraft" value="<%=showDraft %>"/>
			<input type="submit" name="submit" value="Go Back To Submissions"/>		
		</form>
	</div>
	<div class="row">
		<table class="detailed">
			<tr>
				<td>Submission ID</td>
				<td><%= submission.getId() %></td>
			</tr>
			<tr>
				<td>Survey ID</td>
				<td><%= surveyId %></td>
			</tr>
			<tr>
				<td>Server Entry Time</td>
				<td><%= submission.getServerSubmissionTime() %></td>
			</tr>
			<tr>
				<td>Handset Submission Time</td>
				<td><%= submission.getHandsetSubmissionTime() %></td>
			</tr>
			<tr>
				<td>CKW Name</td>
				<td><%= submission.getInterviewerName()%></td>
			</tr>
			<tr>
				<td>CKW ID</td>
				<td><%= submission.getInterviewerId() %></td>
			</tr>
			<tr>
				<td>Phone Number</td>
				<td><%= submission.getPhoneNumber() %></td>
			</tr>
			<tr>
				<td>Location</td>
				<td><%= submission.getLocation() %></td>
			</tr>	
			<%
				Survey survey = submission.getSurvey();
				for (String questionName : submission.getAnswerOrder()) { 
			%>
			<tr>
				<%
					Answer answer = submission.getAnswer(questionName);
					if (answer == null) {
				%>
					<td>Question name missing</td>
					<td>Answer text is missing</td>
				<%		 
					}
					else {
				%>
					<td>
						<%
						    String displayName = survey.getBackEndSurveyXml().getXlation("en", answer.getParentQuestion());
							if (answer.getInstance() > 0) {
							    int showInstance = answer.getInstance() + 1;
							    displayName = displayName + ", " + showInstance;
							}
						%>
						<%= displayName %>
					</td>
					<td>
						<%=answer.getFriendlyAnswerText(false, survey) %>
					</td>
				<%		    
					}
				%>
			</tr>
			<% } %>
		</table>
	</div>	
	<div class="row">
		<form id="statusChange" action="<%=changeStatusUrl %>" method="post">
			<table class="detailed">
				<tr>
					<td>Customer Care Review Status</td>
					<td>
						<select name="ccStatus">
							<%for (CustomerCareStatus surveyStatus : CustomerCareStatus.values()) { %>
									<option value="<%= surveyStatus.getHtmlParameterValue() %>"
										<% if (submission.getCustomerCareStatus().getHtmlParameterValue() == surveyStatus.getHtmlParameterValue()) { %>
											selected="true"
										<% } %>
								>
									<%= surveyStatus.getDisplayName() %>
								</option>
							<% } %>
						</select>				
					</td>
					<td>Data Team Review Status</td>
					<td>
						<select name="dtStatus">
							<%for (SubmissionStatus surveyStatus : SubmissionStatus.values()) { %>
								<option value="<%= surveyStatus.getHtmlParameterValue() %>"  
									<% if (submission.getStatus() == surveyStatus) { %>
										selected="true"
									<% } %>
								>
									<%= surveyStatus.getDisplayName() %>
								</option>
							<% } %>
						</select>				
					</td>
				</tr>
				<tr>
					
					<td>Existing Customer Care Team Review Comments</td>
					<td>
						<textarea name="ccReview" readonly="readonly"><%= submission.getCustomerCareReview() %></textarea>
						
					</td>
					<td>Existing Data Team Review Comments</td>
					<td>
						<textarea name="dtReview" readonly="readonly"><%= submission.getDataTeamReview() %></textarea>
					</td>
				</tr>
				<tr>
					
					<td>Add More Customer Care Team Review Comments</td>
					<td>
						<textarea name="addCcReview" ></textarea>
						
					</td>
					<td>Add More Data Team Review Comments</td>
					<td>
						<textarea name="addDtReview" ></textarea>
					</td>
				</tr>
				<tr>
					<td colspan="4" >
						<input type="submit" value="Submit"/>
					</td>
				</tr>
			</table>
			<input type="hidden" name="submissionId" value="<%=submission.getId() %>"/>
			<input type="hidden" name="surveyId" value="<%=surveySalesforceId %>"/>
			<input type="hidden" name="startDate" value="<%=startDate %>"/>
			<input type="hidden" name="endDate" value="<%=endDate %>"/>
			<input type="hidden" name="status" value="<%=status %>"/>
		</form>
	</div>
	<div class="row">
		<form action="<%=surveySubmissionUrl %>" method="post">
			<input type="hidden" name="surveyId" value="<%=surveySalesforceId %>"/>
			<input type="hidden" name="startDate" value="<%=startDate %>"/>
			<input type="hidden" name="endDate" value="<%=endDate %>"/>
			<input type="hidden" name="status" value="<%=status %>"/>
			<input type="submit" name="submit" value="Go Back To Submissions"/>		
		</form>
	</div>
	</body>
</html>