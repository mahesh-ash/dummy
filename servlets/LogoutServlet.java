package servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import org.apache.log4j.Logger;
import com.google.gson.JsonObject;
import utils.LoggerUtil;

@WebServlet("/LogoutServlet")
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerUtil.getLogger(LogoutServlet.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        JsonObject json = new JsonObject();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
            logger.info("User session invalidated successfully.");
        }

        json.addProperty("status", "success");
        json.addProperty("message", "Logged out successfully.");
        response.getWriter().write(json.toString());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}
