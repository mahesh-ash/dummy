package servlets;

import com.google.gson.JsonObject;
import dao.CartDao;
import model.User;
import utils.LoggerUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/ReorderServlet")
public class ReorderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final CartDao cartDao = new CartDao();
    private static final Logger logger = LoggerUtil.getLogger(ReorderServlet.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        JsonObject jsonResponse = new JsonObject();

        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonResponse.addProperty("status", "error");
            logger.error("user not logged in");
            jsonResponse.addProperty("message", "Not logged in.");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        User user = (User) session.getAttribute("user");
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            cartDao.reorderItems(user.getId(), orderId);
            jsonResponse.addProperty("status", "ok");
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("status", "error");
            logger.error("Invalid order Id");
            jsonResponse.addProperty("message", "Invalid order ID.");
        } catch (SQLException e) {
            log("Database error reordering items", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.addProperty("status", "error");
            jsonResponse.addProperty("message", "Internal server error.");
        }
        response.getWriter().write(jsonResponse.toString());
    }
}