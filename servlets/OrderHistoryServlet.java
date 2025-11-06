package servlets;

import com.google.gson.Gson;
import dao.OrderDao;
import model.User;
import utils.LoggerUtil;
import org.apache.log4j.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/OrderHistoryServlet")
public class OrderHistoryServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerUtil.getLogger(OrderHistoryServlet.class);
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.error("User not logged in");
            response.getWriter().write("{\"error\":\"Not logged in.\"}");
            return;
        }

        User user = (User) session.getAttribute("user");
        try {
            String jsonResponse = gson.toJson(OrderDao.getOrderHistory(user.getId()));
            response.getWriter().write(jsonResponse);
        } catch (SQLException e) {
            logger.error("Database error retrieving order history", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Internal server error.\"}");
        }
    }
}
