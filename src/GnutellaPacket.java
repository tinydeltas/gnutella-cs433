import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <pre>
 * Packet defines the Fishnet packet headers and some constants.
 * </pre>
 */
public class GnutellaPacket {
    private static final int HEADER_ID_SIZE = 16;
    private static final int HEADER_LENGTH_SIZE = 4;
    private static final int HEADER_SIZE = 5;
    public static final int MAX_PACKET_SIZE = 128;  // bytes  //may bring these back if we decide we need them
    //public static final int MAX_PAYLOAD_SIZE = MAX_PACKET_SIZE - HEADER_SIZE;  // bytes
    public static final int MAX_TTL = 15;           // max hop count

    private UUID messageID;
    private int payloadDescriptor;
    private int ttl;
    private int hops;
    private byte[] payload;

    public static final int QUERY = 128;     // 0x80 = Query (see section 3.2.3)
    public static final int HITQUERY = 129;  // 0x81 = QueryHits (see section 3.2.4)
    public static final int OBTAIN = 2;
    public static final int PUSH = 3;

    // 0x00 = Ping (see section 3.2.1)
    // 0x01 = Pong (see section 3.2.2)

    // 0x40 = Push (see section 3.2.5)

    public static final int DEF_TTL = 3;
    public static final int DEF_HOPS = 3;


    public GnutellaPacket(UUID messageID, int payloadDescriptor, int ttl, int hops, byte[] payload){
        if(!this.isValid(messageID, payloadDescriptor, ttl, hops, payload)){
            throw new IllegalArgumentException("Arguments passed to constructor of GnutellaPacket are invalid");
        }

        this.messageID = messageID;
        this.payloadDescriptor = payloadDescriptor;
        this.payload = payload;
        this.ttl = ttl;
        this.hops = hops;

        Debug.DEBUG("Creating new packet: " + this.toString(), "GnutellaPacket constructor");
    }

    private boolean isValid(UUID messageID, int payloadDescriptor, int ttl, int hops, byte[] payload){
        //TO-DO - actually check the arguments for validity
        return true;
    }


    /**
     * Provides a string representation of the packet.
     * @return A string representation of the packet.
     */
    public String toString() {
	return "GnutellaPacket: " + this.messageID + "; " + this.payloadDescriptor + "; ttl: " + this.ttl + " hops: " + this.hops +
            " contents: " + Utility.byteArrayToString(this.payload);
    }

    /**
     * @return The address of the destination node
     */
    public UUID getMessageID() {
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

        // write 16 bytes for message ID
        byte[] seqByteArray = Utility.uuidToByteArray(this.messageID);
        int paddingLength = HEADER_ID_SIZE - seqByteArray.length;
        for(int i = 0; i < paddingLength ; i++) {
            byteStream.write(0);
        }
        byteStream.write(seqByteArray, 0, Math.min(seqByteArray.length, 16));

    	byteStream.write(this.payloadDescriptor);
    	byteStream.write(this.ttl);
    	byteStream.write(this.hops);

        Debug.DEBUG("Message ID: " + this.messageID, "pack");
        Debug.DEBUG("Descriptor: " + this.payloadDescriptor, "pack");
        Debug.DEBUG("TTL: " + this.ttl, "pack");
        Debug.DEBUG("Hops: " + this.hops, "pack");
        Debug.DEBUG("Payload: " + Utility.byteArrayToString(this.payload), "pack");
        Debug.DEBUG("Payload length: " + this.payload.length + "+" + HEADER_SIZE, "pack");

        int length = this.payload.length + HEADER_SIZE;
        byte[] lengthByteArray = (BigInteger.valueOf(length)).toByteArray();
        paddingLength = HEADER_LENGTH_SIZE - lengthByteArray.length;
        Debug.DEBUG("Padding length: " + paddingLength, "pack");
        for(int i = 0; i < paddingLength; i++) {
            byteStream.write(0);
        }

        byteStream.write(lengthByteArray, 0, Math.min(lengthByteArray.length, 4));
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
        Debug.DEBUG("Unpacking packet ", "unpack");

        ByteArrayInputStream byteStream = new ByteArrayInputStream(packedPacket);

        byte[] seqByteArray = new byte[16];
        if(byteStream.read(seqByteArray, 0, 16) != 16) {
            return null;
        }
        UUID messageID = Utility.byteArrayToUUID(seqByteArray);

    	int payloadDescriptor = byteStream.read();
    	int ttl = byteStream.read();
    	int hops = byteStream.read();

        byte[] lengthByteArray = new byte[4];
    	if(byteStream.read(lengthByteArray, 0, 4) != 4) {
    	    return null;
    	}
    	int packetLength = (new BigInteger(lengthByteArray)).intValue();

        Debug.DEBUG("ID: " + messageID + "descriptor: " + payloadDescriptor +
        " ttl: " + ttl + " hops: " + hops + "length: " + packetLength, "unpack");

    	byte[] payload = new byte[packetLength - HEADER_SIZE];
    	byteStream.read(payload, 0, payload.length);

    	if((HEADER_SIZE + payload.length) != packetLength) {
            Debug.DEBUG("Error, wrong size. Header size: " + HEADER_SIZE +
            " + payload length: " + payload.length + " != packet length: " +
            packetLength, "unpack");
    	    return null;
    	}

    	try {
    	    return new GnutellaPacket(messageID, payloadDescriptor, ttl, hops, payload);
    	}catch(IllegalArgumentException e) {
            e.printStackTrace();
    	}
    	return null;
    }

}
