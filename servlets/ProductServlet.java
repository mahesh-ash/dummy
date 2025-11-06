package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dao.ProductDao;
import model.Product;
import utils.LoggerUtil;

@WebServlet("/ProductServlet")
public class ProductServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerUtil.getLogger(ProductServlet.class);

    private ProductDao productDao;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        this.productDao = new ProductDao();
        this.gson = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .create();
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        String productIdParam = request.getParameter("productId");
        String categoryIdParam = request.getParameter("category_id");
        String query = request.getParameter("query");
        String filter = request.getParameter("filter");

        try {
            if (productIdParam != null && !productIdParam.isEmpty()) {
                int pid;
                try {
                    pid = Integer.parseInt(productIdParam);
                } catch (NumberFormatException nfe) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Invalid productId\"}");
                    return;
                }

                Product prod = productDao.getProductById(pid);
                if (prod == null) {
                    out.print("{}");
                    return;
                }

                Map<String, Object> map = productToMap(prod);

                try {
                    List<String> imgs = productDao.getImagesForProduct(pid);
                    if (imgs != null && !imgs.isEmpty()) {
                        map.put("images", imgs);
                    }
                } catch (Exception t) {
                    logger.debug("getImagesForProduct failed", t);
                }

                try {
                    double disc = productDao.getActiveDiscountPercent(pid);
                    if (disc > 0.0) {
                        map.put("discountPercent", disc);
                        Double price = toDoubleSafe(map.get("price"));
                        if (price != null) {
                            double dp = Math.round((price - (price * disc / 100.0)) * 100.0) / 100.0;
                            map.put("discountedPrice", dp);
                        }
                    } else {
                        map.put("discountPercent", 0.0);
                    }
                } catch (Exception t) {
                    logger.debug("getActiveDiscountPercent failed", t);
                }

                out.print(gson.toJson(map));
                return;
            }

            Integer categoryId = null;
            if (categoryIdParam != null && !categoryIdParam.isEmpty()) {
                try {
                    categoryId = Integer.parseInt(categoryIdParam);
                } catch (NumberFormatException e) {
                    logger.error("Invalid categoryId param", e);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Invalid category_id\"}");
                    return;
                }
            }

            if (query != null) {
                query = query.trim().replaceAll("[^a-zA-Z0-9 ]", "");
            }

            List<Product> products = productDao.getFilteredProducts(categoryId, query, filter);
            List<Map<String, Object>> outList = new ArrayList<>(products.size());

            for (Product p : products) {
                Map<String, Object> map = productToMap(p);
                final Integer pid = (Integer) map.get("productId");

                try {
                    List<String> imgs = productDao.getImagesForProduct(pid);
                    if (imgs != null && !imgs.isEmpty()) {
                        map.put("images", imgs);
                    }
                } catch (Exception t) {
                    logger.debug("getImagesForProduct failed for pid=" + pid, t);
                }

                try {
                    double disc = productDao.getActiveDiscountPercent(pid);
                    if (disc > 0.0) {
                        map.put("discountPercent", disc);
                        Double price = toDoubleSafe(map.get("price"));
                        if (price != null) {
                            double dp = Math.round((price - (price * disc / 100.0)) * 100.0) / 100.0;
                            map.put("discountedPrice", dp);
                        }
                    } else {
                        map.put("discountPercent", 0.0);
                    }
                } catch (Exception t) {
                    logger.debug("getActiveDiscountPercent failed for pid=" + pid, t);
                }

                outList.add(map);
            }

            out.print(gson.toJson(outList));
        } catch (Exception ex) {
            logger.error("ProductServlet error", ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Internal server error\"}");
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private Map<String, Object> productToMap(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            m.put("productId", safeGetInt(p, "getProductId", "getProduct_id"));
            m.put("categoryId", safeGetInt(p, "getCategoryId", "getCategory_id", "getCatId"));
            m.put("productName", safeGetString(p, "getProductName", "getProduct_name", "getName"));
            m.put("description", safeGetString(p, "getDescription", "getDesc"));
            m.put("price", safeGetDouble(p, "getPrice", "getProductPrice"));
            m.put("stock", safeGetIntOrNull(p, "getStock", "getQty"));

            // image handling: prefer getImageBase64(), then getImageData() byte[], then legacy string getters
            String imageBase64 = getImageBase64FromProduct(p);
            if (imageBase64 != null) {
                m.put("image", imageBase64);
            } else {
                String image = safeGetString(p, "getImageUrl", "getImage", "getImg");
                if (image != null) {
                    m.put("image", image);
                }
            }
        } catch (Exception e) {
            logger.debug("productToMap reflection issue", e);
        }
        return m;
    }

    private String getImageBase64FromProduct(Product p) {
        try {
            Method m = p.getClass().getMethod("getImageBase64");
            Object v = m.invoke(p);
            if (v instanceof String) {
                String s = (String) v;
                if (!s.isEmpty()) return s;
            }
        } catch (NoSuchMethodException ignored) {}
        catch (Exception ex) { logger.debug("getImageBase64 invoke failed", ex); }

        try {
            Method m = p.getClass().getMethod("getImageData");
            Object v = m.invoke(p);
            if (v instanceof byte[]) {
                byte[] b = (byte[]) v;
                if (b != null && b.length > 0) {
                    return Base64.getEncoder().encodeToString(b);
                }
            }
        } catch (NoSuchMethodException ignored) {}
        catch (Exception ex) { logger.debug("getImageData invoke failed", ex); }

        return null;
    }

    private Integer safeGetInt(Product p, String... methods) {
        for (String m : methods) {
            try {
                Method method = p.getClass().getMethod(m);
                Object v = method.invoke(p);
                if (v instanceof Number) return ((Number) v).intValue();
                if (v instanceof String) {
                    try { return Integer.parseInt((String) v); } catch (NumberFormatException ignored) {}
                }
            } catch (NoSuchMethodException ns) { continue; }
            catch (Exception ex) { logger.debug("safeGetInt invoke failed for " + m, ex); }
        }
        return null;
    }

    private Integer safeGetIntOrNull(Product p, String... methods) {
        return safeGetInt(p, methods);
    }

    private Double safeGetDouble(Product p, String... methods) {
        for (String m : methods) {
            try {
                Method method = p.getClass().getMethod(m);
                Object v = method.invoke(p);
                if (v instanceof Number) return ((Number) v).doubleValue();
                if (v instanceof String) {
                    try { return Double.parseDouble((String) v); } catch (NumberFormatException ignored) {}
                }
            } catch (NoSuchMethodException ns) { continue; }
            catch (Exception ex) { logger.debug("safeGetDouble invoke failed for " + m, ex); }
        }
        return null;
    }

    private String safeGetString(Product p, String... methods) {
        for (String m : methods) {
            try {
                Method method = p.getClass().getMethod(m);
                Object v = method.invoke(p);
                if (v != null) return String.valueOf(v);
            } catch (NoSuchMethodException ns) { continue; }
            catch (Exception ex) { logger.debug("safeGetString invoke failed for " + m, ex); }
        }
        return null;
    }

    private Double toDoubleSafe(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return null; }
    }
}
