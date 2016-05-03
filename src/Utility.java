import java.io.UnsupportedEncodingException;

//Same as the Utility code from HW4 for Fishnet

/**
 * <pre>   
 * Provides some useful static methods
 * </pre>   
 */
public class Utility {
    
    private static final String CHARSET = "US-ASCII";

    public static byte[] stringToByteArray(String msg) {
    	try {
    	    return msg.getBytes(CHARSET);	
    	}catch(UnsupportedEncodingException e) {
    	    System.err.println("Exception occured while converting string to byte array. String: " + msg + " Exception: " + e);
    	}
    	return null;
    }
    
    public static String byteArrayToString(byte[] msg) {
    	try {
    	    return new String(msg, CHARSET);
    	}catch(UnsupportedEncodingException e) {
    	    System.err.println("Exception occured while converting byte array to string. Exception: " + e);
    	}
    	return null;
    }

}
