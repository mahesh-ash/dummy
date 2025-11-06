package servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dao.WishlistDao;
import model.Product;

@WebServlet("/WishlistServlet")
public class WishlistServlet extends HttpServlet {
    
	private static final long serialVersionUID = 1L;
	private WishlistDao wishlistDao;
    private Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public void init() {
        wishlistDao = new WishlistDao();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Integer userId = resolveUserId(req);
        if (userId == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().print(gson.toJson(Map.of("error", "Not logged in")));
            return;
        }

        String check = req.getParameter("check");
        String count = req.getParameter("count");

        if (check != null) {
            int productId;
            try {
                productId = Integer.parseInt(check);
            } catch (NumberFormatException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().print(gson.toJson(Map.of("error", "Invalid product id")));
                return;
            }
            try {
                boolean exists = wishlistDao.exists(userId, productId);
                resp.getWriter().print(gson.toJson(Map.of("inWishlist", exists)));
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().print(gson.toJson(Map.of("error", "DB error")));
            }
            return;
        }

        if (count != null) {
            try {
                int c = wishlistDao.countForUser(userId);
                resp.getWriter().print(gson.toJson(Map.of("count", c)));
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().print(gson.toJson(Map.of("error", "DB error")));
            }
            return;
        }

        try {
            List<Product> products = wishlistDao.getWishlistProducts(userId);
            resp.getWriter().print(gson.toJson(products));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print(gson.toJson(Map.of("error", "DB error")));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Integer userId = resolveUserId(req);
        Map<String, Object> result = new HashMap<>();
        if (userId == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            result.put("status", "failed");
            result.put("message", "Not logged in");
            resp.getWriter().print(gson.toJson(result));
            return;
        }

        String action = req.getParameter("action");
        String productIdParam = req.getParameter("productId");
        Integer productId = null;
        if (productIdParam != null && !productIdParam.isBlank()) {
            try {
                productId = Integer.parseInt(productIdParam);
            } catch (NumberFormatException e) {
                productId = null;
            }
        }

        try {
            if ("add".equals(action)) {
                if (productId == null) {
                    result.put("status", "failed");
                    result.put("message", "Missing productId");
                } 
                else if (wishlistDao.exists(userId, productId)) {
                    result.put("status", "ok");
                    result.put("added", false);
                    result.put("message", "Already in wishlist");
                } 
                else {
                    boolean added = wishlistDao.addToWishlist(userId, productId);
                    result.put("status", added ? "ok" : "failed");
                    result.put("added", added);
                    result.put("count", wishlistDao.countForUser(userId));
                }
            } 
            else if ("remove".equals(action)) {
                if (productId == null) {
                    result.put("status", "failed");
                    result.put("message", "Missing productId");
                } 
                else if (!wishlistDao.exists(userId, productId)) {
                    result.put("status", "ok");
                    result.put("removed", false);
                    result.put("message", "Was not in wishlist");
                }
                else {
                    boolean removed = wishlistDao.removeFromWishlist(userId, productId);
                    result.put("status", removed ? "ok" : "failed");
                    result.put("removed", removed);
                    result.put("count", wishlistDao.countForUser(userId));
                }
            } 
            else if ("clearAll".equals(action)) {
                boolean cleared = wishlistDao.clearWishlistForUser(userId);
                result.put("status", cleared ? "ok" : "failed");
                result.put("count", wishlistDao.countForUser(userId));
            } 
            else {
                result.put("status", "failed");
                result.put("message", "Unknown action");
            }
        }
        catch (Exception e) {
            result.put("status", "failed");
            result.put("message", e.getMessage());
        }

        resp.getWriter().print(gson.toJson(result));
    }

    private Integer resolveUserId(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute("userId") != null) {
            Object v = s.getAttribute("userId");
            if (v instanceof Integer) return (Integer) v;
            try { return Integer.parseInt(String.valueOf(v)); } catch (Exception ignored) {}
        }
        String id = req.getParameter("userId");
        if (id == null || id.isBlank()) return null;
        try { return Integer.parseInt(id); } catch (NumberFormatException e) { return null; }
    }

}
