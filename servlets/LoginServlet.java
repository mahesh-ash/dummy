package servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import dao.UserDao;
import model.User;
import utils.LoggerUtil;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerUtil.getLogger(LoginServlet.class);
    private static final int SESSION_TIMEOUT = 30 * 60;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("Login request initiated");
        response.setContentType("application/json;charset=UTF-8");

        Gson gson = new Gson();
        JsonObject json = new JsonObject();
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || password == null) {
            json.addProperty("status", "error");
            json.addProperty("message", "Missing credentials");
            response.getWriter().write(json.toString());
            return;
        }

        if ("admin@gmail.com".equalsIgnoreCase(email)) {
            json.addProperty("status", "error");
            json.addProperty("message", "Admin must use admin login page");
            logger.info("Admin login attempt through user portal");
            response.getWriter().write(json.toString());
            return;
        }

        UserDao userDao = new UserDao();
        String status;
        try {
            status = userDao.isValid(email, password);
        } catch (Exception e) {
            json.addProperty("status", "error");
            json.addProperty("message", "Server error");
            response.getWriter().write(json.toString());
            return;
        }

        if ("valid".equalsIgnoreCase(status)) {
            User user = userDao.getUserDetails(email);
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(SESSION_TIMEOUT);
            session.setAttribute("userId", user.getId());
            session.setAttribute("user", user);
            session.setAttribute("userEmail", email);
            session.setAttribute("isAdmin", false);

            json.addProperty("status", "success");
            json.addProperty("redirectUrl", "/Ecommerce_Website/Html/navbar.html");
            json.add("user", gson.toJsonTree(user));
            logger.info("User successfully logged in: " + email);
        } else {
            json.addProperty("status", "error");
            json.addProperty("message", "Invalid email or password");
        }

        response.getWriter().write(json.toString());
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }
}
