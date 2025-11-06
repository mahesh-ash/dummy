package dao;
import java.sql.*;
import java.util.*;

import utils.DBUtils;

public class ReactivationDao {

  public static boolean createRequest(String email, Integer userId, String message) throws SQLException {
    String sql = "INSERT INTO Ecommerce_Website.user_reactivation_request (user_email, user_id, status, message) VALUES (?, ?, 'pending', ?)";
    try (Connection c = DBUtils.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, email.toLowerCase());
      if (userId != null) ps.setInt(2, userId); else ps.setNull(2, Types.INTEGER);
      ps.setString(3, message);
      return ps.executeUpdate() > 0;
    }
  }

  public static List<Map<String,Object>> listPendingRequests() throws SQLException {
    List<Map<String,Object>> out = new ArrayList<>();
    String sql = "SELECT request_id, user_email, user_id, message, status, created_at FROM Ecommerce_Website.user_reactivation_request WHERE status='pending' ORDER BY created_at DESC";
    try (Connection c = DBUtils.getConnection();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        Map<String,Object> m = new HashMap<>();
        m.put("requestId", rs.getInt("request_id"));
        m.put("userEmail", rs.getString("user_email"));
        m.put("userId", rs.getObject("user_id"));
        m.put("message", rs.getString("message"));
        m.put("status", rs.getString("status"));
        m.put("createdAt", rs.getTimestamp("created_at"));
        out.add(m);
      }
    }
    return out;
  }

  public static boolean updateRequestStatus(int requestId, String newStatus, String adminMsg) throws SQLException {
    String sql = "UPDATE Ecommerce_Website.user_reactivation_request SET status=?, admin_message=?, updated_at=SYSUTCDATETIME() WHERE request_id=?";
    try (Connection c = DBUtils.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, newStatus);
      ps.setString(2, adminMsg);
      ps.setInt(3, requestId);
      return ps.executeUpdate() > 0;
    }
  }

  public static Map<String,Object> getRequestById(int requestId) throws SQLException {
    String sql = "SELECT request_id, user_email, user_id, message, status FROM Ecommerce_Website.user_reactivation_request WHERE request_id=?";
    try (Connection c = DBUtils.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, requestId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          Map<String,Object> m = new HashMap<>();
          m.put("requestId", rs.getInt("request_id"));
          m.put("userEmail", rs.getString("user_email"));
          m.put("userId", rs.getObject("user_id"));
          m.put("message", rs.getString("message"));
          m.put("status", rs.getString("status"));
          return m;
        }
      }
    }
    return null;
  }
}
