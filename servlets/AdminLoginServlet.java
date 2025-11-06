package servlets;

import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import org.apache.log4j.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import model.User;

@WebServlet("/AdminLoginServlet")
public class AdminLoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(AdminLoginServlet.class);

    private static final String DEFAULT_ADMIN_EMAIL = "admin@gmail.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";
    private static final int SESSION_TIMEOUT = 30 * 60;

    @Override
    public void init() throws ServletException {
        super.init();
        ServletContext ctx = getServletContext();
        if (ctx.getAttribute("adminPassword") == null) {
            ctx.setAttribute("adminPassword", DEFAULT_ADMIN_PASSWORD);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        Gson gson = new Gson();
        JsonObject json = new JsonObject();

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || password == null) {
            json.addProperty("status", "error");
            json.addProperty("message", "Missing credentials");
            logger.error("Missing credentials in admin login");
            response.getWriter().write(json.toString());
            return;
        }

        ServletContext ctx = getServletContext();
        Object pwObj = ctx.getAttribute("adminPassword");
        String currentAdminPassword =
                pwObj != null ? String.valueOf(pwObj) : DEFAULT_ADMIN_PASSWORD;

        if (DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(email)
                && currentAdminPassword.equals(password)) {

            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(SESSION_TIMEOUT);

            User adminUser = new User();
            adminUser.setId(0);
            adminUser.setName("Admin");
            adminUser.setEmail(DEFAULT_ADMIN_EMAIL);

            session.setAttribute("admin", adminUser);
            session.setAttribute("user", adminUser);
            session.setAttribute("userObj", adminUser);
            session.setAttribute("adminId", 0);
            session.setAttribute("isAdmin", Boolean.TRUE);
            session.setAttribute("username", adminUser.getName());
            session.setAttribute("email", adminUser.getEmail());



            if (ctx.getAttribute("adminPassword") == null) {
                ctx.setAttribute("adminPassword", DEFAULT_ADMIN_PASSWORD);
            }


            logger.info("Admin logged in successfully. Session id=" + session.getId()
                    + " isAdmin=" + session.getAttribute("isAdmin")
                    + " user=" + session.getAttribute("user"));

            json.addProperty("status", "success");
            json.addProperty("redirectUrl", "/Ecommerce_Website/Html/Admin.html");
            json.add("user", gson.toJsonTree(adminUser));

            response.getWriter().write(json.toString());
            return;
        }

        json.addProperty("status", "error");
        json.addProperty("message", "Invalid admin credentials");
        logger.warn("Invalid admin credentials attempt for email=" + email);
        response.getWriter().write(json.toString());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doPost(req, resp);
    }
}
