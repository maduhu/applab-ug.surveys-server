package applab.surveys.server;

import applab.server.ApplabServlet;
import applab.server.DatabaseHelpers;
import applab.server.DateHelpers;
import applab.server.ServletRequestContext;
import applab.surveys.ProcessMarketSubmissions;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.rpc.ServiceException;

import java.rmi.RemoteException;
import java.util.Calendar;
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
	private Logger logger;

	public void doApplabPost(HttpServletRequest request,
			HttpServletResponse response, ServletRequestContext context)
			throws Exception {
		logger = Logger.getLogger(UpdateCommodityPrices.class);
		prcoessMarketSubmisions(request, logger);
	}

	public void doApplabGet(HttpServletRequest request,
			HttpServletResponse response, ServletRequestContext context)
			throws Exception {
		logger = Logger.getLogger(UpdateCommodityPrices.class);
		prcoessMarketSubmisions(request, logger);
	}

	private void prcoessMarketSubmisions(HttpServletRequest request,
			Logger logger) throws ServiceException, InvalidIdFault,
			UnexpectedErrorFault, LoginFault, RemoteException, Exception {
		calendar = Calendar.getInstance();
		String startDay = "startday";
		String days = "days";
		String marketDay = "marketday";
		String startDayValue = request.getParameter(startDay);
		String daysValue = request.getParameter(days);
		String marketDayValue = request.getParameter(marketDay);
		daysBacktoProcess = -1;
		daysToProcess = 0;
		if (startDayValue != null) {
			daysBacktoProcess = Integer.parseInt(startDayValue);
		}
		if (daysValue != null) {
			daysToProcess = Integer.parseInt(daysValue);
		}
		calendar.add(Calendar.DATE, daysBacktoProcess);
		if (marketDayValue == null) {
			Calendar tempCalendar = Calendar.getInstance();
			tempCalendar.add(Calendar.DATE, -1);
			marketDay = DateHelpers.getDayOfWeek(tempCalendar.getTime());
		} else {
			marketDay = marketDayValue;
		}
		logger.warn("Market Day: " + marketDay);
		surveysSalesforceProxy = new SurveysSalesforceProxy();
		this.calendar = Calendar.getInstance();
		processMarketSubmissions = new ProcessMarketSubmissions(
				Calendar.getInstance(), daysBacktoProcess, daysToProcess,
				marketDay, surveysSalesforceProxy);
		processMarketSubmissions.performSubmissionProcessing();

	}

}