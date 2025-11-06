package utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class LoggerUtil {
	
	 public static final Logger logger = LogManager.getLogger("servlets");
      
       public static Logger getLogger(Class<?> clazz) {
    	    return LogManager.getLogger(clazz);
    	  }

	    public static void info(String msg) {
	        logger.info(msg);
	    }

	    public static void debug(String msg) {
	        logger.debug(msg);
	    }

	    public static void error(String msg) {
	        logger.error(msg);
	    }

		public static void trace(String msg) {
			logger.trace(msg);
			
		}

		public static void warn(String msg) {
			logger.warn(msg);
			
		}

}
