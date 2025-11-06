package dao;

import model.Category;
import utils.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        String query = "SELECT category_id, category_name "
                     + "FROM Ecommerce_Website.M_S_CATEGORY "
                     + "ORDER BY category_name";

        try {
            conn = DBUtils.getConnection();
            ps = conn.prepareStatement(query);
            rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("category_id");
                String name = rs.getString("category_name");

                Category category = new Category(id, name);
                categories.add(category);
            }

        } catch (SQLException e) {
            e.printStackTrace();

        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                   e.getMessage();
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
                  e.getMessage();
                }
            }
        }

        return categories;
    }
}
