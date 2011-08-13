/*
 * Copyright (C) 2010 Grameen Foundation
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package applab.surveys.server;

import java.io.BufferedReader;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import applab.server.ApplabConfiguration;
import applab.server.ApplabServlet;
import applab.server.DatabaseHelpers;
import applab.server.ServletRequestContext;
import applab.server.WebAppId;
import applab.surveys.Survey;

import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.enterprise.SessionHeader;
import com.sforce.soap.enterprise.SforceServiceLocator;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.schemas._class.SaveSurveyForm.SaveSurveyFormBindingStub;
import com.sforce.soap.schemas._class.SaveSurveyForm.SaveSurveyFormServiceLocator;
import com.sforce.soap.schemas._class.SaveSurveyForm.SaveSurveyXML;

/**
 * Called from our survey designer to save an x-form (http://www.w3.org/TR/xforms/) into
 * our backend database.
 * 
 * We store the raw XML as a blob in the zebrasurvey table, and shred the questions 
 * into rows on the zebrasurveyquestions table.
 * 
 * XForms notes:
 * Namespace = http://www.w3.org/2002/xforms
 *
 */
public class SaveDesignerForm extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    public void doApplabPost(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {
        String survey_id = request.getParameter("surveyId");

        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        reader.close();
        log(sb.toString());
        String xform_data1 = sb.toString();
        String xform_data = xform_data1.replaceAll("\'", "\\'");

        log(xform_data);

        // Load the survey from salesforce
        Survey survey = new Survey(survey_id);
        survey.loadSurvey(survey_id, false);

        SaveSurveyFormServiceLocator serviceLocator = new SaveSurveyFormServiceLocator();
        SaveSurveyFormBindingStub serviceStub = (SaveSurveyFormBindingStub)serviceLocator.getSaveSurveyForm();

        // Use soap api to login and get session info
        SforceServiceLocator soapServiceLocator = new SforceServiceLocator();
        soapServiceLocator.setSoapEndpointAddress((String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceAddress", ""));
        SoapBindingStub binding = (SoapBindingStub)soapServiceLocator.getSoap();
        LoginResult loginResult = binding.login((String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceUsername", ""),
                (String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforcePassword", "")
                        + (String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceToken", ""));
        SessionHeader sessionHeader = new SessionHeader(loginResult.getSessionId());

        // Share the session info with our webservice
        serviceStub.setHeader("http://soap.sforce.com/schemas/class/SaveSurveyForm", "SessionHeader", sessionHeader);

        SaveSurveyXML saveSurveyXML = new SaveSurveyXML();

        // If the survey does not exist in the backend then save it as a new survey
        if (!survey.existsInDb()) {
            if (!SurveyDatabaseHelpers.saveXform(survey_id, xform_data, survey.getName(), DatabaseHelpers.formatDateTime(new Date()))) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save to the database");
            }
            saveSurveyXML.setIsNew(true);
        }
        else {

            // All the checks to stop the wrong status being saved are in the form designer so just save the form
            if (!SurveyDatabaseHelpers.saveXform(survey_id, survey.getName(), xform_data)) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update to the database");
            }
            saveSurveyXML.setIsNew(false);
        }

        // Save the survey to salesforce if it is required. Do this for all surveys. The webservice will check if
        // the survey actually needs saving there. Stops two calls to salesforce being made by doing the work here
        saveSurveyXML.setSurveyName(survey_id);
        saveSurveyXML.setContents(xform_data);
        SaveSurveyXML resultSaveSurveyXML = serviceStub.saveSurveyXML(saveSurveyXML);
        if (!resultSaveSurveyXML.getSuccess()) {
            log("Failed to save form to Salesforce " + resultSaveSurveyXML.getErrorMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save form to Salesforce " + resultSaveSurveyXML.getErrorMessage());
        }
        log("Saved form to Salesforce " + resultSaveSurveyXML.getErrorMessage());
    }
}
