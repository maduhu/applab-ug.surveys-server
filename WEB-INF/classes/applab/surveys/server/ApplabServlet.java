package applab.surveys.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class ApplabServlet extends HttpServlet {    
    private static final long serialVersionUID = 1L;

    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            doApplabGet(request, response);
        }
        catch (Exception exception) {
            StringWriter stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter));

            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, stringWriter.toString());
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    protected void doApplabGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
        super.doGet(request, response);
    }

    /**
     * wrapper around core logic that handles exceptions and sends back the relevent error codes
     */
    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            doApplabPost(request, response);
        }
        catch (Exception exception) {
            StringWriter stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter));

            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, stringWriter.toString());
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    protected void doApplabPost(HttpServletRequest request, HttpServletResponse response) throws Exception {
        super.doPost(request, response);
    }
}
