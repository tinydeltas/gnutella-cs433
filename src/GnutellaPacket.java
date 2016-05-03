import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

/**
 * <pre>   
 * Packet defines the Fishnet packet headers and some constants.
 * </pre>   
 */
public class GnutellaPacket {

    public static final int BROADCAST_ADDRESS = 255;
    public static final int MAX_ADDRESS = 255;
    public static final int HEADER_SIZE = 5;
    public static final int MAX_PACKET_SIZE = 128;  // bytes
    public static final int MAX_PAYLOAD_SIZE = MAX_PACKET_SIZE - HEADER_SIZE;  // bytes
    public static final int MAX_TTL = 15;           // max hop count

    private int messageID;
    private int payloadDescriptor;
    private int ttl;
    private int hops;
    private byte[] payload;


    public GnutellaPacket(int messageID, int payloadDescriptor, int ttl, int hops, byte[] payload){
        if(!this.isValid(messageID, payloadDescriptor, ttl, hops, payload)){
            throw new IllegalArgumentException("Arguments passed to constructor of GnutellaPacket are invalid");
        }
        this.messageID = messageID;
        this.payloadDescriptor = payloadDescriptor;
        this.payload = payload;
        this.ttl = ttl;
        this.hops = hops;
    }

    boolean isValid(int messageID, int payloadDescriptor, int ttl, int hops, byte[] payload){
        //TO-DO - actually check the arguments for validity
        return true;
    }


    /**
     * Provides a string representation of the packet.
     * @return A string representation of the packet.
     */
    public String toString() {
	return new String("GnutellaPacket: " + this.messageID + "; " + this.payloadDescriptor + "; ttl: " + this.ttl + " hops: " + this.hops + 
			 " contents: " + Utility.byteArrayToString(this.payload));
    }

    /**
     * @return The address of the destination node
     */
    public int getMessageID() {
	return this.messageID;
    }
    
    /**
     * @return The address of the src node
     */
    public int getPayloadDescriptor() {
	return this.payloadDescriptor;
    }

    /**
     * @return The TTL of the packet
     */
    public int getTTL() {
	return this.ttl;
    }

    /**
     * Sets the TTL of this packet
     * @param ttl TTL to set
     */
    public void setTTL(int ttl) {
	   this.ttl = ttl;
    }
    
    /**
     * @return The sequence number of this packet
     */
    public int getHops() {
	return this.hops;
    }


    /**
     * @return The payload of this packet
     */
    public byte[] getPayload() {
	   return this.payload;
    }

    /**
     * Convert the Packet object into a byte array for sending over the wire.
     * Format:
     *        destination address: 1 byte
     *        source address: 1 byte
     *        ttl (time to live): 1 byte
     *        protocol: 1 byte
     *        packet length: 1 byte
     *        packet sequence num: 4 bytes
     *        payload: <= MAX_PAYLOAD_SIZE bytes
     * @return A byte[] for transporting over the wire. Null if failed to pack for some reason
     */
    public byte[] pack() {	
	
    	ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    	byteStream.write(this.messageID);
    	byteStream.write(this.payloadDescriptor);
    	byteStream.write(this.ttl);
    	byteStream.write(this.hops);
    	byteStream.write(this.payload.length + HEADER_SIZE); //plus header size? CHECK

    	//byte[] seqByteArray = (BigInteger.valueOf(this.seq)).toByteArray();
    	/*int paddingLength = 4;  //4 - seqByteArray.length;
    	for(int i = 0; i < paddingLength; i++) {
    	    byteStream.write(0);
    	}*/
    	
    	byteStream.write(this.payload, 0, this.payload.length);

    	return byteStream.toByteArray();
    }

    /**
     * Unpacks a byte array to create a Packet object
     * Assumes the array has been formatted using pack method in Packet
     * @param packedPacket String representation of the packet
     * @return Packet object created or null if the byte[] representation was corrupted
     */
    public static GnutellaPacket unpack(byte[] packedPacket){
	
    	ByteArrayInputStream byteStream = new ByteArrayInputStream(packedPacket);
    	
    	int messageID = byteStream.read();
    	int payloadDescriptor = byteStream.read();
    	int ttl = byteStream.read();
    	int hops = byteStream.read();
    	int packetLength = byteStream.read();
    	
    	/*byte[] seqByteArray = new byte[4];
    	if(byteStream.read(seqByteArray, 0, 4) != 4) {
    	    return null;
    	}

    	int seq = (new BigInteger(seqByteArray)).intValue();*/

    	byte[] payload = new byte[byteStream.available()];
    	byteStream.read(payload, 0, payload.length);

    	if((HEADER_SIZE + payload.length) != packetLength) {
    	    return null;
    	}	
    	
    	try {
    	    return new GnutellaPacket(messageID, payloadDescriptor, ttl, hops, payload);
    	}catch(IllegalArgumentException e) {
    	    // will return null
    	}
    	return null;
    }
    
}
