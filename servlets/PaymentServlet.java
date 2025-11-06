package servlets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.CartDao;
import dao.CouponDao;
import dao.OrderDao;
import model.CartItem;
import model.DiscountResult;
import model.Paymentrequest;
import model.User;
import utils.DBUtils;
import utils.LoggerUtil;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet("/PaymentServlet")
public class PaymentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();
    private final CartDao cartDao = new CartDao();
    private final CouponDao couponDao = new CouponDao();
    private static final Logger logger = LoggerUtil.getLogger(PaymentServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JsonObject out = new JsonObject();
        HttpSession session = request.getSession(false);
        int sessionUserId = -1;
        if (session != null) {
            Object userObj = session.getAttribute("user");
            if (userObj instanceof User) sessionUserId = ((User) userObj).getId();
        }
        if ("listCoupons".equalsIgnoreCase(action)) {
            try {
                List<Map<String, Object>> coupons = couponDao.listActiveCoupons(sessionUserId);
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                for (Map<String, Object> c : coupons) {
                    com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                    o.addProperty("couponId", (Integer) c.get("couponId"));
                    o.addProperty("code", (String) c.get("code"));
                    o.addProperty("label", (String) c.get("label"));
                    o.addProperty("minAmount", ((Number) c.get("minAmount")).doubleValue());
                    o.addProperty("newUserOnly", (Boolean) c.get("newUserOnly"));
                    o.addProperty("applicable", (Boolean) c.get("applicable"));
                    arr.add(o);
                }
                out.add("coupons", arr);
                out.addProperty("status", "ok");
                response.setStatus(javax.servlet.http.HttpServletResponse.SC_OK);
                response.getWriter().write(out.toString());
                return;
            } catch (Exception e) {
                logger.error("Could not list coupons", e);
                sendError(response, out, "Could not list coupons", javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }
        sendError(response, out, "Invalid GET action", javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
    }

    @Override
    protected void doPost(HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JsonObject out = new JsonObject();
        HttpSession session = request.getSession(false);
        if (session == null) {
            sendError(response, out, "Not logged in", javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof User)) {
            sendError(response, out, "User not logged in or session expired", javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        User user = (User) userObj;
        int userId = user.getId();
        if (userId <= 0) { 
        	sendError(response, out, "Could not determine user id", javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        	return;
        	}

        if ("validateCoupon".equalsIgnoreCase(action)) {
            try (BufferedReader br = request.getReader(); Connection conn = DBUtils.getConnection()) {
                com.google.gson.JsonObject payload = gson.fromJson(br, com.google.gson.JsonObject.class);
                String code = payload.has("code") && !payload.get("code").isJsonNull() ? payload.get("code").getAsString() : null;
                double amount = payload.has("amount") && !payload.get("amount").isJsonNull() ? payload.get("amount").getAsDouble() : 0.0;
                DiscountResult dr = couponDao.validateAndComputeDiscount(conn, code, amount, userId);
                if (!dr.isValid()) {
                    out.addProperty("status", "error");
                    out.addProperty("valid", false);
                    out.addProperty("message", dr.getMessage());
                    response.setStatus(javax.servlet.http.HttpServletResponse.SC_OK);
                    response.getWriter().write(out.toString());
                    return;
                }
                out.addProperty("status", "ok");
                out.addProperty("valid", true);
                out.addProperty("discountAmount", dr.getDiscountAmount());
                out.addProperty("newAmount", dr.getNewAmount());
                response.setStatus(javax.servlet.http.HttpServletResponse.SC_OK);
                response.getWriter().write(out.toString());
                return;
            } catch (Exception e) {
                logger.error("Coupon validation failed", e);
                sendError(response, out, "Coupon validation failed", javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }

        Paymentrequest payReq;
        try (BufferedReader br = request.getReader()) {
            payReq = gson.fromJson(br, Paymentrequest.class);
        } catch (Exception e) {
            sendError(response, out, "Invalid JSON payload", javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (payReq == null || payReq.getAmount() == null || payReq.getSelectedItems() == null || payReq.getSelectedItems().isEmpty()) {
            sendError(response, out, "Invalid payload", javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        List<Integer> selectedProductIds = payReq.getSelectedItems();
        List<CartItem> itemsInCart;
        try {
            itemsInCart = cartDao.getCartItems(userId);
        } catch (SQLException e) {
            sendError(response, out, "Database error: " + e.getMessage(), javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        List<CartItem> checkoutItems = itemsInCart.stream()
                .filter(item -> selectedProductIds.contains(item.getProductId()))
                .collect(Collectors.toList());

        if (checkoutItems.isEmpty()) {
            sendError(response, out, "No items selected for checkout", javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Connection conn = null;
        try {
            conn = DBUtils.getConnection();
            conn.setAutoCommit(false);

            double amountRequested = payReq.getAmount();
            double originalAmount = payReq.getOriginalAmount() != null ? payReq.getOriginalAmount() : amountRequested;
            String couponCode = payReq.getCouponCode();

            DiscountResult dr = null;
            if (couponCode != null && !couponCode.trim().isEmpty()) {
                dr = couponDao.validateAndComputeDiscount(conn, couponCode, originalAmount, userId);
                if (!dr.isValid()) {
                    try { 
                    	conn.rollback(); 
                    	} catch (SQLException ex) { 
                    		logger.warn("Rollback failed", ex); 
                    		}
                    sendError(response, out, "Coupon invalid: " + dr.getMessage(), javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                amountRequested = dr.getNewAmount();
            }

            int orderId = OrderDao.createOrder(conn, userId, amountRequested);
            OrderDao.createOrderLogs(conn, orderId, checkoutItems);
            OrderDao.clearSelectedCartItems(conn, userId, selectedProductIds);

            if (dr != null && dr.isValid()) {
                couponDao.recordCouponUsage(conn, dr.getCouponId(), userId, orderId, originalAmount, dr.getDiscountAmount());
            }

            conn.commit();
            out.addProperty("status", "ok");
            out.addProperty("orderId", orderId);
            out.addProperty("message", "Payment simulated and order placed for selected items.");
            response.setStatus(javax.servlet.http.HttpServletResponse.SC_OK);
            response.getWriter().write(out.toString());
        } catch (SQLException e) {
            logger.error("Transaction failed", e);
            if (conn != null) try { 
            	conn.rollback(); 
            	} catch (SQLException ex) {
            		logger.error("Rollback failed", ex);
            		}
            sendError(response, out, "Payment failed: " + e.getMessage(), javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (conn != null) try { 
            	conn.close();
            	} catch (SQLException e) {
            		logger.error("Connection close failed", e);
            		}
        }
    }

    private void sendError(javax.servlet.http.HttpServletResponse response, JsonObject jsonObject, String message, int statusCode) throws IOException {
        jsonObject.addProperty("status", "error");
        jsonObject.addProperty("message", message);
        response.setStatus(statusCode);
        response.getWriter().write(jsonObject.toString());
    }
}
