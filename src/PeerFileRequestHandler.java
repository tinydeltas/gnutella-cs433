import java.io.DataOutputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PeerFileRequestHandler extends PeerHandler {

    public PeerFileRequestHandler(GnutellaThread thread, Socket socket) {
        super(thread.peer, thread.welcomeSocket, socket);
    }

    public void onPacketReceive(InetAddress from, byte[] packet) {
        GnutellaPacket pkt = GnutellaPacket.unpack(packet);
        switch (pkt.getPayloadDescriptor()) {
            case GnutellaPacket.OBTAIN:
                onFileQuery(pkt);
                break;
            default:
                System.out.println("Unrecognized descriptor");
                break;
        }
    }

    private void onFileQuery(GnutellaPacket pkt) {
        String file = Utility.byteArrayToString(pkt.getPayload());
        File f = new File(file);
        if (!f.exists() || f.isDirectory())
            return;

        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            Path path = Paths.get(parent.dirRoot + "/" + file);
            out.write(Files.readAllBytes(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

