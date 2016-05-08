import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.UUID;

public class PushMessage {
    private UUID identifier;
    private int fileIndex;
    private InetAddress addr;
    private int port;

//    private static final int HEADER_SIZE = 26;
    private static final int HEADER_ID_SIZE = 16;

    public PushMessage(UUID identifier, int fileIndex, InetAddress addr, int port) {
        if (!this.isValid(identifier, fileIndex, addr, port))
            throw new IllegalArgumentException("Invalid arguments");

        this.identifier = identifier;
        this.fileIndex = fileIndex;
        this.addr = addr;
        this.port = port;
    }

    public UUID getIdentifier() {
        return this.identifier;
    }

    public int getFileIndex() {
        return this.fileIndex;
    }

    public InetAddress getAddr() {
        return this.addr;
    }

    public int getPort() {
        return this.port;
    }

    boolean isValid(UUID identifier, int fileIndex, InetAddress addr, int port) {
        if (port > 65535 || port < 0 || addr == null || fileIndex < 0)
            return false;
        return true;
    }

    public byte[] pack() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        // write 16 bytes for message ID
        byte[] seqByteArray = Utility.uuidToByteArray(this.identifier);
        int paddingLength = HEADER_ID_SIZE - seqByteArray.length;
        for(int i = 0; i < paddingLength ; i++) {
            byteStream.write(0);
        }
        byteStream.write(seqByteArray, 0, Math.min(seqByteArray.length, 16));

        byteStream.write(fileIndex);
        try {
            byteStream.write(addr.getAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }

        byteStream.write(port);
        return byteStream.toByteArray();

    }

    public static PushMessage unpack(byte[] packedPacket) {
        Debug.DEBUG("Unpacking packet ", "unpack");

        ByteArrayInputStream byteStream = new ByteArrayInputStream(packedPacket);
        byte[] seqByteArray = new byte[HEADER_ID_SIZE];
        if(byteStream.read(seqByteArray, 0, 16) != 16) {
            return null;
        }
        UUID identifier = Utility.byteArrayToUUID(seqByteArray);

        int fileIndex = byteStream.read();

        byte[] ip = new byte[4];
        byteStream.read(ip, 0, 4);
        int port = byteStream.read();

        InetAddress addr = null;
        try {
            addr = InetAddress.getByAddress(ip);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new PushMessage(identifier, fileIndex, addr, port);
    }

    public String toString() {
        return "[PushMessage]" + identifier + " FILEINDEX:" + fileIndex + " ADDR:" + addr + " PORT" + port;
    }
}
