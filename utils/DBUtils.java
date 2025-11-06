package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import servlets.AdminServlet;

public class DBUtils {
	
	private static final Logger logger =LoggerUtil.getLogger(DBUtils.class);
	 public DBUtils() {
		 
	 }
	 
	 public static void main(String []args) {
		 getConnection();
	 }

     public static Connection getConnection(){
     Connection connection=null;
     try {
//            String url=UtilityClass.getProperty("db.url");
//
//            String username=UtilityClass.getProperty("db.username");
//
//            String password=UtilityClass.getProperty("db.password");
//
//            String driver=UtilityClass.getProperty("db.driver");
//         
//            if (url == null || username == null || password == null || driver == null) {
//	             throw new SQLException("Database connection properties are missing.");
//            }
//          
    	 String url="jdbc:sqlserver://localhost:1433;database=SqlTraining";
    	 String username="sa";
    		 
         String password="Root@123";
            
            
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            
           connection=DriverManager.getConnection(url,username,password);
           logger.info("Connection Executed Successfully");
    }catch(Exception e){
       e.printStackTrace();
}
     return connection;
}
	}
