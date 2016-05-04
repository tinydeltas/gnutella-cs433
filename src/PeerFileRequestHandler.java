import java.net.InetAddress;
import java.net.Socket;

public class PeerFileRequestHandler extends PeerHandler {

    public PeerFileRequestHandler(GnutellaThread thread, Socket socket) {
        super(thread.peer, thread.welcomeSocket, socket);
    }

    public void onPacketReceive(InetAddress from, byte[] packet) {
        GnutellaPacket pkt = GnutellaPacket.unpack(packet);
        switch (pkt.getPayloadDescriptor()) {
            case GnutellaPacket.FILE:
                onFileQuery(pkt);
                break;
            default:
                System.out.println("Unrecognized descriptor");
                break;
        }
    }

    void onFileQuery(GnutellaPacket pkt) {
        // search through local query
        // send to TCP connection
    }
}

