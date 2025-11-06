package servlets;

import com.google.gson.Gson;
import dao.UserDao;
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

@WebServlet("/ProfileServlet")
public class ProfileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();
    private static final Logger logger = LoggerUtil.getLogger(ProfileServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
        response.getWriter().write(gson.toJson(user));
    }

    @Override
    	    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	        response.setContentType("application/json");
    	        HttpSession session = request.getSession(false);
    	        UserDao userDao = new UserDao();
    	        
    	        if (session == null || session.getAttribute("user") == null) {
    	            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not logged in.");
    	            return;
    	        }

    	        try {
    	            User user = (User) session.getAttribute("user");
    	            

    	            String newName = request.getParameter("fullname");
    	            String newMobile = request.getParameter("phone");
    	            String newAddress = request.getParameter("address");
    	            String newPincode = request.getParameter("pincode");


    	            if (newName != null && !newName.trim().isEmpty()) {
    	                user.setName(newName);
    	            }
    	            if (newMobile != null && !newMobile.trim().isEmpty()) {
    	                user.setMobile(newMobile);
    	            }
    	            if (newAddress != null && !newAddress.trim().isEmpty()) {
    	                user.setAddress(newAddress);
    	            }
    	            if (newPincode != null && !newPincode.trim().isEmpty()) {
    	                try {
    	                    user.setPinCode(Integer.parseInt(newPincode));
    	                } catch (NumberFormatException e) {
                              e.getStackTrace();
    	                }
    	            }


    	            boolean success = userDao.updateUser(user);

    	            if (success) {

    	                session.setAttribute("user", user);
    	                response.getWriter().print("{\"status\":\"success\", \"message\":\"Profile updated successfully\"}");
    	            } else {
    	                response.getWriter().print("{\"status\":\"error\", \"message\":\"Profile update failed\"}");
    	            }

    	        } catch (SQLException e) {
    	            getServletContext().log("servlets.ProfileServlet: Database error updating user profile", e);
    	            logger.error("Database Error");
    	            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error updating user profile.");
    }
    }
}




























