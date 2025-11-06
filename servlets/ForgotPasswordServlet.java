package servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;
import org.mindrot.jbcrypt.BCrypt;
import utils.DBUtils;

@WebServlet("/ForgotPasswordServlet")
public class ForgotPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;


    private static final String GLOBAL_OTP = "654321";

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        JsonObject json = new JsonObject();

        String action = request.getParameter("action");
        if (action == null) action = request.getParameter("actionType");

        if ("sendOtp".equalsIgnoreCase(action)) {
            handleSendOtp(request, response, json);
            return;
        } else if ("reset".equalsIgnoreCase(action) || "verify".equalsIgnoreCase(action)) {
            handleReset(request, response, json);
            return;
        } else {
            
            handleReset(request, response, json);
            return;
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doPost(req, resp);
    }

    private void handleSendOtp(HttpServletRequest request, HttpServletResponse response, JsonObject json) throws IOException {
        String email = request.getParameter("email");
        if (email == null || email.trim().isEmpty()) {
            json.addProperty("status", "error");
            json.addProperty("message", "Email is required.");
            response.getWriter().write(json.toString());
            return;
        }

        email = email.trim();

        try (Connection conn = DBUtils.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM Ecommerce_Website.M_S_USER WHERE email = ?");
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            boolean exists = false;
            if (rs.next() && rs.getInt(1) > 0) exists = true;

            if (!exists) {
                json.addProperty("status", "error");
                json.addProperty("message", "Email not found.");
                response.getWriter().write(json.toString());
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            json.addProperty("status", "error");
            json.addProperty("message", "Server error while validating email.");
            response.getWriter().write(json.toString());
            return;
        }

        boolean mailSent = false;
        String mailError = null;
        try {
            mailSent = sendEmail(email, GLOBAL_OTP);
        } catch (Exception e) {
            mailError = e.getMessage();
            e.printStackTrace();
        }

        json.addProperty("status", "ok");
        json.addProperty("message", mailSent ? "OTP sent to email." : "OTP generation ");
        if (!mailSent && mailError != null) json.addProperty("note", "mailError: " + mailError);
        response.getWriter().write(json.toString());
    }

    private void handleReset(HttpServletRequest request, HttpServletResponse response, JsonObject json) throws IOException {
        String email = request.getParameter("email");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        String otp = request.getParameter("otp");

        if (email == null || newPassword == null || confirmPassword == null || otp == null ||
            email.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() || otp.isEmpty()) {
            json.addProperty("status", "error");
            json.addProperty("message", "All fields are required.");
            response.getWriter().write(json.toString());
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            json.addProperty("status", "error");
            json.addProperty("message", "Passwords do not match.");
            response.getWriter().write(json.toString());
            return;
        }

        if (!GLOBAL_OTP.equals(otp)) {
            json.addProperty("status", "error");
            json.addProperty("message", "Invalid OTP.");
            response.getWriter().write(json.toString());
            return;
        }

        email = email.trim();

        try (Connection conn = DBUtils.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM Ecommerce_Website.M_S_USER WHERE email=?");
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                PreparedStatement updatePs = conn.prepareStatement(
                    "UPDATE Ecommerce_Website.M_S_USER SET password=?, updatedat=GETDATE() WHERE email=?");
                updatePs.setString(1, hashedPassword);
                updatePs.setString(2, email);

                int rows = updatePs.executeUpdate();
                if (rows > 0) {
                    json.addProperty("status", "success");
                    json.addProperty("message", "Password updated successfully!");
                } else {
                    json.addProperty("status", "error");
                    json.addProperty("message", "Password update failed.");
                }
            } else {
                json.addProperty("status", "error");
                json.addProperty("message", "Email not found in system.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            json.addProperty("status", "error");
            json.addProperty("message", "Server error occurred.");
        }

        response.getWriter().write(json.toString());
    }

    private boolean sendEmail(String toEmail, String otp) throws Exception {
        String smtpHost = System.getenv("SMTP_HOST");
        String smtpPort = System.getenv("SMTP_PORT");
        String smtpUser = System.getenv("SMTP_USER");
        String smtpPass = System.getenv("SMTP_PASS");
        String from = System.getenv("SMTP_FROM") != null ? System.getenv("SMTP_FROM") : "noreply@example.com";

        if (smtpHost == null || smtpUser == null || smtpPass == null || smtpPort == null) {
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);

        Session mailSession = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPass);
            }
        });

        Message message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Your OTP for password reset");
        String body = "Your OTP for resetting password is: " + otp + "\n\nIf you did not request this, ignore this email.";
        message.setText(body);

        Transport.send(message);
        return true;
    }
}






//package servlets;
//
//import java.io.IOException;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.util.Properties;
//import javax.mail.Message;
//import javax.mail.PasswordAuthentication;
//import javax.mail.Session;
//import javax.mail.Transport;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.MimeMessage;
//import javax.naming.InitialContext;
//import javax.naming.NamingException;
//import javax.servlet.ServletException;
//import javax.servlet.annotation.WebServlet;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import com.google.gson.JsonObject;
//import org.mindrot.jbcrypt.BCrypt;
//import utils.DBUtils;
//
//@WebServlet("/ForgotPasswordServlet")
//public class ForgotPasswordServlet extends HttpServlet {
//    private static final long serialVersionUID = 1L;
//
//
//    private static final String GLOBAL_OTP = "654321";
//
//    protected void doPost(HttpServletRequest request, HttpServletResponse response)
//            throws ServletException, IOException {
//
//        response.setContentType("application/json;charset=UTF-8");
//        JsonObject json = new JsonObject();
//
//        String action = request.getParameter("action");
//        if (action == null) action = request.getParameter("actionType");
//
//        if ("sendOtp".equalsIgnoreCase(action)) {
//            handleSendOtp(request, response, json);
//            return;
//        } else if ("reset".equalsIgnoreCase(action) || "verify".equalsIgnoreCase(action)) {
//            handleReset(request, response, json);
//            return;
//        } else {
//            handleReset(request, response, json);
//            return;
//        }
//    }
//
//    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
//            throws ServletException, IOException {
//        doPost(req, resp);
//    }
//
//    private void handleSendOtp(HttpServletRequest request, HttpServletResponse response, JsonObject json) throws IOException {
//        String email = request.getParameter("email");
//        if (email == null || email.trim().isEmpty()) {
//            json.addProperty("status", "error");
//            json.addProperty("message", "Email is required.");
//            response.getWriter().write(json.toString());
//            return;
//        }
//
//        email = email.trim();
//
//        try (Connection conn = DBUtils.getConnection()) {
//            PreparedStatement ps = conn.prepareStatement(
//                "SELECT COUNT(*) FROM Ecommerce_Website.M_S_USER WHERE email = ?");
//            ps.setString(1, email);
//            ResultSet rs = ps.executeQuery();
//            boolean exists = false;
//            if (rs.next() && rs.getInt(1) > 0) exists = true;
//
//            if (!exists) {
//                json.addProperty("status", "error");
//                json.addProperty("message", "Email not found.");
//                response.getWriter().write(json.toString());
//                return;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            json.addProperty("status", "error");
//            json.addProperty("message", "Server error while validating email.");
//            response.getWriter().write(json.toString());
//            return;
//        }
//
//        boolean mailSent = false;
//        String mailError = null;
//        try {
//            mailSent = sendEmail(email, GLOBAL_OTP);
//        } catch (Exception e) {
//            mailError = e.getMessage();
//            e.printStackTrace();
//        }
//
//        json.addProperty("status", "ok");
//        json.addProperty("message", mailSent ? "OTP sent to email." : "OTP generation OK (email not sent; test mode).");
//        if (!mailSent && mailError != null) json.addProperty("note", "mailError: " + mailError);
//        response.getWriter().write(json.toString());
//    }
//
//    private void handleReset(HttpServletRequest request, HttpServletResponse response, JsonObject json) throws IOException {
//        String email = request.getParameter("email");
//        String newPassword = request.getParameter("newPassword");
//        String confirmPassword = request.getParameter("confirmPassword");
//        String otp = request.getParameter("otp");
//
//        if (email == null || newPassword == null || confirmPassword == null || otp == null ||
//            email.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() || otp.isEmpty()) {
//            json.addProperty("status", "error");
//            json.addProperty("message", "All fields are required.");
//            response.getWriter().write(json.toString());
//            return;
//        }
//
//        if (!newPassword.equals(confirmPassword)) {
//            json.addProperty("status", "error");
//            json.addProperty("message", "Passwords do not match.");
//            response.getWriter().write(json.toString());
//            return;
//        }
//
//        if (!GLOBAL_OTP.equals(otp)) {
//            json.addProperty("status", "error");
//            json.addProperty("message", "Invalid OTP.");
//            response.getWriter().write(json.toString());
//            return;
//        }
//
//        email = email.trim();
//
//        try (Connection conn = DBUtils.getConnection()) {
//            PreparedStatement ps = conn.prepareStatement(
//                "SELECT COUNT(*) FROM Ecommerce_Website.M_S_USER WHERE email=?");
//            ps.setString(1, email);
//            ResultSet rs = ps.executeQuery();
//
//            if (rs.next() && rs.getInt(1) > 0) {
//                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
//                PreparedStatement updatePs = conn.prepareStatement(
//                    "UPDATE Ecommerce_Website.M_S_USER SET password=?, updatedat=GETDATE() WHERE email=?");
//                updatePs.setString(1, hashedPassword);
//                updatePs.setString(2, email);
//
//                int rows = updatePs.executeUpdate();
//                if (rows > 0) {
//                    json.addProperty("status", "success");
//                    json.addProperty("message", "Password updated successfully!");
//                } else {
//                    json.addProperty("status", "error");
//                    json.addProperty("message", "Password update failed.");
//                }
//            } else {
//                json.addProperty("status", "error");
//                json.addProperty("message", "Email not found in system.");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            json.addProperty("status", "error");
//            json.addProperty("message", "Server error occurred.");
//        }
//
//        response.getWriter().write(json.toString());
//    }
//
//    private boolean sendEmail(String toEmail, String otp) throws Exception {
//        String smtpHost = null;
//        String smtpPort = null;
//        String smtpUser = null;
//        String smtpPass = null;
//        String from = null;
//
//        try {
//            InitialContext ic = new InitialContext();
//            Object o;
//
//            o = ic.lookup("java:comp/env/SMTP_HOST");
//            smtpHost = o != null ? o.toString() : null;
//
//            o = ic.lookup("java:comp/env/SMTP_PORT");
//            smtpPort = o != null ? o.toString() : null;
//
//            o = ic.lookup("java:comp/env/SMTP_USER");
//            smtpUser = o != null ? o.toString() : null;
//
//            o = ic.lookup("java:comp/env/SMTP_PASS");
//            smtpPass = o != null ? o.toString() : null;
//
//            o = ic.lookup("java:comp/env/SMTP_FROM");
//            from = o != null ? o.toString() : smtpUser;
//        } catch (NamingException ne) {
//            throw new Exception("JNDI lookup failed for SMTP settings: " + ne.getMessage(), ne);
//        }
//
//        if (smtpHost == null || smtpPort == null || smtpUser == null || smtpPass == null) {
//            throw new Exception("SMTP configuration incomplete (host/port/user/pass required).");
//        }
//
//        final String authUser = smtpUser;
//        final String authPass = smtpPass;
//
//        Properties props = new Properties();
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.starttls.enable", "true");
//        props.put("mail.smtp.host", smtpHost);
//        props.put("mail.smtp.port", smtpPort);
//        props.put("mail.smtp.connectiontimeout", "10000");
//        props.put("mail.smtp.timeout", "10000");
//
//        Session mailSession = Session.getInstance(props, new javax.mail.Authenticator() {
//            @Override
//            protected PasswordAuthentication getPasswordAuthentication() {
//                return new PasswordAuthentication(authUser, authPass);
//            }
//        });
//
//        Message message = new MimeMessage(mailSession);
//        message.setFrom(new InternetAddress(from));
//        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
//        message.setSubject("Your OTP for password reset");
//        String body = "Your OTP for resetting password is: " + otp + "\n\nIf you did not request this, ignore this email.";
//        message.setText(body);
//
//        Transport.send(message);
//        return true;
//    }
//}
