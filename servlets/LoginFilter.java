//
//package servlets;
//
//import java.io.IOException;
//import javax.servlet.Filter;
//import javax.servlet.FilterChain;
//import javax.servlet.FilterConfig;
//import javax.servlet.ServletException;
//import javax.servlet.ServletRequest;
//import javax.servlet.ServletResponse;
//import javax.servlet.annotation.WebFilter;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.HttpSession;
//
//@WebFilter("/CartServlet")
//public class LoginFilter implements Filter {
//
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
//        throws IOException, ServletException {
//        
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//        HttpServletResponse httpResponse = (HttpServletResponse) response;
//        
//        HttpSession session = httpRequest.getSession(false); 
//        
//
//        boolean isLoggedIn = (session != null && session.getAttribute("userId") != null);
//        
//        if (isLoggedIn) {
//            System.out.println("User loged in");
//            chain.doFilter(request, response);
//        } else {
//
//            System.out.println("LoginFilter: User not logged in, redirecting to login page.");
//            httpResponse.sendRedirect(httpRequest.getContextPath() + "Html/login.html");
//            System.out.println(httpRequest.getContextPath() + "Html/login.html");
//        }
//    }
//
//    public void init(FilterConfig fConfig) throws ServletException {
//
//    }
//
//    public void destroy() {
//
//    }
//}

