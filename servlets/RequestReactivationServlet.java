package servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;

import dao.ReactivationDao;


@WebServlet("/RequestReactivationServlet")
public class RequestReactivationServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.setContentType("application/json");
    JsonObject json = new JsonObject();
    try {
      String email = req.getParameter("email");
      String message = req.getParameter("message");
      Integer userId = null;
      try {
        String uid = req.getParameter("userId");
        if (uid != null && !uid.isEmpty()) userId = Integer.parseInt(uid);
      } catch (Exception ignored) {}
      if (email == null || email.trim().isEmpty()) {
        json.addProperty("status","error");
        json.addProperty("message","Email required");
        resp.getWriter().write(json.toString());
        return;
      }
      boolean ok = ReactivationDao.createRequest(email.trim().toLowerCase(), userId, message);
      if (ok) {
        json.addProperty("status","success");
        json.addProperty("message","Request submitted. Admin will review it.");
      } else {
        json.addProperty("status","error");
        json.addProperty("message","Failed to create request.");
      }
    } catch (Exception e) {
      e.printStackTrace();
      json.addProperty("status","error");
      json.addProperty("message","Server error");
    }
    resp.getWriter().write(json.toString());
  }
}



