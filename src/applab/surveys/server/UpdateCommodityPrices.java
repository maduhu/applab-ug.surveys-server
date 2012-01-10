package applab.surveys.server;

import applab.server.ApplabServlet;
import applab.server.DatabaseHelpers;
import applab.server.DateHelpers;
import applab.server.ServletRequestContext;
import applab.surveys.Answer;
import applab.surveys.Commodity;
import applab.surveys.MarketSurveyObject;
import applab.surveys.ParsedSurveyXml;
import applab.surveys.ProcessMarketSubmissions;
import applab.surveys.Question;
import applab.surveys.Submission;
import applab.surveys.SubmissionStatus;
import applab.surveys.Survey;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.sforce.soap.enterprise.fault.InvalidFieldFault;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.InvalidQueryLocatorFault;
import com.sforce.soap.enterprise.fault.InvalidSObjectFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.MalformedQueryFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;

import applab.surveys.server.SurveysSalesforceProxy;

public class UpdateCommodityPrices extends ApplabServlet {

    private static final long serialVersionUID = -1632348909678853641L;
    private Calendar calendar;
    private int daysBacktoProcess;
    private int daysToProcess;
    private String marketDay;
    private SurveysSalesforceProxy surveysSalesforceProxy;
    private ProcessMarketSubmissions processMarketSubmissions;

    @Override
    public void doApplabPost(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {
        prcoessMarketSubmisions(request);
    }

    @Override
    public void doApplabGet(HttpServletRequest request, HttpServletResponse response, ServletRequestContext context) throws Exception {
        prcoessMarketSubmisions(request);
    }

    private void prcoessMarketSubmisions(HttpServletRequest request) throws ServiceException, InvalidIdFault, UnexpectedErrorFault,
            LoginFault, RemoteException,
            Exception {
        calendar = Calendar.getInstance();
        String startDay = "startday";
        String days = "days";
        String startDayValue = request.getParameter(startDay);
        String daysValue = request.getParameter(days);
        daysBacktoProcess = -1;
        daysToProcess = 0;
        if (startDayValue != null) {
            daysBacktoProcess = Integer.parseInt(startDayValue);
        }
        if (daysValue != null) {
            daysToProcess = Integer.parseInt(daysValue);
        }

        calendar.add(Calendar.DATE, daysBacktoProcess);
        marketDay = DateHelpers.getDayOfWeek(calendar.getTime());
        surveysSalesforceProxy = new SurveysSalesforceProxy();
        this.calendar = Calendar.getInstance();
        processMarketSubmissions = new ProcessMarketSubmissions(calendar, daysBacktoProcess, daysToProcess, surveysSalesforceProxy);
        processMarketSubmissions.performSubmissionProcessing();
        
    }

}