package servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.apache.log4j.Logger;
import com.google.gson.JsonObject;
import dao.UserDao;
import model.User;
import utils.LoggerUtil;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerUtil.getLogger(RegisterServlet.class);

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject jsonResponse = new JsonObject();

        try {
            String name = request.getParameter("fname");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String mobile = request.getParameter("phone");
            String address = request.getParameter("full_address");

            int pincode = 0;
            String pincodeStr = request.getParameter("pincode");
            if (pincodeStr != null && !pincodeStr.isEmpty()) {
                try {
                    pincode = Integer.parseInt(pincodeStr);
                } catch (NumberFormatException e) {
                    jsonResponse.addProperty("status", "error");
                    jsonResponse.addProperty("message", "Invalid pincode format.");
                    response.getWriter().write(jsonResponse.toString());
                    return;
                }
            }

            if (email == null || email.trim().isEmpty()) {
                jsonResponse.addProperty("status", "error");
                jsonResponse.addProperty("message", "Email is required.");
                response.getWriter().write(jsonResponse.toString());
                return;
            }

            email = email.toLowerCase().trim();

            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setMobile(mobile);
            user.setAddress(address);
            user.setPinCode(pincode);

            if (UserDao.isEmailExists(email)) {
                jsonResponse.addProperty("status", "exists");
                jsonResponse.addProperty("message", "User already exists with this email!");
                response.getWriter().write(jsonResponse.toString());
                return;
            }

            boolean success = UserDao.registerUser(user);
            if (success) {
                jsonResponse.addProperty("status", "success");
                jsonResponse.addProperty("message", "Registration successful!");
                logger.info("New user registered: " + email);
            } else {
                jsonResponse.addProperty("status", "error");
                jsonResponse.addProperty("message", "Registration failed. Please try again.");
                logger.warn("Registration failed for user: " + email);
            }
        } catch (Exception ex) {
            logger.error("Server error during registration", ex);
            jsonResponse.addProperty("status", "error");
            jsonResponse.addProperty("message", "Server error. Please try again later.");
        }

        response.getWriter().write(jsonResponse.toString());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}
