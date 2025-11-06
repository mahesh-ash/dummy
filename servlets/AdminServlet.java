package servlets;

import com.google.gson.Gson;
import dao.AdminDao;
import dao.OrderDao;
import model.Order;
import model.Product;
import model.User;
import utils.LoggerUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/AdminServlet")
@MultipartConfig(fileSizeThreshold = 1024 * 100, // 100KB
                 maxFileSize = 1024 * 1024 * 10,  // 10MB
                 maxRequestSize = 1024 * 1024 * 20) // 20MB
public class AdminServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerUtil.getLogger(AdminServlet.class);

    private final AdminDao dao = new AdminDao();
    private final Gson gson = new Gson();

    private boolean isAdminLoggedIn(HttpSession session) {
        return session != null
                && session.getAttribute("isAdmin") != null
                && Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    private boolean requireAdmin(HttpServletRequest req,
                                 HttpServletResponse res)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (!isAdminLoggedIn(session)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().write("{\"error\":\"Admin not logged in\"}");
            logger.error("Admin not logged in");
            return false;
        }
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse res)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        if (action == null || action.trim().isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"error\":\"action required\"}");
            return;
        }

        try {
            switch (action) {
                case "listUsers":
                    if (!requireAdmin(req, res)) return;
                    List<User> users = dao.getAllUsers();
                    res.getWriter().write(gson.toJson(users));
                    break;

                case "getAdmin":
                    if (!requireAdmin(req, res)) return;
                    HttpSession session = req.getSession(false);
                    Object adminObj = session.getAttribute("admin");
                    if (adminObj == null) {
                        adminObj = session.getAttribute("user");
                    }
                    if (adminObj == null) {
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.getWriter().write("{\"error\":\"No admin in session\"}");
                        logger.error("No admin in session");
                        return;
                    }
                    Map<String, Object> out = new HashMap<>();
                    if (adminObj instanceof User) {
                        User u = (User) adminObj;
                        String uname = u.getName() != null ? u.getName() : "";
                        out.put("username", uname);
                        out.put("email", u.getEmail());
                        out.put("id", u.getId());
                        out.put("role", "admin");
                        res.getWriter().write(gson.toJson(out));
                        return;
                    }
                    res.getWriter().write(gson.toJson(adminObj));
                    break;

                case "listOrders":
                    if (!requireAdmin(req, res)) return;
                    OrderDao orderDao = new OrderDao();
                    List<Order> orders = orderDao.listOrders();
                    res.getWriter().write(gson.toJson(orders));
                    break;

                case "listCategories":
                    if (!requireAdmin(req, res)) return;
                    List<Map<String, Object>> cats = dao.getAllCategories();
                    res.getWriter().write(gson.toJson(cats));
                    break;

                default:
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    logger.warn("Unknown GET action: " + action);
                    res.getWriter().write("{\"error\":\"Unknown GET action\"}");
            }
        } catch (SQLException e) {
            logger.error("Internal Server Error", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse res)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        if (action == null || action.trim().isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"error\":\"action required\"}");
            return;
        }

        try {
            switch (action) {
                case "logout":
                    HttpSession session = req.getSession(false);
                    if (session != null) session.invalidate();
                    logger.info("logout Successful");
                    res.getWriter().write("{\"status\":\"ok\"}");
                    break;

                case "listProducts":
                    if (!requireAdmin(req, res)) return;
                    List<Product> products = dao.getAllProducts();
                    res.getWriter().write(gson.toJson(products));
                    break;

                case "addProduct":
                    if (!requireAdmin(req, res)) return;
                    try {
                        int categoryId = Integer.parseInt(req.getParameter("categoryId"));
                        String name = req.getParameter("productName");
                        String desc = req.getParameter("description");
                        double price = Double.parseDouble(req.getParameter("price"));
                        int stock = Integer.parseInt(req.getParameter("stock"));

                        if (stock < 0 || price < 0) {
                            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            res.getWriter().write("{\"error\":\"Price/Stock cannot be negative\"}");
                            return;
                        }

                        byte[] imageBytes = null;
                        Part imagePart = req.getPart("image");
                        if (imagePart != null && imagePart.getSize() > 0) {
                            try (InputStream is = imagePart.getInputStream();
                                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                                byte[] buffer = new byte[4096];
                                int read;
                                while ((read = is.read(buffer)) != -1) {
                                    bos.write(buffer, 0, read);
                                }
                                imageBytes = bos.toByteArray();
                            }
                        }

                        Product newProduct = new Product(categoryId, name, desc, price, stock, imageBytes);
                        boolean ok = dao.addProduct(newProduct);
                        res.getWriter().write(ok ? "{\"status\":\"ok\"}" : "{\"status\":\"fail\"}");
                    } catch (NumberFormatException nfe) {
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        res.getWriter().write("{\"error\":\"Invalid numeric input\"}");
                    }
                    break;

                case "updateProduct":
                    if (!requireAdmin(req, res)) return;
                    try {
                        int productId = Integer.parseInt(req.getParameter("productId"));
                        int categoryId = Integer.parseInt(req.getParameter("categoryId"));
                        String name = req.getParameter("productName");
                        String desc = req.getParameter("description");
                        double price = Double.parseDouble(req.getParameter("price"));
                        int stock = Integer.parseInt(req.getParameter("stock"));

                        if (stock < 0 || price <= 0) {
                            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            res.getWriter().write("{\"error\":\"Invalid price/stock\"}");
                            return;
                        }

                        byte[] imageBytes = null;
                        Part imagePart = req.getPart("image");
                        if (imagePart != null && imagePart.getSize() > 0) {
                            try (InputStream is = imagePart.getInputStream();
                                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                                byte[] buffer = new byte[4096];
                                int read;
                                while ((read = is.read(buffer)) != -1) {
                                    bos.write(buffer, 0, read);
                                }
                                imageBytes = bos.toByteArray();
                            }
                        }

                        Product updateProduct = new Product(productId, categoryId, name, desc, price, stock, imageBytes);
                        boolean ok = dao.updateProduct(updateProduct);
                        res.getWriter().write(ok ? "{\"status\":\"ok\"}" : "{\"status\":\"fail\"}");
                    } catch (NumberFormatException nfe) {
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        res.getWriter().write("{\"error\":\"Invalid numeric input\"}");
                    }
                    break;

                case "deleteProduct":
                    if (!requireAdmin(req, res)) return;
                    try {
                        int pid = Integer.parseInt(req.getParameter("productId"));
                        boolean ok = dao.deleteProduct(pid);
                        res.getWriter().write(ok ? "{\"status\":\"ok\"}" : "{\"status\":\"fail\"}");
                    } catch (NumberFormatException nfe) {
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        res.getWriter().write("{\"error\":\"Invalid productId\"}");
                    }
                    break;

                case "deleteUser":
                    if (!requireAdmin(req, res)) return;
                    try {
                        int uid = Integer.parseInt(req.getParameter("userId"));
                        boolean ok = dao.updateUserStatus(uid, false);
                        res.getWriter().write(ok ? "{\"status\":\"ok\"}" : "{\"status\":\"fail\"}");
                    } catch (NumberFormatException nfe) {
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        res.getWriter().write("{\"error\":\"Invalid userId\"}");
                    }
                    break;

                case "toggleStatus":
                    if (!requireAdmin(req, res)) return;
                    try {
                        int userId = Integer.parseInt(req.getParameter("userId"));
                        boolean active = Boolean.parseBoolean(req.getParameter("active"));
                        boolean ok = dao.updateUserStatus(userId, active);
                        res.getWriter().write(ok ? "{\"status\":\"ok\"}" : "{\"status\":\"fail\"}");
                    } catch (NumberFormatException nfe) {
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        res.getWriter().write("{\"error\":\"Invalid input\"}");
                    }
                    break;

                case "changePassword":
                    if (!requireAdmin(req, res)) return;
                    HttpSession s = req.getSession(false);
                    User adminUserPwd = s == null ? null : (User) s.getAttribute("user");
                    if (adminUserPwd == null) {
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        res.getWriter().write("{\"error\":\"No user in session\"}");
                        return;
                    }
                    String current = req.getParameter("currentPassword");
                    String newPass = req.getParameter("newPassword");
                    boolean ok = dao.changePassword(adminUserPwd.getId(), current, newPass);
                    res.getWriter().write(ok ? "{\"status\":\"ok\"}" : "{\"status\":\"fail\"}");
                    break;

                case "addCategory":
                    if (!requireAdmin(req, res)) return;
                    String catName = req.getParameter("categoryName");
                    String catDesc = req.getParameter("description");
                    if (catName == null || catName.trim().isEmpty()) {
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        res.getWriter().write("{\"error\":\"Category name required\"}");
                        return;
                    }
                    boolean added = dao.addCategory(catName.trim(), catDesc);
                    res.getWriter().write(added ? "{\"status\":\"ok\"}" : "{\"status\":\"fail\"}");
                    break;

                default:
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    logger.warn("unknown Post action: " + action);
                    res.getWriter().write("{\"error\":\"Unknown POST action\"}");
            }
        } catch (SQLException e) {
            logger.error("Internal Server Error", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
