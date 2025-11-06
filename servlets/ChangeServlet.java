package servlets;

import com.google.gson.Gson;
import model.User;
import org.apache.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;
import utils.DBUtils;
import utils.LoggerUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Map;

@WebServlet("/ChangeServlet")
public class ChangeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerUtil.getLogger(ChangeServlet.class);
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(gson.toJson(Map.of("status","error", "message","Not logged in")));
            return;
        }

        
        User user = (User) session.getAttribute("user");
        int userId;
        try {
            userId = user.getId(); 
        } catch (Exception e) {
            logger.error("Unable to read user id from session user object", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Map.of("status","error","message","Server error")));
            return;
        }

        String currentPwd = req.getParameter("currentPassword");
        String newPwd     = req.getParameter("newPassword");
        String confirmPwd = req.getParameter("confirmPassword");


        if (currentPwd == null || newPwd == null || confirmPwd == null ||
            currentPwd.trim().isEmpty() || newPwd.trim().isEmpty() || confirmPwd.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Map.of("status","error","message","All fields are required")));
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Map.of("status","error","message","New password and confirm password do not match")));
            return;
        }
        if (newPwd.length() < 6) { 
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Map.of("status","error","message","New password must be at least 6 characters")));
            return;
        }

        String selectSql = "SELECT password FROM Ecommerce_Website.M_S_USER WHERE user_id = ?";
        String updateSql = "UPDATE Ecommerce_Website.M_S_USER SET password = ? WHERE user_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {

            ps.setInt(1, userId);
            String stored = null;
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stored = rs.getString("password");
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(Map.of("status","error","message","User not found")));
                    return;
                }
            }

            if (stored == null) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(gson.toJson(Map.of("status","error","message","No password set for account")));
                return;
            }

            boolean matches = false;
            if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
                try {
                    matches = BCrypt.checkpw(currentPwd, stored);
                } catch (IllegalArgumentException iae) {
                    logger.warn("Corrupt bcrypt hash for userId=" + userId, iae);
                    matches = false;
                }
            } else {
                
                matches = stored.equals(currentPwd);
            }

            if (!matches) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(gson.toJson(Map.of("status","error","message","Current password is incorrect")));
                return;
            }

            String newHash = BCrypt.hashpw(newPwd, BCrypt.gensalt());
            try (PreparedStatement up = conn.prepareStatement(updateSql)) {
                up.setString(1, newHash);
                up.setInt(2, userId);
                int updated = up.executeUpdate();
                if (updated > 0) {
                   
                out.print(gson.toJson(Map.of("status","ok","message","Password changed successfully")));
                } else {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print(gson.toJson(Map.of("status","error","message","Failed to update password")));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while changing password for userId=" + userId, e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Map.of("status","error","message","Database error")));
        }
    }
}
