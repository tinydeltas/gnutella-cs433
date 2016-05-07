import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.UUID;

//Same as the Utility code from HW4 for Fishnet

/**
 * <pre>
 * Provides some useful static methods
 * </pre>
 */
class Utility {

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

	public static UUID byteArrayToUUID(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		long firstLong = bb.getLong();
		long secondLong = bb.getLong();
		return new UUID(firstLong, secondLong);
	}

	public static byte[] uuidToByteArray(UUID uuid) {
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}

	public static boolean fileExists(String file) {
		File f = new File(file);
		return f.exists() && !f.isDirectory();
	}

// --Commented out by Inspection START (5/7/16, 12:36 AM):
//	public static int byteArrayToInt(byte[] bytes) {
//		return ByteBuffer.wrap(bytes).getInt();
//	}
// --Commented out by Inspection STOP (5/7/16, 12:36 AM)

}
