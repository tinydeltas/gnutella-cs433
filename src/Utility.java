/* Utility
* Provides useful static methods
*/


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;


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

	public static void writeNByteInt(ByteArrayOutputStream byteStream, int val, int n) {
		byte[] byteArray = (BigInteger.valueOf(val)).toByteArray();
		int paddingLength = n - byteArray.length;
		for(int i = 0; i < paddingLength; i++) {
			byteStream.write(0);
		}
		byteStream.write(byteArray, 0, Math.min(byteArray.length, n));
	}

	public static int readNByteInt(ByteArrayInputStream byteStream, int n) {
		byte[] lengthByteArray = new byte[n];
		if(byteStream.read(lengthByteArray, 0, n) != n) {
			return -1;
		}
		return (new BigInteger(lengthByteArray)).intValue();
	}

// --Commented out by Inspection START (5/7/16, 12:36 AM):
//	public static int byteArrayToInt(byte[] bytes) {
//		return ByteBuffer.wrap(bytes).getInt();
//	}
// --Commented out by Inspection STOP (5/7/16, 12:36 AM)

}
