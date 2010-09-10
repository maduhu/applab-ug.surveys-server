package applab.surveys.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import applab.server.ApplabConfiguration;
import applab.server.ApplabConfiguration.ResourceLoader;
import applab.server.ApplabServlet;
import applab.server.ServletRequestContext;

/**
 * Servlet implementation class GetFarmerRegistrationForm
 */
public class GetFarmerRegistrationForm extends ApplabServlet {
	private static final long serialVersionUID = 1L;

    public GetFarmerRegistrationForm() {
        // TODO Auto-generated constructor stub
    }

	@Override
	protected void doApplabGet(HttpServletRequest request,
			HttpServletResponse response, ServletRequestContext context)
			throws Exception {
		
        PrintWriter responseWriter = response.getWriter();
        responseWriter.write("<html><head><style type=\"text/css\">");

		ResourceLoader loader = ApplabConfiguration.getResourceLoader();

		InputStream cssStream = loader.getResourceAsStream("farmerRegistration.css");
		responseWriter.write(convertStreamToString(cssStream));
        responseWriter.write("</style>");
        
        responseWriter.write("<script type=\"text/javascript\">");
        InputStream jsStream = loader.getResourceAsStream("farmerRegistration.js");
        responseWriter.write(convertStreamToString(jsStream));
        responseWriter.write("</script>");

        responseWriter.write("</head><body>");        
        responseWriter.write("<form action=");
        responseWriter.write("\"" + this.getServletContext().getContextPath() + "/processFarmerRegistration\" ");
        responseWriter.write("method=\"POST\" onsubmit=\"return validate(this);\">");

        InputStream htmlStream = loader.getResourceAsStream("farmerRegistration.html");
        responseWriter.write(convertStreamToString(htmlStream));
        
        responseWriter.write("<p><input type=\"submit\" value=\"Send\"/></p>");
        responseWriter.write("<input type=\"text\" style=\"display:none name=\"handsetId\" ");
        responseWriter.write("value=\"" + context.getHandsetId() + "\"/>");
        responseWriter.write("</form></body></html>");
	}

	private String convertStreamToString(InputStream stream) throws IOException {
		if (stream != null) {
			StringBuilder builder = new StringBuilder();
			String line;
			
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
				while ((line = reader.readLine()) != null) {
					builder.append(line).append("\n");
				}
			} finally {
				stream.close();
			}
			return builder.toString();
		} else {       
			return "";
		}
	}
}
