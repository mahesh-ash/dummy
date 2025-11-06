package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

import model.User;
import utils.DBUtils;

public class UserDao {
  
	public static boolean isEmailExists(String email) {
	    Connection conn = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;
	    String sql = "SELECT COUNT(*) FROM Ecommerce_Website.M_S_USER WHERE LOWER(email) = ?";
	    try {
	        conn = DBUtils.getConnection();
	        ps = conn.prepareStatement(sql);
	        ps.setString(1, email.toLowerCase());
	        rs = ps.executeQuery();
	        if (rs.next()) {
	            return rs.getInt(1) > 0;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        if (rs != null) try { 
	        	rs.close(); 
	        	} catch (SQLException e) {
	        		e.printStackTrace();
	        	}
	        if (ps != null) try { 
	        	ps.close();
	        	} catch (SQLException e) {
	        		e.printStackTrace();
	        	}
	        if (conn != null) try { 
	        	conn.close();
	        	} catch (SQLException e) {
	        		e.printStackTrace();
	        	}
	    }
	    return false;
	}

	public static boolean registerUser(User u) {
	    Connection c = null;
	    PreparedStatement ps = null;
	    String email = u.getEmail().toLowerCase();
	    if (isEmailExists(email)) {
	        return false;
	    }
	    String sql = "INSERT INTO Ecommerce_Website.M_S_USER(fullname, email, phone, password, address, pincode) VALUES(?,?,?,?,?,?)";
	    try {
	        c = DBUtils.getConnection();
	        ps = c.prepareStatement(sql);
	        String encrypt = BCrypt.hashpw(u.getPassword(), BCrypt.gensalt());
	        ps.setString(1, u.getName());
	        ps.setString(2, email);
	        ps.setString(3, u.getMobile());
	        ps.setString(4, encrypt);
	        ps.setString(5, u.getAddress());
	        ps.setInt(6, u.getPinCode());
	        return ps.executeUpdate() > 0;
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        if (ps != null) try { 
	        	ps.close(); 
	        	} catch (SQLException e) {
	        		e.getMessage();
	        	}
	        if (c != null) try { 
	        	c.close();
	        	} catch (SQLException e) {
	        		e.getMessage();
	        	}
	    }
	    return false;
	}

	public String isValid(String email, String password) {
	    Connection conn = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;
	    String status = "Invalid Username or Password";
	    String sql = "SELECT * FROM Ecommerce_Website.M_S_USER with(nolock) WHERE email=? and is_active=1";
	    try {
	        conn = DBUtils.getConnection();
	        ps = conn.prepareStatement(sql);
	        ps.setString(1, email);
	        rs = ps.executeQuery();
	        if (rs.next()) {
	            String storedHash = rs.getString("password");
	            if (BCrypt.checkpw(password, storedHash)) {
	                status = "valid";
	            } else {
	                status = "invalid";
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        if (rs != null) try {
	        	rs.close();
	        	} catch (SQLException e) {
	        		e.printStackTrace();
	        	}
	        if (ps != null) try {
	        	ps.close(); 
	        	} catch (SQLException e) {
	        		e.printStackTrace();
	        	}
	        if (conn != null) try { 
	        	conn.close();
	        	} catch (SQLException e) {
	        		e.getMessage();
	        	}
	    }
	    return status;
	}

	public User getUserDetails(String email) {
	    Connection conn = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;
	    User user = null;
	    String sql = "SELECT user_id, fullname, phone, email, address, pincode, password FROM Ecommerce_Website.M_S_USER WHERE email=?";
	    try {
	        conn = DBUtils.getConnection();
	        ps = conn.prepareStatement(sql);
	        ps.setString(1, email);
	        rs = ps.executeQuery();
	        if (rs.next()) {
	            user = new User();
	            user.setId(rs.getInt("user_id"));
	            user.setName(rs.getString("fullname"));
	            user.setMobile(rs.getString("phone"));
	            user.setEmail(rs.getString("email"));
	            user.setAddress(rs.getString("address"));
	            user.setPinCode(rs.getInt("pincode"));
	            user.setPassword(rs.getString("password"));
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        if (rs != null) try {
	        	rs.close(); 
	        	} catch (SQLException e) {
	        		e.printStackTrace();
	        	}
	        if (ps != null) try { 
	        	ps.close();
	        	} catch (SQLException e) {
	        		e.printStackTrace();
	        	}
	        if (conn != null) try { 
	        	conn.close();
	        	} catch (SQLException e) {
	        		e.printStackTrace();
	        	}
	    }
	    return user;
	}

	public static User getUserByEmail(String email) throws SQLException {
	    Connection conn = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;
	    User user = null;
	    String sql = "SELECT user_id, fullname, phone, address, pincode, password FROM Ecommerce_Website.M_S_USER WHERE email = ?";
	    try {
	        conn = DBUtils.getConnection();
	        ps = conn.prepareStatement(sql);
	        ps.setString(1, email);
	        rs = ps.executeQuery();
	        if (rs.next()) {
	            user = new User();
	            user.setId(rs.getInt("user_id"));
	            user.setEmail(email);
	            user.setName(rs.getString("fullname"));
	            user.setMobile(rs.getString("phone"));
	            user.setAddress(rs.getString("address"));
	            user.setPinCode(rs.getInt("pincode"));
	            user.setPassword(rs.getString("password"));
	        }
	    } finally {
	        if (rs != null) try { 
	        	rs.close(); 
	        	} catch (SQLException e) {
	        		e.printStackTrace();
	        	}
	        if (ps != null) try { 
	        	ps.close();
	        	} catch (SQLException e) {
	        		e.getMessage();
	        	}
	        if (conn != null) try { 
	        	conn.close(); 
	        	} catch (SQLException e) {
	        		e.printStackTrace();
	        	}
	    }
	    return user;
	}

	public boolean updateUser(User user) throws SQLException {
	    Connection c = null;
	    PreparedStatement ps = null;
	    String sql = "UPDATE Ecommerce_Website.M_S_USER SET fullname=?, phone=?, address=?, pincode=?, updatedat=GETDATE() WHERE user_id=?";
	    try {
	        c = DBUtils.getConnection();
	        ps = c.prepareStatement(sql);
	        ps.setString(1, user.getName());
	        ps.setString(2, user.getMobile());
	        ps.setString(3, user.getAddress());
	        ps.setInt(4, user.getPinCode());
	        ps.setInt(5, user.getId());
	        int rowsAffected = ps.executeUpdate();
	        return rowsAffected > 0;
	    } finally {
	        if (ps != null) try {
	        	ps.close(); 
	        	} catch (SQLException e) {
	        		e.printStackTrace();
	        	}
	        if (c != null) try { 
	        	c.close();
	        	} catch (SQLException e) {
	        		e.printStackTrace();
	        	}
	    }
	}

    }





