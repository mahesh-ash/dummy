package dao;

import utils.DBUtils;
import model.CartItem;
import model.Order;
import model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderDao {

    public List<Order> listOrders() throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = ""
            + "SELECT o.order_id AS orderId, o.user_id AS userId, "
            + "ISNULL(u.fullname, 'User ' + CAST(o.user_id AS VARCHAR(20))) AS username, "
            + "o.total_amount AS totalAmount, o.orderdate AS orderDate, o.status AS status "
            + "FROM Ecommerce_Website.D_D_ORDER o "
            + "LEFT JOIN Ecommerce_Website.M_S_USER u ON o.user_id = u.user_id "
            + "ORDER BY o.orderdate DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                Order o = new Order();
                o.setOrderId(rs.getInt("orderId"));
                o.setUserId(rs.getInt("userId"));
                o.setUsername(rs.getString("username"));
                o.setTotalAmount(rs.getDouble("totalAmount"));
                Timestamp ts = rs.getTimestamp("orderDate");
                if (ts != null) {
                    o.setOrderDate(new Date(ts.getTime()));
                }
                o.setStatus(rs.getString("status"));
                list.add(o);
            }

        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { e.getMessage(); }
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.getMessage(); }
            }
        }

        return list;
    }

    public static List<Order> getOrderHistory(int userId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String ordersSql = ""
            + "SELECT * FROM Ecommerce_Website.D_D_ORDER "
            + "WHERE user_id = ? "
            + "ORDER BY orderdate DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            ps = conn.prepareStatement(ordersSql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();

            while (rs.next()) {
                Order order = new Order();
                order.setOrderId(rs.getInt("order_id"));
                order.setUserId(rs.getInt("user_id"));
                order.setTotalAmount(rs.getDouble("total_amount"));
                Timestamp ts = rs.getTimestamp("orderdate");
                if (ts != null) {
                    order.setOrderDate(new Date(ts.getTime()));
                }
                order.setStatus(rs.getString("status"));
                order.setItems(getOrderItems(conn, order.getOrderId()));
                orders.add(order);
            }

        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException e) { e.getMessage(); }
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }

        return orders;
    }

    public static List<Order> getOrderHistoryAll(int userId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String ordersSql = ""
            + "SELECT o.order_id, o.user_id, ISNULL(u.fullname, '-') AS username, "
            + "o.total_amount, o.orderdate, o.status "
            + "FROM Ecommerce_Website.D_D_ORDER o "
            + "LEFT JOIN Ecommerce_Website.M_S_USER u ON o.user_id = u.user_id "
            + "WHERE o.user_id = ? "
            + "ORDER BY o.orderdate DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            ps = conn.prepareStatement(ordersSql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();

            while (rs.next()) {
                Order order = new Order();
                order.setOrderId(rs.getInt("order_id"));
                order.setUserId(rs.getInt("user_id"));
                order.setUsername(rs.getString("username"));
                order.setTotalAmount(rs.getDouble("total_amount"));
                Timestamp ts = rs.getTimestamp("orderdate");
                if (ts != null) {
                    order.setOrderDate(new Date(ts.getTime()));
                }
                order.setStatus(rs.getString("status"));
                order.setItems(getOrderItems(conn, order.getOrderId()));
                orders.add(order);
            }

        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { e.getMessage(); }
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException e) { e.getMessage(); }
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.getMessage(); }
            }
        }

        return orders;
    }

    /**
     * NOTE: this method now selects p.image and sets the bytes on OrderItem (if OrderItem supports it).
     * Make sure OrderItem has setImageData(byte[]).
     */
    private static List<OrderItem> getOrderItems(Connection conn, int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String itemsSql = ""
            + "SELECT ol.id, ol.order_id, ol.product_id, ol.quantity, ol.price, ol.total, p.product_name, p.image "
            + "FROM Ecommerce_Website.D_D_ORDERLOGS ol "
            + "JOIN Ecommerce_Website.M_S_DATAS p ON ol.product_id = p.product_id "
            + "WHERE ol.order_id = ?";

        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(itemsSql);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();

            while (rs.next()) {
                OrderItem item = new OrderItem();
                item.setOrderLogId(rs.getInt("id"));
                item.setOrderId(rs.getInt("order_id"));
                item.setProductId(rs.getInt("product_id"));
                item.setProductName(rs.getString("product_name"));
                item.setQuantity(rs.getInt("quantity"));
                item.setPrice(rs.getDouble("price"));
                item.setTotal(rs.getDouble("total"));

               
                items.add(item);
            }

        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }

        return items;
    }

    public static int createOrder(Connection conn, int userId, double totalAmount) throws SQLException {
        String insertOrderSql = "INSERT INTO Ecommerce_Website.D_D_ORDER (user_id, total_amount, orderdate, status) VALUES (?, ?, GETDATE(), ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setDouble(2, totalAmount);
            ps.setString(3, "Paid");
            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("Creating order failed, no rows affected.");
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) return generatedKeys.getInt(1);
                else throw new SQLException("Creating order failed, no ID obtained.");
            }
        }
    }

    public static void createOrderLogs(Connection conn, int orderId, List<CartItem> items) throws SQLException {
        String insertOrderLogSql = "INSERT INTO Ecommerce_Website.D_D_ORDERLOGS (order_id, product_id, quantity, price, total) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertOrderLogSql)) {
            for (CartItem ci : items) {
                double lineTotal = ci.getQty() * ci.getPrice();
                ps.setInt(1, orderId);
                ps.setInt(2, ci.getProductId());
                ps.setInt(3, ci.getQty());
                ps.setDouble(4, ci.getPrice());
                ps.setDouble(5, lineTotal);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public static void clearSelectedCartItems(Connection conn, int userId, List<Integer> selectedItems) throws SQLException {
        if (selectedItems == null || selectedItems.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM Ecommerce_Website.D_D_CART WHERE user_id = ? AND product_id IN (");
        for (int i = 0; i < selectedItems.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        sb.append(")");
        try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            ps.setInt(1, userId);
            for (int i = 0; i < selectedItems.size(); i++) {
                ps.setInt(i + 2, selectedItems.get(i));
            }
            ps.executeUpdate();
        }
    }
}
