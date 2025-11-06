package dao;

import model.DiscountResult;
import utils.DBUtils;

import java.sql.*;
import java.util.*;

public class CouponDao {

    private static final Set<String> FALLBACK_NEW_USER_ONLY_CODES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("WELCOME10", "NEWUSER5"))
    );

    public CouponDao() {
    	
    }

    public DiscountResult validateAndComputeDiscount(Connection conn, String code, double amount, int userId) throws SQLException {
        if (code == null || code.trim().isEmpty()) return new DiscountResult(true, "No coupon");
        String sql = "SELECT coupon_id, code, type, value, min_amount, max_discount, expires_at, usage_limit, used_count, active FROM Ecommerce_Website.D_D_COUPON WHERE LOWER(code) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new DiscountResult(false, "Coupon not found");
                int couponId = rs.getInt("coupon_id");
                String type = rs.getString("type");
                double value = rs.getDouble("value");
                double minAmount = rs.getDouble("min_amount");
                Double maxDiscount = rs.getObject("max_discount") != null ? rs.getDouble("max_discount") : null;
                Timestamp expiresAt = rs.getTimestamp("expires_at");
                Integer usageLimit = rs.getObject("usage_limit") != null ? rs.getInt("usage_limit") : null;
                int usedCount = rs.getInt("used_count");
                boolean active = rs.getBoolean("active");

                boolean newUserOnly = false;
                try (PreparedStatement colCheck = conn.prepareStatement(
                        "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                    colCheck.setString(1, "Ecommerce_Website");
                    colCheck.setString(2, "D_D_COUPON");
                    colCheck.setString(3, "new_user_only");
                    try (ResultSet rc = colCheck.executeQuery()) {
                        if (rc.next()) {
                            try (PreparedStatement p2 = conn.prepareStatement(
                                    "SELECT new_user_only FROM Ecommerce_Website.D_D_COUPON WHERE coupon_id = ?")) {
                                p2.setInt(1, couponId);
                                try (ResultSet r2 = p2.executeQuery()) {
                                    if (r2.next()) newUserOnly = r2.getBoolean("new_user_only");
                                }
                            }
                        } else {
                            if (code != null && FALLBACK_NEW_USER_ONLY_CODES.contains(code.trim().toUpperCase())) newUserOnly = true;
                        }
                    }
                }

                if (newUserOnly) {
                    try (PreparedStatement p3 = conn.prepareStatement(
                            "SELECT COUNT(1) as cnt FROM Ecommerce_Website.D_D_ORDER WHERE user_id = ?")) {
                        p3.setInt(1, userId);
                        try (ResultSet r3 = p3.executeQuery()) {
                            if (r3.next()) {
                                int userOrders = r3.getInt("cnt");
                                if (userOrders > 0) return new DiscountResult(false, "Coupon valid for new users only");
                            }
                        }
                    }
                }

                if (!active) return new DiscountResult(false, "Coupon is inactive");
                if (expiresAt != null && expiresAt.before(new java.util.Date())) return new DiscountResult(false, "Coupon expired");
                if (amount < minAmount) return new DiscountResult(false, "Minimum amount for coupon is ₹" + minAmount);
                if (usageLimit != null && usedCount >= usageLimit) return new DiscountResult(false, "Coupon usage limit reached");

                double discount = 0.0;
                if ("PERCENT".equalsIgnoreCase(type)) {
                    discount = (value / 100.0) * amount;
                } else {
                    discount = value;
                }
                if (maxDiscount != null && discount > maxDiscount) discount = maxDiscount;
                double newAmount = Math.max(0.0, amount - discount);
                DiscountResult dr = new DiscountResult(true, "OK");
                dr.setDiscountAmount(discount);
                dr.setNewAmount(newAmount);
                dr.setCouponId(couponId);
                return dr;
            }
        }
    }


    public void recordCouponUsage(Connection conn, int couponId, int userId, int orderId, double before, double discount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE Ecommerce_Website.D_D_COUPON SET used_count = used_count + 1 WHERE coupon_id = ?")) {
            ps.setInt(1, couponId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Coupon does not exist (coupon_id=" + couponId + ")");
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Ecommerce_Website.D_D_COUPON_USAGES (coupon_id, user_id, order_id, amount_before, discount_amount) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, couponId);
            ps.setInt(2, userId);
            if (orderId > 0) ps.setInt(3, orderId);
            else ps.setNull(3, java.sql.Types.INTEGER);
            ps.setDouble(4, before);
            ps.setDouble(5, discount);
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> listActiveCoupons(int sessionUserId) throws java.sql.SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT coupon_id, code, type, value, min_amount, max_discount, expires_at, usage_limit, used_count, active FROM Ecommerce_Website.D_D_COUPON WHERE active = 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int couponId = rs.getInt("coupon_id");
                    String code = rs.getString("code");
                    String type = rs.getString("type");
                    double v = rs.getDouble("value");
                    double minAmount = rs.getDouble("min_amount");
                    boolean newUserOnly = false;
                    try (PreparedStatement colCheck = conn.prepareStatement(
                            "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                        colCheck.setString(1, "Ecommerce_Website");
                        colCheck.setString(2, "D_D_COUPON");
                        colCheck.setString(3, "new_user_only");
                        try (ResultSet rc = colCheck.executeQuery()) {
                            if (rc.next()) {
                                try (PreparedStatement p2 = conn.prepareStatement(
                                        "SELECT new_user_only FROM Ecommerce_Website.D_D_COUPON WHERE coupon_id = ?")) {
                                    p2.setInt(1, couponId);
                                    try (ResultSet r2 = p2.executeQuery()) {
                                        if (r2.next()) newUserOnly = r2.getBoolean("new_user_only");
                                    }
                                }
                            } else {
                                if (code != null && FALLBACK_NEW_USER_ONLY_CODES.contains(code.trim().toUpperCase())) newUserOnly = true;
                            }
                        }
                    }
                    boolean applicable = true;
                    if (newUserOnly && sessionUserId > 0) {
                        try (PreparedStatement p3 = conn.prepareStatement("SELECT COUNT(1) as cnt FROM Ecommerce_Website.D_D_ORDER WHERE user_id = ?")) {
                            p3.setInt(1, sessionUserId);
                            try (ResultSet r3 = p3.executeQuery()) {
                                if (r3.next()) {
                                    int orders = r3.getInt("cnt");
                                    if (orders > 0) applicable = false;
                                }
                            }
                        }
                    }
                    Map<String, Object> m = new HashMap<>();
                    m.put("couponId", couponId);
                    m.put("code", code);
                    String label = ("PERCENT".equalsIgnoreCase(type)) ? (v + "% off") : ("₹" + v + " off");
                    m.put("label", label);
                    m.put("minAmount", minAmount);
                    m.put("newUserOnly", newUserOnly);
                    m.put("applicable", applicable);
                    out.add(m);
                }
            }
        }
        return out;
    }

	
}
