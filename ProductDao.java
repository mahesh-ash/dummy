package dao;

import model.Product;
import utils.DBUtils;
import utils.LoggerUtil;

import java.sql.*;
import java.util.*;

import org.apache.log4j.Logger;

public class ProductDao {
    private static final Logger logger = LoggerUtil.getLogger(ProductDao.class);

    /**
     * Fetch all products
     */
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM Ecommerce_Website.M_S_DATAS";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                products.add(mapProduct(rs));
            }

        } catch (SQLException e) {
            logger.error("Error fetching all products", e);
        }

        return products;
    }

    /**
     * Fetch products by category
     */
    public List<Product> getProductsByCategoryId(int categoryId) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM Ecommerce_Website.M_S_DATAS WHERE category_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }

        } catch (SQLException e) {
            logger.error("Error fetching products by categoryId=" + categoryId, e);
        }

        return products;
    }

    /**
     * Search products by name
     */
    public List<Product> searchProductsByName(String query) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM Ecommerce_Website.M_S_DATAS WHERE product_name LIKE ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }

        } catch (SQLException e) {
            logger.error("Error searching products by name: " + query, e);
        }

        return products;
    }

    /**
     * Search products by category and name
     */
    public List<Product> searchProductsByCategoryAndName(int categoryId, String query) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM Ecommerce_Website.M_S_DATAS WHERE category_id = ? AND product_name LIKE ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            ps.setString(2, "%" + query + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }

        } catch (SQLException e) {
            logger.error("Error searching products by categoryId=" + categoryId + " and name=" + query, e);
        }

        return products;
    }

    /**
     * Filter products dynamically by category, search query, and price filter
     */
    public List<Product> getFilteredProducts(Integer categoryId, String query, String filter) {
        List<Product> products = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM Ecommerce_Website.M_S_DATAS WHERE 1=1");

        if (categoryId != null) sql.append(" AND category_id = ?");
        if (query != null && !query.isEmpty())
            sql.append(" AND (LOWER(product_name) LIKE ? OR LOWER(description) LIKE ?)");
        if (filter != null) {
            switch (filter) {
                case "low-high":
                    sql.append(" ORDER BY price ASC");
                    break;
                case "high-low":
                    sql.append(" ORDER BY price DESC");
                    break;
            }
        }

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int i = 1;
            if (categoryId != null) ps.setInt(i++, categoryId);
            if (query != null && !query.isEmpty()) {
                String like = "%" + query.toLowerCase() + "%";
                ps.setString(i++, like);
                ps.setString(i++, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) products.add(mapProduct(rs));
            }

        } catch (SQLException e) {
            logger.error("Error filtering products", e);
        }

        return products;
    }

    /**
     * Get product by ID
     */
    public Product getProductById(int productId) {
        String sql = "SELECT * FROM Ecommerce_Website.M_S_DATAS WHERE product_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapProduct(rs);
            }

        } catch (SQLException e) {
            logger.error("Error fetching product by productId=" + productId, e);
        }

        return null;
    }

    /**
     * Map a ResultSet row to Product object
     */
    private Product mapProduct(ResultSet rs) throws SQLException {
        byte[] imageBytes = rs.getBytes("image");

        Product p = new Product(
                rs.getInt("product_id"),
                rs.getInt("category_id"),
                rs.getString("product_name"),
                rs.getString("description"),
                rs.getDouble("price"),
                rs.getInt("stock"),
                imageBytes
        );

        return p;
    }

    /**
     * Get Base64 encoded image for frontend
     */
    public String getBase64Image(int productId) {
        Product p = getProductById(productId);
        if (p != null && p.getImageData() != null && p.getImageData().length > 0)
            return Base64.getEncoder().encodeToString(p.getImageData());
        return null;
    }

    /**
     * Decrement stock safely
     */
    public int decrementStockAndGet(int productId, int qty) {
        String sql = "UPDATE Ecommerce_Website.M_S_DATAS SET stock = stock - ? WHERE product_id = ? AND stock >= ?";
        String select = "SELECT stock FROM Ecommerce_Website.M_S_DATAS WHERE product_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, qty);
            ps.setInt(2, productId);
            ps.setInt(3, qty);

            int updated = ps.executeUpdate();
            if (updated > 0) {
                try (PreparedStatement ps2 = conn.prepareStatement(select)) {
                    ps2.setInt(1, productId);
                    try (ResultSet rs = ps2.executeQuery()) {
                        if (rs.next()) return rs.getInt("stock");
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Error decrementing stock for productId=" + productId, e);
        }

        return -1;
    }

    /**
     * Increment stock
     */
    public int incrementStockAndGet(int productId, int qty) {
        String sql = "UPDATE Ecommerce_Website.M_S_DATAS SET stock = stock + ? WHERE product_id = ?";
        String select = "SELECT stock FROM Ecommerce_Website.M_S_DATAS WHERE product_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, qty);
            ps.setInt(2, productId);

            int updated = ps.executeUpdate();
            if (updated > 0) {
                try (PreparedStatement ps2 = conn.prepareStatement(select)) {
                    ps2.setInt(1, productId);
                    try (ResultSet rs = ps2.executeQuery()) {
                        if (rs.next()) return rs.getInt("stock");
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Error incrementing stock for productId=" + productId, e);
        }

        return -1;
    }

    /**
     * Get current stock
     */
    public int getStock(int productId) {
        String sql = "SELECT stock FROM Ecommerce_Website.M_S_DATAS WHERE product_id = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("stock");
            }

        } catch (SQLException e) {
            logger.error("Error getting stock for productId=" + productId, e);
        }

        return -1;
    }

    /**
     * Get product details as a Map for frontend
     */
    public Map<String, Object> getProductDetailsMap(int productId) {
        Map<String, Object> map = new HashMap<>();
        Product p = getProductById(productId);
        if (p == null) return map;

        map.put("productId", p.getProductId());
        map.put("categoryId", p.getCategoryId());
        map.put("productName", p.getProductName());
        map.put("description", p.getDescription());
        map.put("price", p.getPrice());
        map.put("stock", p.getStock());
        map.put("image", p.getImageData() != null ? Base64.getEncoder().encodeToString(p.getImageData()) : null);

        return map;
    }

    public List<String> getImagesForProduct(int productId) throws SQLException {
        List<String> imgs = new ArrayList<>();
        String sql = "SELECT image FROM Ecommerce_Website.M_S_DATAS WHERE product_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byte[] b = rs.getBytes("image");
                    if (b != null && b.length > 0) {
                        imgs.add(Base64.getEncoder().encodeToString(b));
                    }
                }
            }
        }
        return imgs;
    }

	public double getActiveDiscountPercent(Integer pid) {
		// TODO Auto-generated method stub
		return 0;
	}

}
