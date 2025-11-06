package servlets;

import utils.DBUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ImageServlet - tailored to the schema you provided.
 *
 * Usage examples:
 *  - /Ecommerce_Website/ImageServlet?productId=123           -> serves primary image or blob/text image from M_S_DATAS
 *  - /Ecommerce_Website/ImageServlet?productId=123&all=1     -> returns first image (primary) — you can extend to return gallery JSON
 *  - /Ecommerce_Website/ImageServlet?imgId=456               -> serve from M_S_PRODUCT_IMAGES by id
 *  - /Ecommerce_Website/ImageServlet?path=Assets/foo.jpg     -> serve local file under webapp or real path
 *  - /Ecommerce_Website/ImageServlet?url=https://...         -> proxy remote image (caution: security)
 *
 * Notes:
 *  - The servlet expects these tables (from your DDL): Ecommerce_Website.M_S_DATAS (image column or BLOB)
 *    and Ecommerce_Website.M_S_PRODUCT_IMAGES (product_id, image_path, is_primary).
 *  - If your actual column names differ, update the SQL strings below accordingly.
 */
@WebServlet("/ImageServlet")
public class ImageServlet extends HttpServlet {
    private static final int BUFFER_SIZE = 8 * 1024;
    private static final long CACHE_SECONDS = 60 * 60 * 24; // 1 day cache

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String imgId = trim(req.getParameter("imgId"));
        String productId = trim(req.getParameter("productId"));
        String pathParam = trim(req.getParameter("path"));
        String urlParam = trim(req.getParameter("url"));

        // 1) If explicit imgId was provided - try product images table
        if (imgId != null) {
            if (serveProductImageById(imgId, resp)) return;
        }

        // 2) If productId provided - try product images table (preferred) then fallback to M_S_DATAS.image
        if (productId != null) {
            if (servePrimaryImageForProduct(productId, resp)) return;
            if (serveImageFromProductRow(productId, resp)) return;
        }

        // 3) explicit path param (local resource or filesystem)
        if (pathParam != null) {
            if (serveImageByPath(pathParam, req, resp)) return;
        }

        // 4) proxy remote URL
        if (urlParam != null) {
            if (serveRemoteUrl(urlParam, resp)) return;
        }

        // not found -> return 404 with placeholder (try to serve webapp Asset placeholder if present)
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType("image/png");
        try (InputStream is = getServletContext().getResourceAsStream("/Ecommerce_Website/Assets/placeholder.png")) {
            if (is != null) {
                copyStream(is, resp.getOutputStream());
            } else {
                // return empty 1x1 transparent PNG (minimal fallback)
                resp.getOutputStream().write(new byte[] {});
            }
        } catch (Exception ignored) {}
    }

    // --- Helpers ---

    private boolean serveProductImageById(String imgId, HttpServletResponse resp) {
        String sql = "SELECT image_path FROM Ecommerce_Website.M_S_PRODUCT_IMAGES WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, imgId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String imagePath = rs.getString("image_path");
                    if (imagePath != null && !imagePath.trim().isEmpty()) {
                        return serveImageString(imagePath.trim(), resp);
                    }
                }
            }
        } catch (SQLException | IOException e) {
            log("serveProductImageById error", e);
        }
        return false;
    }

    private boolean servePrimaryImageForProduct(String productId, HttpServletResponse resp) {
        // Query product images table for primary image
        String sql = "SELECT TOP 1 image_path FROM Ecommerce_Website.M_S_PRODUCT_IMAGES WHERE product_id = ? AND is_primary = 1 ORDER BY id";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String path = rs.getString("image_path");
                    if (path != null && !path.trim().isEmpty()) {
                        return serveImageString(path.trim(), resp);
                    }
                }
            }
        } catch (SQLException | IOException e) {
            log("servePrimaryImageForProduct error", e);
        }
        // if not found, return false to allow fallback
        return false;
    }

    private boolean serveImageFromProductRow(String productId, HttpServletResponse resp) {
        String sqlBlob = "SELECT image FROM Ecommerce_Website.M_S_DATAS WHERE product_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlBlob)) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    InputStream bin = rs.getBinaryStream("image");
                    if (bin != null) {
                        // Wrap in BufferedInputStream for mark/reset detection
                        BufferedInputStream bis = new BufferedInputStream(bin);
                        String ct = detectContentTypeFromStream(bis, "image/jpeg");
                        writeImageStream(resp, bis, ct);
                        return true;
                    }

                    // fallback: textual path / URL / data URI
                    String text = rs.getString("image");
                    if (text != null && !text.trim().isEmpty()) {
                        return serveImageString(text.trim(), resp);
                    }
                }
            }
        } catch (SQLException | IOException e) {
            log("serveImageFromProductRow error", e);
        }
        return false;
    }


    private boolean serveImageString(String val, HttpServletResponse resp) throws IOException {
        if (val.startsWith("data:")) {
            return serveDataUri(val, resp);
        }
        if (val.startsWith("http://") || val.startsWith("https://")) {
            return serveRemoteUrl(val, resp);
        }
        // treat it as a path relative to webapp or absolute path
        // normalize: if it doesn't start with '/', try under /Ecommerce_Website/
        String path = val.startsWith("/") ? val : ("/Ecommerce_Website/" + val.replaceAll("^/+", ""));
        return serveImageByPath(path, null, resp);
    }

    private boolean serveImageByPath(String pathParam, HttpServletRequest reqOrNull, HttpServletResponse resp) {
        try {
            String path = pathParam;
            if (!path.startsWith("/")) path = "/" + path;

            // Try servlet context resource (inside .war)
            InputStream is = getServletContext().getResourceAsStream(path);
            if (is != null) {
                String mime = getServletContext().getMimeType(path);
                if (mime == null) mime = "application/octet-stream";
                writeImageStream(resp, is, mime);
                return true;
            }

            // Try file system absolute path
            File f = new File(path);
            if (!f.exists()) {
                // try real path relative to webapp
                String real = getServletContext().getRealPath(path);
                if (real != null) f = new File(real);
            }
            if (f.exists() && f.isFile()) {
                String mime = getServletContext().getMimeType(f.getName());
                if (mime == null) mime = "application/octet-stream";
                try (InputStream fis = new FileInputStream(f)) {
                    writeImageStream(resp, fis, mime);
                    return true;
                }
            }

            // Additional attempt: sometimes DB paths include spaces or different casing -> try decode trick
            String altReal = getServletContext().getRealPath(path.replaceAll(" ", "%20"));
            if (altReal != null) {
                File ff = new File(altReal);
                if (ff.exists() && ff.isFile()) {
                    String mime = getServletContext().getMimeType(ff.getName());
                    if (mime == null) mime = "application/octet-stream";
                    try (InputStream fis = new FileInputStream(ff)) {
                        writeImageStream(resp, fis, mime);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log("serveImageByPath error", e);
        }
        return false;
    }

    private boolean serveRemoteUrl(String remoteUrl, HttpServletResponse resp) {
        // Proxy remote image (be cautious: may be abused). You can restrict allowed hosts here.
        HttpURLConnection con = null;
        try {
            URL u = new URL(remoteUrl);
            con = (HttpURLConnection) u.openConnection();
            con.setConnectTimeout(5000);
            con.setReadTimeout(10000);
            con.setInstanceFollowRedirects(true);
            int code = con.getResponseCode();
            if (code >= 200 && code < 300) {
                String ct = con.getContentType();
                if (ct == null) ct = "application/octet-stream";
                try (InputStream is = con.getInputStream()) {
                    writeImageStream(resp, is, ct);
                    return true;
                }
            }
        } catch (Exception e) {
            log("serveRemoteUrl error for " + remoteUrl, e);
        } finally {
            if (con != null) con.disconnect();
        }
        return false;
    }

    private boolean serveDataUri(String dataUri, HttpServletResponse resp) {
        try {
            int comma = dataUri.indexOf(',');
            if (comma < 0) return false;
            String meta = dataUri.substring(5, comma); // skip "data:"
            String dataPart = dataUri.substring(comma + 1);
            boolean isBase64 = meta.contains(";base64");
            String mime = meta.split(";")[0];
            if (mime == null || mime.trim().isEmpty()) mime = "application/octet-stream";
            byte[] bytes;
            if (isBase64) {
                bytes = java.util.Base64.getDecoder().decode(dataPart);
            } else {
                bytes = dataPart.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                writeImageStream(resp, is, mime);
                return true;
            }
        } catch (Exception e) {
            log("serveDataUri error", e);
        }
        return false;
    }

    private void writeImageStream(HttpServletResponse resp, InputStream in, String contentType) throws IOException {
        if (contentType == null || contentType.trim().isEmpty()) contentType = "application/octet-stream";
        resp.setContentType(contentType);
        resp.setHeader("Cache-Control", "public, max-age=" + CACHE_SECONDS);
        resp.setDateHeader("Expires", System.currentTimeMillis() + (CACHE_SECONDS * 1000));
        try (BufferedInputStream bis = new BufferedInputStream(in);
             BufferedOutputStream bos = new BufferedOutputStream(resp.getOutputStream())) {
            copyStream(bis, bos);
            bos.flush();
        }
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        int r;
        while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
    }

    private String detectContentTypeFromStream(InputStream in, String fallback) {
        try {
            if (!in.markSupported()) in = new BufferedInputStream(in);
            in.mark(16);
            int b1 = in.read();
            int b2 = in.read();
            in.reset();
            if (b1 == 0xFF && b2 == 0xD8) return "image/jpeg";
            if (b1 == 0x89 && b2 == 0x50) return "image/png";
            if (b1 == 'G' && b2 == 'I') return "image/gif";
        } catch (Exception ignored) {}
        return fallback;
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}
