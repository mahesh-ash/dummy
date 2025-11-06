package dao;

import model.Product;
import model.User;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDao {

    public List<Product> getAllProducts() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT TOP (200) p.product_id, p.product_name, p.category_id, " +
                "c.category_name, p.description, p.price, p.stock, p.image " +
                "FROM Ecommerce_Website.M_S_DATAS p " +
                "LEFT JOIN Ecommerce_Website.M_S_CATEGORY c ON p.category_id = c.category_id " +
                "ORDER BY p.product_id";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Product p = new Product();
                p.setProductId(rs.getInt("product_id"));
                p.setProductName(rs.getString("product_name"));
                p.setCategoryId(rs.getInt("category_id"));
                p.setDescription(rs.getString("description"));
                p.setPrice(rs.getDouble("price"));
                p.setStock(rs.getInt("stock"));

                // read binary image
                byte[] img = rs.getBytes("image");
                p.setImageData(img);

                String cat = rs.getString("category_name");
                if (cat != null) p.setCategoryname(cat);

                list.add(p);
            }
        }

        return list;
    }

    public boolean addProduct(Product p) throws SQLException {
        String sql = "INSERT INTO Ecommerce_Website.M_S_DATAS (category_id, product_name, description, price, stock, image) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, p.getCategoryId());
            ps.setString(2, p.getProductName());
            ps.setString(3, p.getDescription());
            ps.setDouble(4, p.getPrice());
            ps.setInt(5, p.getStock());

            if (p.getImageData() != null) {
                ps.setBytes(6, p.getImageData());
            } else {
                ps.setNull(6, Types.VARBINARY);
            }

            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateProduct(Product p) throws SQLException {
        String sql = "UPDATE Ecommerce_Website.M_S_DATAS SET category_id=?, product_name=?, description=?, price=?, stock=?, image=? WHERE product_id=?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, p.getCategoryId());
            ps.setString(2, p.getProductName());
            ps.setString(3, p.getDescription());
            ps.setDouble(4, p.getPrice());
            ps.setInt(5, p.getStock());

            if (p.getImageData() != null) {
                ps.setBytes(6, p.getImageData());
            } else {
                ps.setNull(6, Types.VARBINARY);
            }

            ps.setInt(7, p.getProductId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteProduct(int productId) throws SQLException {
        String sql = "DELETE FROM Ecommerce_Website.M_S_DATAS WHERE product_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, fullname, email, phone, is_active as status FROM Ecommerce_Website.M_S_USER";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("user_id"));
                u.setName(rs.getString("fullname"));
                u.setEmail(rs.getString("email"));
                u.setMobile(rs.getString("phone"));
                u.setStatus(rs.getInt("status"));
                list.add(u);
            }
        }

        return list;
    }

    public boolean updateUserStatus(int userId, boolean active) throws SQLException {
        String sql = "UPDATE Ecommerce_Website.M_S_USER SET is_active = ? WHERE user_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteUser(int userId) throws SQLException {
        return updateUserStatus(userId, false);
    }

    public boolean changePassword(int userId, String currentPwd, String newPwd) throws SQLException {
        String checkSql = "SELECT password FROM Ecommerce_Website.M_S_USER WHERE user_id = ?";
        String updateSql = "UPDATE Ecommerce_Website.M_S_USER SET password = ? WHERE user_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {

            checkPs.setInt(1, userId);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    String existing = rs.getString("password");
                    if (!existing.equals(currentPwd)) return false;
                } else {
                    return false;
                }
            }

            try (PreparedStatement upd = conn.prepareStatement(updateSql)) {
                upd.setString(1, newPwd);
                upd.setInt(2, userId);
                return upd.executeUpdate() > 0;
            }
        }
    }

    public List<Map<String, Object>> getAllCategories() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT category_id, category_name, description FROM Ecommerce_Website.M_S_CATEGORY";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("category_id", rs.getInt("category_id"));
                row.put("category_name", rs.getString("category_name"));
                row.put("description", rs.getString("description"));
                list.add(row);
            }
        }

        return list;
    }

    public boolean addCategory(String name, String description) throws SQLException {
        String sql = "INSERT INTO Ecommerce_Website.M_S_CATEGORY (category_name, description) VALUES (?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, description);
            return ps.executeUpdate() > 0;
        }
    }
}
