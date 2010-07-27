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
import java.util.ArrayList;


public class formList extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.getWriter().write("<forms>");
            SalesforceProxy salesforceProxy = SalesforceProxy.login();

            // get the published survey
            ArrayList<String> pub_list = salesforceProxy.getPublishedSurveys();
            if (pub_list.size() > 0) {
                for (int i = 0; i < pub_list.size(); i++) {
                    if (DatabaseHelpers.zebraSurveyIdExists(pub_list.get(i))) {
                        String survey_name = DatabaseHelpers.getSurveyName(pub_list.get(i));
                        response.getWriter().write(
                                "<form url=\"http://" + ApplabConfiguration.getHostUrl() + "/getForm?surveyid=" + pub_list.get(i) + "\" >"
                                        + survey_name + "</form>");
                    }
                }
            }
            else {
                response.getWriter().write("<form>Don't Click</form>");
            }
            response.getWriter().write("</forms>");
            salesforceProxy.logout();
        }
        catch (Exception e) {
            e.printStackTrace();
            // this.logout();
        }
    }
}
