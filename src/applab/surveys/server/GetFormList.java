package applab.surveys.server;

/*

 Copyright (C) 2010 Grameen Foundation
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy of
 the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 License for the specific language governing permissions and limitations under
 the License.
 */

import javax.servlet.http.*;

import applab.server.*;
import applab.surveys.Survey;

import java.io.PrintWriter;
import java.util.ArrayList;

public class GetFormList extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    protected void doApplabGet(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {
        writeFormsList(response.getWriter(), context.getHandsetId(), ApplabConfiguration.getHostUrl()/*context.getUrl()*/);
        context.close();
    }

    public static void writeFormsList(PrintWriter writer, String imei, String url) throws Exception {
        SurveysSalesforceProxy salesforceProxy = new SurveysSalesforceProxy();
        try {
            ArrayList<Survey> publishedSurveys = salesforceProxy.getPublishedSurveys(imei);
            if (publishedSurveys.size() > 0) {
                writer.write("<forms>");
                for (Survey survey : publishedSurveys) {
                    writer.write("<form url=\"" + url + "getForm?surveyid=" + survey.getSalesforceId()
                            + "\" >" + survey.getName() + "</form>");
                }
                writer.write("</forms>");
            }
            else {
                writer.write("No Surveys available");
            }
        }
        finally {
            salesforceProxy.dispose();
        }
    }
}
