package dao;

import utils.DBUtils;
import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WishlistDao {

	public boolean addToWishlist(int userId, int productId) {
	    String sql = "INSERT INTO Ecommerce_Website.M_S_WISHLIST (user_id, product_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)";
	    Connection conn = null;
	    PreparedStatement ps = null;
	    try {
	        conn = DBUtils.getConnection();
	        ps = conn.prepareStatement(sql);
	        ps.setInt(1, userId);
	        ps.setInt(2, productId);
	        ps.executeUpdate();
	        return true;
	    } catch (SQLIntegrityConstraintViolationException dup) {
	        return false;
	    } catch (SQLException e) {
	        e.printStackTrace();
	        return false;
	    } finally {
	        if (ps != null) {
	            try {
	            	ps.close(); 
	            	} catch (SQLException e) {
	            		e.getMessage();
	            	}
	        }
	        if (conn != null) {
	            try { 
	            	conn.close();
	            	} catch (SQLException e) {
	            		
	            		e.getMessage();
	            	}
	        }
	    }
	}

	public boolean exists(int userId, int productId) throws SQLException {
	    String sql = "SELECT TOP 1 * FROM Ecommerce_Website.M_S_WISHLIST WHERE user_id=? AND product_id=?";
	    Connection conn = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;
	    try {
	        conn = DBUtils.getConnection();
	        ps = conn.prepareStatement(sql);
	        ps.setInt(1, userId);
	        ps.setInt(2, productId);
	        rs = ps.executeQuery();
	        return rs.next();
	    } finally {
	        if (rs != null) {
	            try { 
	            	rs.close(); 
	            	} catch (SQLException e) {
	            		e.printStackTrace();
	            	}
	        }
	        if (ps != null) {
	            try { 
	            	ps.close(); 
	            	} catch (SQLException e) {
	            		e.getMessage();
	            	}
	        }
	        if (conn != null) {
	            try { 
	            	conn.close();
	            	} catch (SQLException e) {
	            		e.printStackTrace();
	            	}
	        }
	    }
	}

	public boolean removeFromWishlist(int userId, int productId) {
	    String sql = "DELETE FROM Ecommerce_Website.M_S_WISHLIST WHERE user_id = ? AND product_id = ?";
	    Connection conn = null;
	    PreparedStatement ps = null;
	    try {
	        conn = DBUtils.getConnection();
	        ps = conn.prepareStatement(sql);
	        ps.setInt(1, userId);
	        ps.setInt(2, productId);
	        int updated = ps.executeUpdate();
	        return updated > 0;
	    } catch (SQLException e) {
	        e.printStackTrace();
	        return false;
	    } finally {
	        if (ps != null) {
	            try { ps.close(); } catch (SQLException ignored) {}
	        }
	        if (conn != null) {
	            try { conn.close(); } catch (SQLException ignored) {}
	        }
	    }
	}

	public boolean clearWishlistForUser(int userId) {
	    String sql = "DELETE FROM Ecommerce_Website.M_S_WISHLIST WHERE user_id = ?";
	    Connection conn = null;
	    PreparedStatement ps = null;
	    try {
	        conn = DBUtils.getConnection();
	        ps = conn.prepareStatement(sql);
	        ps.setInt(1, userId);
	        ps.executeUpdate();
	        return true;
	    } catch (SQLException e) {
	        e.printStackTrace();
	        return false;
	    } finally {
	        if (ps != null) {
	            try {
	            	ps.close(); 
	            	} catch (SQLException e) {
	            		e.printStackTrace();
	            	}
	        }
	        if (conn != null) {
	            try {
	            	conn.close();
	            	} catch (SQLException e) {
	            		e.printStackTrace();
	            	}
	        }
	    }
	}

	public List<Product> getWishlistProducts(int userId) {
	    List<Product> out = new ArrayList<>();
	    String sql = "SELECT d.* FROM Ecommerce_Website.M_S_WISHLIST w JOIN Ecommerce_Website.M_S_DATAS d ON w.product_id = d.product_id WHERE w.user_id = ? ORDER BY w.created_at DESC";
	    Connection conn = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;
	    try {
	        conn = DBUtils.getConnection();
	        ps = conn.prepareStatement(sql);
	        ps.setInt(1, userId);
	        rs = ps.executeQuery();
	        while (rs.next()) {
	            out.add(mapProduct(rs));
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        if (rs != null) {
	            try { 
	            	rs.close();
	            	} catch (SQLException e) {
	            		e.printStackTrace();
	            	}
	        }
	        if (ps != null) {
	            try { 
	            	ps.close(); 
	            	} catch (SQLException e) {
	            		e.printStackTrace();
	            	}
	        }
	        if (conn != null) {
	            try { 
	            	conn.close(); 
	            	} catch (SQLException e) {
	            		e.printStackTrace();
	            	}
	        }
	    }
	    return out;
	}

	public int countForUser(int userId) {
	    String sql = "SELECT COUNT(1) cnt FROM Ecommerce_Website.M_S_WISHLIST WHERE user_id = ?";
	    Connection conn = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;
	    try {
	        conn = DBUtils.getConnection();
	        ps = conn.prepareStatement(sql);
	        ps.setInt(1, userId);
	        rs = ps.executeQuery();
	        if (rs.next()) return rs.getInt("cnt");
	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        if (rs != null) {
	            try {
	            	rs.close();
	            	} catch (SQLException e) {
	            		e.printStackTrace();
	            	}
	        }
	        if (ps != null) {
	            try {
	            	ps.close();
	            	} catch (SQLException e) {
	            		e.getMessage();
	            	}
	        }
	        if (conn != null) {
	            try { 
	            	conn.close();
	            	} catch (SQLException e) {
	            		e.printStackTrace();
	            	}
	        }
	    }
	    return 0;
	}

	private Product mapProduct(ResultSet rs) throws SQLException {
	    Product p = new Product();
	    p.setProductId(rs.getInt("product_id"));
	    p.setCategoryId(rs.getInt("category_id"));
	    p.setProductName(rs.getString("product_name"));
	    p.setDescription(rs.getString("description"));
	    p.setPrice(rs.getDouble("price"));
	    p.setStock(rs.getInt("stock"));

	    // If image column is VARBINARY:
	    p.setImageData(rs.getBytes("image"));

	    // If for any reason the image column is still varchar (legacy), fallback:
	    // String imgPath = rs.getString("image");
	    // if (imgPath != null) p.setImageData(imgPath.getBytes());

	    return p;
	}


}
