package servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dao.CartDao;
import dao.ProductDao;
import model.CartItem;
import utils.LoggerUtil;

@WebServlet("/CartServlet")
public class CartServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerUtil.getLogger(CartServlet.class);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final CartDao cartDao = new CartDao();
    private final ProductDao productDao = new ProductDao();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            logger.warn("User not logged in");
            response.getWriter().print("{\"error\":\"Not logged in\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        try {
            List<CartItem> items = cartDao.getCartItems(userId);
            response.getWriter().print(gson.toJson(items));
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logger.error("Internal Server Error");
            response.getWriter().print("{\"error\":\"DB error\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().print("{\"error\":\"Not logged in\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        Map<Integer, Integer> updatedStocks = new HashMap<>();

        try {
            if ("add".equals(action)) {
                int productId = Integer.parseInt(request.getParameter("productId"));
                int qty = 1;
                try { qty = Integer.parseInt(request.getParameter("qty")); } catch (NumberFormatException ignored) {}

                int newStock = productDao.decrementStockAndGet(productId, qty);
                if (newStock < 0) {
                    logger.warn("Out of stock now for productId=" + productId);
                    response.getWriter().print(gson.toJson(Map.of("status", "fail", "message", "Out of stock")));
                    return;
                }

                try {
                    cartDao.addToCart(userId, productId, qty);
                    updatedStocks.put(productId, newStock);
                } catch (SQLException cartEx) {
                    productDao.incrementStockAndGet(productId, qty);
                    cartEx.printStackTrace();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().print(gson.toJson(Map.of("status", "error", "message", "Cart insert failed")));
                    return;
                }

            } else if ("update".equals(action)) {
                int productId = Integer.parseInt(request.getParameter("productId"));
                int newQty = Integer.parseInt(request.getParameter("qty"));

                List<CartItem> items = cartDao.getCartItems(userId);
                int currentQty = 0;
                for (CartItem it : items) {
                    if (it.getProductId() == productId) { currentQty = it.getQty(); break; }
                }

                if (newQty == currentQty) {
                	
                } 
                else if (newQty > currentQty) {
                    int diff = newQty - currentQty;
                    int newStock = productDao.decrementStockAndGet(productId, diff);
                    if (newStock < 0) {
                        response.getWriter().print(gson.toJson(Map.of("status", "fail", "message", "Not enough stock")));
                        return;
                    }
                    try {
                        cartDao.updateQuantity(userId, productId, newQty);
                        updatedStocks.put(productId, newStock);
                        logger.info("Cart quantity increased for productId=" + productId);
                    } catch (SQLException ex) {
                        productDao.incrementStockAndGet(productId, diff);
                        ex.printStackTrace();
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        response.getWriter().print(gson.toJson(Map.of("status", "error", "message", "Cart update failed")));
                        return;
                    }
                } else {
                    int diff = currentQty - newQty;
                    try {
                        cartDao.updateQuantity(userId, productId, newQty);
                        int newStock = productDao.incrementStockAndGet(productId, diff);
                        if (newStock >= 0) updatedStocks.put(productId, newStock);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        response.getWriter().print(gson.toJson(Map.of("status", "error", "message", "Cart update failed")));
                        return;
                    }
                }

            }
            else if ("remove".equals(action)) {
                int productId = Integer.parseInt(request.getParameter("productId"));

                List<CartItem> itemsBefore = cartDao.getCartItems(userId);
                int currentQty = 0;
                for (CartItem it : itemsBefore) {
                    if (it.getProductId() == productId) { currentQty = it.getQty(); break; }
                }

                try {
                    cartDao.removeFromCart(userId, productId);
                    if (currentQty > 0) {
                        int newStock = productDao.incrementStockAndGet(productId, currentQty);
                        if (newStock >= 0) updatedStocks.put(productId, newStock);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().print(gson.toJson(Map.of("status", "error", "message", "Remove failed")));
                    return;
                }

            } else if ("clear".equals(action)) {
                List<CartItem> itemsBefore = cartDao.getCartItems(userId);
                for (CartItem it : itemsBefore) {
                    int pid = it.getProductId();
                    int qty = it.getQty();
                    if (qty > 0) {
                        int newStock = productDao.incrementStockAndGet(pid, qty);
                        if (newStock >= 0) updatedStocks.put(pid, newStock);
                    }
                }
                try {
                    cartDao.clearCart(userId);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    logger.error("Clear cart failed");
                    response.getWriter().print(gson.toJson(Map.of("status", "error", "message", "Clear cart failed")));
                    return;
                }

            } 
            else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print(gson.toJson(Map.of("status", "error", "message", "Unknown action")));
                return;
            }

            List<CartItem> itemsAfter = cartDao.getCartItems(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ok");
            result.put("items", itemsAfter);
            if (!updatedStocks.isEmpty()) result.put("updatedStocks", updatedStocks);

            response.getWriter().print(gson.toJson(result));

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\":\"DB error\"}");
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.error("Invalid input");
            response.getWriter().print("{\"error\":\"Invalid numeric input\"}");
        }
    }
}
