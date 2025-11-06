package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import model.CartItem;
import utils.DBUtils;

public class CartDao {

    public CartDao() {
    }

    // Add item to cart, increment if already exists
    public void addToCart(int userId, int productId, int qty) throws SQLException {
        String select = "SELECT cart_id, quantity FROM Ecommerce_Website.D_D_CART WHERE user_id=? AND product_id=?";
        String insert = "INSERT INTO Ecommerce_Website.D_D_CART (user_id, product_id, quantity) VALUES (?,?,?)";
        String update = "UPDATE Ecommerce_Website.D_D_CART SET quantity = ?, updatedat = GETDATE() WHERE cart_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement psSelect = conn.prepareStatement(select)) {

            psSelect.setInt(1, userId);
            psSelect.setInt(2, productId);
            try (ResultSet rs = psSelect.executeQuery()) {
                if (rs.next()) {
                    int cartId = rs.getInt("cart_id");
                    int existing = rs.getInt("quantity");
                    try (PreparedStatement psUpdate = conn.prepareStatement(update)) {
                        psUpdate.setInt(1, existing + qty);
                        psUpdate.setInt(2, cartId);
                        psUpdate.executeUpdate();
                    }
                    return;
                }
            }

            try (PreparedStatement psInsert = conn.prepareStatement(insert)) {
                psInsert.setInt(1, userId);
                psInsert.setInt(2, productId);
                psInsert.setInt(3, qty);
                psInsert.executeUpdate();
            }
        }
    }

    // Update quantity of an item in cart
    public void updateQuantity(int userId, int productId, int qty) throws SQLException {
        String update = "UPDATE Ecommerce_Website.D_D_CART SET quantity = ?, updatedat = GETDATE() WHERE user_id=? AND product_id=?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setInt(1, qty);
            ps.setInt(2, userId);
            ps.setInt(3, productId);
            ps.executeUpdate();
        }
    }

    // Remove single item from cart
    public void removeFromCart(int userId, int productId) throws SQLException {
        String del = "DELETE FROM Ecommerce_Website.D_D_CART WHERE user_id=? AND product_id=?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(del)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    // Remove multiple items from cart
    public void removeItemsFromCart(int userId, List<Integer> productIds) throws SQLException {
        if (productIds == null || productIds.isEmpty()) return;

        String placeholders = String.join(",", Collections.nCopies(productIds.size(), "?"));
        String sql = "DELETE FROM Ecommerce_Website.D_D_CART WHERE user_id = ? AND product_id IN (" + placeholders + ")";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int idx = 2;
            for (Integer pid : productIds) {
                ps.setInt(idx++, pid);
            }
            ps.executeUpdate();
        }
    }

    // Get all cart items for a user
    public List<CartItem> getCartItems(int userId) throws SQLException {
        List<CartItem> list = new ArrayList<>();
        String sql = "SELECT c.product_id, c.quantity, p.product_name, p.price, p.image " +
                     "FROM Ecommerce_Website.D_D_CART c " +
                     "LEFT JOIN Ecommerce_Website.M_S_DATAS p ON c.product_id = p.product_id " +
                     "WHERE c.user_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int pid = rs.getInt("product_id");
                    int qty = rs.getInt("quantity");
                    String name = rs.getString("product_name");
                    double price = rs.getDouble("price");

                    // Convert VARBINARY image to Base64
                    String imageStr = null;
                    byte[] imgBytes = rs.getBytes("image");
                    if (imgBytes != null && imgBytes.length > 0) {
                        imageStr = Base64.getEncoder().encodeToString(imgBytes);
                    }

                    list.add(new CartItem(pid, name, price, imageStr, qty));
                }
            }
        }

        return list;
    }

    // Clear all items from cart
    public void clearCart(int userId) throws SQLException {
        String del = "DELETE FROM Ecommerce_Website.D_D_CART WHERE user_id=?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(del)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    // Reorder previous order items into cart
    public void reorderItems(int userId, int orderId) throws SQLException {
        String itemsSql = "SELECT product_id, quantity FROM Ecommerce_Website.D_D_ORDERLOGS WHERE order_id = ?";
        String addToCartSql = "MERGE INTO Ecommerce_Website.D_D_CART AS target " +
                              "USING (VALUES (?, ?, ?)) AS source (user_id, product_id, quantity) " +
                              "ON target.user_id = source.user_id AND target.product_id = source.product_id " +
                              "WHEN MATCHED THEN UPDATE SET target.quantity = target.quantity + source.quantity " +
                              "WHEN NOT MATCHED THEN INSERT (user_id, product_id, quantity) VALUES (source.user_id, source.product_id, source.quantity);";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement itemsPs = conn.prepareStatement(itemsSql)) {

            conn.setAutoCommit(false);

            itemsPs.setInt(1, orderId);
            try (ResultSet rs = itemsPs.executeQuery();
                 PreparedStatement addToCartPs = conn.prepareStatement(addToCartSql)) {

                while (rs.next()) {
                    int productId = rs.getInt("product_id");
                    int quantity = rs.getInt("quantity");

                    addToCartPs.setInt(1, userId);
                    addToCartPs.setInt(2, productId);
                    addToCartPs.setInt(3, quantity);
                    addToCartPs.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
