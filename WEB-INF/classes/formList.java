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

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.ArrayList;

import javax.xml.rpc.ServiceException;
import com.sforce.soap.enterprise.*;
import com.sforce.soap.enterprise.sobject.*;

import configuration.applabConfig;

public class formList extends HttpServlet {

    private SoapBindingStub binding;
    applabConfig applab = new applabConfig();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.getWriter().write("<forms>");
            configuration.sfConnect.login();
            // get the published survey
            System.out.println("KK");
            ArrayList<String> pub_list = configuration.sfConnect.getPublishedSurveys();
            // configuration.sfConnect.logout();
            if (pub_list.size() > 0) {
                for (int i = 0; i < pub_list.size(); i++) {
                    if (configuration.DbConnect.zebraSurveyIdExists(pub_list.get(i))) {
                        String survey_name = configuration.DbConnect.getSurveyName(pub_list.get(i));
                        response.getWriter().write(
                                "<form url=\"http://" + applab.getZebraUrl() + "/surveyList?surveyid=" + pub_list.get(i) + "\" >"
                                        + survey_name + "</form>");
                    }
                }
            }
            else {
                response.getWriter().write("<form>Don't Click</form>");
            }
            response.getWriter().write("</forms>");
        }
        catch (Exception e) {
            e.printStackTrace();
            // this.logout();
        }
    }
}
