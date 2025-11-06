package servlets;

import dao.CategoryDao;
import model.Category;
import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/CategoryServlet")
public class CategoryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final CategoryDao dao = new CategoryDao();
    private final Gson gson = new Gson();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Category> categories = dao.getAllCategories();
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(gson.toJson(categories));
    }
}
